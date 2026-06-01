package com.cms.service.impl;

import com.cms.config.TenantContext;
import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatGroupAccess;
import com.cms.dto.DtoChatGroupCreate;
import com.cms.dto.DtoChatMember;
import com.cms.entity.*;
import com.cms.mapper.ChatMapper;
import com.cms.repository.ChatGroupMemberRepository;
import com.cms.repository.ChatGroupRepository;
import com.cms.service.IChatGroupService;
import com.cms.entity.User;
import com.cms.exception.ForbiddenException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.ValidationException;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatGroupService implements IChatGroupService {

  private final ChatGroupRepository groupRepository;
  private final ChatGroupMemberRepository memberRepository;
  private final UserRepository userRepository;
  private final ChatMapper chatMapper;
  private final ChatGroupTenantRouter tenantRouter;
  private final ObjectProvider<ChatGroupService> selfProvider;

  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  @Override
  @Transactional
  public DtoChatGroup createGroup(DtoChatGroupCreate dto, Long creatorId) {
    String tenantId = normalizeTenantId(dto.getTenantId());

    // Visitor erişimi yalnızca TC (tenant_id dolu) için anlamlıdır
    if (Boolean.TRUE.equals(dto.getVisitorAccess()) && tenantId == null) {
      throw new ValidationException(
          "visitorAccess=true sadece tenant chat (TC) için geçerlidir — tenantId şart");
    }

    // TC oluşturma yetkisi: yalnızca ADMIN (3) ve üzeri.
    // AC için (tenantId null) mevcut chat:read yetkisi kafidir.
    if (tenantId != null && currentUserRoleLevel() < 3) {
      throw new ForbiddenException(
          "Tenant chat (TC) oluşturmak için ADMIN veya SUPER_ADMIN olmalısın");
    }

    ChatGroup group = new ChatGroup();
    group.setName(dto.getName());
    group.setDescription(dto.getDescription());
    group.setType(ChatGroupType.GROUP);
    group.setCreatedBy(creatorId);
    group.setVisibilityLevel(currentUserRoleLevel());
    group.setTenantId(tenantId);
    group.setVisitorAccess(Boolean.TRUE.equals(dto.getVisitorAccess()));
    group = groupRepository.save(group);

    addMemberInternal(group.getId(), creatorId, ChatMemberRole.OWNER);

    if (dto.getMemberIds() != null) {
      for (Long memberId : dto.getMemberIds()) {
        if (!memberId.equals(creatorId)) {
          addMemberInternal(group.getId(), memberId, ChatMemberRole.MEMBER);
        }
      }
    }

    return chatMapper.toGroupDto(group);
  }

  @Override
  @Transactional
  public DtoChatGroup getOrCreateDm(Long currentUserId, Long targetUserId) {
    return groupRepository.findDmBetween(currentUserId, targetUserId, ChatGroupType.DM)
        .map(chatMapper::toGroupDto)
        .orElseGet(() -> {
          ChatGroup dm = new ChatGroup();
          dm.setType(ChatGroupType.DM);
          dm.setCreatedBy(currentUserId);
          dm.setVisibilityLevel(4); // DMs are always private
          dm = groupRepository.save(dm);
          addMemberInternal(dm.getId(), currentUserId, ChatMemberRole.OWNER);
          addMemberInternal(dm.getId(), targetUserId, ChatMemberRole.MEMBER);
          return chatMapper.toGroupDto(dm);
        });
  }

  @Override
  public List<DtoChatGroup> getMyGroups(Long userId) {
    int roleLevel = currentUserRoleLevel();
    // X-Tenant-Id (TenantContext) modeli: JwtTenantFilter chat'i X-Tenant-Id'ye gore
    // yonlendirir — header yoksa basedb (AC), header varsa o tenant (TC). Burada yalnizca
    // MEVCUT context'in gruplari dondurulur. Cross-DB aggregate (mapAllTenants) YOK; bu hem
    // OSIV ile uyumludur hem de filter/prompt/panel ile ayni X-Tenant-Id modeline oturur.
    return groupRepository.findGroupsByUserIdAndRole(userId, roleLevel)
        .stream()
        .map(chatMapper::toGroupDto)
        .sorted(Comparator.comparing(DtoChatGroup::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  @Override
  public DtoChatGroup getGroupById(UUID groupId, Long requesterId) {
    return selfProvider.getObject().getGroupByIdInTenant(groupId, requesterId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public DtoChatGroup getGroupByIdInTenant(UUID groupId, Long requesterId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    tenantRouter.useGroupDatabase(group);
    checkReadAccess(group, groupId, requesterId);
    return chatMapper.toGroupDto(group);
  }

  @Override
  @Transactional
  public DtoChatMember addMember(UUID groupId, Long targetUserId, Long requesterId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    return selfProvider.getObject().addMemberInTenant(group, groupId, targetUserId, requesterId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public DtoChatMember addMemberInTenant(
      ChatGroup group, UUID groupId, Long targetUserId, Long requesterId) {
    tenantRouter.useGroupDatabase(group);
    checkWriteAccess(group, groupId, requesterId);

    // Invite hierarchy: requester can only invite users with a lower role level.
    // SUPER_ADMIN (level 4) is exempt and may invite anyone.
    int requesterLevel = currentUserRoleLevel();
    int targetLevel = userRoleLevelFromBasedb(targetUserId);
    if (requesterLevel < 4 && targetLevel >= requesterLevel) {
      throw new ForbiddenException("You can only invite users with a lower role than yours");
    }

    if (memberRepository.existsByIdGroupIdAndIdUserId(groupId, targetUserId)) {
      throw new com.cms.exception.ConflictException("User is already a member of this group");
    }

    ChatGroupMember member = addMemberInternal(groupId, targetUserId, ChatMemberRole.MEMBER);
    return toMemberDto(member);
  }

  @Override
  @Transactional
  public void removeMember(UUID groupId, Long targetUserId, Long requesterId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    selfProvider.getObject().removeMemberInTenant(group, groupId, targetUserId, requesterId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void removeMemberInTenant(
      ChatGroup group, UUID groupId, Long targetUserId, Long requesterId) {
    tenantRouter.useGroupDatabase(group);
    checkWriteAccess(group, groupId, requesterId);
    memberRepository.deleteByIdGroupIdAndIdUserId(groupId, targetUserId);
  }

  @Override
  @Transactional
  public void deleteGroup(UUID groupId, Long requesterId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    selfProvider.getObject().deleteGroupInTenant(group, groupId, requesterId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteGroupInTenant(ChatGroup group, UUID groupId, Long requesterId) {
    tenantRouter.useGroupDatabase(group);
    if (currentUserRoleLevel() < 4 && !group.getCreatedBy().equals(requesterId)) {
      throw new ForbiddenException("Only the group owner or SUPER_ADMIN can delete this group");
    }
    groupRepository.delete(group);
  }

  @Override
  public List<DtoChatMember> getMembers(UUID groupId, Long requesterId) {
    return selfProvider.getObject().getMembersInTenant(groupId, requesterId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public List<DtoChatMember> getMembersInTenant(UUID groupId, Long requesterId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    tenantRouter.useGroupDatabase(group);
    checkReadAccess(group, groupId, requesterId);
    return memberRepository.findByIdGroupId(groupId)
        .stream()
        .map(this::toMemberDto)
        .toList();
  }

  @Override
  public boolean isMember(UUID groupId, Long userId) {
    return tenantRouter.findGroup(groupId)
        .map(group -> selfProvider.getObject().isMemberInTenant(group, groupId, userId))
        .orElse(false);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public boolean isMemberInTenant(ChatGroup group, UUID groupId, Long userId) {
    tenantRouter.useGroupDatabase(group);
    return memberRepository.existsByIdGroupIdAndIdUserId(groupId, userId);
  }

  @Override
  public DtoChatGroupAccess resolveGroupAccess(UUID groupId, Long userId) {
    return selfProvider.getObject().resolveGroupAccessInTenant(groupId, userId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public DtoChatGroupAccess resolveGroupAccessInTenant(UUID groupId, Long userId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    tenantRouter.useGroupDatabase(group);
    boolean member = memberRepository.existsByIdGroupIdAndIdUserId(groupId, userId);
    boolean canRead = canReadGroup(group, groupId, userId);
    boolean canWrite = canWriteGroup(group, groupId, userId);

    String denialCode = null;
    String denialMessage = null;
    if (!canWrite) {
      denialCode = "CHAT_WRITE_FORBIDDEN";
      if (!canRead) {
        denialMessage = "Bu gruba erişim yetkiniz yok.";
      } else if (!member) {
        denialMessage = "Bu gruba mesaj yazamazsınız. Yalnızca davetli üyeler mesaj gönderebilir.";
      } else {
        denialMessage = "Bu gruba mesaj yazamazsınız.";
      }
    }

    return DtoChatGroupAccess.builder()
        .groupId(groupId)
        .member(member)
        .canRead(canRead)
        .canWrite(canWrite)
        .denialCode(denialCode)
        .denialMessage(denialMessage)
        .build();
  }

  void checkReadAccess(UUID groupId, Long userId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    tenantRouter.useGroupDatabase(group);
    checkReadAccess(group, groupId, userId);
  }

  void checkReadAccess(ChatGroup group, UUID groupId, Long userId) {
    if (canReadGroup(group, groupId, userId)) {
      return;
    }
    throw new ForbiddenException("You do not have access to this group", "CHAT_READ_FORBIDDEN");
  }

  /**
   * Yazma erişimi: üye VEYA rol seviyesi {@code visibilityLevel}'ın üstünde (strict).
   * Böylece gruptan çıkarılan kullanıcı, visibility sayesinde görmeye devam edebilir
   * ama mesaj yazamaz.
   */
  void checkWriteAccess(UUID groupId, Long userId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    tenantRouter.useGroupDatabase(group);
    checkWriteAccess(group, groupId, userId);
  }

  void checkWriteAccess(ChatGroup group, UUID groupId, Long userId) {
    if (canWriteGroup(group, groupId, userId)) {
      return;
    }
    throw new ForbiddenException(
        "You cannot send messages in this group",
        "CHAT_WRITE_FORBIDDEN");
  }

  /**
   * Anonim guest yazma erişimi: grup tenant'a ait olmalı ({@code tenantId != null})
   * ve {@code visitorAccess=true} işaretli olmalı. Guest'in userId/rolü/üyeliği yok;
   * tek kapı bu flag. Kayıtlı VISITOR ile aynı kapıyı paylaşır.
   */
  @Override
  public void checkGuestWriteAccess(UUID groupId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    tenantRouter.useGroupDatabase(group);
    if (group.getTenantId() == null || group.getTenantId().isBlank() || !group.isVisitorAccess()) {
      throw new ForbiddenException(
          "This group is not open to guests",
          "CHAT_GUEST_FORBIDDEN");
    }
  }

  private boolean canReadGroup(ChatGroup group, UUID groupId, Long userId) {
    if (memberRepository.existsByIdGroupIdAndIdUserId(groupId, userId)) {
      return true;
    }
    // Herkese açık TC grubu (tenantId + visitorAccess): tüm admin rolleri görebilir.
    if (isPublicTenantGroup(group)) {
      return true;
    }
    int roleLevel = currentUserRoleLevel();
    if (group.getType() == ChatGroupType.DM) {
      return roleLevel >= 4;
    }
    return roleLevel >= group.getVisibilityLevel();
  }

  private boolean canWriteGroup(ChatGroup group, UUID groupId, Long userId) {
    if (memberRepository.existsByIdGroupIdAndIdUserId(groupId, userId)) {
      return true;
    }
    // Herkese açık TC grubu (tenantId + visitorAccess): tüm admin rolleri yazabilir.
    if (isPublicTenantGroup(group)) {
      return true;
    }
    int roleLevel = currentUserRoleLevel();
    if (group.getType() == ChatGroupType.DM) {
      return roleLevel >= 4;
    }
    return roleLevel > group.getVisibilityLevel();
  }

  /**
   * Herkese açık tenant chat (TC) grubu mu? tenantId dolu + visitorAccess=true.
   * Bu gruplarda görünürlük/üyelik kuralı atlanır; tüm admin rolleri (ADMIN/EDITOR/VIEWER)
   * görür ve yazar (kullanıcı tercihi: yalnızca herkese açık TC grupları).
   */
  private boolean isPublicTenantGroup(ChatGroup group) {
    return group.getTenantId() != null
        && !group.getTenantId().isBlank()
        && group.isVisitorAccess();
  }

  /**
   * Mevcut authenticated user'ın role seviyesini SecurityContext'ten okur — DB query yok.
   * <p>JwtAuthenticationFilter authority listesini ROLE_ prefix'iyle koyduğu için burada
   * ROLE_SUPER_ADMIN/ROLE_ADMIN/... aranır. Hiçbir rol bulunamazsa VIEWER (1) döner.
   *
   * <p>Bu method TenantContext'e BAĞIMSIZ — TC group'larında bile (TenantContext=tenantX)
   * doğru çalışır çünkü kullanıcı kimliği SecurityContext'te zaten cache'lenmiş halde
   * (CachedUserPrincipal).
   *
   * @return 4=SUPER_ADMIN, 3=ADMIN, 2=EDITOR, 1=VIEWER (en yüksek rol kazanır)
   */
  private int currentUserRoleLevel() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return 1;
    Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
    if (authorities == null) return 1;
    Set<String> roleNames = authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
    if (roleNames.contains("ROLE_SUPER_ADMIN")) return 4;
    if (roleNames.contains("ROLE_ADMIN")) return 3;
    if (roleNames.contains("ROLE_EDITOR")) return 2;
    return 1;
  }

  /**
   * SecurityContext'te olmayan bir user için (ör. davet edilen target) role seviyesini
   * basedb'den okur. TC group'unda iken bile TenantContext geçici olarak basedb'ye
   * çevrilir ki tenant DB'sinde olmayan user kaydı için yanlış VIEWER düşmesin.
   *
   * <p>{@code Propagation.REQUIRES_NEW} kullanılarak ayrı bir transaction açılır —
   * çünkü mevcut transaction çoktan bir tenant DataSource'a bağlanmış olabilir.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  protected int userRoleLevelFromBasedb(Long userId) {
    String originalTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId(defaultTenant);
      return userRepository.findById(userId)
          .map(u -> {
            Set<String> roles = u.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());
            if (roles.contains("SUPER_ADMIN")) return 4;
            if (roles.contains("ADMIN")) return 3;
            if (roles.contains("EDITOR")) return 2;
            return 1;
          })
          .orElse(1);
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }

  private ChatGroupMember addMemberInternal(UUID groupId, Long userId, ChatMemberRole role) {
    ChatGroupMember member = new ChatGroupMember();
    member.setId(new ChatGroupMemberId(groupId, userId));
    member.setRole(role);
    return memberRepository.save(member);
  }

  /**
   * DTO'dan gelen tenantId'yi normalize eder. Boş/whitespace string'i null'a çevirir
   * (AC için) — controller'ın query/header normalization işini hafifletir.
   */
  private String normalizeTenantId(String tenantId) {
    if (tenantId == null) return null;
    String trimmed = tenantId.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private DtoChatMember toMemberDto(ChatGroupMember member) {
    DtoChatMember dto = new DtoChatMember();
    dto.setUserId(member.getId().getUserId());
    dto.setRole(member.getRole());
    dto.setJoinedAt(member.getJoinedAt());
    userRepository.findById(member.getId().getUserId()).ifPresent(u -> {
      dto.setUsername(u.getUsername());
      dto.setFirstName(u.getFirstName());
      dto.setLastName(u.getLastName());
    });
    return dto;
  }
}

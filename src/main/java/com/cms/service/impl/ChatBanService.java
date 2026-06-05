package com.cms.service.impl;

import com.cms.dto.DtoChatBan;
import com.cms.dto.DtoChatBanEvent;
import com.cms.dto.DtoChatBanRequest;
import com.cms.entity.ChatBan;
import com.cms.entity.ChatGroup;
import com.cms.exception.ForbiddenException;
import com.cms.exception.ValidationException;
import com.cms.repository.ChatBanRepository;
import com.cms.service.IChatBanService;
import com.cms.util.ChatTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * TC (tenant chat) ban yönetimi. Ban kaydı grubun TENANT DB'sinde tutulur
 * ({@code tenantRouter.useGroupDatabase}). Enforcement: guest/visitor gönderim
 * yolunda {@link #isGuestBanned}/{@link #isVisitorBanned} çağrılır (TenantContext
 * o anda zaten grubun tenant'ına set olduğundan ekstra switch gerekmez).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBanService implements IChatBanService {

  private final ChatBanRepository banRepository;
  private final ChatGroupTenantRouter tenantRouter;
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  @Transactional
  public DtoChatBan ban(UUID groupId, DtoChatBanRequest req, Long requesterId, String requesterUsername) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    tenantRouter.useGroupDatabase(group);
    validateTcGroup(group);
    validateTarget(req.getSessionId(), req.getVisitorId());

    // Idempotent: zaten banlıysa mevcut kaydı dön
    ChatBan ban = findExisting(groupId, req.getSessionId(), req.getVisitorId());
    if (ban == null) {
      ban = banRepository.save(ChatBan.builder()
          .groupId(groupId)
          .sessionId(req.getSessionId())
          .visitorId(req.getVisitorId())
          .bannedByUserId(requesterId)
          .bannedByUsername(requesterUsername)
          .reason(req.getReason())
          .build());
      log.info("TC ban: group={} session={} visitor={} by={}",
          groupId, req.getSessionId(), req.getVisitorId(), requesterUsername);
    }
    DtoChatBan dto = toDto(ban);
    broadcast(group, "BANNED", dto);
    return dto;
  }

  @Override
  @Transactional
  public void unban(UUID groupId, UUID sessionId, Long visitorId, Long requesterId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    tenantRouter.useGroupDatabase(group);
    validateTarget(sessionId, visitorId);
    ChatBan ban = findExisting(groupId, sessionId, visitorId);
    if (ban == null) {
      return; // idempotent
    }
    banRepository.delete(ban);
    log.info("TC unban: group={} session={} visitor={} by={}", groupId, sessionId, visitorId, requesterId);
    broadcast(group, "UNBANNED", toDto(ban));
  }

  @Override
  @Transactional(readOnly = true)
  public List<DtoChatBan> listBans(UUID groupId) {
    ChatGroup group = tenantRouter.requireGroup(groupId);
    tenantRouter.useGroupDatabase(group);
    return banRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isGuestBanned(UUID groupId, UUID sessionId) {
    return sessionId != null && banRepository.existsByGroupIdAndSessionId(groupId, sessionId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isVisitorBanned(UUID groupId, Long visitorId) {
    return visitorId != null && banRepository.existsByGroupIdAndVisitorId(groupId, visitorId);
  }

  private ChatBan findExisting(UUID groupId, UUID sessionId, Long visitorId) {
    return (sessionId != null)
        ? banRepository.findByGroupIdAndSessionId(groupId, sessionId).orElse(null)
        : banRepository.findByGroupIdAndVisitorId(groupId, visitorId).orElse(null);
  }

  private void validateTcGroup(ChatGroup group) {
    if (group.getTenantId() == null || group.getTenantId().isBlank()) {
      throw new ForbiddenException(
          "Ban yalnızca tenant chat (TC) gruplarında yapılır", "CHAT_BAN_AC_FORBIDDEN");
    }
  }

  private void validateTarget(UUID sessionId, Long visitorId) {
    if ((sessionId != null) == (visitorId != null)) {
      throw new ValidationException("Tam olarak bir hedef gönderin: sessionId VEYA visitorId");
    }
  }

  private void broadcast(ChatGroup group, String action, DtoChatBan dto) {
    DtoChatBanEvent event = new DtoChatBanEvent(
        action, dto.getGroupId(), dto.getSessionId(), dto.getVisitorId(), dto.getBannedByUsername());
    messagingTemplate.convertAndSend(ChatTopics.banTopic(group.getTenantId(), group.getId()), event);
  }

  private DtoChatBan toDto(ChatBan b) {
    return DtoChatBan.builder()
        .id(b.getId())
        .groupId(b.getGroupId())
        .sessionId(b.getSessionId())
        .visitorId(b.getVisitorId())
        .bannedByUserId(b.getBannedByUserId())
        .bannedByUsername(b.getBannedByUsername())
        .reason(b.getReason())
        .createdAt(b.getCreatedAt())
        .build();
  }
}

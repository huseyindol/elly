package com.cms.service.impl;

import com.cms.dto.DtoChatGroup;
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
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  @Override
  @Transactional
  public DtoChatGroup createGroup(DtoChatGroupCreate dto, Long creatorId) {
    ChatGroup group = new ChatGroup();
    group.setName(dto.getName());
    group.setDescription(dto.getDescription());
    group.setType(ChatGroupType.GROUP);
    group.setCreatedBy(creatorId);
    group.setVisibilityLevel(getUserRoleLevel(creatorId));
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
    int roleLevel = getUserRoleLevel(userId);
    return groupRepository.findGroupsByUserIdAndRole(userId, roleLevel)
        .stream()
        .map(chatMapper::toGroupDto)
        .toList();
  }

  @Override
  public DtoChatGroup getGroupById(UUID groupId, Long requesterId) {
    ChatGroup group = findGroupOrThrow(groupId);
    checkAccess(groupId, requesterId);
    return chatMapper.toGroupDto(group);
  }

  @Override
  @Transactional
  public DtoChatMember addMember(UUID groupId, Long targetUserId, Long requesterId) {
    findGroupOrThrow(groupId);
    checkAccess(groupId, requesterId);

    // Invite hierarchy: requester can only invite users with a lower role level.
    // SUPER_ADMIN (level 4) is exempt and may invite anyone.
    int requesterLevel = getUserRoleLevel(requesterId);
    int targetLevel = getUserRoleLevel(targetUserId);
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
    findGroupOrThrow(groupId);
    checkAccess(groupId, requesterId);
    memberRepository.deleteByIdGroupIdAndIdUserId(groupId, targetUserId);
  }

  @Override
  @Transactional
  public void deleteGroup(UUID groupId, Long requesterId) {
    ChatGroup group = findGroupOrThrow(groupId);
    if (getUserRoleLevel(requesterId) < 4 && !group.getCreatedBy().equals(requesterId)) {
      throw new ForbiddenException("Only the group owner or SUPER_ADMIN can delete this group");
    }
    groupRepository.delete(group);
  }

  @Override
  public List<DtoChatMember> getMembers(UUID groupId, Long requesterId) {
    findGroupOrThrow(groupId);
    checkAccess(groupId, requesterId);
    return memberRepository.findByIdGroupId(groupId)
        .stream()
        .map(this::toMemberDto)
        .toList();
  }

  @Override
  public boolean isMember(UUID groupId, Long userId) {
    return memberRepository.existsByIdGroupIdAndIdUserId(groupId, userId);
  }

  private ChatGroup findGroupOrThrow(UUID groupId) {
    return groupRepository.findById(groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Chat group not found: " + groupId));
  }

  void checkAccess(UUID groupId, Long userId) {
    if (getUserRoleLevel(userId) < 4 && !isMember(groupId, userId)) {
      throw new ForbiddenException("You are not a member of this group");
    }
  }

  @Override
  public void checkPublicAccess(UUID groupId) {
    ChatGroup group = findGroupOrThrow(groupId);
    if (group.getVisibilityLevel() > 1) {
      throw new ForbiddenException("This chat group is not public");
    }
  }

  // Returns 4=SUPER_ADMIN, 3=ADMIN, 2=EDITOR, 1=VIEWER (highest role wins)
  private int getUserRoleLevel(Long userId) {
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
  }

  private ChatGroupMember addMemberInternal(UUID groupId, Long userId, ChatMemberRole role) {
    ChatGroupMember member = new ChatGroupMember();
    member.setId(new ChatGroupMemberId(groupId, userId));
    member.setRole(role);
    return memberRepository.save(member);
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

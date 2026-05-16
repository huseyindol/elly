package com.cms.chat.service.impl;

import com.cms.chat.dto.DtoChatGroup;
import com.cms.chat.dto.DtoChatGroupCreate;
import com.cms.chat.dto.DtoChatMember;
import com.cms.chat.entity.*;
import com.cms.chat.mapper.ChatMapper;
import com.cms.chat.repository.ChatGroupMemberRepository;
import com.cms.chat.repository.ChatGroupRepository;
import com.cms.chat.service.IChatGroupService;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.exception.ForbiddenException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
          dm = groupRepository.save(dm);
          addMemberInternal(dm.getId(), currentUserId, ChatMemberRole.OWNER);
          addMemberInternal(dm.getId(), targetUserId, ChatMemberRole.MEMBER);
          return chatMapper.toGroupDto(dm);
        });
  }

  @Override
  public List<DtoChatGroup> getMyGroups(Long userId) {
    return groupRepository.findGroupsByUserId(userId)
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
    if (!isSuperAdmin(requesterId) && !group.getCreatedBy().equals(requesterId)) {
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
    if (!isSuperAdmin(userId) && !isMember(groupId, userId)) {
      throw new ForbiddenException("You are not a member of this group");
    }
  }

  private boolean isSuperAdmin(Long userId) {
    return userRepository.findById(userId)
        .map(u -> u.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getName())))
        .orElse(false);
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

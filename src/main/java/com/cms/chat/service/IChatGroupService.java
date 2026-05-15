package com.cms.chat.service;

import com.cms.chat.dto.DtoChatGroup;
import com.cms.chat.dto.DtoChatGroupCreate;
import com.cms.chat.dto.DtoChatMember;

import java.util.List;
import java.util.UUID;

public interface IChatGroupService {

  DtoChatGroup createGroup(DtoChatGroupCreate dto, Long creatorId);

  DtoChatGroup getOrCreateDm(Long currentUserId, Long targetUserId);

  List<DtoChatGroup> getMyGroups(Long userId);

  DtoChatGroup getGroupById(UUID groupId, Long requesterId);

  DtoChatMember addMember(UUID groupId, Long targetUserId, Long requesterId);

  void removeMember(UUID groupId, Long targetUserId, Long requesterId);

  void deleteGroup(UUID groupId, Long requesterId);

  List<DtoChatMember> getMembers(UUID groupId, Long requesterId);

  boolean isMember(UUID groupId, Long userId);
}

package com.cms.service;

import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatGroupAccess;
import com.cms.dto.DtoChatGroupCreate;
import com.cms.dto.DtoChatMember;

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

  DtoChatGroupAccess resolveGroupAccess(UUID groupId, Long userId);

  boolean isMember(UUID groupId, Long userId);

  /**
   * Anonim guest'in yazma erişimini doğrular: grup tenant'a ait + visitorAccess=true.
   * Aksi halde {@code ForbiddenException(CHAT_GUEST_FORBIDDEN)}.
   */
  void checkGuestWriteAccess(UUID groupId);
}

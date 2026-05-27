package com.cms.service;

import com.cms.dto.DtoChatGroup;
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

  boolean isMember(UUID groupId, Long userId);

  /**
   * Guest (anonim) kullanıcıların erişim kontrolü.
   * Sadece visibilityLevel=1 (public) gruplara izin verir.
   */
  void checkPublicAccess(UUID groupId);
}

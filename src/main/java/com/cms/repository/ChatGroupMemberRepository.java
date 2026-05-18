package com.cms.repository;

import com.cms.entity.ChatGroupMember;
import com.cms.entity.ChatGroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMember, ChatGroupMemberId> {

  List<ChatGroupMember> findByIdGroupId(UUID groupId);

  boolean existsByIdGroupIdAndIdUserId(UUID groupId, Long userId);

  void deleteByIdGroupIdAndIdUserId(UUID groupId, Long userId);
}

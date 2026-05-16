package com.cms.chat.repository;

import com.cms.chat.entity.ChatGroup;
import com.cms.chat.entity.ChatGroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, UUID> {

  @Query("""
      SELECT g FROM ChatGroup g
      INNER JOIN ChatGroupMember m ON m.id.groupId = g.id
      WHERE m.id.userId = :userId
      ORDER BY g.updatedAt DESC
      """)
  List<ChatGroup> findGroupsByUserId(@Param("userId") Long userId);

  @Query("""
      SELECT g FROM ChatGroup g
      INNER JOIN ChatGroupMember m1 ON m1.id.groupId = g.id AND m1.id.userId = :userId1
      INNER JOIN ChatGroupMember m2 ON m2.id.groupId = g.id AND m2.id.userId = :userId2
      WHERE g.type = :type
      """)
  Optional<ChatGroup> findDmBetween(@Param("userId1") Long userId1,
      @Param("userId2") Long userId2,
      @Param("type") ChatGroupType type);
}

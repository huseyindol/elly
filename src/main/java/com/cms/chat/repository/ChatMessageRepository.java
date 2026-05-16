package com.cms.chat.repository;

import com.cms.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

  @Query("""
      SELECT m FROM ChatMessage m
      WHERE m.groupId = :groupId
        AND m.deletedAt IS NULL
        AND (:before IS NULL OR m.createdAt < :before)
      ORDER BY m.createdAt DESC
      """)
  List<ChatMessage> findByGroupIdCursor(@Param("groupId") UUID groupId,
      @Param("before") Date before,
      Pageable pageable);

  @Query("""
      SELECT m FROM ChatMessage m
      WHERE m.groupId = :groupId
        AND m.deletedAt IS NULL
        AND m.id NOT IN (
            SELECT r.id.messageId FROM ChatMessageRead r WHERE r.id.userId = :userId
        )
      ORDER BY m.createdAt ASC
      """)
  List<ChatMessage> findUnreadByGroupIdAndUserId(@Param("groupId") UUID groupId,
      @Param("userId") Long userId);
}

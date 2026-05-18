package com.cms.repository;

import com.cms.entity.ChatMessageRead;
import com.cms.entity.ChatMessageReadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ChatMessageReadRepository extends JpaRepository<ChatMessageRead, ChatMessageReadId> {

  @Modifying
  @Query(value = """
      INSERT INTO chat_message_reads (message_id, user_id, read_at)
      SELECT m.id, :userId, NOW()
      FROM chat_messages m
      WHERE m.group_id = :groupId
        AND m.deleted_at IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM chat_message_reads r
            WHERE r.message_id = m.id AND r.user_id = :userId
        )
      """, nativeQuery = true)
  int markAllAsRead(@Param("groupId") UUID groupId, @Param("userId") Long userId);
}

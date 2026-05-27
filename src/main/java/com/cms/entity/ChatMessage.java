package com.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_messages_group_created", columnList = "group_id, created_at"),
    @Index(name = "idx_chat_messages_group_id_cursor", columnList = "group_id, id"),
    @Index(name = "idx_chat_messages_parent_id", columnList = "parent_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "sender_id", nullable = true)
  private Long senderId;

  @Column(name = "session_id")
  private UUID sessionId;

  @Column(name = "sender_display_name", length = 100)
  private String senderDisplayName;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "content_type", nullable = false, length = 10)
  private ChatMessageType contentType = ChatMessageType.TEXT;

  @Column(name = "file_url", length = 500)
  private String fileUrl;

  @Column(name = "parent_id")
  private UUID parentId;

  @Column(name = "deleted_at")
  private Date deletedAt;

  @Column(name = "edited_at")
  private Date editedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Date createdAt;
}

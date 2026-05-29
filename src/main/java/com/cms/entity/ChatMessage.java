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

  /**
   * Admin gönderdiyse basedb users.id; visitor gönderdiyse NULL.
   * Polymorphic sender — bkz. {@link #senderType}.
   */
  @Column(name = "sender_id")
  private Long senderId;

  /**
   * Polymorphic discriminator. Default 'ADMIN' (geriye uyumlu — mevcut AC mesajları).
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "sender_type", nullable = false, length = 10)
  private ChatMessageSenderType senderType = ChatMessageSenderType.ADMIN;

  /**
   * Visitor (tenant DB visitor_identities.id) — sender_type=VISITOR ise dolu.
   */
  @Column(name = "visitor_id")
  private Long visitorId;

  /**
   * Anonim guest oturum kimliği (guest token'daki sessionId) — sender_type=GUEST ise dolu.
   */
  @Column(name = "session_id")
  private UUID sessionId;

  /**
   * Anonim guest'in ekranda görünecek adı — sender_type=GUEST ise dolu.
   * Guest hesabı olmadığı için isim mesaja denormalize edilir.
   */
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

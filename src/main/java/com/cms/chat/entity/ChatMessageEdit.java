package com.cms.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "chat_message_edits", indexes = {
    @Index(name = "idx_chat_message_edits_message", columnList = "message_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageEdit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "message_id", nullable = false)
  private UUID messageId;

  @Column(name = "previous_content", nullable = false, columnDefinition = "TEXT")
  private String previousContent;

  @Column(name = "edited_at", nullable = false)
  private Date editedAt = new Date();
}

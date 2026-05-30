package com.cms.dto;

import com.cms.entity.ChatMessageSenderType;
import com.cms.entity.ChatMessageType;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class DtoChatMessage {
  private UUID id;
  private UUID groupId;
  /** Admin gönderdiyse basedb users.id; visitor gönderdiyse null. */
  private Long senderId;
  /** Visitor gönderdiyse visitor_identities.id (tenant DB); admin gönderdiyse null. */
  private Long visitorId;
  /** Polymorphic discriminator — frontend rozet/avatar mapping'i için. */
  private ChatMessageSenderType senderType;
  /** Frontend display için zenginleştirilmiş alan (admin'in username'i veya visitor display_name). */
  private String senderUsername;
  /** GUEST gönderdiyse guest oturum kimliği (session_id) — frontend "kendi mesajım" eşleştirmesi için. */
  private UUID sessionId;
  /** GUEST gönderdiyse ekran adı (denormalize); ADMIN/VISITOR'da null. */
  private String senderDisplayName;
  private String content;
  private ChatMessageType contentType;
  private String fileUrl;
  private UUID parentId;
  private boolean deleted;
  private Date editedAt;
  private Date createdAt;
}

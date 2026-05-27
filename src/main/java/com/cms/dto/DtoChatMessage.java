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
  private String content;
  private ChatMessageType contentType;
  private String fileUrl;
  private UUID parentId;
  private boolean deleted;
  private Date editedAt;
  private Date createdAt;
}

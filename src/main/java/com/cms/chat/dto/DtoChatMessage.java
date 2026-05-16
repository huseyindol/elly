package com.cms.chat.dto;

import com.cms.chat.entity.ChatMessageType;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class DtoChatMessage {
  private UUID id;
  private UUID groupId;
  private Long senderId;
  private String senderUsername;
  private String content;
  private ChatMessageType contentType;
  private String fileUrl;
  private UUID parentId;
  private boolean deleted;
  private Date editedAt;
  private Date createdAt;
}

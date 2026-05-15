package com.cms.chat.dto;

import com.cms.chat.entity.ChatGroupType;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class DtoChatGroup {
  private UUID id;
  private String name;
  private String description;
  private ChatGroupType type;
  private Long createdBy;
  private Date createdAt;
  private Date updatedAt;
}

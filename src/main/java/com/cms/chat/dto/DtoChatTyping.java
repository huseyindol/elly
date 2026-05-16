package com.cms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoChatTyping {
  private UUID groupId;
  private Long userId;
  private String username;
  private boolean typing;
}

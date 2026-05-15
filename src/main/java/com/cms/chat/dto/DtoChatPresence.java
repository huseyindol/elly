package com.cms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoChatPresence {
  private Long userId;
  private String username;
  private String status; // "ONLINE" | "OFFLINE"
}

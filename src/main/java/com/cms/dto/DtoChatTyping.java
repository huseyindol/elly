package com.cms.dto;

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
  /** GUEST typing'inde dolu (guest oturum kimliği) — alıcı kendi typing'ini ayırt eder. ADMIN/VISITOR'da null. */
  private String sessionId;
}

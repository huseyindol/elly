package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/** TC ban kaydı yanıtı (panel ban listesi). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoChatBan {
  private UUID id;
  private UUID groupId;
  private UUID sessionId;        // guest ban hedefi (null olabilir)
  private Long visitorId;        // visitor ban hedefi (null olabilir)
  private Long bannedByUserId;
  private String bannedByUsername;
  private String reason;
  private Date createdAt;
}

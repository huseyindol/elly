package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * WS ban olayı — {@code /topic/tenant/{tenantId}/group/{groupId}/bans} üzerinden yayınlanır.
 * Banlanan guest kendi sessionId'siyle eşleşince input'unu kilitler; panel ban listesini tazeler.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoChatBanEvent {
  private String action;        // BANNED | UNBANNED
  private UUID groupId;
  private UUID sessionId;       // guest hedefi (null olabilir)
  private Long visitorId;       // visitor hedefi (null olabilir)
  private String byUsername;    // işlemi yapan admin
}

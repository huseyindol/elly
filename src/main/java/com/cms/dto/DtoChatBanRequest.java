package com.cms.dto;

import lombok.Data;

import java.util.UUID;

/** Ban isteği — tam olarak BİR hedef gönderilir (sessionId VEYA visitorId). */
@Data
public class DtoChatBanRequest {
  private UUID sessionId;   // guest ban hedefi
  private Long visitorId;   // kayıtlı visitor ban hedefi
  private String reason;    // opsiyonel
}

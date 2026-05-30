package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Guest chat token yanıtı. Frontend bu token'ı STOMP CONNECT'te
 * {@code Authorization: Bearer <token>} olarak gönderir.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoGuestTokenResponse {
  private String token;
  /** Token geçerlilik süresi (saniye). */
  private long expiresIn;
  private String displayName;
  private String tenantId;
  /** Guest oturum kimliği — frontend "kendi mesajım" tespiti için saklar (msg.sessionId ile karşılaştırır). */
  private String sessionId;
}

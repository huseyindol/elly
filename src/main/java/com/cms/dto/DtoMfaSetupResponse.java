package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 2FA setup başlatma yanıtı.
 * Frontend bu bilgiyle qrcode.js kullanarak QR kodu render eder.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DtoMfaSetupResponse {
  /** Base32 secret — kullanıcıya gösterilir; Authenticator'a manuel girilebilir */
  private String secret;
  /** otpauth://totp/... URI — QR olarak render edilmeli */
  private String qrUri;
  /** Uygulama adı — QR üzerinde görünür */
  private String issuer;
}

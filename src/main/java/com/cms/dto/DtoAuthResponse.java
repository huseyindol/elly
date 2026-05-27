package com.cms.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoAuthResponse {
  private String token;
  private String refreshToken;
  private String type = "Bearer";
  private Long userId;
  private String username;
  private String email;
  private String userCode;
  private Long expiredDate;
  private String message;

  /** Kullanıcının rolleri: ["SUPER_ADMIN", "ADMIN", ...] */
  private List<String> roles;

  /** Kullanıcının tüm izinleri: ["posts:create", "posts:read", ...] */
  private List<String> permissions;

  // ── 2FA ────────────────────────────────────────────────────────────────
  /** true ise tam JWT henüz yok; sadece mfaToken geçerli */
  private Boolean mfaRequired;

  /** Kısa ömürlü (5 dk) MFA token — sadece /api/v1/auth/mfa/verify'e gönderilir */
  private String mfaToken;
}


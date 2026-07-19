package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sosyal giriş isteği: {@code POST /api/v1/public/{tid}/auth/social/{provider}}.
 *
 * <p>Frontend (tenant site) provider'dan aldığı authorization {@code code}'u ve redirect'te
 * kullandığı {@code redirectUri}'yi gönderir; code→token takası ve profil doğrulaması
 * BACKEND'de yapılır (client secret'lar backend'de kalır).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoSocialLogin {

  @NotBlank(message = "code zorunludur")
  private String code;

  /** Provider'a authorize isteğinde kullanılan redirect URI (takasta birebir aynı olmalı). */
  @NotBlank(message = "redirectUri zorunludur")
  private String redirectUri;

  /** Public URL (/api/v1/public/{tid}) kullanılmadıysa fallback tenant. */
  private String tenantId;
}

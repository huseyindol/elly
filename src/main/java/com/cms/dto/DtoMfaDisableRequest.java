package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 2FA deaktivasyonu: mevcut şifreyi doğrulayarak 2FA'yı kapat.
 * POST /api/v1/auth/mfa/disable
 */
@Getter
@Setter
@NoArgsConstructor
public class DtoMfaDisableRequest {

  @NotBlank(message = "Şifre zorunludur")
  private String password;
}

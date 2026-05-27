package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Login akışının 2. adımı: MFA kodu doğrulama.
 * POST /api/v1/auth/mfa/verify
 */
@Getter
@Setter
@NoArgsConstructor
public class DtoMfaVerifyRequest {

  @NotBlank(message = "mfaToken zorunludur")
  private String mfaToken;

  @NotBlank(message = "Doğrulama kodu zorunludur")
  @Pattern(regexp = "\\d{6}", message = "Doğrulama kodu 6 haneli sayı olmalıdır")
  private String code;
}

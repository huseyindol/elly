package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 2FA setup tamamlama: ilk kodu girerek setup'ı doğrulama.
 * POST /api/v1/auth/mfa/setup/verify
 */
@Getter
@Setter
@NoArgsConstructor
public class DtoMfaSetupVerifyRequest {

  @NotBlank(message = "Doğrulama kodu zorunludur")
  @Pattern(regexp = "\\d{6}", message = "Doğrulama kodu 6 haneli sayı olmalıdır")
  private String code;
}

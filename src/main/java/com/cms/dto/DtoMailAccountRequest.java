package com.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DtoMailAccountRequest {

  @NotBlank(message = "Hesap adı zorunludur")
  private String name;

  @NotBlank(message = "Gönderici adresi zorunludur")
  @Email(message = "Geçersiz e-posta formatı")
  private String fromAddress;

  @NotBlank(message = "SMTP sunucu adresi zorunludur")
  private String smtpHost;

  @NotNull(message = "SMTP port zorunludur")
  @Min(value = 1, message = "Port 1-65535 aralığında olmalıdır")
  @Max(value = 65535, message = "Port 1-65535 aralığında olmalıdır")
  private Integer smtpPort;

  @NotBlank(message = "SMTP kullanıcı adı zorunludur")
  private String smtpUsername;

  /** Oluşturma sırasında zorunlu; güncelleme sırasında boş bırakılırsa mevcut şifre korunur. */
  private String smtpPassword;

  private Boolean isDefault = false;
  private Boolean active = true;
}

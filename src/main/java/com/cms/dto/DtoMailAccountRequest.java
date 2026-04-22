package com.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Mail hesabi istegi — v2 DB-based.
 *
 * <p>SMTP bilgileri DB'ye kaydedilir, {@code smtpPassword} AES-256-CBC ile
 * sifrelenir. {@code fromAddress} hem sender display hem From header icin
 * kullanilir. Tum alanlar zorunludur; guncelleme sirasinda {@code smtpPassword}
 * bos/null gelirse mevcut sifre korunur (servis katmani sorumlu).
 */
@Getter
@Setter
public class DtoMailAccountRequest {

  @NotBlank(message = "Hesap adi zorunludur")
  @Size(max = 255, message = "Hesap adi en fazla 255 karakter olabilir")
  private String name;

  @NotBlank(message = "From adresi zorunludur")
  @Email(message = "Gecersiz from adresi (email formatinda olmali)")
  @Size(max = 255)
  private String fromAddress;

  @NotBlank(message = "SMTP host zorunludur")
  @Size(max = 255)
  private String smtpHost;

  @NotNull(message = "SMTP port zorunludur")
  @Min(value = 1, message = "Port 1-65535 araliginda olmalidir")
  @Max(value = 65535, message = "Port 1-65535 araliginda olmalidir")
  private Integer smtpPort;

  @NotBlank(message = "SMTP username zorunludur")
  @Size(max = 255)
  private String smtpUsername;

  /**
   * SMTP sifresi (duz metin). Servis katmaninda AES ile sifrelenir.
   * Update isteklerinde bos/null gelirse mevcut sifre korunur.
   */
  private String smtpPassword;

  private Boolean active = true;
}

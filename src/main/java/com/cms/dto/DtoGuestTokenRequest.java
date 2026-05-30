package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Anonim website ziyaretçisinin guest chat token isteği.
 * <p>tenantId zorunlu — guest hangi tenant'ın chat grubuna bağlanacağını belirtir
 * (her tenant kendi DB'sinde chat tutuyor).
 */
@Data
public class DtoGuestTokenRequest {

  @NotBlank(message = "displayName zorunludur")
  @Size(max = 80, message = "displayName en fazla 80 karakter olabilir")
  private String displayName;

  /**
   * Opsiyonel: {@code /api/v1/public/{tenantId}/auth/guest-token} yolundan gelince
   * tenantId path'ten alınır (PublicApiFilter TenantContext'e set eder); body'de
   * gönderilmesi gerekmez. Direkt {@code /api/v1/auth/guest-token} çağrısında ise body'de
   * gönderilmelidir.
   */
  private String tenantId;

  /**
   * Opsiyonel: cihaz bazlı kalıcı guest kimliği (UUID). Frontend localStorage'da tutar ve
   * her token isteğinde gönderir; backend bunu sessionId olarak kullanır (geçerli UUID
   * değilse yenisi üretilir). Böylece aynı tarayıcıdan dönen guest'in eski mesajları "kendi"
   * olarak eşleşir. Kimlik doğrulama DEĞİL — yalnızca sahiplik işareti (displayName gibi client'tan gelir).
   */
  private String clientId;
}

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

  @NotBlank(message = "tenantId zorunludur")
  private String tenantId;
}

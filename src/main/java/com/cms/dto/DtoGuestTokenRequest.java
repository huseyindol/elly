package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DtoGuestTokenRequest {

  @NotBlank(message = "Display name is required")
  @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
  private String displayName;

  /**
   * Opsiyonel tenant kimliği.
   * Verilirse guest token bu tenant'a bağlanır ve mesajlar o tenant DB'sine yazılır.
   * Verilmezse basedb kullanılır (geriye dönük uyumlu).
   */
  @Size(max = 50, message = "Tenant ID must be at most 50 characters")
  private String tenantId;
}

package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SUPER_ADMIN/ADMIN tarafından panel UI'sından oluşturulan kullanıcı isteği.
 * Public register'dan farklı: tenant context JWT'den gelir, atanacak roller body'de
 * verilir (default: ADMIN). Verification mail panel frontend URL'i ile gönderilir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoCreateUser {
  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

  @NotBlank(message = "Password is required")
  @Pattern(
      regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
      message = "Password must be at least 8 characters and contain at least one uppercase letter, one lowercase letter, and one digit"
  )
  private String password;

  private String firstName;
  private String lastName;

  /**
   * Atanacak rol id'leri. Null/boş bırakılırsa ADMIN rolü otomatik atanır
   * (panel-create varsayılan davranışı). audience="tenant" iken kullanılamaz.
   */
  private List<Long> roleIds;

  /**
   * "panel" (default) → panel kullanıcısı: roleIds/ADMIN default + PANEL frontend mail.
   * "tenant" → tenant-site kullanıcısı: TENANT rolü + o tenant'ın kendi frontend-url'i
   * ile doğrulama maili (app.tenants.datasources.{tid}.frontend-url).
   */
  private String audience;
}

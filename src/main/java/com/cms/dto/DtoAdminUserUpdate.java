package com.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SUPER_ADMIN/ADMIN'in panel'den BAŞKA bir kullanıcıyı güncelleme isteği
 * (PUT /api/v1/users/{id}). Self-update ({@code /users/me} + DtoUserUpdate) ile
 * karışmasın diye ayrı DTO: burada {@code newPassword} eski şifre sorulmadan set
 * edilir (admin reset) ve kullanıcının açık oturumları sonlandırılır.
 * Tüm alanlar opsiyonel — yalnız dolu gelenler uygulanır.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoAdminUserUpdate {

  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;

  @Email(message = "Email should be valid")
  private String email;

  private String firstName;
  private String lastName;

  /**
   * Dolu gönderilirse admin şifre sıfırlaması: eski şifre sorulmaz, refresh token'lar
   * temizlenir + auth cache evict (açık oturumlar sonlanır). @Pattern null'u valid sayar.
   */
  @Pattern(
      regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
      message = "Password must be at least 8 characters and contain at least one uppercase letter, one lowercase letter, and one digit"
  )
  private String newPassword;
}

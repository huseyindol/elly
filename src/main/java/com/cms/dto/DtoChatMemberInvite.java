package com.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E-posta ile gruba üye davet isteği (POST /groups/{id}/members/by-email).
 * E-posta, grubun ait olduğu tenant DB'sindeki users tablosunda aranır;
 * bulunamazsa 404. Bulunursa userId ile davetle aynı akış (hiyerarşi kontrolü dahil).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoChatMemberInvite {

  @NotBlank(message = "email is required")
  @Email(message = "Email should be valid")
  private String email;
}

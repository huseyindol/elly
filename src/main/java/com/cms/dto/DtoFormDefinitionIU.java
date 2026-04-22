package com.cms.dto;

import com.cms.entity.form.FormSchema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating/updating FormDefinition.
 *
 * <p>Mail+Form v2: Artik varsayilan mail hesabi yoktur. Form olustururken
 * <ul>
 *   <li>{@code senderMailAccountId} — hangi DB mail hesabindan gonderilecegi (zorunlu)</li>
 *   <li>{@code recipientEmail} — bildirim hedef adresi (zorunlu, tek adres)</li>
 * </ul>
 * her ikisi de istemciden alinir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoFormDefinitionIU {

  @NotBlank(message = "title zorunludur")
  @Size(max = 255, message = "title en fazla 255 karakter olabilir")
  private String title;

  private Integer version;

  @NotNull(message = "schema zorunludur")
  private FormSchema schema;

  private Boolean active;

  /**
   * Form submit bildirimini gonderen mail hesabi (MailAccount.id).
   * Ilgili hesap aktif ve ENV profili kullanilabilir olmalidir, aksi halde 422.
   */
  @NotNull(message = "senderMailAccountId zorunludur")
  private Long senderMailAccountId;

  /**
   * Form submit bildiriminin gidecegi e-posta adresi (v1 tek adres).
   */
  @NotBlank(message = "recipientEmail zorunludur")
  @Email(message = "Gecersiz recipientEmail formati")
  @Size(max = 255, message = "recipientEmail en fazla 255 karakter olabilir")
  private String recipientEmail;

  /**
   * Opsiyonel ozel konu; bos birakilirsa varsayilan kullanilir.
   */
  @Size(max = 255, message = "notificationSubject en fazla 255 karakter olabilir")
  private String notificationSubject;

  /**
   * Bildirim etkinligi. Null ise varsayilan true olarak kabul edilir.
   */
  private Boolean notificationEnabled;
}

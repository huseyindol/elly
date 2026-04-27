package com.cms.dto;

import com.cms.entity.form.FormSchema;

import jakarta.validation.constraints.Pattern;
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
 * <p>Mail+Form v4: Bildirim opsiyonel.
 * <ul>
 *   <li>{@code notificationEnabled} null/true ise: {@code senderMailAccountId} ve
 *       {@code recipientEmail} zorunlu — service katmaninda
 *       ({@code FormDefinitionService.validateNotificationConfig}) dogrulanir
 *       ve hatali olursa 422 doner.</li>
 *   <li>{@code notificationEnabled} false ise: bu iki alan bos birakilabilir.
 *       Form submit edildiginde mail tetiklenmez.</li>
 * </ul>
 * Geriye uyumluluk: notificationEnabled belirtilmediyse default true (mail zorunlu).
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
   * notificationEnabled=true ise zorunlu — service katmaninda dogrulanir.
   * Ilgili hesap aktif olmalidir, aksi halde 422.
   */
  private Long senderMailAccountId;

  /**
   * Form submit bildiriminin gidecegi e-posta adresi (coklu alici icin virgulle ayrilabilir).
   * notificationEnabled=true ise zorunlu — service katmaninda format ve doluluk kontrolu yapilir.
   * Pattern: tek veya virgulle ayrilmis email listesi (^$ kabul edilir; bos string null gibi).
   */
  @Pattern(
      regexp = "^$|^\\s*[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(\\s*,\\s*[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})*\\s*$",
      message = "Gecersiz e-posta formati. Birden fazla adres icin araya virgul koyunuz.")
  @Size(max = 1000, message = "recipientEmail en fazla 1000 karakter olabilir")
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

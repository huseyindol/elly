package com.cms.dto;

import java.util.Date;

import com.cms.entity.form.FormSchema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for FormDefinition responses.
 *
 * <p>Mail+Form v2: Form submit sonrasi bildirim icin {@code senderMailAccountId}
 * ve {@code recipientEmail} zorunludur. {@code senderMailAccountName} ve
 * {@code senderFromAddress} salt okunur — bagli MailAccount'un panel icin
 * kullanilan metadata'sidir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoFormDefinition {
  private Long id;
  private String title;
  private Integer version;
  private FormSchema schema;
  private Boolean active;

  /** Form submit bildirimi gonderen mail hesabi (zorunlu). */
  private Long senderMailAccountId;

  /** Bagli MailAccount adi (salt okunur, UI display icin). */
  private String senderMailAccountName;

  /** Bagli MailAccount gonderici adresi (salt okunur, UI display icin). */
  private String senderFromAddress;

  /** Form submit bildiriminin gidecegi adres (v2 tek adres). */
  private String recipientEmail;

  /** Opsiyonel konu; null ise varsayilan ("Yeni form gonderimi: {title}") kullanilir. */
  private String notificationSubject;

  /** Bildirim aktif/pasif (default true). */
  private Boolean notificationEnabled;

  private Date createdAt;
  private Date updatedAt;
}

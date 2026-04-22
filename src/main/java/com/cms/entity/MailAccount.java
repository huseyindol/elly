package com.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Tenant'a ozgu SMTP hesabi — DB-based (Mail+Form v2).
 *
 * <p>Her tenant kendi DB'sinde birden fazla hesap tutabilir (or. info@, sales@,
 * support@). Formlar hangi hesabin kullanilacagini {@code sender_mail_account_id}
 * FK ile acikca secer — varsayilan hesap kavrami YOKTUR.
 *
 * <p>{@code smtpPassword} alani {@link com.cms.util.AesEncryptor} ile
 * AES-256-CBC sifreli olarak saklanir; API response'larda hicbir zaman dönmez.
 */
@Getter
@Setter
@Entity
@Table(
    name = "mail_accounts",
    indexes = {
        @Index(name = "idx_mail_account_active", columnList = "active")
    })
public class MailAccount extends BaseEntity {

  /** Gorunen ad — panelde gosterilir (ör. "Satis Hesabi"). */
  @Column(nullable = false)
  private String name;

  /** Gonderici adresi — From header'inda kullanilir (ör. sales@firma.com). */
  @Column(name = "from_address", nullable = false, length = 255)
  private String fromAddress;

  /** SMTP sunucu adresi (ör. smtp.gmail.com, smtp.office365.com). */
  @Column(name = "smtp_host", nullable = false, length = 255)
  private String smtpHost;

  /** SMTP port (587 = STARTTLS, 465 = SSL/TLS). */
  @Column(name = "smtp_port", nullable = false)
  private Integer smtpPort;

  /** SMTP kimlik dogrulama kullanici adi (genellikle e-posta adresi). */
  @Column(name = "smtp_username", nullable = false, length = 255)
  private String smtpUsername;

  /**
   * SMTP sifresi — AES-256-CBC ile sifreli olarak saklanir.
   * Ham deger hicbir zaman DTO/response ile disariya acilmamalidir.
   */
  @Column(name = "smtp_password", nullable = false, length = 512)
  private String smtpPassword;

  /** Hesap aktiflik durumu; pasif hesaplar form'da sender olarak secilemez. */
  @Column(nullable = false)
  private Boolean active = true;
}

package com.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Tenant'a özgü SMTP hesabı.
 *
 * <p>Her tenant kendi DB'sinde birden fazla hesap tutabilir
 * (ör. info@, sales@, product@). Formlar hangi hesabın kullanılacağını
 * {@code mail_account_id} FK ile seçer.
 *
 * <p>smtpPassword alanı {@link com.cms.util.AesEncryptor} ile
 * AES-256-CBC şifreli olarak saklanır; API response'larda hiçbir zaman dönmez.
 */
@Getter
@Setter
@Entity
@Table(
    name = "mail_accounts",
    indexes = {
        @Index(name = "idx_mail_account_default", columnList = "is_default"),
        @Index(name = "idx_mail_account_active", columnList = "active")
    })
public class MailAccount extends BaseEntity {

  /** Görünen ad — panelde gösterilir (ör. "Satış Hesabı"). */
  @Column(nullable = false)
  private String name;

  /** Gönderici adresi — From header'ında kullanılır (ör. sales@firma.com). */
  @Column(name = "from_address", nullable = false)
  private String fromAddress;

  /** SMTP sunucu adresi (ör. smtp.gmail.com, smtp.office365.com). */
  @Column(name = "smtp_host", nullable = false)
  private String smtpHost;

  /** SMTP port (587 = STARTTLS, 465 = SSL). */
  @Column(name = "smtp_port", nullable = false)
  private Integer smtpPort;

  /** SMTP kimlik doğrulama kullanıcı adı (genellikle e-posta adresi). */
  @Column(name = "smtp_username", nullable = false)
  private String smtpUsername;

  /**
   * SMTP şifresi — AES-256-CBC ile şifreli olarak saklanır.
   * Ham değer hiçbir zaman DTO/response ile dışarıya açılmamalıdır.
   */
  @Column(name = "smtp_password", nullable = false, length = 512)
  private String smtpPassword;

  /**
   * Varsayılan hesap bayrağı.
   * Formun mail_account_id'si null olduğunda bu hesap kullanılır.
   * Aynı anda yalnızca bir hesap varsayılan olabilir.
   */
  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  /** Hesap aktiflik durumu; pasif hesaplar seçilemez. */
  @Column(nullable = false)
  private Boolean active = true;
}

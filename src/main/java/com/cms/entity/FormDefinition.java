package com.cms.entity;

import org.hibernate.annotations.Type;

import com.cms.entity.form.FormSchema;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for storing form definitions with JSON schema.
 * The schema field contains the complete form structure including fields,
 * validation rules, and conditions.
 *
 * <p>Mail+Form v2: Form submission otomatik mail bildirimi tetikler:
 * <ul>
 *   <li>{@code senderMailAccount}: Hangi SMTP hesabindan gonderilecegi (zorunlu)</li>
 *   <li>{@code recipientEmail}: Bildirim hedef adresi (coklu alici icin virgulle ayrilabilir)</li>
 *   <li>{@code notificationSubject}: Bos ise varsayilan konu kullanilir</li>
 *   <li>{@code notificationEnabled}: Devre disi birakilabilir</li>
 * </ul>
 */
@Entity
@Table(name = "form_definitions", indexes = {
    @Index(name = "idx_form_def_title", columnList = "title"),
    @Index(name = "idx_form_def_active", columnList = "active"),
    @Index(name = "idx_form_def_sender_mail", columnList = "sender_mail_account_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormDefinition extends BaseEntity {
  private String title;

  private Integer version;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private FormSchema schema;

  private Boolean active;

  /**
   * Form submit sonrasi bildirim gönderen profil.
   * {@link MailAccount#getActive() active=false} ise form submit 422 döndürür.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_mail_account_id", nullable = false)
  private MailAccount senderMailAccount;

  /**
   * Form submit sonrasi bildirim alici adres (coklu alici icin virgulle ayrilabilir).
   */
  @Column(name = "recipient_email", nullable = false, length = 1000)
  private String recipientEmail;

  /**
   * Opsiyonel özel konu; bos ise varsayilan ("Yeni form gonderimi: {title}") kullanilir.
   */
  @Column(name = "notification_subject", length = 255)
  private String notificationSubject;

  /**
   * Bildirim acik/kapali (varsayilan: true).
   * false ise form submit yine basarili olur ama mail gonderilmez.
   */
  @Column(name = "notification_enabled", nullable = false)
  private Boolean notificationEnabled = true;
}

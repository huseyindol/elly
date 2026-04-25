package com.cms.entity;

import java.util.Date;

import com.cms.enums.EmailStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "email_logs", indexes = {
    @Index(name = "idx_email_status", columnList = "status"),
    @Index(name = "idx_email_status_created", columnList = "status, createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog extends BaseEntity {

  @Column(nullable = false, length = 1000)
  private String recipient;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false)
  private String templateName;

  @Column(columnDefinition = "TEXT")
  private String payloadJson;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmailStatus status;

  private int retryCount;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  private Date sentAt;

  /**
   * Bu maili göndermek için kullanılacak hesap.
   * NULL ise {@link com.cms.service.IMailAccountService#getDefaultEntity()} devreye girer.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mail_account_id")
  private MailAccount mailAccount;
}

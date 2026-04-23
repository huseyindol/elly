package com.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "email_templates",
    indexes = {
        @Index(name = "idx_email_templates_key", columnList = "template_key"),
        @Index(name = "idx_email_templates_active", columnList = "active")
    },
    uniqueConstraints = @UniqueConstraint(
        name = "uq_email_templates_tenant_key",
        columnNames = {"tenant_id", "template_key"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate extends BaseEntity {

  /** null = global tüm tenantlar için geçerli; değer var = tenant-specific override */
  @Column(name = "tenant_id", length = 64)
  private String tenantId;

  @Column(name = "template_key", nullable = false, length = 100)
  private String templateKey;

  /** Thymeleaf inline expression destekler: "Hoşgeldin [[${userName}]]" */
  @Column(name = "subject", nullable = false, length = 255)
  private String subject;

  @Column(name = "html_body", nullable = false, columnDefinition = "TEXT")
  private String htmlBody;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Column(name = "version", nullable = false)
  private Integer version = 1;

  @Version
  @Column(name = "optimistic_lock_version", nullable = false)
  private Long optimisticLockVersion = 0L;
}

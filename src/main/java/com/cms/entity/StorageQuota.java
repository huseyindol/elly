package com.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

/**
 * Tenant başına depolama kotası — HER tenant DB'sinde (basedb + tenant1 + tenant2) tutulur.
 * Enforcement upload anındaki (tenant) context'te çalıştığı için cross-DB / OSIV sorunu yok.
 * tenant_id = o context (ör. "tenant1" / "basedb"); pratikte her DB'de tek satır.
 */
@Entity
@Table(name = "storage_quota")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageQuota {

  @Id
  @Column(name = "tenant_id", length = 64)
  private String tenantId;

  /** Tenant limiti (byte). null/0 → config varsayılanı ({@code app.storage.default-quota-bytes}). */
  @Column(name = "limit_bytes")
  private Long limitBytes;

  @Column(name = "used_bytes", nullable = false)
  @Builder.Default
  private long usedBytes = 0L;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Date updatedAt;
}

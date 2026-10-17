package com.cms.entity;

import java.util.Date;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

/**
 * Tenant başına AI kullanım kotası (Aylık Token Limiti + Günlük İstek Limiti).
 * Her tenant DB'sinde tutulur; null/0 değerlerde uygulama varsayılanları kullanılır.
 */
@Entity
@Table(name = "ai_quota")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiQuota {

  @Id
  @Column(name = "tenant_id", length = 64)
  private String tenantId;

  /** Aylık toplam token limiti. null/0 → varsayılan (1.000.000 token). */
  @Column(name = "monthly_token_limit")
  private Long monthlyTokenLimit;

  /** Günlük başarılı AI çağrı (istek) limiti. null/0 → varsayılan (250 istek). */
  @Column(name = "daily_request_limit")
  private Long dailyRequestLimit;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Date updatedAt;
}

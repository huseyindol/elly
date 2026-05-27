package com.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;

/**
 * Tenant DB'sinde tutulan ziyaretçi kimliği. İki tipte olabilir:
 * <ul>
 *   <li><b>Kayıtlı tenant user'ı</b> → {@code tenantUserId} dolu, {@code sessionToken} null.
 *       Mevcut tenant user table'ındaki user'a bağlanır.</li>
 *   <li><b>Anonim ziyaretçi</b> → {@code sessionToken} dolu (cookie), {@code tenantUserId} null.
 *       MVP'de bu destek opsiyonel; ileride aktif edilir.</li>
 * </ul>
 *
 * DB CHECK constraint: en az biri dolu olmalı.
 */
@Entity
@Table(name = "visitor_identities", indexes = {
    @Index(name = "idx_visitor_identities_tenant_user", columnList = "tenant_user_id"),
    @Index(name = "idx_visitor_identities_session", columnList = "session_token")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class VisitorIdentity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Kayıtlı tenant user'ının id'si (tenant DB users.id). Anonim ziyaretçide null. */
  @Column(name = "tenant_user_id")
  private Long tenantUserId;

  /** Anonim ziyaretçi cookie token'ı. Kayıtlı user'da null. */
  @Column(name = "session_token", unique = true)
  private UUID sessionToken;

  @Column(name = "display_name", nullable = false, length = 80)
  private String displayName;

  @Column(name = "email", length = 255)
  private String email;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Date createdAt;

  @Column(name = "last_seen_at", nullable = false)
  private Date lastSeenAt = new Date();
}

package com.cms.entity;

import java.util.Date;
import java.util.UUID;

import com.cms.enums.CallStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebRTC görüntülü çağrı metadata kaydı (her tenant DB'sinde). Video kaydı DEĞİL —
 * yalnız kim/ne zaman/durum. {@code id} = uygulama tarafından üretilen callId (registry
 * ile aynı UUID); DB üretmez, çağrı yaratılırken set edilir.
 */
@Entity
@Table(name = "call_logs", indexes = {
    @Index(name = "idx_call_logs_status", columnList = "status"),
    @Index(name = "idx_call_logs_started_at", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallLog {

  /** callId — CallSessionService üretir (DB @GeneratedValue DEĞİL). */
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  /** Arayan kullanıcı id (tenant DB users.id). Tenant-site guest yoksa da tenant kullanıcısı → dolu. */
  @Column(name = "caller_user_id")
  private Long callerUserId;

  @Column(name = "caller_display_name", length = 100)
  private String callerDisplayName;

  /** Cevaplayan/hedef kullanıcı id. Ring-all'da cevaplanınca set edilir; cevapsızsa null. */
  @Column(name = "callee_user_id")
  private Long calleeUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private CallStatus status;

  @Column(name = "started_at", nullable = false)
  private Date startedAt;

  @Column(name = "answered_at")
  private Date answeredAt;

  @Column(name = "ended_at")
  private Date endedAt;
}

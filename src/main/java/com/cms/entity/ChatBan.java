package com.cms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;

/**
 * TC (tenant chat) ban kaydı: bir grupta bir guest (session_id) veya kayıtlı visitor
 * (visitor_id) yazma yetkisini kaldırır. Banlı kişi mesajları OKUYABİLİR ama YAZAMAZ —
 * gönderim service katmanında reddedilir (CHAT_BANNED). Tam olarak bir hedef set edilir.
 */
@Entity
@Table(name = "chat_bans", indexes = {
    @Index(name = "idx_chat_bans_group_session", columnList = "group_id, session_id"),
    @Index(name = "idx_chat_bans_group_visitor", columnList = "group_id, visitor_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBan {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  /** Guest ban hedefi (anonim ziyaretçi sessionId'si). visitor_id ile birlikte set EDİLMEZ. */
  @Column(name = "session_id")
  private UUID sessionId;

  /** Kayıtlı visitor ban hedefi (visitor_identities.id). session_id ile birlikte set EDİLMEZ. */
  @Column(name = "visitor_id")
  private Long visitorId;

  @Column(name = "banned_by_user_id", nullable = false)
  private Long bannedByUserId;

  @Column(name = "banned_by_username", length = 100)
  private String bannedByUsername;

  @Column(name = "reason", length = 300)
  private String reason;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Date createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Date updatedAt;
}

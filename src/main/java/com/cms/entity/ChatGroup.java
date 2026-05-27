package com.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "chat_groups", indexes = {
    @Index(name = "idx_chat_groups_created_by", columnList = "created_by"),
    @Index(name = "idx_chat_groups_tenant", columnList = "tenant_id"),
    @Index(name = "idx_chat_groups_visitor", columnList = "visitor_access")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "name", length = 100)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 10)
  private ChatGroupType type;

  @Column(name = "created_by", nullable = false)
  private Long createdBy;

  // 1=VIEWER(public), 2=EDITOR+, 3=ADMIN+, 4=SUPER_ADMIN(private)
  @Column(name = "visibility_level", nullable = false)
  private int visibilityLevel = 1;

  /**
   * Tenant Chat (TC) için: hangi tenant'a ait (tenant DB'sinde). NULL ise basedb'deki
   * mevcut admin chat (AC) kayıtlarıdır. Bu alan denormalize olabilir gibi durur fakat
   * group'un hangi DB'de tutulduğu bilgisini servis katmanının yan veriden hızlıca
   * elde edebilmesi için tutulur (örn. çapraz DB lookup yapmadan).
   */
  @Column(name = "tenant_id", length = 64)
  private String tenantId;

  /**
   * Visitor erişimi açık mı? TRUE ise website ziyaretçileri (Z) bu group'u listeleyebilir
   * ve {@code /api/v1/public/{tenantId}/chat/...} endpoint'leri üzerinden mesaj yazabilir.
   * Default FALSE — mevcut AC kayıtları etkilenmez.
   */
  @Column(name = "visitor_access", nullable = false)
  private boolean visitorAccess = false;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Date createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Date updatedAt;
}

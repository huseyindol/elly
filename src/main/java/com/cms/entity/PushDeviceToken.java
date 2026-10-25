package com.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "push_device_tokens", indexes = {
    @Index(name = "idx_push_device_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_push_device_tokens_token", columnList = "token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushDeviceToken extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "token", nullable = false, length = 512)
  private String token;

  @Column(name = "platform", nullable = false, length = 32)
  private String platform;

  @Column(name = "voip_token", length = 512)
  private String voipToken;

  @Column(name = "device_name", length = 120)
  private String deviceName;
}

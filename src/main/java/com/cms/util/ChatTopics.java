package com.cms.util;

import com.cms.entity.ChatGroup;

import java.util.UUID;

/**
 * Chat WebSocket topic isimlerinin üretimini tek noktada tutar.
 *
 * <p>AC=TC tek model: topic'ler daima tenant-aware —
 * {@code /topic/tenant/{tenantId}/group/{groupId}}. Legacy {@code /topic/group/{id}}
 * formatı yalnız tenant hiçbir şekilde çözülemezse üretilir (normalde olmamalı).
 */
public final class ChatTopics {

  private ChatTopics() {
    // utility
  }

  /**
   * AC=TC: entity'de tenant_id NULL kalmış eski (basedb dönemi) satırlar için mevcut
   * TenantContext'e düşülür — broadcast, panel'in abone olduğu tenant-aware topic'e
   * gitsin. (Broadcast eden servisler zaten grubun DB'sine context switch etmiş olur.)
   */
  private static String tenantOf(ChatGroup group) {
    String tid = group.getTenantId();
    return (tid == null || tid.isBlank()) ? com.cms.config.TenantContext.getTenantId() : tid;
  }

  /** Group entity'sinden mesaj topic'i türetir (tenant NULL fallback'li). */
  public static String messageTopic(ChatGroup group) {
    return messageTopic(tenantOf(group), group.getId());
  }

  /** Group entity'sinden ban topic'i türetir (tenant NULL fallback'li). */
  public static String banTopic(ChatGroup group) {
    return banTopic(tenantOf(group), group.getId());
  }

  public static String messageTopic(String tenantId, UUID groupId) {
    if (tenantId == null || tenantId.isBlank()) {
      return "/topic/group/" + groupId;
    }
    return "/topic/tenant/" + tenantId + "/group/" + groupId;
  }

  public static String typingTopic(ChatGroup group) {
    return messageTopic(group) + "/typing";
  }

  public static String readTopic(ChatGroup group) {
    return messageTopic(group) + "/read";
  }

  public static String typingTopic(String tenantId, UUID groupId) {
    return messageTopic(tenantId, groupId) + "/typing";
  }

  public static String readTopic(String tenantId, UUID groupId) {
    return messageTopic(tenantId, groupId) + "/read";
  }

  /** Ban/unban olayları (TC). Guest kendi sessionId'siyle eşleşince input'unu kilitler. */
  public static String banTopic(String tenantId, UUID groupId) {
    return messageTopic(tenantId, groupId) + "/bans";
  }

  /** Kullanıcı gruba davet edildiğinde kişisel topic (legacy — plain DtoChatGroup). */
  public static String userGroupJoinedTopic(Long userId) {
    return "/topic/user/" + userId + "/groups/joined";
  }

  /** Kullanıcı gruptan çıkarıldığında kişisel topic. */
  public static String userGroupRemovedTopic(Long userId) {
    return "/topic/user/" + userId + "/groups/removed";
  }

  /** JOINED / REMOVED membership event'leri (banner metni dahil). */
  public static String userMembershipTopic(Long userId) {
    return "/topic/user/" + userId + "/membership";
  }
}

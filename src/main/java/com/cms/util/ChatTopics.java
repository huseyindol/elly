package com.cms.util;

import com.cms.entity.ChatGroup;

import java.util.UUID;

/**
 * Chat WebSocket topic isimlerinin üretimini tek noktada tutar.
 *
 * <ul>
 *   <li>Admin chat (AC, basedb) → {@code /topic/group/{groupId}} (mevcut format)</li>
 *   <li>Tenant chat (TC, tenant DB) → {@code /topic/tenant/{tenantId}/group/{groupId}}</li>
 * </ul>
 *
 * Bu sayede broadcast eden tarafların if/else duplikasyonu önlenir.
 */
public final class ChatTopics {

  private ChatTopics() {
    // utility
  }

  /** Group entity'sinden mesaj topic'i türetir. */
  public static String messageTopic(ChatGroup group) {
    return messageTopic(group.getTenantId(), group.getId());
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

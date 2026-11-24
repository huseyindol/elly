package com.cms.service;

import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatMembershipEvent;
import com.cms.repository.UserRepository;
import com.cms.util.ChatTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Grup davet / çıkarma WebSocket bildirimleri.
 * Hem kullanıcıya özel topic ({@code /topic/tenant/{tid}/user/{id}/...}) hem de
 * Spring user queue ({@code /user/queue/...}) kullanır — panel hangisini
 * dinliyorsa alsın.
 *
 * <p>Tenant açıkça verilir ({@code tenantId}, ya da JOINED'de grubun tenant'ı): bildirim o tenant'taki
 * oturumlara gider. Kullanıcı numaraları ve
 * adları tenant'a özeldir; tenant'sız teslimat başka tenant'taki aynı numaralı/adlı kullanıcıya düşerdi.
 * Tenant, isteğin geçerli tenant'ıdır ve servis çağrısından ÖNCE yakalanır — grup yönlendiricisi servicing sırasında
 * bağlamı değiştirebilir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMembershipNotifier {

  private final SimpMessagingTemplate messagingTemplate;
  private final UserRepository userRepository;

  public void notifyJoined(String tenantId, Long userId, DtoChatGroup group, DtoChatMembershipEvent event) {
    // Tek kaynak: grubun tenant'ı (istek tenant'ı yalnız grubun tenant'ı eksikse yedek).
    String tenant = notBlank(group.getTenantId()) ? group.getTenantId() : tenantId;
    messagingTemplate.convertAndSend(ChatTopics.userGroupJoinedTopic(tenant, userId), group);
    messagingTemplate.convertAndSend(ChatTopics.userMembershipTopic(tenant, userId), event);
    // GEÇİŞ: eski tenant'sız konular — istemciler tenant'lı konuya geçince kaldırılır.
    messagingTemplate.convertAndSend(ChatTopics.userGroupJoinedTopic(userId), group);
    messagingTemplate.convertAndSend(ChatTopics.userMembershipTopic(userId), event);

    userRepository.findById(userId).ifPresentOrElse(user -> {
      String userKey = ChatTopics.userKey(tenant, user.getUsername());
      messagingTemplate.convertAndSendToUser(userKey, "/queue/groups/joined", group);
      messagingTemplate.convertAndSendToUser(userKey, "/queue/membership", event);
      log.info("Chat JOINED → userId={} username={} groupId={}", userId, user.getUsername(), group.getId());
    }, () -> log.warn("Chat JOINED → userId={} not found, topic-only delivery", userId));
  }

  public void notifyRemoved(String tenantId, Long userId, UUID groupId, DtoChatMembershipEvent event) {
    messagingTemplate.convertAndSend(ChatTopics.userGroupRemovedTopic(tenantId, userId), event);
    messagingTemplate.convertAndSend(ChatTopics.userMembershipTopic(tenantId, userId), event);
    // GEÇİŞ: eski tenant'sız konular — istemciler tenant'lı konuya geçince kaldırılır.
    messagingTemplate.convertAndSend(ChatTopics.userGroupRemovedTopic(userId), event);
    messagingTemplate.convertAndSend(ChatTopics.userMembershipTopic(userId), event);

    userRepository.findById(userId).ifPresentOrElse(user -> {
      String userKey = ChatTopics.userKey(tenantId, user.getUsername());
      messagingTemplate.convertAndSendToUser(userKey, "/queue/groups/removed", event);
      messagingTemplate.convertAndSendToUser(userKey, "/queue/membership", event);
      log.info("Chat REMOVED → userId={} username={} groupId={}", userId, user.getUsername(), groupId);
    }, () -> log.warn("Chat REMOVED → userId={} not found, topic-only delivery", userId));
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}

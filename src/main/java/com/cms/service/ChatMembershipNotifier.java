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
 * Hem public topic ({@code /topic/user/{id}/...}) hem de
 * Spring user queue ({@code /user/queue/...}) kullanır — panel hangisini
 * dinliyorsa alsın.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMembershipNotifier {

  private final SimpMessagingTemplate messagingTemplate;
  private final UserRepository userRepository;

  public void notifyJoined(Long userId, DtoChatGroup group, DtoChatMembershipEvent event) {
    messagingTemplate.convertAndSend(ChatTopics.userGroupJoinedTopic(userId), group);
    messagingTemplate.convertAndSend(ChatTopics.userMembershipTopic(userId), event);

    userRepository.findById(userId).ifPresentOrElse(user -> {
      messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/groups/joined", group);
      messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/membership", event);
      log.info("Chat JOINED → userId={} username={} groupId={}", userId, user.getUsername(), group.getId());
    }, () -> log.warn("Chat JOINED → userId={} not found in basedb, topic-only delivery", userId));
  }

  public void notifyRemoved(Long userId, UUID groupId, DtoChatMembershipEvent event) {
    messagingTemplate.convertAndSend(ChatTopics.userGroupRemovedTopic(userId), event);
    messagingTemplate.convertAndSend(ChatTopics.userMembershipTopic(userId), event);

    userRepository.findById(userId).ifPresentOrElse(user -> {
      messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/groups/removed", event);
      messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/membership", event);
      log.info("Chat REMOVED → userId={} username={} groupId={}", userId, user.getUsername(), groupId);
    }, () -> log.warn("Chat REMOVED → userId={} not found in basedb, topic-only delivery", userId));
  }
}

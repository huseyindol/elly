package com.cms.controller.impl;

import com.cms.dto.DtoChatMessage;
import com.cms.dto.DtoChatMessageSend;
import com.cms.dto.DtoChatPresence;
import com.cms.dto.DtoChatRead;
import com.cms.dto.DtoChatTyping;
import com.cms.service.IChatGroupService;
import com.cms.service.IChatMessageService;
import com.cms.service.ChatPresenceService;
import com.cms.service.ChatTypingService;
import com.cms.service.ChatRateLimitService;
import com.cms.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

  private final IChatMessageService messageService;
  private final IChatGroupService groupService;
  private final ChatPresenceService presenceService;
  private final ChatTypingService typingService;
  private final ChatRateLimitService rateLimitService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat/{groupId}/send")
  public void sendMessage(@DestinationVariable UUID groupId,
      @Payload DtoChatMessageSend payload,
      SimpMessageHeaderAccessor headerAccessor) {
    TenantContext.setTenantId(null);
    try {
      Long userId = resolveUserId(headerAccessor);
      rateLimitService.checkRateLimit(userId);
      DtoChatMessage saved = messageService.saveMessage(groupId, userId, payload);
      messagingTemplate.convertAndSend("/topic/group/" + groupId, saved);
      log.debug("Message sent to group {} by user {}", groupId, userId);
    } finally {
      TenantContext.clear();
    }
  }

  @MessageMapping("/chat/{groupId}/typing")
  public void typing(@DestinationVariable UUID groupId,
      SimpMessageHeaderAccessor headerAccessor) {
    TenantContext.setTenantId(null);
    try {
      Long userId = resolveUserId(headerAccessor);
      String username = resolveUsername(headerAccessor);
      typingService.setTyping(groupId, userId);
      DtoChatTyping event = new DtoChatTyping(groupId, userId, username, true);
      messagingTemplate.convertAndSend("/topic/group/" + groupId + "/typing", event);
    } finally {
      TenantContext.clear();
    }
  }

  @MessageMapping("/chat/{groupId}/read")
  public void markRead(@DestinationVariable UUID groupId,
      SimpMessageHeaderAccessor headerAccessor) {
    TenantContext.setTenantId(null);
    try {
      Long userId = resolveUserId(headerAccessor);
      String username = resolveUsername(headerAccessor);
      messageService.markGroupAsRead(groupId, userId);
      DtoChatRead event = new DtoChatRead(groupId, userId, username);
      messagingTemplate.convertAndSend("/topic/group/" + groupId + "/read", event);
    } finally {
      TenantContext.clear();
    }
  }

  @EventListener
  public void handleSessionConnect(SessionConnectEvent event) {
    TenantContext.setTenantId(null);
    try {
      SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
      Long userId = resolveUserIdFromSessionAttrs(accessor);
      String username = resolveUsernameFromSessionAttrs(accessor);
      if (userId == null) return;
      boolean firstSession = presenceService.userConnected(userId);
      if (firstSession) {
        DtoChatPresence presence = new DtoChatPresence(userId, username, "ONLINE");
        messagingTemplate.convertAndSend("/topic/presence", presence);
        log.debug("User {} connected → ONLINE", username);
      }
    } finally {
      TenantContext.clear();
    }
  }

  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    TenantContext.setTenantId(null);
    try {
      SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
      Long userId = resolveUserIdFromSessionAttrs(accessor);
      String username = resolveUsernameFromSessionAttrs(accessor);
      if (userId == null) return;
      boolean lastSession = presenceService.userDisconnected(userId);
      if (lastSession) {
        DtoChatPresence presence = new DtoChatPresence(userId, username, "OFFLINE");
        messagingTemplate.convertAndSend("/topic/presence", presence);
        log.debug("User {} disconnected → OFFLINE", username);
      }
    } finally {
      TenantContext.clear();
    }
  }

  private Long resolveUserId(SimpMessageHeaderAccessor accessor) {
    Map<String, Object> attrs = accessor.getSessionAttributes();
    if (attrs == null) throw new com.cms.exception.UnauthorizedException("No session attributes");
    Object userId = attrs.get("userId");
    if (userId == null) throw new com.cms.exception.UnauthorizedException("userId not found in session");
    return (userId instanceof Long l) ? l : Long.valueOf(userId.toString());
  }

  private String resolveUsername(SimpMessageHeaderAccessor accessor) {
    Map<String, Object> attrs = accessor.getSessionAttributes();
    if (attrs == null) return "unknown";
    Object username = attrs.get("username");
    return username != null ? username.toString() : "unknown";
  }

  private Long resolveUserIdFromSessionAttrs(SimpMessageHeaderAccessor accessor) {
    try {
      Map<String, Object> attrs = accessor.getSessionAttributes();
      if (attrs == null) return null;
      Object userId = attrs.get("userId");
      if (userId == null) return null;
      return (userId instanceof Long l) ? l : Long.valueOf(userId.toString());
    } catch (Exception e) {
      return null;
    }
  }

  private String resolveUsernameFromSessionAttrs(SimpMessageHeaderAccessor accessor) {
    try {
      Map<String, Object> attrs = accessor.getSessionAttributes();
      if (attrs == null) return "unknown";
      Object username = attrs.get("username");
      return username != null ? username.toString() : "unknown";
    } catch (Exception e) {
      return "unknown";
    }
  }
}

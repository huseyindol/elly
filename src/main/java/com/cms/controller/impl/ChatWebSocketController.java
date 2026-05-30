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
import com.cms.exception.ForbiddenException;
import com.cms.util.ChatTopics;
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

  /**
   * AC mesajı: {@code /app/chat/{groupId}/send}.
   * TenantContext basedb'ye sabitlenir (mevcut davranış).
   */
  @MessageMapping("/chat/{groupId}/send")
  public void sendMessage(@DestinationVariable UUID groupId,
      @Payload DtoChatMessageSend payload,
      SimpMessageHeaderAccessor headerAccessor) {
    Long userId = resolveUserId(headerAccessor);
    rateLimitService.checkRateLimit(userId);
    String sessionTenant = resolveOptionalSessionTenant(headerAccessor);

    TenantContext.setTenantId(null);
    String activeTenant = null;
    try {
      DtoChatMessage saved;
      try {
        saved = messageService.saveMessage(groupId, userId, payload);
      } catch (ForbiddenException e) {
        // Eski AC grupları JWT tenantId varken yanlışlıkla tenant DB'ye yazılmış olabilir
        if (sessionTenant == null || sessionTenant.isBlank()) {
          throw e;
        }
        log.debug("AC send denied on basedb for group {}, retrying tenant {}", groupId, sessionTenant);
        TenantContext.setTenantId(sessionTenant);
        activeTenant = sessionTenant;
        saved = messageService.saveMessage(groupId, userId, payload);
      }
      messagingTemplate.convertAndSend(ChatTopics.messageTopic(activeTenant, groupId), saved);
      log.debug("AC message sent to group {} by user {} (tenant={})", groupId, userId, activeTenant);
    } finally {
      TenantContext.clear();
    }
  }

  /**
   * TC mesajı: {@code /app/tenant-chat/{tenantId}/{groupId}/send}.
   * TenantContext destination'daki {@code tenantId}'ye set edilir; mesaj o tenant
   * DB'sine yazılır ve broadcast'i tenant-aware topic'e gider.
   */
  @MessageMapping("/tenant-chat/{tenantId}/{groupId}/send")
  public void sendTenantMessage(@DestinationVariable String tenantId,
      @DestinationVariable UUID groupId,
      @Payload DtoChatMessageSend payload,
      SimpMessageHeaderAccessor headerAccessor) {
    TenantContext.setTenantId(tenantId);
    try {
      DtoChatMessage saved;
      if (isGuestSession(headerAccessor)) {
        // Anonim guest — userId yok; sessionId + displayName ile yaz
        String sessionId = resolveSessionId(headerAccessor);
        String displayName = resolveUsername(headerAccessor);
        rateLimitService.checkRateLimitForGuest(sessionId);
        saved = messageService.saveGuestMessage(groupId, sessionId, displayName, payload);
        log.debug("TC guest message sent to tenant={} group={} session={}", tenantId, groupId, sessionId);
      } else {
        Long userId = resolveUserId(headerAccessor);
        rateLimitService.checkRateLimit(userId);
        saved = messageService.saveMessage(groupId, userId, payload);
        log.debug("TC message sent to tenant={} group={} by user={}", tenantId, groupId, userId);
      }
      messagingTemplate.convertAndSend(ChatTopics.messageTopic(tenantId, groupId), saved);
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
      DtoChatTyping event = new DtoChatTyping(groupId, userId, username, true, null);
      messagingTemplate.convertAndSend(ChatTopics.typingTopic(null, groupId), event);
    } finally {
      TenantContext.clear();
    }
  }

  @MessageMapping("/tenant-chat/{tenantId}/{groupId}/typing")
  public void typingTenant(@DestinationVariable String tenantId,
      @DestinationVariable UUID groupId,
      SimpMessageHeaderAccessor headerAccessor) {
    TenantContext.setTenantId(tenantId);
    try {
      DtoChatTyping event;
      if (isGuestSession(headerAccessor)) {
        // Anonim guest — userId yok; sessionId + displayName ile yayınla (alıcı kendi typing'ini ayırır)
        String sessionId = resolveSessionId(headerAccessor);
        String displayName = resolveUsername(headerAccessor);
        event = new DtoChatTyping(groupId, null, displayName, true, sessionId);
      } else {
        Long userId = resolveUserId(headerAccessor);
        String username = resolveUsername(headerAccessor);
        typingService.setTyping(groupId, userId);
        event = new DtoChatTyping(groupId, userId, username, true, null);
      }
      messagingTemplate.convertAndSend(ChatTopics.typingTopic(tenantId, groupId), event);
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
      messagingTemplate.convertAndSend(ChatTopics.readTopic(null, groupId), event);
    } finally {
      TenantContext.clear();
    }
  }

  @MessageMapping("/tenant-chat/{tenantId}/{groupId}/read")
  public void markReadTenant(@DestinationVariable String tenantId,
      @DestinationVariable UUID groupId,
      SimpMessageHeaderAccessor headerAccessor) {
    // Guest oturumlarında read-receipt takibi yok (chat_message_reads userId bazlı) → no-op.
    if (isGuestSession(headerAccessor)) {
      return;
    }
    TenantContext.setTenantId(tenantId);
    try {
      Long userId = resolveUserId(headerAccessor);
      String username = resolveUsername(headerAccessor);
      messageService.markGroupAsRead(groupId, userId);
      DtoChatRead event = new DtoChatRead(groupId, userId, username);
      messagingTemplate.convertAndSend(ChatTopics.readTopic(tenantId, groupId), event);
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

  /** Session loginSource=website ise anonim guest oturumu. */
  private boolean isGuestSession(SimpMessageHeaderAccessor accessor) {
    Map<String, Object> attrs = accessor.getSessionAttributes();
    if (attrs == null) return false;
    Object loginSource = attrs.get("loginSource");
    return loginSource != null && "website".equals(loginSource.toString());
  }

  private String resolveSessionId(SimpMessageHeaderAccessor accessor) {
    Map<String, Object> attrs = accessor.getSessionAttributes();
    if (attrs == null) throw new com.cms.exception.UnauthorizedException("No session attributes");
    Object sessionId = attrs.get("sessionId");
    if (sessionId == null) throw new com.cms.exception.UnauthorizedException("sessionId not found in session");
    return sessionId.toString();
  }

  private String resolveOptionalSessionTenant(SimpMessageHeaderAccessor accessor) {
    Map<String, Object> attrs = accessor.getSessionAttributes();
    if (attrs == null) return null;
    Object tenantId = attrs.get("tenantId");
    if (tenantId == null) return null;
    String value = tenantId.toString().trim();
    return value.isEmpty() ? null : value;
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

package com.cms.config;

import com.cms.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtUtil jwtUtil;
  private final UserAuthCacheService userAuthCacheService;

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          String authHeader = accessor.getFirstNativeHeader("Authorization");
          if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new org.springframework.messaging.MessageDeliveryException(
                message, "Missing Authorization header in STOMP CONNECT");
          }
          try {
            String token = authHeader.substring(7);
            String loginSource = jwtUtil.extractLoginSource(token);

            boolean isAdmin = "admin".equals(loginSource);
            boolean isGuest = "website".equals(loginSource);

            if (!isAdmin && !isGuest) {
              throw new org.springframework.messaging.MessageDeliveryException(
                  message, "Chat requires admin or website login");
            }

            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();

            if (isAdmin) {
              String username = jwtUtil.extractUsername(token);
              Long userId = jwtUtil.extractUserId(token);

              // H4: Token version kontrolü — iptal edilmiş admin token'larla bağlantı kurulamaz
              Long jwtTokenVersion = jwtUtil.extractTokenVersion(token);
              com.cms.dto.CachedUserDetails cachedUser = userAuthCacheService.getUserFromCache(username);
              if (cachedUser != null && cachedUser.getTokenVersion() != null
                  && jwtTokenVersion != null
                  && !jwtTokenVersion.equals(cachedUser.getTokenVersion())) {
                throw new org.springframework.messaging.MessageDeliveryException(
                    message, "Token revoked — please login again");
              }

              UsernamePasswordAuthenticationToken auth =
                  new UsernamePasswordAuthenticationToken(username, null,
                      List.of(new SimpleGrantedAuthority("chat:read"),
                              new SimpleGrantedAuthority("chat:manage")));
              accessor.setUser(auth);

              if (sessionAttrs != null) {
                sessionAttrs.put("userId", userId);
                sessionAttrs.put("username", username);
                sessionAttrs.put("tenantId", null); // admin → basedb
              }

              // Admin her zaman basedb
              TenantContext.setTenantId(null);
              log.debug("WebSocket admin authenticated: user={}, userId={}", username, userId);

            } else {
              // Guest (website) bağlantısı
              String sessionId = jwtUtil.extractSessionId(token);
              String displayName = jwtUtil.extractDisplayName(token);
              String guestTenantId = jwtUtil.extractTenantId(token); // null ise basedb

              UsernamePasswordAuthenticationToken auth =
                  new UsernamePasswordAuthenticationToken(sessionId, null,
                      List.of(new SimpleGrantedAuthority("chat:read")));
              accessor.setUser(auth);

              if (sessionAttrs != null) {
                sessionAttrs.put("userId", null);
                sessionAttrs.put("sessionId", sessionId);
                sessionAttrs.put("displayName", displayName);
                sessionAttrs.put("username", displayName);
                sessionAttrs.put("tenantId", guestTenantId); // null → basedb, değer → tenant DB
              }

              // Guest: tenantId varsa o DB'ye, yoksa basedb
              TenantContext.setTenantId(guestTenantId);
              log.debug("WebSocket guest connected: sessionId={}, displayName={}, tenantId={}",
                  sessionId, displayName, guestTenantId);
            }

            log.debug("WebSocket CONNECT completed. loginSource={}", loginSource);
          } catch (org.springframework.messaging.MessageDeliveryException e) {
            throw e;
          } catch (Exception e) {
            throw new org.springframework.messaging.MessageDeliveryException(
                message, "Invalid or expired token: " + e.getMessage());
          }
        }
        return message;
      }
    });
  }
}

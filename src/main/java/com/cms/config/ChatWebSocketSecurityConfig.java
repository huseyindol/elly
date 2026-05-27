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
            if (!"admin".equals(loginSource)) {
              throw new org.springframework.messaging.MessageDeliveryException(
                  message, "Chat requires admin panel login (loginSource=admin)");
            }

            String username = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractUserId(token);

            // H4: Token version kontrolü — iptal edilmiş token'larla WebSocket bağlantısı kurulamaz
            // Yalnızca admin loginSource için uygulanır
            Long jwtTokenVersion = jwtUtil.extractTokenVersion(token);
            com.cms.dto.CachedUserDetails cachedUser = userAuthCacheService.getUserFromCache(username);
            if (cachedUser != null && cachedUser.getTokenVersion() != null
                && jwtTokenVersion != null
                && !jwtTokenVersion.equals(cachedUser.getTokenVersion())) {
              throw new org.springframework.messaging.MessageDeliveryException(
                  message, "Token revoked — please login again");
            }
            // cachedUser null ise (Redis down veya cache miss) → fail-open, JWT'nin kendi TTL'si koruyor

            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("chat:read"),
                new SimpleGrantedAuthority("chat:manage"));

            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
            accessor.setUser(auth);

            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            if (sessionAttrs != null) {
              sessionAttrs.put("userId", userId);
              sessionAttrs.put("username", username);
            }

            // Chat her zaman basedb'de çalışır
            TenantContext.setTenantId(null);

            log.debug("WebSocket authenticated: user={}, userId={}", username, userId);
          } catch (org.springframework.messaging.MessageDeliveryException e) {
            throw e;
          } catch (Exception e) {
            throw new org.springframework.messaging.MessageDeliveryException(
                message, "Invalid JWT token: " + e.getMessage());
          }
        }
        return message;
      }
    });
  }
}

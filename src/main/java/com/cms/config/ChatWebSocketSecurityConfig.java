package com.cms.config;

import com.cms.dto.CachedUserDetails;
import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.repository.UserRepository;
import com.cms.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final RedisTemplate<String, Object> redisTemplate;

  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          // H4: authenticateConnect() içindeki jwtUtil.validateToken(token, username, tokenVersion)
          // çağrısı zaten token version kontrolü yapıyor — iptal edilmiş token'lar reject edilir.
          authenticateConnect(accessor, message);
        } else if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
          // SEND/SUBSCRIBE vb. — REST ile aynı SecurityContext (ROLE_* dahil)
          SecurityContextHolder.getContext().setAuthentication(auth);
        }

        return message;
      }

      @Override
      public void afterSendCompletion(
          Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && !StompCommand.CONNECT.equals(accessor.getCommand())) {
          SecurityContextHolder.clearContext();
        }
      }
    });
  }

  private void authenticateConnect(StompHeaderAccessor accessor, Message<?> message) {
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new org.springframework.messaging.MessageDeliveryException(
          message, "Missing Authorization header in STOMP CONNECT");
    }

    try {
      String token = authHeader.substring(7);
      String loginSource = jwtUtil.extractLoginSource(token);

      boolean isAdmin = "admin".equals(loginSource);
      boolean isTenant = "tenant".equals(loginSource);
      boolean isGuest = "website".equals(loginSource);
      if (!isAdmin && !isTenant && !isGuest) {
        throw new org.springframework.messaging.MessageDeliveryException(
            message, "Chat requires admin, tenant or guest login (loginSource=admin|tenant|website)");
      }

      // Guest (anonim website ziyaretçisi) — DB user'ı yok, token version yok.
      if (isGuest) {
        authenticateGuestConnect(accessor, message, token);
        return;
      }

      String username = jwtUtil.extractUsername(token);

      String tenantIdClaim = null;
      try {
        tenantIdClaim = jwtUtil.extractTenantId(token);
      } catch (Exception ignored) {
        // tenantId claim opsiyonel
      }

      // Tenant user kendi tenant DB'sinde yaşar → user'ı YÜKLEMEDEN ÖNCE context'i
      // o tenant'a kur (admin'i loadUserFromDbAndCache zaten basedb'ye yönlendirir).
      // Aksi halde tenant user basedb'de aranır ve bulunamaz → CONNECT reddedilir.
      String originalTenant = TenantContext.getTenantId();
      if (isTenant && tenantIdClaim != null && !tenantIdClaim.isBlank()) {
        TenantContext.setTenantId(tenantIdClaim);
      }
      CachedUserDetails cachedUser;
      try {
        cachedUser = resolveCachedUser(username, loginSource);
      } finally {
        TenantContext.setTenantId(originalTenant);
      }

      Long tokenVersion = cachedUser.getTokenVersion() != null ? cachedUser.getTokenVersion() : 0L;
      if (!jwtUtil.validateToken(token, cachedUser.getUsername(), tokenVersion)) {
        throw new org.springframework.messaging.MessageDeliveryException(
            message, "Invalid or expired JWT token");
      }

      List<GrantedAuthority> authorities = cachedUser.getAuthorities().stream()
          .map(SimpleGrantedAuthority::new)
          .collect(Collectors.toList());

      JwtAuthenticationFilter.CachedUserPrincipal principal =
          new JwtAuthenticationFilter.CachedUserPrincipal(cachedUser, authorities);
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(principal, null, authorities);
      accessor.setUser(auth);
      SecurityContextHolder.getContext().setAuthentication(auth);

      Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
      if (sessionAttrs != null) {
        sessionAttrs.put("userId", cachedUser.getId());
        sessionAttrs.put("username", cachedUser.getUsername());
        sessionAttrs.put("loginSource", loginSource);
        sessionAttrs.put("tenantId", tenantIdClaim);
      }

      TenantContext.clear();

      log.debug("WebSocket authenticated: user={}, userId={}, loginSource={}, tenantId={}, roles={}",
          cachedUser.getUsername(), cachedUser.getId(), loginSource, tenantIdClaim,
          cachedUser.getAuthorities().stream()
              .filter(a -> a.startsWith("ROLE_"))
              .collect(Collectors.toSet()));
    } catch (org.springframework.messaging.MessageDeliveryException e) {
      throw e;
    } catch (Exception e) {
      throw new org.springframework.messaging.MessageDeliveryException(
          message, "Invalid JWT token: " + e.getMessage());
    }
  }

  /**
   * Guest CONNECT: token type=guest doğrulanır; minimal principal + session attrs set edilir.
   * DB user'ı yok, token version kontrolü yok (guest hesabı yok). Sadece chat:guest authority verilir.
   */
  private void authenticateGuestConnect(StompHeaderAccessor accessor, Message<?> message, String token) {
    if (!Boolean.TRUE.equals(jwtUtil.validateGuestToken(token))) {
      throw new org.springframework.messaging.MessageDeliveryException(
          message, "Invalid or expired guest token");
    }
    String sessionId = jwtUtil.extractSessionId(token);
    String displayName = jwtUtil.extractDisplayName(token);
    String tenantIdClaim = jwtUtil.extractTenantId(token);

    if (sessionId == null || tenantIdClaim == null || tenantIdClaim.isBlank()) {
      throw new org.springframework.messaging.MessageDeliveryException(
          message, "Guest token missing sessionId/tenantId");
    }

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("chat:guest"));
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("guest:" + sessionId, null, authorities);
    accessor.setUser(auth);
    SecurityContextHolder.getContext().setAuthentication(auth);

    Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
    if (sessionAttrs != null) {
      sessionAttrs.put("sessionId", sessionId);
      sessionAttrs.put("username", displayName != null ? displayName : "guest");
      sessionAttrs.put("loginSource", "website");
      sessionAttrs.put("tenantId", tenantIdClaim);
    }

    TenantContext.clear();
    log.debug("WebSocket guest authenticated: sessionId={}, displayName={}, tenantId={}",
        sessionId, displayName, tenantIdClaim);
  }

  private CachedUserDetails resolveCachedUser(String username, String loginSource) {
    CachedUserDetails cached = getUserFromCache(username);
    if (cached != null) {
      return cached;
    }
    return loadUserFromDbAndCache(username, loginSource);
  }

  private CachedUserDetails getUserFromCache(String username) {
    try {
      String cacheKey = UserAuthCacheService.USER_CACHE_PREFIX + username;
      Object cached = redisTemplate.opsForValue().get(cacheKey);
      if (cached instanceof CachedUserDetails details) {
        return details;
      }
      if (cached != null) {
        return objectMapper.convertValue(cached, CachedUserDetails.class);
      }
    } catch (Exception e) {
      log.debug("WebSocket auth cache read failed (user: {}): {}", username, e.getMessage());
    }
    return null;
  }

  private CachedUserDetails loadUserFromDbAndCache(String username, String loginSource) {
    String originalTenant = TenantContext.getTenantId();
    try {
      if ("admin".equals(loginSource)) {
        TenantContext.setTenantId(defaultTenant);
      }

      User user = userRepository.findByUsername(username)
          .orElseThrow(() -> new org.springframework.security.core.userdetails
              .UsernameNotFoundException("User not found: " + username));

      Set<String> authorities = new HashSet<>();
      if (user.getRoles() != null) {
        for (Role role : user.getRoles()) {
          authorities.add("ROLE_" + role.getName());
          if (role.getPermissions() != null) {
            for (Permission permission : role.getPermissions()) {
              authorities.add(permission.getName());
            }
          }
        }
      }

      CachedUserDetails cached = CachedUserDetails.builder()
          .id(user.getId())
          .username(user.getUsername())
          .password(user.getPassword())
          .email(user.getEmail())
          .isActive(user.getIsActive())
          .tokenVersion(user.getTokenVersion())
          .authorities(authorities)
          .build();

      try {
        String cacheKey = UserAuthCacheService.USER_CACHE_PREFIX + username;
        redisTemplate.opsForValue().set(
            cacheKey, cached, UserAuthCacheService.USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
      } catch (Exception e) {
        log.debug("WebSocket auth cache write failed (user: {}): {}", username, e.getMessage());
      }

      return cached;
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }
}

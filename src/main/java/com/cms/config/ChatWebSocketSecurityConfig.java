package com.cms.config;

import com.cms.dto.CachedUserDetails;
import com.cms.entity.ChatGroup;
import com.cms.entity.ChatGroupType;
import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.repository.ChatGroupMemberRepository;
import com.cms.repository.ChatGroupRepository;
import com.cms.repository.UserRepository;
import com.cms.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

  private static final Pattern USER_TOPIC_PATTERN =
      Pattern.compile("^/topic/user/(\\d+)/(groups/joined|groups/removed|membership|rtc)$");
  private static final Pattern TENANT_GROUP_TOPIC_PATTERN =
      Pattern.compile("^/topic/tenant/([^/]+)/group/([0-9a-fA-F-]{36})(?:/(typing|read|bans))?$");
  private static final Pattern LEGACY_GROUP_TOPIC_PATTERN =
      Pattern.compile("^/topic/group/([0-9a-fA-F-]{36})(?:/(typing|read|bans))?$");
  private static final Pattern RTC_CLIENT_TOPIC_PATTERN =
      Pattern.compile("^/topic/rtc-client/([^/]+)/rtc$");

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final ChatGroupRepository chatGroupRepository;
  private final ChatGroupMemberRepository chatGroupMemberRepository;
  private final ObjectMapper objectMapper;
  private final RedisTemplate<String, Object> redisTemplate;

  // basedb kaldırıldı — WS CONNECT'te tenant daima token tenantId claim'inden çözülür.

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
          if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor, message);
          }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
          throw new org.springframework.messaging.MessageDeliveryException(
              message, "Unauthenticated STOMP SUBSCRIBE rejected");
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

      // Tenant zorunlu: token tenantId claim'i yoksa hangi DB belirsiz → reddet (basedb yok).
      String tenantIdClaim;
      try {
        tenantIdClaim = jwtUtil.extractTenantId(token);
      } catch (Exception e) {
        tenantIdClaim = null;
      }
      if (tenantIdClaim == null || tenantIdClaim.isBlank()) {
        throw new org.springframework.messaging.MessageDeliveryException(
            message, "Token tenant bilgisi icermiyor (TENANT_REQUIRED)");
      }

      // Hem admin hem tenant user kendi tenant DB'sinde → user'ı YÜKLEMEDEN ÖNCE
      // context'i o tenant'a kur (basedb yok).
      String originalTenant = TenantContext.getTenantId();
      TenantContext.setTenantId(tenantIdClaim);
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
      // basedb forcing YOK — MEVCUT context'te (authenticateConnect token tenantId'sinden set eder).
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

  /**
   * STOMP SUBSCRIBE yetkilendirmesi:
   * - /user/queue/** : kendi oturumuna özel kuyruklar (tüm doğrulanmış oturumlara açık)
   * - /topic/presence, /topic/groups/new, /topic/groups/deleted : yalnız loginSource="admin"
   * - /topic/user/{userId}/** : yalnız kendi userId'si ile eşleşen oturumlar
   * - /topic/rtc-client/{clientRef}/rtc : doğrulanmış oturumlar (clientRef boş olamaz)
   * - /topic/tenant/{tid}/group/{groupId}(/.*)? ve /topic/group/{groupId}(/.*)? :
   *   grup erişim kontrolü (üyelik, visitorAccess veya rol görünürlüğü) + tenant izolasyonu
   */
  public void authorizeSubscribe(StompHeaderAccessor accessor, Message<?> message) {
    String destination = accessor.getDestination();
    if (destination == null || destination.isBlank()) {
      throw new org.springframework.messaging.MessageDeliveryException(
          message, "Missing STOMP SUBSCRIBE destination");
    }

    Map<String, Object> attrs = accessor.getSessionAttributes();
    String loginSource = attrs != null && attrs.get("loginSource") != null
        ? attrs.get("loginSource").toString()
        : null;
    String sessionTenant = attrs != null && attrs.get("tenantId") != null
        ? attrs.get("tenantId").toString()
        : null;
    Long sessionUserId = null;
    if (attrs != null && attrs.get("userId") != null) {
      Object rawId = attrs.get("userId");
      sessionUserId = (rawId instanceof Long l) ? l : Long.valueOf(rawId.toString());
    }

    // 1. Kişisel kuyruklar (/user/queue/**)
    if (destination.startsWith("/user/queue/")) {
      return;
    }

    // 2. Panel genel yayınları
    if ("/topic/presence".equals(destination)
        || "/topic/groups/new".equals(destination)
        || "/topic/groups/deleted".equals(destination)) {
      if (!"admin".equals(loginSource)) {
        throw new org.springframework.messaging.MessageDeliveryException(
            message, "Only admin sessions can subscribe to " + destination);
      }
      return;
    }

    // 3. Kullanıcıya özel topic (/topic/user/{userId}/...)
    Matcher userMatcher = USER_TOPIC_PATTERN.matcher(destination);
    if (userMatcher.matches()) {
      Long targetUserId = Long.valueOf(userMatcher.group(1));
      if (sessionUserId == null || !sessionUserId.equals(targetUserId)) {
        throw new org.springframework.messaging.MessageDeliveryException(
            message, "Cannot subscribe to another user's topic: " + destination);
      }
      return;
    }

    // 4. WebRTC caller sinyalleşme kanalı (/topic/rtc-client/{clientRef}/rtc)
    Matcher rtcClientMatcher = RTC_CLIENT_TOPIC_PATTERN.matcher(destination);
    if (rtcClientMatcher.matches()) {
      return;
    }

    // 5. Tenant-scoped veya legacy grup kanalları
    Matcher tenantGroupMatcher = TENANT_GROUP_TOPIC_PATTERN.matcher(destination);
    if (tenantGroupMatcher.matches()) {
      String targetTenantId = tenantGroupMatcher.group(1);
      UUID groupId = UUID.fromString(tenantGroupMatcher.group(2));
      authorizeGroupTopicSubscribe(
          message, accessor, loginSource, sessionTenant, sessionUserId, targetTenantId, groupId);
      return;
    }

    Matcher legacyGroupMatcher = LEGACY_GROUP_TOPIC_PATTERN.matcher(destination);
    if (legacyGroupMatcher.matches()) {
      UUID groupId = UUID.fromString(legacyGroupMatcher.group(1));
      authorizeGroupTopicSubscribe(
          message, accessor, loginSource, sessionTenant, sessionUserId, sessionTenant, groupId);
      return;
    }

    throw new org.springframework.messaging.MessageDeliveryException(
        message, "Forbidden STOMP subscription destination: " + destination);
  }

  private void authorizeGroupTopicSubscribe(
      Message<?> message,
      StompHeaderAccessor accessor,
      String loginSource,
      String sessionTenant,
      Long sessionUserId,
      String targetTenantId,
      UUID groupId) {
    if (targetTenantId == null || targetTenantId.isBlank()) {
      throw new org.springframework.messaging.MessageDeliveryException(
          message, "Group subscription requires tenantId");
    }

    // Tenant ve Guest oturumları yalnızca kendi tenant'ındaki gruplara abone olabilir
    if (("website".equals(loginSource) || "tenant".equals(loginSource))
        && !targetTenantId.equals(sessionTenant)) {
      throw new org.springframework.messaging.MessageDeliveryException(
          message, "Cross-tenant group subscription forbidden");
    }

    String originalTenant = TenantContext.getTenantId();
    TenantContext.setTenantId(targetTenantId);
    try {
      ChatGroup group = chatGroupRepository.findById(groupId).orElse(null);
      if (group == null) {
        throw new org.springframework.messaging.MessageDeliveryException(
            message, "Chat group not found: " + groupId);
      }

      if ("website".equals(loginSource)) {
        if (!group.isVisitorAccess()) {
          throw new org.springframework.messaging.MessageDeliveryException(
              message, "Guest cannot subscribe to non-visitor group: " + groupId);
        }
        return;
      }

      boolean isMember = sessionUserId != null
          && chatGroupMemberRepository.existsByIdGroupIdAndIdUserId(groupId, sessionUserId);
      if (isMember) {
        return;
      }

      if ("tenant".equals(loginSource)) {
        if (!group.isVisitorAccess()) {
          throw new org.springframework.messaging.MessageDeliveryException(
              message, "Tenant user cannot subscribe to group without membership: " + groupId);
        }
        return;
      }

      if ("admin".equals(loginSource)) {
        boolean isPublicTenantGroup = group.getTenantId() != null
            && !group.getTenantId().isBlank()
            && group.isVisitorAccess();
        if (isPublicTenantGroup) {
          return;
        }
        int roleLevel = resolveRoleLevel(accessor);
        boolean canRead = (group.getType() == ChatGroupType.DM)
            ? roleLevel >= 4
            : roleLevel >= group.getVisibilityLevel();
        if (!canRead) {
          throw new org.springframework.messaging.MessageDeliveryException(
              message, "Insufficient role level to subscribe to group: " + groupId);
        }
        return;
      }

      throw new org.springframework.messaging.MessageDeliveryException(
          message, "Unauthorized loginSource for group subscription");
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }

  private int resolveRoleLevel(StompHeaderAccessor accessor) {
    if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth)) {
      return 1;
    }
    Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
    if (authorities == null) return 1;
    Set<String> names = authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
    if (names.contains("ROLE_SUPER_ADMIN")) return 4;
    if (names.contains("ROLE_ADMIN")) return 3;
    if (names.contains("ROLE_EDITOR")) return 2;
    return 1;
  }
}

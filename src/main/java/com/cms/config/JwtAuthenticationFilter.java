package com.cms.config;

import com.cms.dto.CachedUserDetails;
import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.repository.UserRepository;
import com.cms.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final RedisTemplate<String, Object> redisTemplate;
  private final UserAuthCacheService userAuthCacheService;


  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    final String authorizationHeader = request.getHeader("Authorization");

    // Authorization header yoksa token işlemlerine girmeyi atla
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      chain.doFilter(request, response);
      return;
    }

    // Authorization header varsa token işlemlerine gir
    // Authorization header gönderilmişse token MUTLAKA geçerli olmalı
    String jwt = authorizationHeader.substring(7);
    final String username;

    try {
      username = jwtUtil.extractUsername(jwt);
    } catch (Exception e) {
      // Token geçersiz veya decrypt edilemiyor
      // Authorization header gönderilmişse token geçerli olmalı, hata döndür
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid or expired token");
      return;
    }

    // Username çıkarıldıysa ve henüz authentication yapılmadıysa
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        // Önce Redis cache'ten dene
        CachedUserDetails cachedUser = getUserFromCache(username);

        if (cachedUser == null) {
          // Cache'te yok — DB'den al ve cache'e yaz
          cachedUser = loadUserFromDbAndCache(username);
        }

        // Token'ı validate et (tokenVersion kontrolü ile)
        Long currentTokenVersion = cachedUser.getTokenVersion() != null ? cachedUser.getTokenVersion() : 0L;
        if (!jwtUtil.validateToken(jwt, cachedUser.getUsername(), currentTokenVersion)) {
          sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid or expired token");
          return;
        }

        // Cache'teki authorities'den Spring Security authority'leri oluştur
        List<GrantedAuthority> authorities = cachedUser.getAuthorities().stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        // CachedUserPrincipal oluştur
        CachedUserPrincipal principal = new CachedUserPrincipal(cachedUser, authorities);

        // Token geçerli, authentication set et
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            principal, null, authorities);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

      } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid token or user not found");
        return;
      } catch (Exception e) {
        log.error("Authentication error for user {}: {}", username, e.getMessage());
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid token or user not found");
        return;
      }
    }

    chain.doFilter(request, response);
  }

  /**
   * Redis'ten CachedUserDetails oku.
   */
  private CachedUserDetails getUserFromCache(String username) {
    try {
      String cacheKey = UserAuthCacheService.USER_CACHE_PREFIX + username;
      Object cached = redisTemplate.opsForValue().get(cacheKey);
      if (cached instanceof CachedUserDetails) {
        return (CachedUserDetails) cached;
      }
      // Jackson deserialize etmiş olabilir — map'ten dönüştür
      if (cached != null) {
        return objectMapper.convertValue(cached, CachedUserDetails.class);
      }
    } catch (Exception e) {
      log.debug("Redis cache okunamadı (user: {}): {}", username, e.getMessage());
    }
    return null;
  }

  /**
   * DB'den User'ı yükle, CachedUserDetails'a dönüştür ve Redis'e yaz.
   */
  private CachedUserDetails loadUserFromDbAndCache(String username) {
    String originalTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId(defaultTenant); // basedb'ye geç

      User user = userRepository.findByUsername(username)
          .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
              "User not found: " + username));

      // Authority string'lerini topla
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

      // Redis'e yaz
      try {
        String cacheKey = UserAuthCacheService.USER_CACHE_PREFIX + username;
        redisTemplate.opsForValue().set(cacheKey, cached, UserAuthCacheService.USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
      } catch (Exception e) {
        log.debug("Redis cache yazılamadı (user: {}): {}", username, e.getMessage());
      }

      return cached;

    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }

  /**
   * Kullanıcının cache'ini invalidate et.
   * @deprecated UserAuthCacheService.evictUserCache() kullanın
   */
  public void evictUserCache(String username) {
    userAuthCacheService.evictUserCache(username);
  }

  private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("result", false);
    errorResponse.put("status", status.value());
    errorResponse.put("error", status.getReasonPhrase());
    errorResponse.put("errorCode", errorCode);
    errorResponse.put("message", message);

    objectMapper.writeValue(response.getWriter(), errorResponse);
  }

  /**
   * Cache'ten okunan kullanıcı bilgilerini Spring Security ile uyumlu hale getiren UserDetails implementasyonu.
   */
  public static class CachedUserPrincipal implements UserDetails {
    private final CachedUserDetails cachedUser;
    private final List<GrantedAuthority> authorities;

    public CachedUserPrincipal(CachedUserDetails cachedUser, List<GrantedAuthority> authorities) {
      this.cachedUser = cachedUser;
      this.authorities = authorities;
    }

    public CachedUserDetails getCachedUser() {
      return cachedUser;
    }

    public Long getUserId() {
      return cachedUser.getId();
    }

    @Override
    public java.util.Collection<? extends GrantedAuthority> getAuthorities() {
      return authorities;
    }

    @Override
    public String getPassword() {
      return cachedUser.getPassword() != null ? cachedUser.getPassword() : "";
    }

    @Override
    public String getUsername() {
      return cachedUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
      return cachedUser.getIsActive() != null && cachedUser.getIsActive();
    }
  }
}

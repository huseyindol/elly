package com.cms.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kullanıcı auth cache yönetimi.
 * Circular dependency sorununu önlemek için JwtAuthenticationFilter'dan ayrı tutulur.
 * Hem filter hem service'ler bu servisi kullanır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthCacheService {

  private final RedisTemplate<String, Object> redisTemplate;

  public static final String USER_CACHE_PREFIX = "auth:user:";
  public static final long USER_CACHE_TTL_MINUTES = 30;

  /**
   * Kullanıcının auth cache'ini temizle.
   * Password değişikliği, rol değişikliği, token version güncelleme gibi durumlarda çağrılır.
   */
  public void evictUserCache(String username) {
    try {
      String cacheKey = USER_CACHE_PREFIX + username;
      redisTemplate.delete(cacheKey);
      log.debug("User auth cache evicted: {}", username);
    } catch (Exception e) {
      log.debug("User auth cache evict hatası (user: {}): {}", username, e.getMessage());
    }
  }
}

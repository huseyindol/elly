package com.cms.config;

import com.cms.dto.CachedUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
  private final ObjectMapper objectMapper;

  public static final String USER_CACHE_PREFIX = "auth:user:";
  public static final long USER_CACHE_TTL_MINUTES = 30;
  public static final String MFA_ATTEMPT_PREFIX = "auth:mfa:attempts:";
  public static final int MAX_MFA_VERIFY_ATTEMPTS = 5;
  public static final long MFA_ATTEMPT_TTL_MINUTES = 5;

  private final Map<String, MfaAttemptEntry> localMfaAttempts = new ConcurrentHashMap<>();

  private record MfaAttemptEntry(int count, long expiresAtMillis) {}

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

  /**
   * Redis'ten CachedUserDetails oku.
   * Cache miss veya Redis down ise null döner (fail-open).
   */
  public CachedUserDetails getUserFromCache(String username) {
    try {
      String cacheKey = USER_CACHE_PREFIX + username;
      Object cached = redisTemplate.opsForValue().get(cacheKey);
      if (cached instanceof CachedUserDetails details) {
        return details;
      }
      if (cached != null) {
        return objectMapper.convertValue(cached, CachedUserDetails.class);
      }
    } catch (Exception e) {
      log.debug("Redis cache okunamadı (user: {}): {}", username, e.getMessage());
    }
    return null;
  }

  public boolean isMfaLockedOut(String tenantId, Long userId) {
    return getMfaAttempts(tenantId, userId) >= MAX_MFA_VERIFY_ATTEMPTS;
  }

  public int getMfaAttempts(String tenantId, Long userId) {
    String key = mfaKey(tenantId, userId);
    try {
      if (redisTemplate != null && redisTemplate.opsForValue() != null) {
        Object val = redisTemplate.opsForValue().get(key);
        if (val instanceof Number n) {
          return n.intValue();
        }
        if (val != null) {
          return Integer.parseInt(val.toString());
        }
      }
    } catch (Exception e) {
      log.debug("Redis MFA attempt read fallback (key={}): {}", key, e.getMessage());
    }
    MfaAttemptEntry entry = localMfaAttempts.get(key);
    if (entry == null) return 0;
    if (System.currentTimeMillis() > entry.expiresAtMillis()) {
      localMfaAttempts.remove(key);
      return 0;
    }
    return entry.count();
  }

  public int recordFailedMfaAttempt(String tenantId, Long userId) {
    String key = mfaKey(tenantId, userId);
    int next = getMfaAttempts(tenantId, userId) + 1;
    long expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(MFA_ATTEMPT_TTL_MINUTES);
    localMfaAttempts.put(key, new MfaAttemptEntry(next, expiresAt));
    try {
      if (redisTemplate != null && redisTemplate.opsForValue() != null) {
        redisTemplate.opsForValue().set(key, next, MFA_ATTEMPT_TTL_MINUTES, TimeUnit.MINUTES);
      }
    } catch (Exception e) {
      log.debug("Redis MFA attempt write fallback (key={}): {}", key, e.getMessage());
    }
    return next;
  }

  public void clearMfaAttempts(String tenantId, Long userId) {
    String key = mfaKey(tenantId, userId);
    localMfaAttempts.remove(key);
    try {
      if (redisTemplate != null) {
        redisTemplate.delete(key);
      }
    } catch (Exception e) {
      log.debug("Redis MFA attempt delete fallback (key={}): {}", key, e.getMessage());
    }
  }

  private String mfaKey(String tenantId, Long userId) {
    return MFA_ATTEMPT_PREFIX + (tenantId != null ? tenantId : "default") + ":" + userId;
  }
}

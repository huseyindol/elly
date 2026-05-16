package com.cms.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPresenceService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String KEY_PREFIX = "chat:presence:";

  @Value("${chat.presence.ttl-seconds:300}")
  private long presenceTtlSeconds;

  public boolean userConnected(Long userId) {
    String key = KEY_PREFIX + userId;
    Long count = redisTemplate.opsForValue().increment(key);
    redisTemplate.expire(key, presenceTtlSeconds, TimeUnit.SECONDS);
    boolean firstSession = count != null && count == 1;
    if (firstSession) {
      log.debug("User {} is now ONLINE", userId);
    }
    return firstSession; // true = ilk bağlantı → ONLINE broadcast gerekir
  }

  public boolean userDisconnected(Long userId) {
    String key = KEY_PREFIX + userId;
    Long count = redisTemplate.opsForValue().decrement(key);
    boolean lastSession = count != null && count <= 0;
    if (lastSession) {
      redisTemplate.delete(key);
      log.debug("User {} is now OFFLINE", userId);
    }
    return lastSession; // true = son bağlantı kapandı → OFFLINE broadcast gerekir
  }

  public boolean isOnline(Long userId) {
    String key = KEY_PREFIX + userId;
    Object val = redisTemplate.opsForValue().get(key);
    if (val == null) return false;
    try {
      return Long.parseLong(val.toString()) > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public void refreshHeartbeat(Long userId) {
    String key = KEY_PREFIX + userId;
    if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
      redisTemplate.expire(key, presenceTtlSeconds, TimeUnit.SECONDS);
    }
  }
}

package com.cms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatTypingService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String KEY_PREFIX = "chat:typing:";

  @Value("${chat.typing.ttl-seconds:5}")
  private long typingTtlSeconds;

  public void setTyping(UUID groupId, Long userId) {
    String key = KEY_PREFIX + groupId + ":" + userId;
    redisTemplate.opsForValue().set(key, "1", typingTtlSeconds, TimeUnit.SECONDS);
  }

  public void clearTyping(UUID groupId, Long userId) {
    redisTemplate.delete(KEY_PREFIX + groupId + ":" + userId);
  }
}

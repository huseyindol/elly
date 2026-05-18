package com.cms.service;

import com.cms.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatRateLimitService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String KEY_PREFIX = "chat:rate:";

  @Value("${chat.rate-limit.max-messages-per-second:10}")
  private int maxMessagesPerSecond;

  public void checkRateLimit(Long userId) {
    String key = KEY_PREFIX + userId;
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1) {
      // İlk mesaj bu saniyede — TTL 1 saniye koy
      redisTemplate.expire(key, 1, TimeUnit.SECONDS);
    }
    if (count != null && count > maxMessagesPerSecond) {
      throw new TooManyRequestsException("Rate limit exceeded: max " + maxMessagesPerSecond + " messages/second");
    }
  }
}

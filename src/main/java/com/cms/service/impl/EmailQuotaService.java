package com.cms.service.impl;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.cms.config.TenantContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Mail+Form v3 (B2) — SMTP hesabi bazinda gunluk gonderim kotasi takibi.
 *
 * <p>Redis anahtari: {@code mail:quota:{tenantId}:{accountId}:{yyyyMMdd}} (48 saat TTL).
 * Redis erisilemezse veya test ortaminda {@link StringRedisTemplate} yoksa
 * bellek-ici (ConcurrentHashMap) fallback devreye girer.
 */
@Slf4j
@Service
public class EmailQuotaService {

  public static final int DEFAULT_DAILY_LIMIT = 500;
  private static final String KEY_PREFIX = "mail:quota:";
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

  private final StringRedisTemplate stringRedisTemplate;
  private final ConcurrentHashMap<String, AtomicLong> fallbackCounters = new ConcurrentHashMap<>();

  public EmailQuotaService(@Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public int resolveEffectiveLimit(Integer configuredLimit) {
    return (configuredLimit != null && configuredLimit > 0) ? configuredLimit : DEFAULT_DAILY_LIMIT;
  }

  public long getDailySentCount(String tenantId, Long mailAccountId) {
    if (mailAccountId == null) {
      return 0L;
    }
    String key = buildKey(tenantId, mailAccountId);
    if (stringRedisTemplate != null) {
      try {
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
          return 0L;
        }
        return Long.parseLong(raw.trim());
      } catch (Exception e) {
        log.debug("Redis mail quota okunamadi, fallback kullaniliyor: key={}, err={}", key, e.getMessage());
      }
    }
    AtomicLong counter = fallbackCounters.get(key);
    return counter != null ? counter.get() : 0L;
  }

  public boolean isQuotaExceeded(String tenantId, Long mailAccountId, Integer configuredLimit) {
    int limit = resolveEffectiveLimit(configuredLimit);
    long current = getDailySentCount(tenantId, mailAccountId);
    return current >= limit;
  }

  public long incrementDailySentCount(String tenantId, Long mailAccountId) {
    if (mailAccountId == null) {
      return 0L;
    }
    String key = buildKey(tenantId, mailAccountId);
    if (stringRedisTemplate != null) {
      try {
        Long val = stringRedisTemplate.opsForValue().increment(key);
        if (val != null && val == 1L) {
          stringRedisTemplate.expire(key, 48, TimeUnit.HOURS);
        }
        return val != null ? val : 1L;
      } catch (Exception e) {
        log.debug("Redis mail quota artirilamadi, fallback kullaniliyor: key={}, err={}", key, e.getMessage());
      }
    }
    return fallbackCounters.computeIfAbsent(key, k -> new AtomicLong(0L)).incrementAndGet();
  }

  private String buildKey(String tenantId, Long mailAccountId) {
    String effectiveTenant = (tenantId != null && !tenantId.isBlank())
        ? tenantId
        : (TenantContext.getTenantId() != null ? TenantContext.getTenantId() : "default");
    String day = LocalDate.now(ZoneOffset.UTC).format(DATE_FMT);
    return KEY_PREFIX + effectiveTenant + ":" + mailAccountId + ":" + day;
  }
}

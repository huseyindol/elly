package com.cms.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cms.config.TenantContext;
import com.cms.exception.FormValidationException;
import com.cms.exception.TooManyRequestsException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Mail+Form v3 (B3) — Public form spam korumasi:
 * <ol>
 *   <li>Honeypot alan kontrolu ({@code honeypot}, {@code _hp}, {@code _honeypot}, {@code website_url_hp})</li>
 *   <li>IP + Tenant + Form bazli dakikada maksimum gonderim limiti (Redis + in-memory fallback)</li>
 * </ol>
 */
@Slf4j
@Service
public class FormSpamProtectionService {

  public static final List<String> HONEYPOT_KEYS = List.of("_hp", "_honeypot", "website_url_hp");
  private static final String KEY_PREFIX = "form:rate:";
  private static final long WINDOW_MS = 60_000L;

  private final StringRedisTemplate stringRedisTemplate;
  private final ConcurrentHashMap<String, WindowCounter> fallbackWindows = new ConcurrentHashMap<>();

  @Value("${form.rate-limit.max-submits-per-minute:5}")
  private int maxSubmitsPerMinute = 5;

  public FormSpamProtectionService(@Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public void setMaxSubmitsPerMinute(int maxSubmitsPerMinute) {
    this.maxSubmitsPerMinute = maxSubmitsPerMinute;
  }

  /**
   * Top-level DtoFormSubmit.honeypot alanini kontrol eder.
   */
  public void validateTopLevelHoneypot(String honeypot) {
    if (honeypot != null && !honeypot.isBlank()) {
      log.warn("Form spam engellendi (top-level honeypot dolu): tenant={}", TenantContext.getTenantId());
      throw new FormValidationException("Spam algilandi (honeypot)");
    }
  }

  /**
   * Payload icindeki honeypot anahtarlarini kontrol edip temizler ve aktif HTTP istegi
   * varsa IP bazli rate limit uygular.
   */
  public void verifyAndSanitizeSubmission(Long formDefinitionId, Map<String, Object> payload) {
    if (payload != null) {
      for (String key : HONEYPOT_KEYS) {
        Object val = payload.remove(key);
        if (val != null && !val.toString().isBlank()) {
          log.warn("Form spam engellendi (payload honeypot '{}' dolu): formId={}, tenant={}",
              key, formDefinitionId, TenantContext.getTenantId());
          throw new FormValidationException("Spam algilandi (honeypot)");
        }
      }
    }

    String clientIp = resolveClientIpFromContext();
    if (clientIp != null) {
      checkRateLimit(TenantContext.getTenantId(), formDefinitionId, clientIp);
    }
  }

  /**
   * Belirli bir tenant, form ve istemci IP'si icin dakikada maksimum gonderim limitini denetler.
   */
  public void checkRateLimit(String tenantId, Long formDefinitionId, String clientIp) {
    String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
    String effectiveIp = (clientIp != null && !clientIp.isBlank()) ? clientIp : "unknown";
    String key = KEY_PREFIX + effectiveTenant + ":" + formDefinitionId + ":" + effectiveIp;

    long count = incrementAndGet(key);
    if (count > maxSubmitsPerMinute) {
      log.warn("Form rate limit asildi: key={}, count={}, limit={}", key, count, maxSubmitsPerMinute);
      throw new TooManyRequestsException(
          "Cok fazla form gonderimi yaptiniz. Lutfen bir dakika bekleyip tekrar deneyin.");
    }
  }

  private long incrementAndGet(String key) {
    if (stringRedisTemplate != null) {
      try {
        Long val = stringRedisTemplate.opsForValue().increment(key);
        if (val != null && val == 1L) {
          stringRedisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        if (val != null) {
          return val;
        }
      } catch (Exception e) {
        log.debug("Redis form rate-limit kullanilamadi, fallback devreye giriyor: {}", e.getMessage());
      }
    }

    long now = System.currentTimeMillis();
    WindowCounter counter = fallbackWindows.compute(key, (k, existing) -> {
      if (existing == null || (now - existing.windowStartMs) >= WINDOW_MS) {
        return new WindowCounter(now, 1L);
      }
      existing.count++;
      return existing;
    });
    return counter.count;
  }

  private String resolveClientIpFromContext() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes servletAttrs) {
      HttpServletRequest request = servletAttrs.getRequest();
      String xff = request.getHeader("X-Forwarded-For");
      if (xff != null && !xff.isBlank()) {
        return xff.split(",")[0].trim();
      }
      String realIp = request.getHeader("X-Real-IP");
      if (realIp != null && !realIp.isBlank()) {
        return realIp.trim();
      }
      return request.getRemoteAddr();
    }
    return null;
  }

  private static final class WindowCounter {
    private final long windowStartMs;
    private long count;

    private WindowCounter(long windowStartMs, long count) {
      this.windowStartMs = windowStartMs;
      this.count = count;
    }
  }
}

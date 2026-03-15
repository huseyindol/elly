package com.cms.config;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis bağlantı hatalarında uygulamanın çökmesini önler.
 * Cache erişilemez olduğunda işlem cache'siz devam eder,
 * veri doğrudan veritabanından okunur.
 */
@Slf4j
public class GracefulCacheErrorHandler implements CacheErrorHandler {

  @Override
  public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
    log.warn("Cache GET hatası [cache={}, key={}]: {}", cache.getName(), key, exception.getMessage());
  }

  @Override
  public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key,
      @Nullable Object value) {
    log.warn("Cache PUT hatası [cache={}, key={}]: {}", cache.getName(), key, exception.getMessage());
  }

  @Override
  public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
    log.warn("Cache EVICT hatası [cache={}, key={}]: {}", cache.getName(), key, exception.getMessage());
  }

  @Override
  public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
    log.warn("Cache CLEAR hatası [cache={}]: {}", cache.getName(), exception.getMessage());
  }
}

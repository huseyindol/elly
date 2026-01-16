package com.cms.controller.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DtoCacheInfo;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
public class CacheController {

  private final CacheManager cacheManager;

  @GetMapping
  public List<DtoCacheInfo> getCacheStats() {
    List<DtoCacheInfo> cacheInfos = new ArrayList<>();

    cacheManager.getCacheNames().forEach(cacheName -> {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache instanceof CaffeineCache) {
        CaffeineCache caffeineCache = (CaffeineCache) cache;
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

        CacheStats stats = nativeCache.stats();
        List<String> keys = nativeCache.asMap().keySet().stream()
            .map(Object::toString)
            .collect(Collectors.toList());

        cacheInfos.add(DtoCacheInfo.builder()
            .name(cacheName)
            .size(nativeCache.estimatedSize())
            .estimatedSize(nativeCache.estimatedSize())
            .stats(String.format("Hit: %d, Miss: %d, HitRate: %.2f",
                stats.hitCount(), stats.missCount(), stats.hitRate()))
            .keys(keys)
            .build());
      } else {
        cacheInfos.add(DtoCacheInfo.builder()
            .name(cacheName)
            .stats("N/A - Not a Caffeine Cache")
            .keys(Collections.emptyList())
            .build());
      }
    });

    return cacheInfos;
  }

  @DeleteMapping("/{name}")
  public String clearCache(@PathVariable String name) {
    Cache cache = cacheManager.getCache(name);
    if (cache != null) {
      cache.clear();
      return name + " cache temizlendi!";
    }
    return "Cache bulunamadı: " + name;
  }

  @DeleteMapping
  public String clearAllCaches() {
    cacheManager.getCacheNames().forEach(name -> {
      Cache cache = cacheManager.getCache(name);
      if (cache != null) {
        cache.clear();
      }
    });
    return "Tüm cacheler temizlendi!";
  }
}

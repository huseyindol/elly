package com.cms.controller.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.config.TenantContext;
import com.cms.dto.DtoCacheInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

  private static final String[] CACHE_NAMES = {
      "pages", "posts", "components", "widgets", "banners",
      "formDefinitions", "formSubmissions", "comments", "ratings", "cmsContents"
  };

  private final RedisTemplate<String, Object> redisTemplate;

  @GetMapping
  public List<DtoCacheInfo> getCacheStats() {
    String tenantId = resolveTenantId();
    List<DtoCacheInfo> cacheInfos = new ArrayList<>();

    for (String cacheName : CACHE_NAMES) {
      String pattern = tenantId + "::" + cacheName + "::*";
      Set<String> keys = scanKeys(pattern);

      cacheInfos.add(DtoCacheInfo.builder()
          .tenantId(tenantId)
          .name(cacheName)
          .keyCount(keys.size())
          .stats(buildRedisStats())
          .keys(new ArrayList<>(keys))
          .build());
    }

    return cacheInfos;
  }

  @DeleteMapping("/{name}")
  public String clearCache(@PathVariable String name) {
    String tenantId = resolveTenantId();
    String pattern = tenantId + "::" + name + "::*";
    Set<String> keys = scanKeys(pattern);

    if (!keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.info("Cache temizlendi [tenant={}, cache={}, keys={}]", tenantId, name, keys.size());
      return name + " cache temizlendi! (" + keys.size() + " key silindi, tenant: " + tenantId + ")";
    }
    return "Cache boş veya bulunamadı: " + name + " (tenant: " + tenantId + ")";
  }

  @DeleteMapping
  public String clearAllCaches() {
    String tenantId = resolveTenantId();
    String pattern = tenantId + "::*";
    Set<String> keys = scanKeys(pattern);

    if (!keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.info("Tüm cacheler temizlendi [tenant={}, keys={}]", tenantId, keys.size());
    }
    return "Tüm cacheler temizlendi! (" + keys.size() + " key silindi, tenant: " + tenantId + ")";
  }

  @DeleteMapping("/all-tenants")
  public String clearAllTenantsCache() {
    Set<String> keys = scanKeys("*");

    if (!keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.info("Tüm tenant cache'leri temizlendi [keys={}]", keys.size());
    }
    return "Tüm tenant cache'leri temizlendi! (" + keys.size() + " key silindi)";
  }

  private Set<String> scanKeys(String pattern) {
    Set<String> keys = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

    try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
        (RedisConnection connection) -> connection.keyCommands().scan(options))) {
      if (cursor != null) {
        while (cursor.hasNext()) {
          keys.add(new String(cursor.next()));
        }
      }
    }
    return keys;
  }

  private String buildRedisStats() {
    try {
      var connectionFactory = redisTemplate.getConnectionFactory();
      if (connectionFactory == null) {
        return "N/A - ConnectionFactory not available";
      }
      Properties info = connectionFactory.getConnection().serverCommands().info("stats");
      if (info != null) {
        return String.format("Hits: %s, Misses: %s",
            info.getProperty("keyspace_hits", "N/A"),
            info.getProperty("keyspace_misses", "N/A"));
      }
    } catch (Exception e) {
      log.debug("Redis stats alınamadı: {}", e.getMessage());
    }
    return "N/A";
  }

  private String resolveTenantId() {
    String tenantId = TenantContext.getTenantId();
    return (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
  }
}

---
name: redis-cache-patterns
description: Elly'deki Redis cache konfigürasyonu, tenant-isolated key yapısı, TTL, invalidation ve graceful error handling pattern'leri. Cache ekleme, invalidation sorunu veya performans optimizasyonunda otomatik aktif et.
version: 1.0.0
---

# Elly Redis Cache Patterns

## Mimari

```
CacheConfig.java (@EnableCaching)
  → RedisCacheManager (tenant-isolated prefix)
  → GracefulCacheErrorHandler (Redis down → DB fallback, uygulama çökmez)

Key formatı: {tenantId}::{cacheName}::{key}
Örnek:       tenant1::posts::42
```

## Tenant İzolasyonu — Otomatik

CacheConfig'de `computePrefixWith` tanımlı:
```java
.computePrefixWith(cacheName -> {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) tenantId = "default";
    return tenantId + "::" + cacheName + "::";
})
```

**Sonuç:** `@Cacheable` / `@CacheEvict` kullanırken tenant key'i eklemeye gerek yok — otomatik.

## Serializasyon

- Key: `StringRedisSerializer`
- Value: `GenericJackson2JsonRedisSerializer` (polymorphic type info aktif)
- Hibernate6Module: lazy-loaded proxy'ler JSON'a dönüşürken hata vermez
- MixIn'ler: circular reference'ları kırmak için (Component ↔ Page, Comment ↔ parentComment)

## Varsayılan TTL

- Genel: **10 dakika** (`Duration.ofMinutes(10)`)
- Auth cache: **30 dakika** (UserAuthCacheService, ayrı key pattern: `auth:user:{username}`)

## Standart Cache Kullanım Pattern'i

### Okuma: @Cacheable
```java
@Cacheable(value = "posts", key = "#id")
public PostResponse getById(Long id) { ... }

@Cacheable(value = "posts", key = "'getAllPosts'")
public List<PostResponse> getAll() { ... }

// Sayfalama için key pattern:
@Cacheable(value = "posts", 
    key = "'paged_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
public Page<PostResponse> getAllPaged(Pageable pageable) { ... }
```

### Yazma/Silme: @CacheEvict
```java
@CacheEvict(value = "posts", allEntries = true)
public PostResponse create(PostCreateRequest request) { ... }

@CacheEvict(value = "posts", allEntries = true)
public PostResponse update(Long id, PostUpdateRequest request) { ... }

@CacheEvict(value = "posts", allEntries = true)
public void delete(Long id) { ... }
```

**Neden `allEntries = true`?** Tek bir kayıt değiştiğinde list/paged cache'ler de geçersiz olur. Tenant prefix sayesinde sadece o tenant'ın cache'i temizlenir.

## Graceful Error Handling

`GracefulCacheErrorHandler` → Redis erişilemezse:
- GET: `log.warn`, DB'den okur
- PUT: `log.warn`, cache'e yazmaz
- EVICT/CLEAR: `log.warn`, stale data riski var ama uygulama çökmez

## Yeni Cache Ekleme Checklist

1. Service method'una `@Cacheable(value = "xxx", key = "...")` ekle
2. Yazma/silme method'larına `@CacheEvict(value = "xxx", allEntries = true)` ekle
3. Cache adı unique olmalı — mevcut cache adları:
   - `posts`, `pages`, `components`, `widgets`, `banners`
   - `comments`, `ratings`, `forms`, `formSubmissions`
   - `cmsContents`, `mailAccounts`
   - `auth:user:{username}` (RedisTemplate ile, annotation değil)
4. **Test:** Farklı tenant'larla aynı key'in farklı veri döndürdüğünü doğrula
5. **Lazy loading:** Cache'e konan nesne detach olmalı — lazy field'lar yüklenmemişse NPE verir

## Anti-Pattern'ler

- `@Cacheable` + `@Transactional` aynı method'da dikkat: transaction rollback olursa cache zehirlenir
- Cache key'inde entity referansı kullanma (toString yetersiz) — primitive/String key kullan
- `@CacheEvict` olmadan `@Cacheable` ekleme → stale data
- Auth cache'ini annotation ile yönetme → `UserAuthCacheService` + RedisTemplate kullan

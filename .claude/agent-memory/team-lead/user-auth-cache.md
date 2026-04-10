# User Auth Redis Cache — Elly CMS

## Durum: ✅ Tamamlandı (2026-04-10)

## Problem
Her HTTP isteğinde 3 SQL sorgusu (users + roles/permissions + tenants) çalışıyordu.
Cache'li istekte bile ~189ms sürüyordu.

## Çözüm
`auth:user:{username}` key'inde Redis cache — 30 dk TTL.

### Yeni Dosyalar
- `com.cms.dto.CachedUserDetails` — Hafif cache DTO (JPA entity değil)
- `com.cms.config.UserAuthCacheService` — Eviction servisi (circular dep önleme)

### Değiştirilen Dosyalar
- `JwtAuthenticationFilter` — Önce Redis, miss → DB + Redis write
- `AuthService` — login/refresh → evictUserCache()
- `UserService` — profil/şifre → evictUserCache()
- `RoleService` — rol atama → evictUserCache()

### Cache Invalidation
- Login / refresh token → token version değişir
- Profil güncelleme → username/email değişebilir
- Şifre değişikliği → password değişir
- Rol atama → authorities değişir

### Performans
- 3 SQL sorgusu → 1 Redis GET (~189ms → ~50ms)

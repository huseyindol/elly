---
name: spring-security-patterns
description: Elly'deki JWT, 2FA (TOTP) ve Spring Security pattern'leri. Auth flow değişikliği, yeni endpoint güvenliği, token handling, SecurityConfig güncelleme veya MFA konfigürasyonu ile ilgili konularda otomatik aktif et.
version: 2.0.0
---

# Elly Spring Security & Auth Patterns

> OAuth2 TAMAMEN kaldırıldı (7e160df). Kimlik doğrulama: BCrypt username/parola + JWE-şifreli JWT
> (+ isteğe bağlı 2FA/TOTP). `AdminLoginInterceptor` da silindi — koruma RBAC `@PreAuthorize` ile.

## JWT Yapısı
```java
// Token üretimi (JwtUtil — JWE, AES-256-GCM)
generateToken(userId, username, tokenVersion, tenantId, loginSource)   // Access
generateRefreshToken(userId, username, tenantId, loginSource)          // Refresh
generateMfaToken(userId, loginSource, tenantId)                        // MFA ara token (type=mfa)
generateGuestToken(...)                                                // Guest (loginSource=website)

// JWT Claims
// - loginSource: "admin" | "tenant" | "website" (guest)
// - tenantId: "tenant1" | "tenant2"  ← ZORUNLU, yoksa TENANT_REQUIRED (basedb YOK)
// - tokenVersion: şifre değişikliği/oturum iptali sonrası eski token'ları geçersiz kılar
// - guest token: userId yok; sessionId + displayName + tenantId claim'leri var
```

## Token Süreleri (application.properties)
| Token | Property | Env | Varsayılan |
|-------|----------|-----|------------|
| Access | `jwt.expiration` | `JWT_EXPIRATION` | 1.800.000 ms (30dk) |
| Refresh | `jwt.refresh.expiration` | `JWT_REFRESH_EXPIRATION` | 604.800.000 ms (7 gün) |
| MFA | `jwt.mfa.expiration` | (env yok, sabit) | 300.000 ms (5dk) |
| Guest | `jwt.guest.expiration` | `JWT_GUEST_EXPIRATION` | 3.600.000 ms (1sa) |

Cookie max-age'leri JWT sürelerinden TÜRETİLİR (`AuthCookieWriter`) — ayrı cookie env'i yoktur.

## Token Okuma — header-only (4ce65c2)
- Backend token'ı YALNIZCA `Authorization: Bearer` header'ından okur (`JwtAuthenticationFilter`)
- Cookie'den token OKUNMAZ (`AuthTokenResolver` silindi); login'de HttpOnly cookie yine YAZILIR (BFF/SSR için)

## Login Akışları

| Akış | Endpoint | Rol şartı | loginSource |
|------|----------|-----------|-------------|
| Panel | `POST /api/v1/auth/login` + `loginType:"admin"` | panel rolü (SUPER_ADMIN/ADMIN/EDITOR/VIEWER) | admin |
| Tenant | `POST /api/v1/public/{tid}/auth/login` + `loginType:"tenant"` | TENANT rolü | tenant |
| Guest | `POST /api/v1/public/{tid}/auth/guest-token` (tenantId+sessionId zorunlu) | — | website |

- Ayrım SİMETRİK: panel kullanıcısı tenant site'a giremez, tersi de (AuthService.login rol kontrolü)
- Login her zaman CONTEXT tenant'ında doğrular — cross-tenant login yapısal olarak imkânsız
- Register (public) → TENANT rolü atar; admin user yalnız panel'den `POST /api/v1/users` ile

## 2FA (TOTP) — 51ee215
- RFC 6238, harici kütüphanesiz `TotpService` (SHA1, 6 hane, 30sn, ±1 adım tolerans)
- Secret AES şifreli saklanır (`User.mfa_enabled/mfa_secret/mfa_setup_verified`)
- **Akış:** login'de MFA aktif+setupVerified ise JWT ÜRETİLMEZ, yalnız `mfaToken` (5dk) döner →
  `POST /api/v1/auth/mfa/verify` + TOTP kodu → tam JWT (`issueFullAuthToken`)
- Setup: `GET /api/v1/auth/mfa/setup` (QR/otpauth URI) → `POST /api/v1/auth/mfa/setup/verify`
- Diğer: `GET /mfa/status`, `POST /mfa/disable` (`MfaController`)
- DİKKAT: `/mfa/setup` her çağrıda YENİ secret üretir ve mfaEnabled=false yapar — aktif 2FA'yı sıfırlar

## SecurityConfig Kuralları (güncel)
```java
// permitAll: /api/v1/auth/**, GET+POST /api/v1/public/**, /ws/**, swagger, actuator
// Stateless, CSRF disabled, DaoAuthenticationProvider
// Method security: @EnableMethodSecurity + @PreAuthorize("hasAuthority('xxx:yyy')")
```

## Yeni Endpoint Güvenliği — Checklist
1. Public mi? → `SecurityConfig`'e `requestMatchers(...).permitAll()`; anonim tenant akışı ise
   `PublicApiFilter` allowlist'ine ekle
2. Method-level: `@PreAuthorize("hasAuthority('modul:eylem')")` — permission `PermissionConstants`'ta tanımlı olmalı
3. `@Transactional` auth endpoint'i ise TenantContext'i CONTROLLER'da (transaction'dan ÖNCE) kur
4. Hiyerarşi gerekiyorsa: rol seviyeleri SUPER_ADMIN=4, ADMIN=3, EDITOR=2, VIEWER/TENANT=1

## Refresh Token Rotation
- Refresh body-based; tenant, refresh token'ın KENDİ tenantId claim'inden çözülür
- Rotation: eski token revoke + `deleteByUserId` + flush → yeni token kaydı
- Şifre reset / kullanıcı silme / pasifleştirme → refresh token cleanup + auth cache evict (açık oturumlar düşer)

## Filter Sırası
```
JwtTenantFilter (@Order(1))     ← JWT tenantId claim → TenantContext (finally'de clear)
  → JwtAuthenticationFilter     ← Bearer token doğrulama + Redis auth cache + tokenVersion kontrolü
                                   tenantId claim yoksa TENANT_REQUIRED (400)
(PublicApiFilter — /api/v1/public/{tid}/** anonim akışı: tenant çözer, path rewrite eder)
```

---
name: spring-security-patterns
description: Elly'deki JWT, OAuth2 ve Spring Security pattern'leri. Auth flow değişikliği, yeni endpoint güvenliği, token handling, SecurityConfig güncelleme veya OAuth2 konfigürasyonu ile ilgili konularda otomatik aktif et.
version: 1.0.0
---

# Elly Spring Security & Auth Patterns

## JWT Yapısı
```java
// Token oluşturma
JwtUtil.generateToken(user, loginSource, tenantId)        // Access token
JwtUtil.generateRefreshToken(user, loginSource, tenantId) // Refresh token

// JWT Claims
// - sub: kullanıcı email'i
// - loginSource: "ADMIN" | "TENANT" | "OAUTH2"
// - tenantId: "basedb" | "tenant1" | "tenant2"
```

## Login Akışları

| Akış | Endpoint | Interceptor | loginSource |
|------|----------|-------------|-------------|
| Admin | `POST /api/auth/admin/login` | `AdminLoginInterceptor` | ADMIN |
| Tenant | `POST /api/auth/login` | `TenantLoginInterceptor` | TENANT |
| OAuth2 | `/oauth2/authorize/{provider}` | `OAuth2AuthenticationSuccessHandler` | OAUTH2 |

## SecurityConfig Kuralları

```java
// Public endpoint ekleme
.requestMatchers("/api/public/**").permitAll()
.requestMatchers("/api/auth/**").permitAll()

// Admin-only
.requestMatchers("/api/admin/**").hasRole("ADMIN")

// Authenticated tenant kullanıcıları
.requestMatchers("/api/**").authenticated()
```

## Yeni Endpoint Güvenliği — Checklist
1. `SecurityConfig.java`'ya uygun `requestMatchers` kuralı ekle
2. Method-level güvenlik gerekiyorsa: `@PreAuthorize("hasRole('ADMIN')")`
3. Admin endpoint ise: `loginSource` claim'ini doğrula
4. Tenant endpoint ise: `tenantId` claim'i context'e set edilmiş mi kontrol et

## OAuth2 Provider'lar
- Google, Facebook, GitHub
- User bilgisi: `CustomOAuth2UserService` → `CustomOAuth2User`
- Başarı callback: `OAuth2AuthenticationSuccessHandler.onAuthenticationSuccess()`
- Başarı sonrası JWT üretimi aynı `JwtUtil` ile yapılır

## Filter Sırası
```
JwtAuthenticationFilter        ← JWT doğrulama, SecurityContext'e yükle
  → JwtTenantFilter             ← tenantId'yi TenantContext'e set et
    → AdminLoginInterceptor     ← /admin/** için loginSource kontrolü
    → TenantLoginInterceptor    ← /api/** için tenant kontrolü
```

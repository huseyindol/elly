# RBAC Permission System — Elly CMS

## Durum: ✅ Tamamlandı (2026-04-10)

## Mimari
- **User** → ManyToMany → **Role** → ManyToMany → **Permission**
- Spring Security `@EnableMethodSecurity` + `@PreAuthorize("hasAuthority('modül:işlem')")` yapısı
- Tüm API'ler artık JWT authenticated (`.anyRequest().authenticated()`)

## Entity'ler
- `Permission` — name (ör: `posts:create`), module, description
- `Role` — name (ör: `SUPER_ADMIN`), ManyToMany → Permission
- `User` — ManyToMany → Role (FetchType.EAGER)

## DB Tabloları
- `permissions`, `roles`, `role_permissions`, `user_roles`

## Varsayılan Roller
| Rol | İzinler |
|-----|---------|
| SUPER_ADMIN | Tüm izinler (40+) |
| ADMIN | roles:* ve users:manage hariç |
| EDITOR | İçerik modülleri (posts, pages, components, widgets, banners, assets, comments, forms, ratings, contents, basic_infos) |
| VIEWER | Tüm *:read izinleri |

## Permission Modülleri
POSTS, PAGES, COMPONENTS, WIDGETS, BANNERS, ASSETS, COMMENTS, FORMS, RATINGS,
CONTENTS, BASIC_INFOS, MAIL, EMAILS, CACHE, TENANTS, USERS, ROLES

## Önemli Dosyalar
- `com.cms.config.PermissionConstants` — Tüm permission string sabitleri
- `com.cms.config.CustomUserDetailsService` — getAuthorities() roller/izinler
- `com.cms.config.SecurityConfig` — @EnableMethodSecurity, .anyRequest().authenticated()
- `com.cms.config.DataInitializer` — Başlangıç veri seed'i (idempotent)
- `com.cms.service.impl.RoleService` — Multi-tenant aware rol yönetimi (defaultTenant)
- `com.cms.controller.impl.RoleController` — /api/v1/roles CRUD + atamalar

## Rol Yönetim API
- GET /api/v1/roles — tüm roller
- GET /api/v1/roles/{id} — rol detayı
- POST /api/v1/roles — yeni rol
- PUT /api/v1/roles/{id} — rol güncelle
- DELETE /api/v1/roles/{id} — rol sil
- PUT /api/v1/roles/{roleId}/permissions — role izin ata
- PUT /api/v1/roles/users/{userId}/roles — kullanıcıya rol ata
- GET /api/v1/roles/permissions — tüm izinler
- GET /api/v1/roles/permissions/module/{module} — modüle göre izinler

## Public Endpoint'ler (Auth gerektirmeyen)
- /api/v1/auth/** — login, register, token refresh, public-token
- /oauth2/**, /login/oauth2/** — OAuth2
- /swagger-ui/**, /api-docs/** — API docs
- /actuator/** — health check

## Admin Panel Kullanıcı Yönetimi (2026-04-29'dan itibaren)

### Panel Admin Kullanıcıları (basedb)
```
GET  /api/v1/users       → basedb admin kullanıcılarını listele  (users:manage)
GET  /api/v1/users/{id}  → tek admin kullanıcı                   (users:manage)
```
Admin JWT (loginSource=admin) ile kullanılır.

### Tenant Kullanıcı Yönetimi (tenant DB'leri)
Tüm endpoint'ler `AdminLoginInterceptor` (loginSource=admin) + `users:manage` yetkisi gerektirir.
```
POST   /api/v1/admin/tenants/{tenantId}/users              → oluştur
GET    /api/v1/admin/tenants/{tenantId}/users              → listele
GET    /api/v1/admin/tenants/{tenantId}/users/{id}         → tek kullanıcı
PUT    /api/v1/admin/tenants/{tenantId}/users/{id}         → güncelle
DELETE /api/v1/admin/tenants/{tenantId}/users/{id}         → sil
PATCH  /api/v1/admin/tenants/{tenantId}/users/{id}/status  → aktif/pasif (?isActive=true/false)
```

### Rol Atama — Admin Kullanıcılarına (basedb)
```
PUT /api/v1/roles/users/{userId}/roles
Body: { "roleIds": [1, 2] }
Yetki: users:manage
```

## Önemli Notlar
1. Mevcut kullanıcılara DataInitializer ile SUPER_ADMIN atanır
2. Roller her zaman defaultTenant (basedb) üzerinde tutulur
3. Tenant kullanıcıları rol atama şu an desteklenmiyor (ileride eklenecek)
4. User /me endpoint'leri sadece authentication gerektirir (spesifik permission yok)
5. AccessDeniedException → 403 JSON handler GlobalExceptionHandler'da
6. Tenant kullanıcısı register: POST /api/auth/register + body'de "tenantId" alanı

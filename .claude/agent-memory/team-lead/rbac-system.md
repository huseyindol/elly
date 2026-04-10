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

## Önemli Notlar
1. Mevcut kullanıcılara DataInitializer ile SUPER_ADMIN atanır
2. Roller her zaman defaultTenant (basedb) üzerinde tutulur
3. Yeni register'da otomatik rol ataması henüz yok
4. User /me endpoint'leri sadece authentication gerektirir (spesifik permission yok)
5. AccessDeniedException → 403 JSON handler GlobalExceptionHandler'da

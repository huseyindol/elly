# Elly CMS — Proje Değişiklik Günlüğü (Changelog)

Bu dosya projedeki orta ve büyük ölçekli geliştirmelerin kaydını tutar.
Her ajan (Claude, Antigravity) bu dosyayı okuyarak projenin geçmişini anlayabilir.

---

## [2026-04-12] Tenant-Based Gmail SMTP Mail Gönderimi
**Tip:** 🆕 Özellik + 🐛 Bugfix | **Boyut:** Orta

### Yapılanlar
- Spring Mail auto-config (`MailSenderAutoConfiguration`) exclude edildi — pod crash root cause çözüldü
- `spring.mail.*` property'leri tamamen kaldırıldı — mail gönderimi artık 100% DB-based (MailAccount)
- RabbitMQ retry queue (30sn TTL gecikme) eklendi — tight-loop retry önlendi
- `TenantMailSenderFactory`: Gmail SSL/TLS port otomasyonu + 10sn connection timeout eklendi
- `POST /api/v1/mail-accounts/{id}/verify` endpoint'i eklendi — mail göndermeden SMTP doğrulaması

### Dosyalar
- Güncellenen: EllyApplication.java, application.properties, RabbitMQConfig.java, TenantMailSenderFactory.java, EmailQueueService.java, MailAccountService.java, MailAccountController.java, IMailAccountService.java, IMailAccountController.java

### Konfigürasyon
- Gmail: smtp.gmail.com:587 (STARTTLS) | Google App Password gerekli (2FA açık olmalı)
- Her tenant panelden kendi mail hesabını ekler → `mail_accounts` tablosu (AES şifreli)

### Breaking Changes
- `spring.mail.*` env variable'ları artık kullanılmıyor (MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD)
- RabbitMQ'da `email-retry-queue` ve `email-retry-exchange` yeni oluşacak

### Detay Dosyası
- `.claude/agent-memory/team-lead/tenant-mail-smtp.md`

---

## [2026-04-10] RBAC Permission System
**Tip:** 🔒 Güvenlik | **Boyut:** Büyük

### Yapılanlar
- **User** → ManyToMany → **Role** → ManyToMany → **Permission** yapısı
- Spring Security `@EnableMethodSecurity` + `@PreAuthorize` ile method-level authorization
- Tüm 16 controller'a permission kontrolü eklendi
- 4 varsayılan rol (SUPER_ADMIN, ADMIN, EDITOR, VIEWER) + 40+ permission
- DataInitializer ile otomatik seed
- Rol yönetim API'si: /api/v1/roles

### Dosyalar
- Entity: Permission.java, Role.java, User.java (roles eklendi)
- Config: PermissionConstants.java, SecurityConfig.java, CustomUserDetailsService.java, DataInitializer.java
- Service: IRoleService.java, RoleService.java
- Controller: IRoleController.java, RoleController.java
- Exception: GlobalExceptionHandler.java (403 handler)

### Breaking Changes
- Tüm API'ler artık JWT + permission gerektirir (eskiden permitAll)
- Public endpoint'ler: /api/v1/auth/**, /oauth2/**, /swagger-ui/**, /actuator/**

---

## [2026-04-10] User Auth Redis Cache Optimization
**Tip:** ⚡ Performans | **Boyut:** Orta

### Yapılanlar
- Her HTTP isteğindeki 3 SQL sorgusunu (users, roles/permissions, tenants) Redis cache ile elimine ettik
- `auth:user:{username}` key'inde 30 dk TTL ile cache
- Cache invalidation: login, refresh, profil güncelleme, şifre değişikliği, rol atama
- CachedUserDetails DTO + UserAuthCacheService (circular dependency önleme)

### Dosyalar
- Yeni: CachedUserDetails.java, UserAuthCacheService.java
- Güncellenen: JwtAuthenticationFilter.java, AuthService.java, UserService.java, RoleService.java

### Performans Etkisi
- Cache'li isteklerde ~189ms → ~50ms (3 SQL → 1 Redis GET)

---

## Değişiklik Kayıt Kuralları

> Bu dosya her orta/büyük geliştirmeden sonra GÜNCELLENMELİDİR.
> Format: Tarih, Tip, Boyut, Yapılanlar, Dosyalar, Breaking Changes

### Otomatik Tetikleme Kuralı
Aşağıdaki durumlardan herhangi birinde bu dosya güncellenmelidir:
1. **Yeni entity / tablo** eklenmesi
2. **Yeni servis / controller** eklenmesi
3. **Güvenlik yapılandırmasında** değişiklik
4. **Mimari değişiklik** (cache, filter, interceptor, vb.)
5. **API endpoint ekleme/silme/değişiklik** (breaking change)
6. **Performans optimizasyonu**
7. **3+ dosyayı etkileyen** herhangi bir geliştirme

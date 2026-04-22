# Elly CMS — Proje Değişiklik Günlüğü (Changelog)

Bu dosya projedeki orta ve büyük ölçekli geliştirmelerin kaydını tutar.
Her ajan (Claude, Antigravity) bu dosyayı okuyarak projenin geçmişini anlayabilir.

---

## [2026-04-21] Mail+Form v2 — DB-based SMTP (AES) + Form-level Sender/Recipient Secimi
**Tip:** 🆕 Özellik + 🔒 Güvenlik + ↩️ Mimari Reversal + 💥 Breaking | **Boyut:** Büyük

### Ozet
v1'de ENV'e tasinmis SMTP kredensiyelleri **geri DB'ye alindi**; ama v1'in form-level explicit sender/recipient secim pattern'i korundu. Hibrit sonuc:
- SMTP host/port/username/password her tenant DB'sinde (`mail_accounts`), `smtp_password` AES-256-CBC sifreli
- ENV'de sadece tek secret: `AES_SECRET_KEY` (32 ASCII karakter)
- Bir tenant birden fazla mail hesabi tanimlayabilir (varsayilan yok)
- Form olustururken hangi hesabin gonderecegi + mailin kime gidecegi explicit secilir

### Yapılanlar
- **v1'den geri alinanlar:** `EnvMailProfileResolver`, `DtoAvailableProfile`, `config/mail/` klasoru, tum `MAIL_{TENANT}_{PROFILE}_*` ENV'leri (15+ secret), `/mail-accounts/available-profiles` endpoint, `MailAccount.envProfileKey`, `mail_accounts.is_default` — hepsi silindi
- **v1'den korunanlar:** `FormDefinition.senderMailAccountId` + `recipientEmail` + `notificationSubject` + `notificationEnabled`, `form-notification.html` template, `FormController`, `FormSubmissionService.triggerNotification()` akisi, RabbitMQ retry/DLQ
- **v0'dan geri getirilenler:** `AesEncryptor.java` (silinmisti), `aes.secret-key` property, `MailAccount` tum SMTP kolonlari (host/port/username/password/fromAddress)
- `MailAccountService`: create()'de smtpPassword zorunlu + `aesEncryptor.encrypt()`; update()'de smtpPassword null/bos ise mevcut sifreli deger korunur (admin UX); delete/update sonrasi `mailSenderFactory.evict(id)`
- `TenantMailSenderFactory`: `ConcurrentHashMap<Long, JavaMailSender>` cache (mailAccountId key), decrypt + SSL/STARTTLS port otomasyonu, 10sn timeout
- `IMailAccountService.getAllActive()` + `/mail-accounts/active` endpoint — form picker UI icin aktif hesap listesi
- `MailAccountRepository.findAllByActiveTrue()` — active=true filtresi
- Response DTO `DtoMailAccountResponse`: `smtpPassword` ASLA donmez
- `FormMapper`: `senderMailAccountName` + `senderFromAddress` read-only alanlar entity'den cekilir (UI display)
- Migration: `db-migration-mail-form-v2.sql` — pre-v1 state'den v2 target'a tek seferde (v1 SQL hic uygulanmamisti)

### Dosyalar

**Yeni:**
- `src/main/resources/db-migration-mail-form-v2.sql` — tek birleşik migration
- `docs/MAIL_FORM_V2_GUIDE.md` — uçtan uca CURL rehberi (AES setup + 7 adım akışı + password preservation)

**Geri getirilen (v1'de silinmişti):**
- `src/main/java/com/cms/util/AesEncryptor.java`

**Yeniden yazılan:**
- Entity: `MailAccount.java` (envProfileKey → host/port/username/password/fromAddress + active, is_default yok)
- DTO: `DtoMailAccountRequest.java` (@Email, @Min/@Max validation), `DtoMailAccountResponse.java` (smtpPassword asla dönmez), `DtoFormDefinition.java` (senderProfileKey → senderMailAccountName + senderFromAddress)
- Mapper: `MailAccountMapper.java` (smtpPassword ignore — service encrypt), `FormMapper.java` (senderMailAccount.id/name/fromAddress source)
- Service: `MailAccountService.java` (AES + evict), `MailTestService.java` (account.fromAddress direkt), `EmailQueueService.java` (account.fromAddress), `FormDefinitionService.validateSender()` (sadece active + recipient valid check), `TenantMailSenderFactory.java` (mailAccountId cache + AES decrypt)
- Controller: `MailAccountController.java` (`/available-profiles` silindi, `/active` eklendi), `IMailAccountController.java`
- Repository: `MailAccountRepository.java` (`findAllByActiveTrue()` eklendi, `findByEnvProfileKey` silindi)

**Silinen:**
- `src/main/java/com/cms/config/mail/EnvMailProfileResolver.java` (+ boş `config/mail/` klasörü)
- `src/main/java/com/cms/dto/DtoAvailableProfile.java`
- `src/main/resources/db-migration-mail-form-v1.sql` (hiç uygulanmamıştı)
- `docs/MAIL_FORM_V1_GUIDE.md`

**Güncellenen (v2 javadoc + küçük ayarlar):**
- `DtoFormDefinitionIU.java`, `FormDefinition.java` javadoc'ları
- `FormSubmissionService.java` (EnvMailProfileResolver bağımlılığı temizlendi)
- `application.properties` — `aes.secret-key=${AES_SECRET_KEY:12345...}` geri eklendi

**DevOps:**
- `.env`, `.env.local`, `.env.remote` — tüm `MAIL_*` bloklar → tek `AES_SECRET_KEY`
- `k8s/1-secret.template.yaml` — 15 `MAIL_*_B64` placeholder → tek `AES_SECRET_KEY_B64`
- `k8s/1-secret.local.yaml` — `AES_SECRET_KEY` eklendi (32 char base64)
- `k8s/1-configmap.yaml` — mail comment v2 açıklamasına güncellendi
- `.github/workflows/deploy.yml` — 15+ `MAIL_*_B64` export → tek `AES_SECRET_KEY_B64` export + docs

### Breaking Changes
- ⚠️ `form_definitions` **TRUNCATE CASCADE** (mevcut formlar + submission'lar silinir)
- ⚠️ `mail_accounts.is_default` kolonu silinir — uygulama varsayilan hesap konseptiyle kodlanmissa kirilir
- ⚠️ `MailAccount.envProfileKey` → SMTP kolonlari geri geldi; v1 fork'u olan baska ortam varsa uyumsuz
- ⚠️ Tüm `MAIL_{TENANT}_{PROFILE}_*` ENV'leri artik OKUNMUYOR — hepsi silinmeli (aksi takdirde sadece boşa alan)
- ⚠️ GitHub Secrets'tan `MAIL_*` keys silinebilir; `AES_SECRET_KEY` secret'i eklenmeli (32 ASCII karakter)
- ⚠️ `GET /api/v1/mail-accounts/available-profiles` endpoint **silindi**; panel bunu çağırıyorsa güncellenmeli
- ⚠️ `GET /api/v1/mail-accounts/active` endpoint eklendi (form picker için)
- ⚠️ `DtoMailAccountRequest` artik SMTP alanlarini bekler (`envProfileKey` yok)
- ⚠️ Pre-v1 veya v1 ortaminda `aes.secret-key` ENV'i silinmisti — geri eklenmeli

### Deployment
1. GitHub Secrets'a `AES_SECRET_KEY` ekle (32 ASCII karakter, yeni üret: `openssl rand -base64 32 | cut -c1-32`)
2. Eski `MAIL_*` secret'ları silinebilir (artık kullanılmıyor)
3. `.env.remote`'a AES_SECRET_KEY gir (prod değeri)
4. Migration her tenant DB'sinde ayrı çalıştır:
   ```bash
   psql -d elly_basedb  -f src/main/resources/db-migration-mail-form-v2.sql
   psql -d elly_tenant1 -f src/main/resources/db-migration-mail-form-v2.sql
   psql -d elly_tenant2 -f src/main/resources/db-migration-mail-form-v2.sql
   ```
5. Deploy sonrası panel'den `POST /mail-accounts` ile hesap ekle → `verify` et → form oluşturmada seç

### Detay Rehber
- **Uçtan uca CURL:** `docs/MAIL_FORM_V2_GUIDE.md`
- **v3 yol haritası:** `.claude/agent-memory/team-lead/v2-mail-form-roadmap.md`
- **v2 karar gerekçesi:** Kullanıcı geri bildirimi — "gönderim yapacak mail bilgilerini host,port,username,password db'de tutulsun birden fazla olabilir"

---

## [2026-04-19] Mail+Form v1 — ENV-based SMTP Profil + Form Notification
> ⚠️ **v2'de geri alındı (2026-04-21).** Form-level sender/recipient pattern'i korundu, ama ENV-based kredensiyel yaklaşımı DB + AES ile değiştirildi. Bu giriş tarihsel referans için tutuluyor — v1 migration SQL hiç uygulanmadığı için TRUNCATE adımı zaten başlanmamıştı.
**Tip:** 🆕 Özellik + 🔒 Güvenlik + 💥 Breaking | **Boyut:** Büyük

### Yapılanlar
- **SMTP kredansialleri DB'den tamamen ENV'e taşındı** — AES şifreleme ihtiyacı kalktı, `AesEncryptor.java` ve `aes.secret-key` kaldırıldı
- **Tenant başına N adet adlandırılmış profil** — `MAIL_{TENANT}_{PROFILE}_{HOST|PORT|USERNAME|PASSWORD|FROM}` ENV konvansiyonu (hiçbir default yok, 5 alan zorunlu)
- `EnvMailProfileResolver` — startup'ta ENV'i tarayıp `{tenant → profileMap}` oluşturur, `incomplete` sayısını log'lar
- `GET /api/v1/mail-accounts/available-profiles` — Admin'in ENV'deki profilleri görüp seçebildiği endpoint
- `MailAccount` entity sadeleşti: `name` + `envProfileKey` (kredansial, `smtp_*`, `from_address`, `is_default` tamamen silindi)
- `FormDefinition` artık iki alan zorunlu tutar: `senderMailAccountId` (FROM) + `recipientEmail` (TO). İsteğe bağlı: `notificationSubject`, `notificationEnabled`
- v1 sabit template: `templates/emails/form-notification.html` (Thymeleaf, responsive inline CSS, payload tablosu)
- `FormSubmissionService` submit sonrası `triggerNotification()` çağırır → `EmailRequest(mailAccountId, to=recipientEmail, templateName="form-notification", dynamicData)` → mevcut RabbitMQ kuyruğuna düşer
- Mail gönderim hatası submission'ı durdurmaz (try-catch); `EmailLog` üzerinden durum takibi yapılır
- Migration: `db-migration-mail-form-v1.sql` — `TRUNCATE mail_accounts + form_definitions CASCADE`, `DROP` legacy kolonlar, `ADD` yeni kolonlar + FK + unique index

### Dosyalar

**Yeni:**
- `src/main/java/com/cms/config/EnvMailProfileResolver.java`
- `src/main/resources/templates/emails/form-notification.html`
- `src/main/resources/db-migration-mail-form-v1.sql`
- `docs/MAIL_FORM_V1_GUIDE.md` — uçtan uca CURL rehberi
- `.claude/agent-memory/team-lead/v2-mail-form-roadmap.md` — v2 plan (multi-recipient, form-bazlı template, runtime refresh, bounce, rate limiting, CAPTCHA)

**Güncellenen:**
- Entity: `MailAccount.java`, `FormDefinition.java`
- DTO: `DtoMailAccount.java`, `DtoMailAccountIU.java`, `DtoFormDefinition.java`, `DtoFormDefinitionIU.java`, `DtoEmailProfile.java` (yeni)
- Mapper: `MailAccountMapper.java`, `FormMapper.java`
- Service: `MailAccountService.java`, `FormDefinitionService.java`, `FormSubmissionService.java`
- Controller: `MailAccountController.java` + `IMailAccountController.java`, `FormController.java`
- Config: `TenantMailSenderFactory.java` (envProfileKey'den alır), `application.properties` (aes.secret-key silindi)
- DevOps: `k8s/1-secret.template.yaml`, `k8s/1-configmap.yaml`, `.github/workflows/deploy.yml`
- ENV: `.env`, `.env.local`, `.env.remote`
- Bootstrap: `EllyApplication.java` (MailSenderAutoConfiguration zaten exclude)

**Silinen:**
- `src/main/java/com/cms/util/AesEncryptor.java`

### Breaking Changes
- ⚠️ `mail_accounts` tablosu **TRUNCATE** edilir — mevcut SMTP kayıtları kaybolur (migration'dan önce yedek al)
- ⚠️ `form_definitions` tablosu **TRUNCATE** edilir — mevcut formlar kaybolur
- ⚠️ `MailAccount` artık `smtp_host/smtp_port/smtp_username/smtp_password/from_address/is_default` içermiyor; yerine `env_profile_key` var
- ⚠️ `FormDefinition.mailAccountId` → `senderMailAccountId` (ZORUNLU), yeni alan: `recipientEmail` (ZORUNLU)
- ⚠️ `EmailRequest.mailAccountId` artık **zorunlu** (tenant'ın varsayılan hesabı diye bir şey yok)
- ⚠️ `AES_SECRET_KEY` ENV variable'ı artık kullanılmıyor — K8s Secret + GitHub Secret'lerden kaldırılabilir
- ⚠️ Legacy `MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD/MAIL_SMTP_AUTH/MAIL_SMTP_STARTTLS` artık kullanılmıyor — sadece `MAIL_{TENANT}_{PROFILE}_*` profilleri geçerli

### Deployment
1. GitHub Secrets'a 5 profil × tenant adedi × 5 alan ekle (örn: `MAIL_TENANT1_SUPPORT_HOST`, ...)
2. `k8s/1-secret.template.yaml` placeholder'ları hazır
3. `.github/workflows/deploy.yml`'de base64 export blokları eklendi
4. Deploy sonrası `envsubst` ile K8s Secret dolar, pod env'den okur
5. Migration her tenant DB'sinde ayrı ayrı çalıştırılır

### Detay Rehber
- **Uçtan uca CURL:** `docs/MAIL_FORM_V1_GUIDE.md`
- **v2 roadmap:** `.claude/agent-memory/team-lead/v2-mail-form-roadmap.md`

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

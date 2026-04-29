# Elly CMS — Proje Değişiklik Günlüğü (Changelog)

Bu dosya projedeki orta ve büyük ölçekli geliştirmelerin kaydını tutar.
Her ajan (Claude, Antigravity) bu dosyayı okuyarak projenin geçmişini anlayabilir.

---

## [2026-04-29] Multi-Tenant User Routing + Admin Tenant User Management
**Tip:** 🔧 Bugfix + 🆕 Özellik | **Boyut:** Orta

### Özet
3 kritik bug düzeltildi ve panel admininin tenant DB'lerindeki kullanıcıları yönetebileceği yeni endpoint seti eklendi.

### Bug Fix 1 — JwtAuthenticationFilter hardcoded basedb
**Sorun:** `loadUserFromDbAndCache()` her zaman `TenantContext.setTenantId(defaultTenant)` yapıyordu. `JwtTenantFilter` doğru context'i set etse bile override ediliyordu.
**Fix:** `TenantContext.setTenantId(defaultTenant)` satırı kaldırıldı. JwtTenantFilter'ın set ettiği context geçerli:
- admin JWT → null → defaultDataSource (basedb)
- tenant JWT → "tenant1" → tenant1 DB

### Bug Fix 2 — AuthService.register() her zaman basedb'ye yazıyordu
**Fix:** `register()` artık `DtoRegister.tenantId` alanını destekliyor. `tenantId` varsa o DB'ye switch edip kaydeder, finally'de restore eder.

### Bug Fix 3 — UserService.executeInDefaultTenant() basedb'yi zorluyor
**Fix:** `getAllUsers()` ve `getUserById()` methodlarındaki `executeInDefaultTenant()` wrapper'ı kaldırıldı. Mevcut TenantContext'e bırakıldı.

### Yeni Özellik — Admin Tenant User Management
**Endpoint seti:** `AdminLoginInterceptor` (loginSource=admin) + `users:manage` yetkisi gerektirir.

| Method | Path | Açıklama |
|--------|------|---------|
| POST | `/api/v1/admin/tenants/{tenantId}/users` | Kullanıcı oluştur |
| GET | `/api/v1/admin/tenants/{tenantId}/users` | Tüm kullanıcıları listele |
| GET | `/api/v1/admin/tenants/{tenantId}/users/{id}` | Tek kullanıcı |
| PUT | `/api/v1/admin/tenants/{tenantId}/users/{id}` | Güncelle |
| DELETE | `/api/v1/admin/tenants/{tenantId}/users/{id}` | Sil |
| PATCH | `/api/v1/admin/tenants/{tenantId}/users/{id}/status?isActive=` | Aktif/pasif |

**Yeni dosyalar:**
- `dto/DtoAdminUserCreate.java`, `dto/DtoAdminUserUpdate.java`
- `service/ITenantUserService.java`, `service/impl/TenantUserService.java`
- `controller/ITenantUserController.java`, `controller/impl/TenantUserController.java`

**WebMvcConfig güncellendi:** AdminLoginInterceptor artık `/api/v1/admin/**` path'ini de kapsamaktadır.

### Mimari Açıklama
```
basedb   → Panel admin kullanıcıları (loginSource="admin")
tenant1  → Tenant1 site kullanıcıları (loginSource="tenant", tenantId="tenant1")
tenant2  → Tenant2 site kullanıcıları
```
Panel guide: `docs/USER_ADMIN_GUIDE.md`

---

## [2026-04-23] v4 Email Templates — Uygulama + Bugfix Oturumu
**Tip:** 🆕 Özellik + 🔧 Bugfix + 🔒 Güvenlik | **Boyut:** Büyük

### Özet
Aynı oturumda 4 bağımsız sorun çözüldü ve v4 Email Templates özelliği sıfırdan uygulandı, production'a alındı.

### Bugfix 1 — Legacy `ApiKeyFilter` Kaldırıldı
**Sorun:** `GET /api/v1/emails` → 401 `INVALID_API_KEY`. Eski `ApiKeyFilter.java` tüm `/api/v1/emails/**` isteklerinde `X-API-KEY` header zorunlu kılıyordu; JWT+RBAC sistemi ile çelişiyordu.
**Fix:** `src/main/java/com/cms/filter/ApiKeyFilter.java` silindi. `application.properties`'den `email.api.key` property'si kaldırıldı. `docs/MAIL_GUIDE.md` güncellendi (X-API-KEY ekleme, JWT ile çalışan sistemde anti-pattern).

### Bugfix 2 — RBAC Rol Senkronizasyonu (`DataInitializer.syncRole`)
**Sorun:** `GET /api/v1/admin/rabbit/queues` → 403 `ACCESS_DENIED`. Yeni `rabbit:read`, `rabbit:manage` permission'ları mevcut rollere atanmamıştı; eski `createRoleIfNotExists` eksik permission ekleme yapmıyordu.
**Fix:** `DataInitializer.java`'daki `createRoleIfNotExists` → `syncRole` olarak yeniden yazıldı. `syncRole` mevcut rolde eksik permission varsa `addAll` yapar, yoksa idempotent geçer. Her deploy'da otomatik çalışır.

### Bugfix 3 — RabbitMQ Management API Double URL Encoding
**Sorun:** `GET /api/v1/admin/rabbit/queues` → 500, `"404 Not Found: Object Not Found"`. vhost `/` → `%2F` encode ediliyordu ama Spring `RestClient` bunu `%252F` olarak tekrar encode ediyordu.
**Fix:** `RabbitMgmtClientConfig.java`'ya `DefaultUriBuilderFactory.EncodingMode.NONE` eklendi — RestClient pre-encoded URI segment'lerini yeniden encode etmez.

### v4 Email Templates — Yeni Özellik
**Hedef:** Classpath-bağımlı tek template'den panel üzerinden yönetilebilir, tenant-aware, DB-hosted template sistemine geçiş.

**Mimari karar:** DB-first → classpath fallback. Tenant-specific template varsa o kullanılır, yoksa global (tenantId=null) template, o da yoksa classpath HTML dosyası.

**Circular dependency çözümü:** `EmailTemplateService` → `EmailTemplateRenderer` → `IEmailTemplateService` döngüsü `EmailTemplateLookupService` ile kırıldı (ayrı bean, sadece `EmailTemplateRepository` bağımlılığı).

**Yeni dosyalar:**
| Dosya | Açıklama |
|-------|----------|
| `entity/EmailTemplate.java` | JPA entity, `tenant_id=null` = global, optimistic lock |
| `repository/EmailTemplateRepository.java` | `findByTenantIdAndTemplateKey`, `findByTenantIdIsNull...` |
| `dto/DtoEmailTemplate.java` + IU + Preview DTO'ları | API contract |
| `service/IEmailTemplateService.java` + `EmailTemplateService.java` | CRUD + önbellek temizleme |
| `service/impl/EmailTemplateLookupService.java` | `@Cacheable("emailTemplates")` ile tenant-aware cached load — circular dep çözümü |
| `service/impl/EmailTemplateRenderer.java` | DB-first → classpath fallback Thymeleaf render |
| `controller/IEmailTemplateController.java` + `EmailTemplateController.java` | `/api/v1/email-templates` CRUD + preview |
| `config/EmailTemplateBootstrapRunner.java` | İlk deploy'da classpath + inline template'leri DB'ye seed eder (idempotent) |
| `src/main/resources/migration/db-migration-email-templates-v4.sql` | Tablo + index migration |

**Yeni permission'lar:** `EMAIL_TEMPLATES_READ`, `EMAIL_TEMPLATES_MANAGE` — `PermissionConstants.java` + `DataInitializer.syncRole`

**Bootstrap seed template'leri (3 adet):**
- `form-notification` — classpath'tan (zaten vardı)
- `welcome` — inline HTML, kullanıcı kayıt hoşgeldin maili (`userName`, `dashboardUrl`)
- `password-reset` — inline HTML, şifre sıfırlama maili (`userName`, `resetUrl`, `resetCode`, `expiresIn`)

**Yeni endpoint'ler:**
| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/v1/email-templates` | `email_templates:read` |
| GET | `/api/v1/email-templates/{key}` | `email_templates:read` |
| POST | `/api/v1/email-templates` | `email_templates:manage` |
| PUT | `/api/v1/email-templates/{key}` | `email_templates:manage` |
| DELETE | `/api/v1/email-templates/{key}` | `email_templates:manage` |
| POST | `/api/v1/email-templates/{key}/preview` | `email_templates:read` |

**DB migration:** 3 ayrı tenant DB'sine manuel `kubectl exec` ile uygulandı (basedb, tenant1, tenant2). ConfigMap `JPA_DDL_AUTO: validate` nedeniyle Hibernate schema oluşturmaz.

**Production incident:** İlk deploy'da `CrashLoopBackOff` — circular dependency hatası. `EmailTemplateLookupService` refaktörü ile çözüldü, ikinci deploy başarılı.

### SQL migration dosyaları `migration/` klasörüne taşındı
`src/main/resources/migration/` altında organize edildi: `db-indexes.sql`, `db-migration-ratings.sql`, `db-migration-mail-form-v2.sql`, `db-migration-email-templates-v4.sql`, `db-migration-mail-accounts.sql`, `db-performance-indexes.sql`, `migration-add-token-version.sql`.

### Dosyalar
**Silinen:**
- `src/main/java/com/cms/filter/ApiKeyFilter.java`

**Değiştirilen:**
- `src/main/java/com/cms/config/DataInitializer.java` — `syncRole` pattern
- `src/main/java/com/cms/config/PermissionConstants.java` — 2 yeni permission
- `src/main/java/com/cms/config/RabbitMgmtClientConfig.java` — `EncodingMode.NONE`
- `src/main/java/com/cms/service/impl/EmailQueueService.java` — `EmailTemplateRenderer` entegre
- `src/main/resources/application.properties` — `email.api.key` kaldırıldı
- `docs/MAIL_GUIDE.md` — X-API-KEY anti-pattern notu
- `.claude/agent-memory/team-lead/elly-admin-panel-integration-prompts.md` — URL düzeltmeleri, v4 template listesi

---

## [2026-04-23] Mail v3 (Retry Endpoint) + v4 Tasarim + RabbitMQ Admin API
**Tip:** ✨ Feature + 🏗 Tasarim | **Boyut:** Buyuk

### Ozet
Uc is parcacigi tek iterasyonda tamamlandi:
1. **v3:** `POST /api/v1/emails/{id}/retry` endpoint'i — FAILED/PENDING mail'i tek call ile reset + re-publish eder
2. **v4 tasarimi:** Template hosting kararina dair 3-opsiyon analizi → **Opsiyon C (Template Registry DB'de)** onerildi. Kullanicinin "CMS hafif kalsin" hedefine uygun, panel-hosted runtime render'siz.
3. **RabbitMQ admin proxy API:** Panel'den RabbitMQ'yu yonetecek CMS backend endpoint'leri (`/api/v1/admin/rabbit/*`). Panel repo ayri oldugu icin UI entegrasyon dokumani da hazirlandi.

Ayrica **mail-smoke-test** skill'i yazildi — kullanici "mail smoke test yap" dedigi zaman tam otomasyon calisir.

### v3 — Email Retry Endpoint

**Yeni endpoint'ler:**
| Method | Path | Permission |
|---|---|---|
| POST | `/api/v1/emails/{id}/retry` | `emails:retry` |
| GET | `/api/v1/emails?status=&page=&size=` | `emails:read` |

**Retry semantik:**
- SENT kayitlari tekrar gonderilmez (400 `VALIDATION_ERROR`)
- `status=PENDING`, `retryCount=0`, `errorMessage=null` olarak reset
- Ayni RabbitMQ exchange + routing key ile tekrar publish
- `@Transactional` + `ResourceNotFoundException(404)` / `ValidationException(400)` hierarşisi
- JOIN FETCH ile MailAccount lazy proxy initialize edilir (consumer pattern'i ile simetrik)

**Degisen dosyalar:**
- `PermissionConstants.java`: `EMAILS_RETRY = "emails:retry"` eklendi
- `IEmailService.java`, `EmailService.java`: `retry(Long)` + `list(EmailStatus, Pageable)` + private `toDto()` helper (DRY)
- `EmailLogRepository.java`: `findByStatus(EmailStatus, Pageable)` eklendi
- `IEmailController.java`, `EmailController.java`: yeni endpoint'ler + `@PageableDefault(size=20, sort="id", desc)`

### v4 Mail Architecture — Tasarim Karari

**Soru:** Template'ler elly-admin-panel'e tasinsin mi? Akis: CMS POST → panel render → HTML → CMS SMTP?

**Cevap:** Hayir, daha iyi bir yol var. `.claude/agent-memory/team-lead/v4-mail-architecture-proposal.md`'de tam analiz:

| Opsiyon | Akis | Verdikt |
|---|---|---|
| A | Panel on-demand render (kullanicinin onerisi) | ❌ Her mailde network hop + tek ariza noktasi |
| B | Panel push to S3/Git | ⚠️ Yeni altyapi gerektirir |
| **C** | **Template Registry CMS DB'de + panel CRUD** | **✅ ONERI** |

**Neden C:**
- Runtime cross-service call YOK (v2 performans profili korunur)
- Panel sadece CRUD UI saglar, render mantigi CMS'te (zaten Thymeleaf entegre)
- Redis cache, tenant-aware override, optimistic lock versiyonlama hazir geldigi icin
- Classpath fallback ile zero-downtime migration

**Uygulama icin hazir:** yeni `email_templates` tablosu semasi + bootstrap runner + 6 endpoint + permission'lar dokumante edildi. Bir sonraki iterasyonda kod yazilabilir.

### RabbitMQ Admin Proxy — Panel Entegrasyonu

**Sorun:** Kullanici "RabbitMQ management UI panel'de olsun" dedi, ama panel'i dogrudan `:15672`'ye bagmak credential hygiene + network izolasyonu acisindan yanlis olur.

**Cozum:** CMS thin proxy. Panel → CMS JWT → CMS → internal RabbitMQ management API.

**Yeni endpoint'ler (CMS'te):**
| Method | Path | Permission |
|---|---|---|
| GET | `/api/v1/admin/rabbit/overview` | `rabbit:read` |
| GET | `/api/v1/admin/rabbit/queues` | `rabbit:read` |
| GET | `/api/v1/admin/rabbit/queues/{name}` | `rabbit:read` |
| GET | `/api/v1/admin/rabbit/queues/{name}/messages?count=10` | `rabbit:read` |
| POST | `/api/v1/admin/rabbit/queues/{name}/purge` | `rabbit:manage` |
| POST | `/api/v1/admin/rabbit/queues/{name}/republish` | `rabbit:manage` |
| DELETE | `/api/v1/admin/rabbit/queues/{name}/contents` | `rabbit:manage` (purge alias) |

**Implementation notlari:**
- `RestClient` (Spring 6.1+) kullanildi — WebFlux starter eklemedi (sync cagrilar icin gereksiz)
- `SimpleClientHttpRequestFactory` + 2s connect / 5s read timeout
- `BrokerUnavailableException extends BaseException` → `GlobalExceptionHandler` otomatik 503 doner
- vhost `/` → `%2F` URL-encode edildi (management API gereksinimi)
- Peek `ackmode=ack_requeue_true` (mesaj queue'dan silinmez)

**Yeni Permission'lar:** `RABBIT_READ`, `RABBIT_MANAGE` — DB seed'e eklenmesi gerekiyor (RBAC migration sonraki is).

**Panel UI entegrasyonu:** `.claude/agent-memory/team-lead/rabbitmq-admin-api-design.md` — tam tasarim, dosya/sayfa/component semasi, API client kodu, confirm modal UX pattern'leri.

### Yeni Skill: `mail-smoke-test`

Kullanici "mail smoke test yap" dediginde tam otomasyon:
1. Login (JWT al)
2. Mail account varsa kullan / yoksa yeni olustur + verify
3. Form varsa kullan / yoksa yeni olustur
4. Dummy payload uret (unique timestamp + random id)
5. `/forms/{id}/submit` cagir
6. EmailLog DB kontrolu + log grep
7. Kullaniciya ozet ciktiyi markdown tablo halinde sun

Detay: `.claude/skills/mail-smoke-test/SKILL.md`

### Sonraki Iterasyon (v4 / v5 kuyrukta)

- [ ] **v4 uygulama:** `email_templates` tablosu + `EmailTemplate` entity + CRUD endpoint'leri + bootstrap runner
- [ ] **RBAC seed:** `rabbit:read`, `rabbit:manage`, `email_templates:*` permission'larini role'lere bagla
- [ ] **Panel UI:** Rabbit yonetim sayfasi + email template editor (elly-admin-panel repo'sunda)
- [ ] **Integration tests:** `RabbitAdminService` wiremock ile 15672 mock

---

## [2026-04-23] Mail+Form v2 — Prod Hotfix'leri + Smoke Test Onayi
**Tip:** 🔧 Hotfix + ⚙️ Konfigurasyon + ✅ Dogrulama | **Boyut:** Orta

### Ozet
v2'nin prod'a ilk deploy'u sonrasi ortaya cikan iki sorun giderildi ve end-to-end smoke test yapildi:
1. **JPA DDL auto "update" orphan constraint yaratiyordu** → ENV-driven, prod'da `validate`
2. **RabbitMQ consumer `LazyInitializationException` atiyordu** → `LEFT JOIN FETCH` ile fix

Mail v2 artik production'da calisiyor — AES encrypt/decrypt, Gmail SMTP, RabbitMQ retry, Thymeleaf template, tum zincir dogrulandi.

### Hotfix 1 — JPA DDL Strategy (commit `d66568d`)
**Sorun:** `JpaConfig.java`'da `hbm2ddl.auto=update` iki yerde hardcoded. Her pod restart'ta Hibernate entity farklarini otomatik `ALTER TABLE` yapmaya calisiyordu:
- Migration'dan once partial schema uyguluyordu → orphan FK (`fknopjtmmekojw3wns3w0k4kii3`)
- Null iceren kolonlara NOT NULL eklemeye calisiyordu → crash
- Named constraint'leri farketmiyor, kendisi hash'li FK olusturuyordu

**Fix:**
- `application.properties`: `app.jpa.ddl-auto=${JPA_DDL_AUTO:update}` (local default)
- `JpaConfig.java`: `@Value` injection, iki hardcoded `"update"` kaldirildi, `setGenerateDdl()` ddl-auto'ya bagli
- `k8s/1-configmap.yaml`: `JPA_DDL_AUTO: "validate"` — prod'da Hibernate sadece dogrular, ALTER TABLE yapmaz

**Sonuc:** Migration'lar artik manuel SQL ile yonetilir; Hibernate beklenmedik degisiklik yapmaz.

### Hotfix 2 — Consumer LazyInitializationException (commit `aa943ef`)
**Sorun:** `EmailQueueService.processEmailMessage` @Transactional degil. `findById(emailLogId)` kendi kisa tx'inde fetch yapiyordu, session kapaniyordu. Sonra `emailLog.getMailAccount()` lazy proxy'ye erisince:

```
Could not initialize proxy [com.cms.entity.MailAccount#1] - no session
```

Her form submit mail'i 3 retry sonrasi FAILED'e dusuyordu.

**Fix:**
- `EmailLogRepository.findByIdWithMailAccount(Long)`: `LEFT JOIN FETCH el.mailAccount` — proxy ayni sorguda init olur
- `EmailQueueService`: `findById()` yerine bunu kullaniyor

**Sonuc:** Submit → EmailLog PENDING → consumer alir → mailAccount init, SMTP gonderir → SENT. Toplam 4.3 saniye.

### Smoke Test Sonuclari (prod, `api.huseyindol.com`)

1. ✅ `POST /api/v1/auth/login` (admin mode) — JWT token
2. ✅ `POST /api/v1/mail-accounts` — `id=1`, smtp_password AES-256-CBC sifreli DB'de
3. ✅ `POST /api/v1/mail-accounts/1/verify` → `"SMTP baglantisi basarili"` (decrypt + Gmail 587/STARTTLS login OK)
4. ✅ `POST /api/v1/forms` — `id=3`, senderMailAccountId=1, recipient=huseyindoldev@gmail.com
5. ✅ `POST /api/v1/forms/3/submit` — submissionId=7, EmailLog id=4
6. ✅ Consumer: `"Mail gonderildi: logId=4, mailAccountId=1, from=huseyindoldev@gmail.com, to=huseyindoldev@gmail.com"` (23:06:19)
7. ✅ EmailLog: `status=SENT`, `retry_count=0`, `sent_at` dolu

### Dosyalar
- `src/main/java/com/cms/config/JpaConfig.java` — ddl-auto ENV-driven
- `src/main/resources/application.properties` — `app.jpa.ddl-auto` + dokuman
- `k8s/1-configmap.yaml` — `JPA_DDL_AUTO: "validate"`
- `src/main/java/com/cms/repository/EmailLogRepository.java` — `findByIdWithMailAccount`
- `src/main/java/com/cms/service/impl/EmailQueueService.java` — JOIN FETCH query kullanimi

### Dagitim Detaylari
- Migration zaten 2026-04-21'de uygulanmisti (3 DB: basedb, tenant1, tenant2).
- `aa943ef` deploy'u sonrasi orphan FK temizligi ek olarak gerekmedi — migration + validate kombinasyonu schema'yi temizledi.
- ConfigMap degisikligi CI workflow'u tarafindan APPLY EDILMIYOR — manuel `kubectl patch` gerekli (ya da rollout restart'tan once `kubectl apply -f k8s/1-configmap.yaml`). Bu v3 roadmap'e not dusuldu.

### Oncul Guvenlik Uyarilari
- Smoke test sirasinda admin sifresi (112233) ve Gmail App Password sohbette paylasildi. User'in ikisini de rotate etmesi gerekir.
- v3 icin AES_SECRET_KEY rotation endpoint'i hala acik (v2-mail-form-roadmap.md madde 8).

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

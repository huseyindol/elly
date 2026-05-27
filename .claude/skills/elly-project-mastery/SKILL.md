---
name: elly-project-mastery
description: Elly CMS projesine tam hakimiyet skill'i. Her yeni oturumda OTOMATIK aktif et — proje durumunu oku, kaldığın yerden devam et, geliştirmeleri kaydet. Sıfırdan başlama, mevcut halinden al.
version: 2.0.0
triggers:
  - oturum başlangıcı
  - "nerede kalmıştık"
  - "devam et"
  - yeni özellik geliştirme
  - hata ayıklama
  - mimari karar
---

# Elly CMS — Proje Hakimiyet Skill'i

Bu skill, Claude'un Elly CMS projesinde tam bağlam sürekliliği sağlaması için tasarlanmıştır.
Her oturumda bu skill aktif olmalı. Projede sıfırdan değil, mevcut birikimden devam et.

---

## OTURUM BAŞLANGICI — Zorunlu Kontrol Listesi

Yeni bir konuşma başladığında şu sırayla oku:

```
1. .claude/agent-memory/team-lead/changelog.md        → Son yapılanlar
2. .claude/agent-memory/team-lead/*.md (aktif olanlar) → Devam eden görevler
3. git log --oneline -10                              → Son commit'ler
4. git status                                         → Uncommitted değişiklikler
```

Bunu yaptıktan sonra kullanıcıya şunu sun:
> "Son oturumda [X] yapıldı. Aktif görev: [Y]. Devam mı edelim?"

---

## PROJE DURUM SNAPSHOTU (2026-04-29)

### Tamamlanan Büyük Özellikler

| Tarih | Özellik | Durum | Detay Dosyası |
|-------|---------|-------|---------------|
| 2026-04-29 | Multi-Tenant User Routing + Admin Tenant User Management | ✅ | changelog.md, docs/USER_ADMIN_GUIDE.md |
| 2026-04-23 | v4 Email Templates — DB-hosted, tenant-aware, panel CRUD + preview | ✅ | changelog.md |
| 2026-04-23 | RabbitMQ Admin Proxy API — CMS thin proxy, JWT korumalı | ✅ | rabbitmq-admin-api-design.md |
| 2026-04-23 | Mail v3 — Email retry endpoint | ✅ | changelog.md |
| 2026-04-21 | Mail+Form v2 — DB-based SMTP (AES) + Form-level sender/recipient secimi | ✅ | docs/MAIL_FORM_V2_GUIDE.md |
| 2026-04-12 | Tenant-Based Gmail SMTP + RabbitMQ Retry | ✅ | tenant-mail-smtp.md |
| 2026-04-10 | RBAC Permission System (Role/Permission/PreAuthorize) | ✅ | changelog.md |
| 2026-04-10 | User Auth Redis Cache (auth:user:{username}) | ✅ | changelog.md |

### Mevcut Mimari Katmanlar

```
EllyApplication.java
  ↓ exclude: MailSenderAutoConfiguration (pod crash önlemi)

JWT Flow:
  Request → JwtTenantFilter → TenantContext.setCurrentTenant(tenantId)
           → JwtAuthenticationFilter → CachedUserDetails (Redis 30dk)

Mail Flow (v2 — DB-based, AES-encrypted):
  ENV: AES_SECRET_KEY (32 ASCII karakter, tek secret)
  → Panel: POST /mail-accounts (name, fromAddress, smtpHost/Port/Username/Password)
          → MailAccountService.create() → aesEncryptor.encrypt(smtpPassword) → DB
  → Panel: POST /mail-accounts/{id}/verify → TenantMailSenderFactory.getMailSender() → testConnection()
  → Panel: GET /mail-accounts/active → form picker listesi
  → POST /forms (senderMailAccountId + recipientEmail ile)
  → POST /forms/{id}/submit → FormSubmission kaydedilir
  → triggerNotification → EmailRequest(mailAccountId, to=recipientEmail, templateName="form-notification")
  → EmailLog(PENDING) → RabbitMQ email-queue
  → EmailQueueService → TenantMailSenderFactory.getMailSender(account) [decrypt] → Gmail SMTP
  → Hata: retry-queue (30sn TTL) → tekrar email-queue (max 3 kez) → DLQ

Auth Flow:
  Admin: POST /api/auth/admin/login → AdminLoginInterceptor
  Tenant: POST /api/auth/login → TenantLoginInterceptor
  OAuth2: Google/Facebook/GitHub → OAuth2AuthenticationSuccessHandler
```

### Aktif Teknoloji Stack

- **Runtime:** Java 21, Spring Boot 3.5.7
- **DB:** PostgreSQL (database-per-tenant: basedb, tenant1, tenant2)
- **Cache:** Redis (`auth:user:{username}` TTL=30dk, genel cache TTL=10dk)
- **Queue:** RabbitMQ (email-queue, email-retry-queue, email-dlq)
- **Auth:** JWT + OAuth2 (Google, Facebook, GitHub)
- **Mail:** DB-based (`mail_accounts` tablosu, smtp_password AES-256-CBC), form-level sender/recipient secimi — v2 (2026-04-21)
- **Security:** RBAC (@PreAuthorize + Permission entity, 40+ permission)
- **Exception:** BaseException hiyerarsisi + GlobalExceptionHandler

### Kritik Konfigürasyon Kararları

| Karar | Neden | Dosya |
|-------|-------|-------|
| spring.mail.* kaldırıldı | Pod CrashLoopBackOff önlemek için | EllyApplication.java |
| DB-based mail SMTP (v2) | Multiple hesap + form-level explicit secim; runtime ekleme icin pod restart gereksiz | MailAccount.java, TenantMailSenderFactory.java |
| AES-256-CBC smtp_password (v2) | Kredensiyelin DB'de duz metin olmasi guvenlik riski | AesEncryptor.java, application.properties |
| is_default kolonu kaldirildi (v2) | Her form explicit hesap secer, varsayilan konsepti yok | db-migration-mail-form-v2.sql |
| Form'da sender + recipient zorunlu | Admin hangi mailden hangi alıcıya gönderileceğini açıkça seçer | FormDefinition.java |
| smtpPassword null/bos -> mevcut korunur | Admin UX: update'te her seferinde App Password girmek zorunda kalmasin | MailAccountService.update() |
| JavaMailSender cache (mailAccountId key) | Her gonderimde yeni sender kurmak yerine reuse | TenantMailSenderFactory |
| Redis user auth cache | Her request'teki 3 SQL → 1 Redis GET | UserAuthCacheService.java |
| Constructor injection zorunlu | @Autowired yasak | Tüm servisler |
| RabbitMQ retry TTL=30sn | Tight-loop retry önlemek için | RabbitMQConfig.java |
|| ApiKeyFilter silindi | JWT+RBAC sistemi ile çelişiyordu, multi-tenant auth’da anti-pattern | (silindi) |
|| DataInitializer.syncRole | Yeni permission’lar mevcut rollere otomatik eklenir; createRoleIfNotExists eski | DataInitializer.java |
|| RabbitMgmtClient EncodingMode.NONE | RestClient pre-encoded %2F’yi %252F’ye çeviriyordu — NONE ile önlendi | RabbitMgmtClientConfig.java |
|| EmailTemplateLookupService ayrı bean | EmailTemplateService↔EmailTemplateRenderer circular dep’i kırmak için | EmailTemplateLookupService.java |
|| Email template DB-first → classpath fallback | Zero-downtime geçiş: DB’de yoksa classpath’tan yükle | EmailTemplateRenderer.java |
|| Bootstrap seed idempotent | existsByKey kontrolü — her deploy’da aynı template tekrar eklenmez | EmailTemplateBootstrapRunner.java |

---

## KRİTİK DOSYALAR — Hızlı Referans

### Config Katmanı
```
src/main/java/com/cms/config/
├── DataSourceConfig.java          — Multi-tenant DataSource routing
├── RabbitMQConfig.java            — Queue/Exchange tanımları + retry
├── SecurityConfig.java            — JWT filter, OAuth2, permitAll listesi
├── TenantMailSenderFactory.java   — Tenant-based JavaMailSender factory
└── RedisConfig.java               — Cache konfigürasyonu
```

### Auth & Security
```
src/main/java/com/cms/
├── filter/JwtTenantFilter.java         — Tenant context kurulumu
├── filter/JwtAuthenticationFilter.java — JWT doğrulama + Redis cache
├── security/CustomUserDetailsService.java
├── util/JwtUtil.java                   — Token üretimi (loginSource, tenantId)
└── entity/Role.java / Permission.java  — RBAC entity'leri
```

### Mail Sistemi (v2 — DB-based + AES, 2026-04-21)
```
src/main/java/com/cms/
├── util/AesEncryptor.java                 — AES-256-CBC encrypt/decrypt (base64(IV):base64(cipher))
├── config/TenantMailSenderFactory.java    — mailAccountId -> JavaMailSender cache, decrypt on build
├── service/impl/EmailQueueService.java    — RabbitMQ consumer
├── service/impl/MailAccountService.java   — CRUD + AES encrypt + SMTP verify + factory evict
├── service/impl/MailTestService.java      — /verify endpoint icin test mail gonderimi
├── service/impl/FormDefinitionService.java — Sender (active?) + recipient (valid email) validation
├── service/impl/FormSubmissionService.java — Submit sonrası triggerNotification
├── entity/MailAccount.java                — name, fromAddress, smtp* (password AES), active
├── entity/FormDefinition.java             — senderMailAccount FK + recipientEmail + notification*
└── entity/EmailLog.java                   — Mail gönderim logu (retry sayaci, status)

src/main/resources/
├── templates/emails/form-notification.html — v2 sabit template (v3'de form-bazli)
├── application.properties                  — aes.secret-key=${AES_SECRET_KEY:12345...}
└── db-migration-mail-form-v2.sql           — TRUNCATE form_definitions + drop is_default + yeni kolonlar

docs/MAIL_FORM_V2_GUIDE.md                 — Uçtan uca CURL rehberi
.claude/agent-memory/team-lead/v2-mail-form-roadmap.md — v2 teslim + v3 yol haritasi
```

### v4 Email Templates (2026-04-23)
```
src/main/java/com/cms/
├── entity/EmailTemplate.java                  — tenant_id=null global, optimistic lock, version
├── repository/EmailTemplateRepository.java    — tenant-aware + global lookup
├── dto/DtoEmailTemplate.java + IU + Preview   — API contract
├── service/IEmailTemplateService.java         — CRUD + preview + existsByKey
├── service/impl/EmailTemplateService.java     — implementation, cache eviction
├── service/impl/EmailTemplateLookupService.java — @Cacheable("emailTemplates"), circular dep çözümü
├── service/impl/EmailTemplateRenderer.java    — DB-first → classpath fallback Thymeleaf render
├── controller/IEmailTemplateController.java   — API contract
└── controller/impl/EmailTemplateController.java — /api/v1/email-templates CRUD + preview

src/main/java/com/cms/config/
└── EmailTemplateBootstrapRunner.java          — İlk deploy seed (form-notification + welcome + password-reset)

src/main/resources/migration/
└── db-migration-email-templates-v4.sql        — email_templates tablo + index migration
```

**Seed template'leri:** `form-notification` (classpath), `welcome` (inline), `password-reset` (inline)
**Permission'lar:** `email_templates:read`, `email_templates:manage`

**Silinen (v1'den v2'ye geri donus):** `EnvMailProfileResolver.java`, `DtoAvailableProfile.java`, `config/mail/` klasoru, tum `MAIL_{TENANT}_{PROFILE}_*` ENV'leri + k8s/GH Actions secret'lari, `MailAccount.envProfileKey` + `is_default` kolonlari, `/available-profiles` endpoint.

---

## SKİLL ENVANTERİ — Görev Türüne Göre Hangi Skill

| Görev | Aktif Edilecek Skill'ler |
|-------|-------------------------|
| Yeni entity/endpoint | elly-conventions, multitenant-routing, error-handling-patterns |
| Cache ekleme/sorunu | redis-cache-patterns |
| Queue/consumer ekleme | rabbitmq-patterns, multitenant-routing |
| Auth/güvenlik değişikliği | spring-security-patterns, multitenant-routing |
| Kod review | elly-conventions, redis-cache-patterns, error-handling-patterns |
| Deployment sorunu | elly-project-mastery (kritik konfigürasyon kararları) |
| Oturum devam | dev-session-tracker, elly-project-mastery |

Tüm skill dosyaları: `.claude/skills/<skill-adi>/SKILL.md`

## GELİŞTİRME KURALLARI — Her Değişiklikte Uygula

### Yeni Özellik Eklerken
1. `elly-conventions` skill'ini aktif et → package/pattern kontrolü
2. Şu sırayı takip et: `Entity → Repository → IService → ServiceImpl → IController → Controller → DTO → Mapper`
3. Her endpoint `RootEntityResponse<T>` döndürmeli
4. Multi-tenant sorgularda `TenantContext.getCurrentTenant()` kullan
5. Cache: `redis-cache-patterns` skill'ine bak — tenant prefix otomatik, key pattern'i takip et
6. Exception: `error-handling-patterns` skill'ine bak — BaseException alt sınıfı kullan
7. Permission ekle: `PermissionConstants.java` + `DataInitializer.java`
8. RabbitMQ consumer ekliyorsan: `rabbitmq-patterns` skill'ine bak — TenantContext set/clear zorunlu

### Değişiklik Sonrası Zorunlu Güncellemeler
```
1. changelog.md güncelle (orta/büyük değişiklik ise)
2. Aktif görev dosyasını güncelle (tamamlanan adımları işaretle)
3. Bu skill dosyasını güncelle (yeni kritik karar varsa)
```

### Test Edilecek Kritik Noktalar
- Multi-tenant izolasyonu: tenant1 verisi tenant2'ye sızmamalı
- Redis cache invalidation: login/logout/rol-değişikliği sonrası
- RabbitMQ retry: SMTP hatası → 30sn bekle → tekrar dene
- JWT claim: `tenantId` ve `loginSource` her token'da mevcut

---

## GÖREV DOSYASI OLUŞTURMA

Yeni büyük görev başlayınca `.claude/agent-memory/team-lead/[gorev-adi].md` oluştur:

```markdown
# [Görev Adı] — Teknik Detay

## Tarih: YYYY-MM-DD
## Durum: [Başlamadı | Devam Ediyor | Beklemede | Tamamlandı]

## Hedef
- Ne yapılacak, neden yapılacak

## Tamamlanan Adımlar
- [x] Adım 1 — açıklama

## Sıradaki Adımlar
- [ ] Adım 2 — açıklama

## Değişen/Değişecek Dosyalar
- dosya.java — açıklama

## Mimari Kararlar
- Önemli karar veya blocker

## Breaking Changes
- Varsa listele
```

---

## SKİLL EVRİMİ — Nasıl Güncellenir

Bu skill dosyası (**bu dosya**) şu durumlarda GÜNCELLENMELİDİR:

1. **Yeni büyük özellik tamamlandığında** → "Tamamlanan Büyük Özellikler" tablosuna ekle
2. **Kritik mimari karar alındığında** → "Kritik Konfigürasyon Kararları" tablosuna ekle
3. **Yeni kritik dosya eklendiğinde** → "Kritik Dosyalar" bölümüne ekle
4. **Yeni geliştirme kuralı belirlediğinde** → "Geliştirme Kuralları" bölümüne ekle
5. **Mevcut bilgi eskidiğinde** → Güncelle veya kaldır

**Güncelleme formatı:** Her güncellemenin yanına tarih ekle `(2026-XX-XX güncellendi)`

---

## OTURUM SONLANDIRMA — Zorunlu Eylemler

Oturum bitiminde:
```
1. Aktif görev dosyasını güncelle (kaldığın adımı işaretle)
2. changelog.md güncelle (büyük değişiklik yapıldıysa)
3. Bu skill dosyasını güncelle (yeni kritik bilgi varsa)
4. Kullanıcıya özet sun: "Bugün X yapıldı. Sonraki adım: Y"
```

---

## HIZLI BAĞLAM KURTARMA

Oturum ortasında bağlam kaybolursa:

```bash
# Son değişiklikleri gör
git log --oneline -5
git diff HEAD~1

# Aktif görev dosyalarını listele
ls .claude/agent-memory/team-lead/

# Uncomitted değişiklikleri gör
git status
git diff
```

Sonra changelog.md ve ilgili görev dosyasını oku → kaldığın yerden devam et.

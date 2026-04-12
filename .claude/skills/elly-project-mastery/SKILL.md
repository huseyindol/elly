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

## PROJE DURUM SNAPSHOTU (2026-04-12)

### Tamamlanan Büyük Özellikler

| Tarih | Özellik | Durum | Detay Dosyası |
|-------|---------|-------|---------------|
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
           
Mail Flow:
  POST /api/v1/emails/send → EmailLog(PENDING) → RabbitMQ email-queue
  → EmailQueueService consumer → TenantMailSenderFactory → Gmail SMTP
  → Hata: retry-queue (30sn TTL) → tekrar email-queue (max 3 kez)
  
Auth Flow:
  Admin: POST /api/auth/admin/login → AdminLoginInterceptor
  Tenant: POST /api/auth/login → TenantLoginInterceptor
  OAuth2: Google/Facebook/GitHub → OAuth2AuthenticationSuccessHandler
```

### Aktif Teknoloji Stack

- **Runtime:** Java 21, Spring Boot 3.5.7
- **DB:** PostgreSQL (database-per-tenant: basedb, tenant1, tenant2)
- **Cache:** Redis (`auth:user:{username}` TTL=30dk)
- **Queue:** RabbitMQ (email-queue, email-retry-queue, email-dlq)
- **Auth:** JWT + OAuth2 (Google, Facebook, GitHub)
- **Mail:** Tenant-based Gmail SMTP (DB'den, AES-256-CBC şifreli)
- **Security:** RBAC (@PreAuthorize + Permission entity)

### Kritik Konfigürasyon Kararları

| Karar | Neden | Dosya |
|-------|-------|-------|
| spring.mail.* kaldırıldı | Pod CrashLoopBackOff önlemek için | EllyApplication.java |
| DB-based mail SMTP | Her tenant kendi Gmail'ini kullanır | TenantMailSenderFactory.java |
| Redis user auth cache | Her request'teki 3 SQL → 1 Redis GET | UserAuthCacheService.java |
| Constructor injection zorunlu | @Autowired yasak | Tüm servisler |
| RabbitMQ retry TTL=30sn | Tight-loop retry önlemek için | RabbitMQConfig.java |

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

### Mail Sistemi
```
src/main/java/com/cms/
├── service/impl/EmailQueueService.java    — RabbitMQ consumer
├── service/impl/MailAccountService.java   — SMTP verify + test
├── entity/MailAccount.java               — DB SMTP bilgileri
└── entity/EmailLog.java                  — Mail gönderim logu
```

---

## GELİŞTİRME KURALLARI — Her Değişiklikte Uygula

### Yeni Özellik Eklerken
1. `elly-conventions` skill'ini aktif et → package/pattern kontrolü
2. Şu sırayı takip et: `Entity → Repository → IService → ServiceImpl → IController → Controller → DTO → Mapper`
3. Her endpoint `RootEntityResponse<T>` döndürmeli
4. Multi-tenant sorgularda `TenantContext.getCurrentTenant()` kullan
5. Cache key'lerinde tenantId ekle: `{entity}:{tenantId}:{id}`
6. Permission ekle: `PermissionConstants.java` + `DataInitializer.java`

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

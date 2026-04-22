# Mail+Form — v2 (Teslim) + v3 Yol Haritasi

> v2 yaptiklarini ve v3'e ertelenen maddeleri burada tutuyoruz. Bir sonraki oturumda `v3 baslayalim` dedigin zaman `## v3 Yapilacaklar` bolumuyle devam et.

---

## v2 (Teslim Edildi — 2026-04-21)

### Karar Ozeti
v1'de SMTP kredensiyelleri ENV'e tasinmisti (`MAIL_{TENANT}_{PROFILE}_*`). **v2'de bu geri alindi:**
- SMTP host/port/username/password tekrar DB'de (`mail_accounts`), smtp_password AES-256-CBC ile sifrelenir.
- Bir tenant birden fazla hesap tanimlayabilir; form olustururken hangi hesabin gonderecegini (`senderMailAccountId`) + mailin kime gidecegini (`recipientEmail`) explicit secer.
- `is_default` konsepti kaldirildi — her form kendi hesabini secer.
- ENV'de sadece `AES_SECRET_KEY` (32 ASCII karakter) kalir.

### Neden Bu Hibrit
Kullanici geri bildirimi: "gönderim yapacak mail bilgilerini host,port,username,password db'de tutulsun birden fazla olabilir birini seçicem form oluşturunca hangi mail adresini seçersem o göndersin"

Yani v1'in ENV yaklasimi (kredensiyel-envde-tut) degil, v0'in DB yaklasimi + v1'in form-level secim pattern'i birlikte.

### Kapsam (v2)
- `MailAccount` entity: id, name, fromAddress, smtpHost, smtpPort, smtpUsername, smtpPassword (AES), active, timestamps
- `MailAccountRepository.findAllByActiveTrue()` — panel form picker UI icin
- `MailAccountService`:
  - `create()` zorunlu smtpPassword + AES encrypt
  - `update()` smtpPassword null/bos ise mevcut sifreli deger korunur (admin UX)
  - `testConnection()` JavaMailSenderImpl.testConnection() ile
  - `getAllActive()` aktif hesaplar listesi
- `TenantMailSenderFactory` — `ConcurrentHashMap<Long, JavaMailSender>` cache (mailAccountId key), decrypt->configure, `evict(id)`/`evictAll()`
- `FormDefinition` entity: `senderMailAccount` (NOT NULL FK) + `recipientEmail` (NOT NULL) + `notificationSubject` + `notificationEnabled` (v1'den korundu)
- `AesEncryptor` GERI GETIRILDI (v1'de silinmisti)
- `EnvMailProfileResolver` + `DtoAvailableProfile` + `/available-profiles` endpoint **silindi**
- Migration: `db-migration-mail-form-v2.sql` — pre-v1 state'den v2 target'a tek seferde (TRUNCATE form_definitions CASCADE, `is_default` drop)
- Dokuman: `docs/MAIL_FORM_V2_GUIDE.md` (MAIL_FORM_V1_GUIDE.md silindi)

### v1'den v2'ye "Geri Donus" Listesi (referans)
Silinen: `EnvMailProfileResolver.java`, `DtoAvailableProfile.java`, tum `MAIL_{TENANT}_{PROFILE}_*` ENV'leri, k8s/GH Actions icindeki 15+ MAIL_* secret, `MailAccount.envProfileKey` alani, `is_default` kolonu
Geri getirilen: `AesEncryptor.java`, `aes.secret-key` property, `MailAccount` SMTP kolonlari, `smtpPassword` encryption

---

## v3 Yapilacaklar (Ertelenen)

Eski v2 roadmap'inde olan ama bu iterasyonda yapilmayan her sey -> v3 kuyruguna tasindi.

### 1. Coklu Recipient Destegi (TO/CC/BCC)
**Neden:** v2 tek adres (`recipientEmail` VARCHAR). Kurumsal kullanimda birden fazla kisi form bildirimi almak ister.

**Kapsam:**
- `FormDefinition.recipientEmail VARCHAR` -> `form_recipients` alt tablosu (type ENUM: TO/CC/BCC)
- `EmailRequest` + `EmailLog` halihazirda tek `to` tasiyor — genisletilmeli (`List<String> to, List<String> cc, List<String> bcc`)
- Migration: eski `recipient_email` -> `form_recipients.type=TO` tasiyip drop
- Form controller DTO guncellemesi + validation (min 1 TO)

**Karar:** Sub-tablo mi JSONB mi? JSONB hizli, ama query/validation kolayliginda sub-tablo iyi. `FormRecipient` entity onerim.

### 2. Form-Bazli Template Yapilandirma
**Neden:** v2 sabit template (`form-notification.html`). Her form farkli gorunum isteyebilir.

**Kapsam:**
- `FormDefinition.notification_template_id BIGINT NULL` — null ise fallback `form-notification.html`
- Yeni entity: `NotificationTemplate` (id, tenantId, name, subject, htmlBody, variables JSONB, active)
- Service: `TemplateRenderService` — `payload` + `template.htmlBody` + Thymeleaf inline engine
- Admin CRUD: `POST /api/v1/notification-templates`
- Degisken haritalama: `{{form.name}}`, `{{form.email}}` — payload alanlari otomatik bind
- Preview endpoint: `POST /api/v1/notification-templates/{id}/preview`

**Kararlar (v3 brainstorming'de verilecek):**
- Template versioning: her guncelleme yeni version mi, overwrite mi?
- Stock kutuphane: login bildirimi, form submission, password reset... — ayri seed
- Visual editor mi yoksa raw HTML mi?

### 3. Bounce / Gonderim Hatasi Bildirimi
**Neden:** v2'de form submit edilir, mail FAILED + DLQ olur, admin farketmez.

**Kapsam:**
- `EmailLog.status=FAILED` oldugunda SUPER_ADMIN rolune sistem-mail
- Dashboard widget: son 24 saat FAILED sayisi
- Webhook/Slack entegrasyonu (opsiyonel)

### 4. Rate Limiting & Gunluk Kota
**Neden:** Gmail SMTP gunluk 500 mail limiti var. Tenant bunu asarsa banlanir.

**Kapsam:**
- `EmailQuotaService`: Redis counter `quota:{tenantId}:{mailAccountId}:{yyyyMMdd}`
- Hesap basina konfigurable limit (`mail_accounts.daily_limit` veya global config)
- Limit asilirsa `EmailLog.status=QUOTA_EXCEEDED`, retry etmez

### 5. Form Submit CAPTCHA & Spam Koruma
**Neden:** Public form endpoint -> bot spam -> mail kotasi yanar.

**Kapsam:**
- reCAPTCHA v3 veya hCaptcha entegrasyonu
- Honeypot field (hidden input dolduruysa = bot)
- IP bazli rate limit (Redis)

### 6. Submission Arsivi & Export
**Neden:** Form submission'lari raporlanmak istenebilir.

**Kapsam:**
- `GET /api/v1/forms/{id}/submissions/export?format=csv|xlsx`
- Tarih filtresi, alan bazli filtre

### 7. Multi-Language Template
**Neden:** Tenant farkli dillerde mail gonderebilmeli.

**Kapsam:**
- `NotificationTemplate.locale` alani
- Form submit'te locale cikaran strateji (user.locale, form.locale, default)
- `i18n` resource bundle entegrasyonu

### 8. AES Anahtar Rotation
**Neden:** v2'de `AES_SECRET_KEY` degistirilirse mevcut `smtp_password` kayitlari decrypt edilemez.

**Kapsam:**
- Admin endpoint: `POST /api/v1/admin/security/rotate-aes-key` — eski key + yeni key body'de
- Tum `mail_accounts.smtp_password` kayitlarini eski key ile decrypt + yeni key ile re-encrypt
- Atomik: transaction icinde, hata olursa rollback
- Log: kaç kayit etkilenedi, hangi tenant
- **Zorluk:** Multi-tenant — her tenant DB'sinde ayri calismali

### 9. Test & Mock SMTP
**Neden:** CI/CD'de mail testleri lokal SMTP gerektiriyor.

**Kapsam:**
- `@Profile("test")` ile MailHog container (docker-compose-test.yml)
- Integration test suite: `MailEndToEndTest`
- Mock `JavaMailSender` bean'i test-only

### 10. Hesap Sifre Maskeleme / Son Kullanim
**Neden:** Panel admin hangi hesabin kullanildigini anlamak isteyebilir.

**Kapsam:**
- `MailAccount.lastUsedAt` kolonu — her gonderimde guncellenir
- Response DTO'da `smtpUsername` icin maskeleme opsiyonu (`sup****@firma.com`)

---

## Oncelik Sirasi (Onerilen v3)
1. **v3.1:** Coklu recipient (en sik talep edilecek)
2. **v3.2:** Form-bazli template
3. **v3.3:** Bounce bildirimi
4. **v3.4:** Rate limiting
5. **v3.5:** CAPTCHA
6. **v3.6:** AES key rotation
7. **v3.7:** Export
8. **v3.8:** Multi-lang
9. **v3.9:** Son kullanim/masking
10. **v3.10:** Test harness

---

## v3 Oturumu Icin Hizli Baglam
Basladiginda:
1. `changelog.md` -> 2026-04-21 v2 kaydi
2. `docs/MAIL_FORM_V2_GUIDE.md` -> mevcut akis
3. `.claude/skills/elly-project-mastery/SKILL.md` -> v2 snapshot
4. Ilk adim: hangi v3 maddesini isteyecegini kullaniciya sor

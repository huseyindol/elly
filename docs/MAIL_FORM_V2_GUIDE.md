# Mail + Form v2 — Uçtan Uca Rehber

> **Hedef:** Panelden birden fazla SMTP hesabı tanımla → form oluştururken hangi hesabın göndereceğini ve bildirimin kime gideceğini seç → submit sırasında seçilen hesaptan seçilen alıcıya mail gitsin.
>
> **Mimari (v2):** SMTP kimlik bilgileri (host / port / username / password / from) her tenant’ın kendi DB’sindeki `mail_accounts` tablosunda saklanır. `smtp_password` AES-256-CBC ile şifrelidir (anahtar ENV’de, data DB’de). Form oluştururken iki zorunlu seçim yaparsın: **sender mail hesabı** (hangi hesaptan çıksın) + **recipient email** (kime gitsin).

v1’den v2’ye değişiklikler:
- `MAIL_{TENANT}_{PROFILE}_*` ENV blokları **kaldırıldı**; hepsi panel üzerinden DB’ye yazılır.
- `mail_accounts.is_default` kolonu kaldırıldı — varsayılan hesap konsepti yok, her form explicit seçim yapar.
- Yeni tek ENV: `AES_SECRET_KEY` (32 ASCII karakter).

---

## 1. Gereksinimler

### 1.1 AES anahtarı (`AES_SECRET_KEY`)

`smtp_password` DB’ye AES-256-CBC ile şifrelenip yazılır. Anahtar ENV’den okunur.

- **Format:** tam 32 ASCII karakter (256 bit).
- **Default (dev):** `12345678901234567890123456789012` — **prod’da mutlaka değiştir.**
- **Rotation:** anahtar değiştirilirse mevcut kayıtlar re-encrypt gerektirir (henüz otomatik job yok; elle migration gerekir).

#### Lokal (`.env` / `.env.local`)

```bash
AES_SECRET_KEY=12345678901234567890123456789012
```

#### Production (GitHub Actions → K8s Secret)

1. GitHub Secrets → **`AES_SECRET_KEY`** ekle (tam 32 karakter).
2. `.github/workflows/deploy.yml` base64 export’u zaten hazır:
   ```bash
   export AES_SECRET_KEY_B64=$(echo -n "${{ secrets.AES_SECRET_KEY }}" | base64)
   ```
3. `k8s/1-secret.template.yaml`:
   ```yaml
   AES_SECRET_KEY: "${AES_SECRET_KEY_B64}"
   ```
4. Pod `envFrom: secretRef: elly-secret` ile otomatik okur — `aes.secret-key` property’si çalışma anında bağlanır.

### 1.2 Gmail App Password (kullanıyorsan)

- 2FA açık olmalı.
- https://myaccount.google.com/apppasswords → 16 haneli **App Password** üret.
- Panelde `smtpPassword` alanına App Password’u gir (hesap şifresi **değil**).

---

## 2. İlk Kurulum

### 2.1 Migration’ı çalıştır (her tenant DB’si için)

```bash
psql -h localhost -p 31432 -U postgres -d elly_basedb  -f src/main/resources/db-migration-mail-form-v2.sql
psql -h localhost -p 31433 -U postgres -d elly_tenant1 -f src/main/resources/db-migration-mail-form-v2.sql
psql -h localhost -p 31434 -U postgres -d elly_tenant2 -f src/main/resources/db-migration-mail-form-v2.sql
```

> ⚠️ `form_definitions` **TRUNCATE** edilir (CASCADE ile `form_submissions` de). Production’da önce yedek al. `mail_accounts` dokunulmaz (sadece `is_default` kolonu düşer).

### 2.2 Uygulamayı başlat

```bash
./mvnw spring-boot:run     # yerel
# veya
make up                    # docker
```

Başlangıçta kritik log:
```
Started EllyApplication in X.X seconds
```
v2’de artık `EnvMailProfileResolver refreshed …` logu **yok**. ENV profili taraması kaldırıldı.

---

## 3. Uçtan Uca CURL Akışı

### 3.1 Admin login → token

```bash
TOKEN=$(curl -sS -X POST http://localhost:8080/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' | jq -r '.data.accessToken')

echo "TOKEN=$TOKEN"
```

> Tenant kullanıcı için `/api/auth/login`.

### 3.2 Mail hesabı oluştur (DB’ye kaydet)

Tüm SMTP alanları request body’de; `smtpPassword` AES ile şifrelenip yazılır.

```bash
curl -sS -X POST http://localhost:8080/api/v1/mail-accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tenant1 Destek",
    "fromAddress": "Destek <support-tenant1@firma.com>",
    "smtpHost": "smtp.gmail.com",
    "smtpPort": 587,
    "smtpUsername": "support-tenant1@firma.com",
    "smtpPassword": "xxxx xxxx xxxx xxxx",
    "active": true
  }' | jq
```

Yanıtta `data.id` → bunu `senderMailAccountId` olarak kullanacaksın. Yanıtta `smtpPassword` **görünmez** (salt okunur DTO yalnızca id / name / fromAddress / smtpHost / smtpPort / smtpUsername / active / timestamps döner).

```bash
MAIL_ACCOUNT_ID=1   # yanıttaki id
```

> **İkinci hesap eklemek istersen** aynı endpoint’e farklı `name` ve SMTP bilgileriyle tekrar POST at. Sınırsız sayıda hesap olabilir.

### 3.3 SMTP bağlantı testi (mail göndermeden doğrula)

```bash
curl -sS -X POST "http://localhost:8080/api/v1/mail-accounts/$MAIL_ACCOUNT_ID/verify" \
  -H "Authorization: Bearer $TOKEN" | jq
```

Başarılı: `{"success":true, "data": true}`. Başarısızsa 400 + anlamlı mesaj (örn. `Authentication failed`).

### 3.4 (Opsiyonel) Aktif hesapları listele (form picker UI için)

Form oluştururken kullanıcıya hangi hesaplardan seçim yaptıracaksan panel bu endpoint’i çağırır:

```bash
curl -sS http://localhost:8080/api/v1/mail-accounts/active \
  -H "Authorization: Bearer $TOKEN" | jq
```

Yanıt, `active=true` olan tüm hesapları id + name + fromAddress ile döner (smtpPassword asla döndürülmez).

### 3.5 Form oluştur (sender + recipient seçilerek)

```bash
curl -sS -X POST http://localhost:8080/api/v1/forms \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Iletisim Formu",
    "version": 1,
    "active": true,
    "senderMailAccountId": '"$MAIL_ACCOUNT_ID"',
    "recipientEmail": "admin@firma.com",
    "notificationSubject": "Yeni iletisim basvurusu",
    "notificationEnabled": true,
    "schema": {
      "fields": [
        { "id": "name",    "label": "Ad Soyad",    "type": "text",     "required": true },
        { "id": "email",   "label": "E-posta",     "type": "email",    "required": true },
        { "id": "message", "label": "Mesaj",       "type": "textarea", "required": true }
      ]
    }
  }' | jq
```

Yanıtta `data.id` → `FORM_ID`. `senderMailAccountName` ve `senderFromAddress` alanları otomatik doldurulup read-only döner (panel görüntüleme için).

```bash
FORM_ID=1
```

### 3.6 Form submit (son kullanıcı tarafı)

```bash
curl -sS -X POST "http://localhost:8080/api/v1/forms/$FORM_ID/submit" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "payload": {
      "name":    "Ali Veli",
      "email":   "ali@example.com",
      "message": "Demo talebi"
    }
  }' | jq
```

Submit başarılı → arka planda:
1. `form_submissions` satırı yazılır.
2. `EmailLog` **PENDING** oluşturulur.
3. RabbitMQ `email-queue` mesajına (tenantId + emailLogId) konur.
4. `EmailQueueService` consumer → `TenantMailSenderFactory.getMailSender(account)` → SMTP → mail gider.
5. `EmailLog.status = SENT` / `FAILED` güncellenir.

### 3.7 Mail gerçekten gitti mi? (EmailLog)

```bash
curl -sS "http://localhost:8080/api/v1/emails/logs?page=0&size=5&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.content[] | {id, recipient, status, retryCount, sentAt, errorMessage}'
```

`status` değerleri:
- `PENDING`  → kuyruğa girdi, consumer henüz işlemedi
- `SENT`     → başarılı (`sentAt` doldu)
- `FAILED`   → tüm retry’lar tükendi (DLQ’ya düştü, `errorMessage` görünür)

`admin@firma.com` inbox’ını kontrol et → "Yeni iletisim basvurusu" konulu mail görünür. HTML template’i `src/main/resources/templates/emails/form-notification.html`.

---

## 4. Hesap Güncelleme (Password Değiştirmeden)

`PUT /api/v1/mail-accounts/{id}` çağrısında `smtpPassword` alanını **boş/null bırakırsan** mevcut şifreli password aynen kalır — panel admin her güncellemede App Password’u yeniden girmek zorunda değildir.

```bash
curl -sS -X PUT "http://localhost:8080/api/v1/mail-accounts/$MAIL_ACCOUNT_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tenant1 Destek (Yenilenmis)",
    "fromAddress": "Destek <support-tenant1@firma.com>",
    "smtpHost": "smtp.gmail.com",
    "smtpPort": 587,
    "smtpUsername": "support-tenant1@firma.com",
    "smtpPassword": null,
    "active": true
  }' | jq
```

Password değiştirmek istersen `smtpPassword` doldur → yeni değer AES ile şifrelenip yazılır, factory cache’i evict edilir (bir sonraki gönderimde yeni credentials kullanılır).

---

## 5. Hata Senaryoları

| Durum | HTTP | Açıklama |
|---|---|---|
| `AES_SECRET_KEY` eksik / 32 karakter değil | Startup fail | AesEncryptor constructor throws |
| `smtpPassword` create’de boş | 400 | Validation: create zorunlu |
| `senderMailAccountId` form’da null | 400 | Validation: zorunlu |
| `senderMailAccount.active=false` | 422 | Form save + submit aynı hatayı verir |
| Seçilen hesap silinmiş | 404 | Form kaydedilemez |
| `recipientEmail` geçersiz | 400 | `@Email` validation |
| SMTP auth başarısız | 400 (verify) / retry (submit) | Gmail App Password doğru mu? 2FA? |
| `smtpPort` 1-65535 dışı | 400 | `@Min/@Max` validation |
| Form submit başarılı ama mail fail | 200 submission + EmailLog FAILED | Retry mekanizması devreye girer (max 3 → DLQ) |

---

## 6. Mimari Notlar

- **Şifreleme noktası:** `MailAccountService.create()` / `update()` — `aesEncryptor.encrypt(plain)` çağrısı burada yapılır.
- **Çözme noktası:** `TenantMailSenderFactory.getMailSender(account)` — password decrypt edilip `JavaMailSenderImpl`’a verilir. Factory `ConcurrentHashMap<Long, JavaMailSender>` ile mailAccountId bazında cache tutar.
- **Cache invalidation:** Hesap update/delete olursa service `mailSenderFactory.evict(id)` çağırır. Auto-refresh yok.
- **Tenant izolasyonu:** mail_accounts tablosu zaten her tenant DB’sinde ayrı. `TenantContext` JWT’den geliyor, `TenantDataSourceRouter` doğru DB’ye yönlendiriyor.
- **RabbitMQ consumer** içinde `TenantContext.setTenantId(msg.tenantId)` + `finally { clear() }` kurulumu korunur (v1’den aynen).
- **Spring Mail auto-config EXCLUDE** hala aktif (`EllyApplication`): `management.health.mail.enabled=false` + `MailSenderAutoConfiguration` exclude → pod crash önlenir, tüm mail sender’lar factory ile yaratılır.

---

## 7. Bilinen v3 Geliştirmeleri

`.claude/agent-memory/team-lead/v2-mail-form-roadmap.md` → v3 maddeleri:
- Çoklu recipient (TO / CC / BCC dizi)
- Form-bazlı özelleştirilmiş template (DB’de HTML body)
- Rate limiting / gönderim kotası (per tenant, per mail account)
- Bounce / hata bildirimi (IMAP ya da webhook)
- CAPTCHA & spam koruma
- Anahtar rotation job’ı (eski AES key → yeni key re-encrypt)

---

## 8. Hızlı Kontrol Listesi

- [ ] `.env`’e **`AES_SECRET_KEY`** eklendi (tam 32 karakter)
- [ ] `db-migration-mail-form-v2.sql` tüm tenant DB’lerinde çalıştı
- [ ] `./mvnw spring-boot:run` başlatıldı, crash yok
- [ ] `POST /mail-accounts` → en az 1 aktif hesap kaydı oluşturuldu
- [ ] `POST /mail-accounts/{id}/verify` → başarılı
- [ ] `GET /mail-accounts/active` → listede hesap görünüyor
- [ ] `POST /forms` → form oluşturuldu (senderMailAccountId + recipientEmail ile)
- [ ] `POST /forms/{id}/submit` → 200 döndü
- [ ] Inbox’ta mail alındı / `EmailLog.status=SENT`

Bir adım patlarsa:
```bash
kubectl logs -n elly -l app=elly-cms -f --tail=200
# veya lokal
tail -f logs/spring.log
```

AES ile ilgili şüphe:
```bash
# Anahtar ENV’e geçmiş mi?
kubectl exec -n elly -it deployment/elly-app -- printenv | grep AES_SECRET_KEY | head -1
# Uzunluk kontrol
echo -n "$AES_SECRET_KEY" | wc -c    # 32 olmalı
```

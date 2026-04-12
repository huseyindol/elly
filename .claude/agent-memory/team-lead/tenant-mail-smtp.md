# Tenant-Based Mail SMTP — Teknik Detay

## Tarih: 2026-04-12
## Durum: ✅ Tamamlandı

## Mimari Kararlar

### Spring Mail Auto-Config Kaldırıldı
- `MailSenderAutoConfiguration` ve `MailSenderValidatorAutoConfiguration` EllyApplication'da exclude edildi
- `spring.mail.*` property'leri tamamen kaldırıldı
- Artık Spring Boot otomatik `JavaMailSender` bean oluşturmuyor
- `MailHealthIndicator` bean olmadığı için doğal olarak devre dışı

### Mail Akışı
```
Panelden MailAccount oluştur (Gmail bilgileri)
    ↓
POST /api/v1/emails/send → EmailLog (PENDING) → RabbitMQ email-queue
    ↓
EmailQueueService (consumer):
  1. TenantContext.setTenantId(tenantId)
  2. EmailLog'dan MailAccount çöz (yoksa default)
  3. TenantMailSenderFactory → JavaMailSender (cache'li)
  4. Thymeleaf template render → MimeMessage gönder
  5. Başarı → SENT | Hata → retry-queue (30sn TTL) → tekrar email-queue
```

### RabbitMQ Retry Mekanizması
- `email-retry-queue`: x-message-ttl=30000ms, DLX=email-exchange
- Başarısız mail → retry-queue → 30sn bekle → ana queue'ya otomatik döner
- MAX_RETRY_COUNT=3 aşılınca → FAILED + DLQ

### Gmail SMTP Konfigürasyonu
- Host: `smtp.gmail.com`
- Port: 587 (STARTTLS) veya 465 (SSL)
- Kullanıcı adı: Gmail adresi
- Şifre: **Google App Password** (2FA aktif olmalı)
- Şifre DB'de AES-256-CBC ile şifreli saklanır

### Port'a Göre Protokol Seçimi
- Port 465 → SSL/TLS (`mail.smtp.ssl.enable=true`)
- Diğer (587) → STARTTLS (`mail.smtp.starttls.enable=true`)
- Connection timeout: 10 saniye (tüm portlar)

## API Endpoint'leri

| Method | URL | Açıklama |
|--------|-----|----------|
| POST | /api/v1/mail-accounts | Yeni mail hesabı oluştur |
| PUT | /api/v1/mail-accounts/{id} | Hesap güncelle |
| GET | /api/v1/mail-accounts | Tüm hesapları listele |
| DELETE | /api/v1/mail-accounts/{id} | Hesap sil |
| PUT | /api/v1/mail-accounts/{id}/default | Varsayılan yap |
| POST | /api/v1/mail-accounts/{id}/test | Test maili gönder |
| POST | /api/v1/mail-accounts/{id}/verify | SMTP bağlantısını doğrula (yeni) |
| POST | /api/v1/emails/send | Template mail gönder |

## Değişen Dosyalar
1. `EllyApplication.java` — MailSender auto-config exclude
2. `application.properties` — spring.mail.* kaldırıldı
3. `RabbitMQConfig.java` — retry queue/exchange eklendi
4. `TenantMailSenderFactory.java` — SSL/TLS otomasyon + timeout
5. `EmailQueueService.java` — retry → retry-exchange'e yönlendir
6. `IMailAccountService.java` — testConnection() eklendi
7. `MailAccountService.java` — testConnection() implementasyonu
8. `IMailAccountController.java` — verifyConnection() eklendi
9. `MailAccountController.java` — POST /{id}/verify endpoint

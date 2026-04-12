# SMTP + RabbitMQ Sorunu — ✅ ÇÖZÜLDÜ

## Durum: ✅ Çözüldü (2026-04-12)
## Orijinal Tarih: 2026-04-10

## Sorun Özeti
RabbitMQ consumer email göndermeye çalışırken SMTP bağlantısı başarısız oluyordu.
K8s'te `spring.mail.host` default olarak `localhost:1025`'e düşüyordu — SMTP sunucu yoktu.

## Belirtiler
- `java.net.ConnectException: Connection refused` — SMTPTransport.openServer()
- Spring Boot `MailHealthIndicator` → health DOWN → `/actuator/health` 503 → K8s startup probe fail → pod CrashLoopBackOff
- Eski ve yeni deployment'lar aynı sorunu yaşıyordu

## Uygulanan Kalıcı Çözüm

### 1. Spring Mail Auto-Config Kaldırıldı
- `MailSenderAutoConfiguration` EllyApplication'da exclude edildi
- `spring.mail.*` property'leri tamamen silindi
- MailHealthIndicator bean olmadığı için otomatik devre dışı

### 2. Tenant-Based Mail Gönderimi (DB-Based)
- Her tenant panelden kendi Gmail hesabını tanımlıyor
- `mail_accounts` tablosunda SMTP bilgileri (AES şifreli)
- `TenantMailSenderFactory` dinamik JavaMailSender üretiyor

### 3. RabbitMQ Retry Resilience
- `email-retry-queue` (TTL=30sn) eklendi — tight-loop retry önlendi
- MAX_RETRY_COUNT=3 aşılınca → FAILED + DLQ

### 4. Connection Timeout
- TenantMailSenderFactory'ye 10sn timeout eklendi
- SSL/TLS port otomasyonu (465=SSL, 587=STARTTLS)

## Detay
- Bkz: `.claude/agent-memory/team-lead/tenant-mail-smtp.md`

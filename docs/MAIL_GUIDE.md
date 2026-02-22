Role: Sen tecrübeli bir Senior Java Backend Developer'sın.
Task: Aşağıdaki gereksinimlere göre "Production-Ready" bir Spring Boot Email Microservice modülü kodla.

Teknoloji Yığını:

Java 21

Spring Boot 3.x

Spring Data JPA (PostgreSQL)

Spring AMQP (RabbitMQ)

Spring Boot Starter Mail (JavaMailSender)

Thymeleaf (Template Engine)

Lombok

Docker & Docker Compose (Mailpit ve RabbitMQ için)

Mimari Gereksinimler:

1. Veritabanı Modeli (EmailLog Entity):

Her mail gönderim isteği önce veritabanına kaydedilmelidir.

Alanlar: id (Long), recipient (String), subject (String), templateName (String), payloadJson (Text/JSON - Dinamik veriler için), status (Enum: PENDING, SENT, FAILED), retryCount (int), errorMessage (String), createdAt, sentAt.

2. API Endpointleri (EmailController):

POST /api/v1/emails/send:

Request Body: EmailRequest { to, subject, templateName, dynamicData (Map<String, Object>) }.

İşlem: Gelen isteği DB'ye PENDING statüsüyle kaydet ve oluşan ID'yi RabbitMQ kuyruğuna gönder.

Response: 202 Accepted ("Mail kuyruğa alındı, ID: ...").

Güvenlik: Header'da X-API-KEY kontrolü yapan bir Interceptor veya Filter ekle.

GET /api/v1/emails/templates:

src/main/resources/templates/emails klasöründeki .html dosyalarının isimlerini liste olarak dön (uzantısız).

3. Mesaj Kuyruğu (RabbitMQ - Producer & Consumer):

Producer: Veritabanına kaydettikten sonra sadece emailLogId'yi kuyruğa atar.

Consumer:

ID'yi alır, DB'den kaydı çeker.

payloadJson'ı tekrar Map'e çevirir.

Thymeleaf Context'ine bu map'i yükler.

templateName'e göre şablonu bulup HTML'i render eder (emails/ klasörü altında ara).

SMTP üzerinden maili gönderir.

Başarılıysa: Statüyü SENT, sentAt'i güncelle.

Hata alırsa: retryCount artır, hatayı kaydet. Eğer retryCount > 3 ise FAILED yap, değilse tekrar denenebilir.

4. Resilience (Dayanıklılık) - EmailRescueJob:

@Scheduled ile 5 dakikada bir çalışan bir job yaz.

Statüsü PENDING olan ve createdAt zamanı 5 dakikadan eski olan mailleri bul (Batch size: 50).

Bunları tekrar RabbitMQ kuyruğuna it.

5. Konfigürasyon (application.properties):

Profile yapısı kur: dev ve prod.

dev profilinde SMTP host/port ayarlarını parametrik yap (Varsayılan: Gmail SMTP).

Kodlama Beklentisi:

Clean Code prensiplerine uy.

Servis katmanlarını ayır (EmailService, EmailQueueService).

Hata yönetimi için Global Exception Handler ekle.

Gerekli tüm Repository, Entity, DTO ve Config sınıflarını yaz.
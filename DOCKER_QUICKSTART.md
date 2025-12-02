# 🚀 Docker Quickstart - 3 Adımda Başla!

## ⚡ Hızlı Başlangıç

### 1️⃣ Environment Dosyası Oluştur

```bash
cp env.example .env
```

`.env` dosyasını düzenle:
```env
DB_PASSWORD=güçlü_bir_şifre_123!
```

### 2️⃣ Başlat

```bash
docker-compose up -d
```

Ya da Makefile kullanarak:
```bash
make setup
```

### 3️⃣ Kontrol Et

```bash
# Swagger UI'ı tarayıcıda aç
open http://localhost:8080/swagger-ui.html

# Health check
curl http://localhost:8080/actuator/health
```

## 📋 Makefile Komutları (Önerilen)

```bash
make help       # Tüm komutları göster
make up         # Başlat
make logs       # Logları göster
make restart    # Yeniden başlat
make down       # Durdur
make ps         # Durum kontrol
```

## 🐳 Manuel Docker Compose Komutları

```bash
# Başlat
docker-compose up -d

# Logları izle
docker-compose logs -f

# Durdur
docker-compose down

# Durum kontrol
docker-compose ps
```

## 🎯 İlk Çalıştırmada Ne Olur?

1. **PostgreSQL** container'ı başlar (1-5 saniye)
   - `elly` schema'sı otomatik oluşturulur
   - Junction table indexleri otomatik eklenir

2. **Spring Boot** uygulaması build edilir (2-5 dakika)
   - Maven dependencies indirilir
   - Kod derlenir
   - JAR oluşturulur

3. **Uygulama** başlar (10-30 saniye)
   - Hibernate tabloları oluşturulur
   - Assets klasörleri hazırlanır
   - API hazır!

## 🌐 Erişim Bilgileri

| Servis | URL | Açıklama |
|--------|-----|----------|
| Swagger UI | http://localhost:8080/swagger-ui.html | API Dokümantasyonu |
| API Docs | http://localhost:8080/api-docs | OpenAPI JSON |
| Health Check | http://localhost:8080/actuator/health | Sağlık Kontrolü |
| PostgreSQL | localhost:5432 | Database (schema: elly) |

## 🔑 Database Bağlantı Bilgileri

```
Host:     localhost
Port:     5432
Database: postgres
Schema:   elly
Username: postgres
Password: .env dosyasındaki değer
```

## ⚠️ Sorun Giderme

### Port Çakışması (8080 veya 5432 kullanımda)

`.env` dosyasında değiştir:
```env
APP_PORT=3000
DB_PORT=5433
```

### İlk Build Çok Uzun Sürüyor

Normal! İlk build 2-5 dakika sürebilir. Maven tüm dependencies'i indiriyor.

```bash
# Logları izle
docker-compose logs -f app
```

### Database Connection Hatası

PostgreSQL'in tamamen başlamasını bekle:
```bash
docker-compose logs postgres
```

"database system is ready to accept connections" mesajını bekle.

### Out of Memory

Docker'a daha fazla memory ver:
- Docker Desktop → Settings → Resources → Memory → 4GB

## 🎓 Sonraki Adımlar

1. ✅ API'yi test et: http://localhost:8080/swagger-ui.html
2. ✅ Database'i incele: `make shell-db`
3. ✅ Logları takip et: `make logs-app`
4. ✅ [DOCKER_SETUP.md](DOCKER_SETUP.md) dosyasını oku (detaylı kılavuz)

## 💡 Faydalı Komutlar

```bash
# Container içine gir
make shell

# Database'e bağlan
make shell-db

# Health check yap
make health

# Kod değişikliği sonrası rebuild
make rebuild

# Database backup al
make backup

# Tüm logları göster
make logs

# Sadece app logları
make logs-app
```

## 🔥 Hot Reload (Development)

Kod değişikliği sonrası:

```bash
# Otomatik rebuild ve restart
make rebuild

# Ya da manuel
docker-compose up -d --build app
```

## 🎯 Production'a Geçiş

```bash
# Production compose kullan
docker-compose -f docker-compose.prod.yml up -d

# Ya da Makefile ile
make prod-up
```

---

**Hazır! Artık geliştirmeye başlayabilirsiniz! 🎉**

Daha fazla bilgi için:
- [DOCKER_SETUP.md](DOCKER_SETUP.md) - Detaylı kılavuz
- [DATABASE_INDEXES.md](DATABASE_INDEXES.md) - Index dokümantasyonu


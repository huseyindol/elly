# 🐳 Elly CMS Docker Setup Kılavuzu

## 📋 Gereksinimler

- Docker Engine 20.10+
- Docker Compose 2.0+
- Minimum 2GB RAM
- Minimum 2GB Disk Space

## 🚀 Hızlı Başlangıç

### 1. Environment Dosyası Oluştur (Opsiyonel)

```bash
# .env dosyası oluştur
cat > .env << EOF
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=your_secure_password_here
DB_PORT=5432
DB_SCHEMA=elly
APP_PORT=8080
SPRING_PROFILES_ACTIVE=docker
EOF
```

### 2. Uygulamayı Başlat

```bash
# Container'ları build et ve başlat
docker-compose up -d

# Logları takip et
docker-compose logs -f

# Sadece app logları
docker-compose logs -f app

# Sadece db logları
docker-compose logs -f postgres
```

### 3. Durum Kontrolü

```bash
# Container'ların durumunu kontrol et
docker-compose ps

# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
# Tarayıcıda aç: http://localhost:8080/swagger-ui.html
```

## 🛠️ Yönetim Komutları

### Container Yönetimi

```bash
# Container'ları durdur
docker-compose stop

# Container'ları başlat (tekrar)
docker-compose start

# Container'ları durdur ve kaldır
docker-compose down

# Volume'lar ile birlikte kaldır (DİKKAT: Tüm data silinir!)
docker-compose down -v

# Yeniden build et ve başlat
docker-compose up -d --build

# Sadece uygulamayı yeniden başlat
docker-compose restart app
```

### Log Yönetimi

```bash
# Tüm logları göster
docker-compose logs

# Son 100 satır
docker-compose logs --tail=100

# Gerçek zamanlı takip
docker-compose logs -f

# Sadece hataları göster
docker-compose logs | grep ERROR
```

### Database Yönetimi

```bash
# PostgreSQL'e bağlan
docker-compose exec postgres psql -U postgres -d postgres

# Veritabanı içinde:
SET search_path TO elly;
\dt                 -- Tabloları listele
\di                 -- İndexleri listele
\d+ pages           -- Page tablosu detayları

# SQL dosyası çalıştır
docker-compose exec -T postgres psql -U postgres -d postgres < backup.sql

# Backup al
docker-compose exec postgres pg_dump -U postgres -d postgres --schema=elly > backup.sql

# Backup'tan geri yükle
docker-compose exec -T postgres psql -U postgres -d postgres < backup.sql
```

### Uygulama Yönetimi

```bash
# Container içine gir
docker-compose exec app sh

# Dosya sistemini kontrol et
docker-compose exec app ls -la /app/assets

# JVM memory kullanımını gör
docker stats elly-app
```

## 🔧 Konfigürasyon

### Port Değiştirme

**.env dosyasında:**
```env
APP_PORT=3000      # Uygulama portu
DB_PORT=5433       # PostgreSQL portu
```

### Veritabanı Şifresi Değiştirme

```env
DB_PASSWORD=yeni_guclu_sifre_123!
```

### JVM Memory Ayarları

**docker-compose.yml** içinde:
```yaml
environment:
  JAVA_OPTS: "-Xmx1g -Xms512m"  # Max 1GB, Min 512MB
```

### Farklı Profile Kullanma

```env
SPRING_PROFILES_ACTIVE=prod
```

## 📊 Monitoring ve Debugging

### Health Check Endpoint'leri

```bash
# Genel health
curl http://localhost:8080/actuator/health

# Detaylı health
curl http://localhost:8080/actuator/health | jq

# Database bağlantısı
curl http://localhost:8080/actuator/health/db | jq

# Disk kullanımı
curl http://localhost:8080/actuator/health/diskSpace | jq
```

### Container Metrikleri

```bash
# Gerçek zamanlı metrics
docker stats

# Sadece Elly containers
docker stats elly-app elly-postgres

# Disk kullanımı
docker system df
```

### Problem Giderme

```bash
# Container yeniden başlatıldı mı?
docker-compose ps

# Son 50 log satırı
docker-compose logs --tail=50 app

# Container'ın içinde shell aç
docker-compose exec app sh

# Network bağlantısını test et
docker-compose exec app ping postgres

# Database bağlantısını test et
docker-compose exec app wget -O- postgres:5432
```

## 🔒 Production Deployment

### 1. Production Compose Dosyası Kullan

```bash
# Production modunda başlat
docker-compose -f docker-compose.prod.yml up -d
```

### 2. Environment Variables'ı Güvenli Tut

```bash
# .env dosyasını git'e ekleme!
echo ".env" >> .gitignore

# Güçlü şifre oluştur
openssl rand -base64 32
```

### 3. Volume Backup Stratejisi

```bash
# Otomatik backup cron job'ı ekle
0 2 * * * docker-compose exec postgres pg_dump -U postgres -d postgres --schema=elly > /backup/elly-$(date +\%Y\%m\%d).sql
```

### 4. Reverse Proxy (Nginx örneği)

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 🎯 Önemli Notlar

### ✅ İlk Çalıştırmada Otomatik Yapılanlar

1. **PostgreSQL Container:**
   - `elly` schema'sı otomatik oluşturulur
   - Junction table indexleri otomatik eklenir
   - Health check aktif hale gelir

2. **Spring Boot Container:**
   - Maven build otomatik çalışır (~2-3 dakika)
   - Hibernate tabloları otomatik oluşturur
   - Assets klasörleri hazırlanır

### ⚠️ Dikkat Edilmesi Gerekenler

1. **İlk Başlatma:** İlk build 2-5 dakika sürebilir
2. **Volume'lar:** `docker-compose down -v` tüm datayı siler!
3. **Port Çakışması:** 8080 veya 5432 portları kullanımda ise .env'de değiştirin
4. **Memory:** Minimum 2GB RAM gereklidir

### 🔄 Güncellemeler

```bash
# Kod değişikliği sonrası
docker-compose up -d --build app

# Sadece database değişikliği
docker-compose restart postgres

# Tüm sistemi güncelle
docker-compose down
docker-compose up -d --build
```

## 🆘 Sık Karşılaşılan Sorunlar

### 1. "Port already in use"

```bash
# Portu kullanan process'i bul
lsof -i :8080
# veya
netstat -an | grep 8080

# .env dosyasında portu değiştir
APP_PORT=3000
```

### 2. "Database connection failed"

```bash
# PostgreSQL container'ının çalıştığını kontrol et
docker-compose ps postgres

# Logları kontrol et
docker-compose logs postgres

# Health check
docker-compose exec postgres pg_isready
```

### 3. "Out of memory"

```bash
# JVM memory'yi azalt
JAVA_OPTS="-Xmx256m -Xms128m"

# Veya Docker'a daha fazla memory ver
# Docker Desktop -> Settings -> Resources -> Memory
```

### 4. "Schema 'elly' does not exist"

```bash
# Init script'i manuel çalıştır
docker-compose exec postgres psql -U postgres -d postgres -f /docker-entrypoint-initdb.d/00-init.sql
```

## 📚 Faydalı Linkler

- **API Docs:** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/actuator/health
- **Database:** postgresql://localhost:5432/postgres (schema: elly)

## 🎓 Best Practices

1. ✅ Her zaman `.env` dosyası kullanın
2. ✅ Production'da güçlü şifreler kullanın
3. ✅ Düzenli database backup'ı alın
4. ✅ Log rotation ayarlayın
5. ✅ Health check endpoint'lerini monitoring edin
6. ✅ Container'ları güncel tutun

---

**İyi kodlamalar! 🚀**


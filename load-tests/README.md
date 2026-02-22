# 🚀 Elly CMS Load & Stress Testing

Bu klasör, Elly CMS projesinin performans testlerini içerir.

## 📁 Dosyalar

- **k6-basic-test.js** - Temel load test (100 kullanıcı, 5.5 dakika)
- **k6-stress-test.js** - Stres testi (500 kullanıcıya kadar, limit bulma)
- **k6-write-test.js** - Yazma operasyonları testi (POST, PUT, DELETE)

## ⚡ Hızlı Başlangıç

```bash
# K6 kur (macOS)
brew install k6

# Uygulamayı başlat
cd ..
./mvnw spring-boot:run

# Test çalıştır
k6 run k6-basic-test.js
```

Veya Makefile ile:

```bash
cd ..
make perf-setup    # K6 kur
make load-test     # Test çalıştır
```

## 📦 Kurulum

### K6 Kurulumu (Önerilen)
```bash
# macOS
brew install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Windows (Chocolatey)
choco install k6
```

### JMeter Kurulumu (Alternatif)
```bash
brew install jmeter
```

## 🎯 Test Senaryoları

### 1. **Basic Load Test** (Başlangıç)
Normal kullanım senaryosu: 10-100 kullanıcı
```bash
k6 run k6-basic-test.js
```
**Ne test eder:**
- Sayfa listeleme (Pages)
- Post listeleme ve detay
- Component listeleme (JOIN'ler)
- Rating istatistikleri

**Beklenen Sonuçlar:**
- ✅ Response time: <500ms (p95)
- ✅ Error rate: <5%
- ✅ Throughput: >100 req/s

---

### 2. **Stress Test** (Limit Bulma)
Sistem limitini bulmak: 50-500 kullanıcı
```bash
k6 run k6-stress-test.js
```
**Ne test eder:**
- Sistemi kademeli olarak zorlar
- Breaking point'i bulur
- Recovery süresini ölçer

**Kritik Metrikler:**
- 🔴 Hangi kullanıcı sayısında sistem yavaşlıyor?
- 🔴 Hangi endpoint'ler problem yaratıyor?
- 🔴 Database connection pool doluyor mu?

---

### 3. **Write Operations Test** (Yazma İşlemleri)
Database yazma operasyonları: POST, PUT, DELETE
```bash
k6 run k6-write-test.js
```
**Ne test eder:**
- Post oluşturma
- Comment ekleme
- Rating ekleme
- Transaction yükü

**Dikkat Edilmesi Gerekenler:**
- ⚠️ Bu test veritabanını kirletir!
- ⚠️ Test sonrası cleanup gerekebilir
- ⚠️ Sadece test environment'ta çalıştırın

---

## 📊 Test Sonuçlarını Okuma

### K6 Çıktısı
```
     ✓ Pages status is 200
     ✓ Posts response time < 500ms
     
     checks.........................: 95.50% ✓ 3820  ✗ 180
     data_received..................: 8.2 MB 41 kB/s
     data_sent......................: 1.1 MB 5.5 kB/s
     http_req_blocked...............: avg=1.2ms   min=1µs   med=4µs    max=100ms  p(95)=3ms   
     http_req_connecting............: avg=500µs   min=0s    med=0s     max=50ms   p(95)=1ms   
     http_req_duration..............: avg=250ms   min=10ms  med=200ms  max=2s     p(95)=450ms 
     http_req_failed................: 4.50%  ✓ 180   ✗ 3820
     http_reqs......................: 4000   20/s
     vus............................: 100    min=0    max=100
     vus_max........................: 100    min=100  max=100
```

### Önemli Metrikler:
- **http_req_duration p(95)**: %95'lik response time (hedef: <500ms)
- **http_req_failed**: Hata oranı (hedef: <%5)
- **http_reqs**: Toplam istek sayısı ve throughput
- **checks**: Assertion başarı oranı

---

## 🔧 Apache Bench ile Hızlı Test

Tek bir endpoint'i hızlıca test etmek için:

```bash
# 1000 istek, 100 eş zamanlı
ab -n 1000 -c 100 http://localhost:8080/api/posts

# POST request ile
ab -n 500 -c 50 -p post-data.json -T application/json http://localhost:8080/api/posts
```

---

## 🐛 Sorun Giderme

### Test Sırasında Hatalar

#### 1. Connection Refused
```
✗ Connection refused
```
**Çözüm:** Uygulamanın çalıştığından emin olun
```bash
cd /Users/huseyindol/Documents/huseyin_server/java_workspace/elly
./mvnw spring-boot:run
```

#### 2. Database Connection Pool Exhausted
```
✗ HikariPool - Connection is not available
```
**Çözüm:** `application.properties`'de pool size artırın:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

#### 3. Too Many Open Files
```
✗ Too many open files
```
**Çözüm (macOS):**
```bash
ulimit -n 10000
```

---

## 📈 Monitoring Sırasında

Test çalışırken ayrı terminal'lerde:

### 1. Application Logs
```bash
tail -f logs/spring.log
```

### 2. Database Connections
```sql
-- PostgreSQL'de aktif connection sayısı
SELECT count(*) FROM pg_stat_activity WHERE datname = 'neondb';
```

### 3. Spring Actuator Metrics
```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Hikari pool status
curl http://localhost:8080/actuator/metrics/hikari.connections.active
```

### 4. System Resources (macOS)
```bash
# CPU ve Memory
top -l 1 | grep -E "CPU|PhysMem"

# JVM Memory
jcmd <PID> VM.native_memory summary
```

---

## 📊 HTML Rapor Oluşturma

K6 ile HTML rapor:
```bash
k6 run k6-basic-test.js --out json=results.json
k6 convert results.json -O results.html
```

---

## ⚠️ UYARILAR

1. **Production'da test yapmayın!** Sadece test/staging environment'ta test yapın.
2. **Rate limiting yok:** Bu testler rate limiting olmadığını varsayar.
3. **Authentication yok:** Authentication gerekliyse test script'lerini güncelleyin.
4. **Database cleanup:** Write testler sonrası test verilerini temizleyin.

---

## 🎯 Best Practices

1. **Küçük başlayın:** Önce 10-50 kullanıcı ile başlayın
2. **İzleyin:** Testler sırasında CPU, memory, DB connections'ı izleyin
3. **Kaydedin:** Her test sonucunu kaydedin ve karşılaştırın
4. **Tekrarlayın:** Aynı testi birkaç kez çalıştırın (consistency)
5. **Gerçekçi olun:** Production traffic pattern'lerini taklit edin

---

## 📚 Daha Fazla Bilgi

- [K6 Documentation](https://k6.io/docs/)
- [JMeter Best Practices](https://jmeter.apache.org/usermanual/best-practices.html)
- [Spring Boot Performance Tuning](https://spring.io/guides/gs/spring-boot/)

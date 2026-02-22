# 🚀 Elly CMS - Load Test Başlangıç Rehberi

Bu rehber, hiç load test yapmamış geliştiriciler için adım adım kurulum ve çalıştırma talimatlarını içerir.

---

## 📋 ÖN HAZIRLIK

### 1. Uygulamanızı Başlatın
```bash
cd /Users/huseyindol/Documents/huseyin_server/java_workspace/elly

# Performance profili ile başlatın
./mvnw spring-boot:run -Dspring-boot.run.profiles=performance

# Veya normal profil ile
./mvnw spring-boot:run
```

### 2. Uygulamanın Çalıştığını Doğrulayın
```bash
# Health check
curl http://localhost:8080/actuator/health

# Beklenen yanıt:
# {"status":"UP"}
```

### 3. Test Verisi Oluşturun (İsteğe bağlı)
Eğer veritabanınız boşsa, önce biraz test verisi oluşturun:

```bash
# Swagger UI'dan manuel olarak veri ekleyin:
# http://localhost:8080/swagger-ui.html

# Veya curl ile:
curl -X POST http://localhost:8080/api/pages \
  -H "Content-Type: application/json" \
  -d '{
    "slug": "homepage",
    "title": "Homepage",
    "content": "Welcome to Elly CMS"
  }'
```

---

## 🎯 SENARYO 1: Apache Bench ile Hızlı Test (5 dakika)

Apache Bench (ab) macOS'te varsayılan olarak yüklüdür.

### Basit GET Testi
```bash
# 1000 istek, 100 eş zamanlı kullanıcı
ab -n 1000 -c 100 http://localhost:8080/api/pages
```

### Çıktıyı Anlama
```
Server Software:        
Server Hostname:        localhost
Server Port:            8080

Document Path:          /api/pages
Document Length:        1234 bytes

Concurrency Level:      100
Time taken for tests:   5.123 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      1234000 bytes
HTML transferred:       1234000 bytes
Requests per second:    195.23 [#/sec] (mean)  ← ÖNEMLİ!
Time per request:       512.3 [ms] (mean)      ← ÖNEMLİ!
Time per request:       5.123 [ms] (mean, across all concurrent requests)
Transfer rate:          234.56 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   0.5      1       5
Processing:    12  489  87.3    467     891
Waiting:       12  488  87.3    466     890
Total:         12  490  87.5    468     892

Percentage of the requests served within a certain time (ms)
  50%    468   ← Median
  66%    521
  75%    567
  80%    598
  90%    678
  95%    745   ← %95'lik response time
  98%    812
  99%    856
 100%    892   ← En yavaş request
```

### Ne Anlama Geliyor?
- **Requests per second:** Sisteminiz saniyede kaç istek işleyebiliyor
  - <50: Çok yavaş 🔴
  - 50-200: Normal 🟡
  - >200: İyi ✅
  
- **Time per request (mean):** Ortalama yanıt süresi
  - <100ms: Mükemmel ✅
  - 100-500ms: İyi 🟢
  - 500-1000ms: Kabul edilebilir 🟡
  - >1000ms: Yavaş 🔴

- **95% percentile:** İsteklerin %95'i bu sürede tamamlanıyor
  - <500ms: İyi ✅
  - >1000ms: Optimizasyon gerekli 🔴

---

## 🎯 SENARYO 2: K6 ile Gerçekçi Test (30 dakika)

### 1. K6 Kurulumu
```bash
# macOS
brew install k6

# Kurulumu doğrula
k6 version
```

### 2. Basic Test Çalıştırın
```bash
cd load-tests
k6 run k6-basic-test.js
```

### 3. Test Çıktısını İzleyin

Terminal'de real-time çıktı göreceksiniz:

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

  execution: local
     script: k6-basic-test.js
     output: -

  scenarios: (100.00%) 1 scenario, 100 max VUs, 6m30s max duration
           * default: Up to 100 looping VUs for 5m30s over 6 stages

running (5m30.2s), 000/100 VUs, 4500 complete and 0 interrupted iterations
default ✓ [======================================] 000/100 VUs  5m30s

     ✓ Pages status is 200
     ✓ Posts response time < 500ms
     ✓ Components status is 200

     checks.........................: 97.25% ✓ 13095  ✗ 370
     data_received..................: 8.2 MB 25 kB/s
     data_sent......................: 1.8 MB 5.4 kB/s
     http_req_blocked...............: avg=987µs  min=2µs    med=5µs    max=89ms  p(95)=3ms   
     http_req_connecting............: avg=451µs  min=0s     med=0s     max=45ms  p(95)=1ms   
     http_req_duration..............: avg=345ms  min=23ms   med=287ms  max=3.2s  p(95)=678ms 
       { expected_response:true }...: avg=312ms  min=23ms   med=267ms  max=1.8s  p(95)=589ms 
     http_req_failed................: 2.74%  ✓ 370    ✗ 13095
     http_req_receiving.............: avg=123µs  min=21µs   med=98µs   max=8ms   p(95)=234µs 
     http_req_sending...............: avg=45µs   min=8µs    med=34µs   max=2ms   p(95)=89µs  
     http_req_tls_handshaking.......: avg=0s     min=0s     med=0s     max=0s    p(95)=0s    
     http_req_waiting...............: avg=344ms  min=23ms   med=286ms  max=3.2s  p(95)=677ms 
     http_reqs......................: 13465  40.9/s
     iteration_duration.............: avg=4.12s  min=4.01s  med=4.11s  max=5.89s p(95)=4.45s 
     iterations.....................: 4500   13.6/s
     vus............................: 2      min=2    max=100
     vus_max........................: 100    min=100  max=100
```

### 4. Önemli Metrikler

| Metrik | Hedef | Açıklama |
|--------|-------|----------|
| `checks` | >95% | Assertion'ların başarı oranı |
| `http_req_duration p(95)` | <500ms | %95'lik response time |
| `http_req_failed` | <5% | Hata oranı |
| `http_reqs` | Değişken | Toplam istek sayısı ve throughput |
| `vus` | Değişken | Şu anda aktif sanal kullanıcı sayısı |

---

## 🎯 SENARYO 3: Stress Test - Limitinizi Bulun (1 saat)

### 1. Stress Test Çalıştırın
```bash
cd load-tests
k6 run k6-stress-test.js
```

Bu test sisteminizi **limitine kadar zorlayacak**:
- 0-50 kullanıcı: Warm-up
- 50-100 kullanıcı: Normal yük
- 100-300 kullanıcı: Yüksek yük
- 300-500 kullanıcı: Ekstrem yük

### 2. İzlenmesi Gerekenler

Test çalışırken **ayrı bir terminal**'de:

#### a) Uygulama Logları
```bash
tail -f logs/elly-performance.log
```

Dikkat edilecek hatalar:
```
ERROR: Connection pool exhausted
ERROR: Query timeout
ERROR: OutOfMemoryError
ERROR: Too many open files
```

#### b) Database Connections
```bash
# PostgreSQL'e bağlanın (eğer local ise)
psql -U postgres -d neondb

# Aktif connection sayısı
SELECT count(*) FROM pg_stat_activity WHERE datname = 'neondb';

# Uzun süren query'ler
SELECT pid, now() - pg_stat_activity.query_start AS duration, query 
FROM pg_stat_activity 
WHERE state = 'active' 
ORDER BY duration DESC;
```

#### c) Actuator Metrics
```bash
# Her 5 saniyede bir izleyin
watch -n 5 'curl -s http://localhost:8080/actuator/metrics/hikari.connections.active | jq'

# CPU usage
curl -s http://localhost:8080/actuator/metrics/system.cpu.usage | jq

# JVM Memory
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq
```

#### d) System Resources (macOS)
```bash
# CPU ve Memory
top -l 1 | grep -E "CPU|PhysMem"

# Java process
ps aux | grep java

# Network connections
netstat -an | grep 8080 | wc -l
```

### 3. Kırılma Noktasını Bulma

Test sırasında şunları not edin:

| Kullanıcı Sayısı | Response Time (p95) | Error Rate | Notlar |
|------------------|---------------------|------------|---------|
| 50 | 234ms | 0% | ✅ Normal |
| 100 | 456ms | 1% | ✅ İyi |
| 200 | 892ms | 5% | 🟡 Yavaşlıyor |
| 300 | 2.3s | 15% | 🔴 **Breaking Point!** |
| 400 | 5s+ | 40% | 💀 Çöküş |

**Örnek Sonuç:**
> "Sistemimiz 200 eş zamanlı kullanıcıya kadar stabil. 300 kullanıcıdan sonra response time 2 saniyeyi geçiyor ve hata oranı %15'e çıkıyor. **Maximum capacity: ~250 concurrent users**"

---

## 📊 SONUÇLARI RAPORLAMA

### Örnek Rapor Template

```markdown
# Elly CMS Load Test Raporu
Tarih: 14 Aralık 2025
Test Ortamı: Local development
Database: Neon PostgreSQL (Cloud)

## Test Konfigürasyonu
- Java Version: 21
- Spring Boot: 3.5.7
- Profile: performance
- Hikari Pool Size: 50
- Tomcat Max Threads: 500

## Sonuçlar

### Basic Load Test (K6)
- Max VUs: 100
- Duration: 5 dakika 30 saniye
- Total Requests: 13,465
- Failed Requests: 370 (2.74%)
- Avg Response Time: 345ms
- P95 Response Time: 678ms
- Throughput: 40.9 req/s

**Değerlendirme:** ✅ Başarılı

### Stress Test
- Max VUs: 500
- Duration: 13 dakika
- Breaking Point: ~300 concurrent users
- Critical Issues:
  - Connection pool tükenme (300+ users)
  - Slow query: `/api/components` (3.2s)
  - Memory usage: %85

**Değerlendirme:** 🔴 Optimization gerekli

## Tespit Edilen Sorunlar

1. **N+1 Query Problem** (CRITICAL)
   - Endpoint: `/api/components`
   - 1 request = 47 database query!
   - Çözüm: Entity Graph kullan

2. **Connection Pool Exhausted** (HIGH)
   - 300+ user'da pool tükeniyor
   - Çözüm: Pool size 50'den 100'e çıkar veya query'leri optimize et

3. **Slow Rating Stats** (MEDIUM)
   - Endpoint: `/api/ratings/stats`
   - Avg: 1.2s
   - Çözüm: Index ekle + caching

## Öneriler

### Hemen (1 gün)
- [ ] Database index'leri ekle
- [ ] N+1 query'leri düzelt (Entity Graph)
- [ ] Connection pool 100'e çıkar

### Bu Hafta
- [ ] Redis cache kurulumu
- [ ] Query optimization
- [ ] DTO pattern uygula

### Bu Ay
- [ ] Monitoring (Prometheus + Grafana)
- [ ] CDN entegrasyonu (file uploads)
- [ ] Load balancer setup

## Kapasite Planlaması

Mevcut Durum:
- Max Concurrent Users: ~250
- Avg Response Time: 345ms
- Throughput: 40 req/s

Hedef (Optimization sonrası):
- Max Concurrent Users: 1000+
- Avg Response Time: <200ms
- Throughput: 200+ req/s
```

---

## 🔍 DEBUGGING: Problem Varsa

### Problem 1: "Connection Refused"
```bash
# Çözüm: Uygulamayı başlatın
./mvnw spring-boot:run
```

### Problem 2: Test Çok Yavaş
```bash
# VU sayısını azaltın
# k6-basic-test.js içinde:
stages: [
  { duration: '30s', target: 10 },  # 50 yerine 10
  { duration: '1m', target: 20 },   # 100 yerine 20
]
```

### Problem 3: Database Connection Error
```properties
# application.properties'de connection pool'u azaltın
spring.datasource.hikari.maximum-pool-size=10
```

### Problem 4: OutOfMemoryError
```bash
# JVM heap size artırın
export MAVEN_OPTS="-Xmx2048m"
./mvnw spring-boot:run
```

---

## ✅ CHECKLIST: Test Öncesi

- [ ] Uygulama çalışıyor (`curl http://localhost:8080/actuator/health`)
- [ ] Database erişilebilir
- [ ] Test verisi mevcut (en az 10 page, 10 post)
- [ ] Disk space yeterli (log dosyaları için)
- [ ] K6 kurulu (`k6 version`)
- [ ] Monitoring için ekstra terminal'ler açık

---

## 📚 Sonraki Adımlar

Test tamamlandıktan sonra:

1. **Sonuçları analiz edin** (yukarıdaki template'i kullanın)
2. **PERFORMANCE_ANALYSIS.md**'yi okuyun (tüm sorunlar ve çözümler orada)
3. **Optimizasyonları uygulayın** (öncelik sırasıyla)
4. **Tekrar test edin** (iyileşmeyi ölçün)
5. **Monitoring setup** yapın (production'a geçmeden önce)

---

**İyi şanslar! 🚀**

Sorularınız için: [PROJECT.md](PROJECT.md) ve [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md) dosyalarına bakın.

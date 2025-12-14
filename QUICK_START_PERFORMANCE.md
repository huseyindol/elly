# 🚀 Hızlı Başlangıç: Elly CMS Performance Testing

Bu dosya, load test ve optimizasyon için **en hızlı başlangıç** rehberidir.

---

## ⚡ 5 Dakikada Load Test

### 1. Terminal'i Açın ve Çalıştırın:

```bash
cd /Users/huseyindol/Documents/huseyin_server/java_workspace/elly

# Adım 1: K6 kurulumu (macOS)
brew install k6

# Adım 2: Uygulamayı başlat
./mvnw spring-boot:run

# Adım 3: Yeni terminal aç ve test çalıştır
k6 run load-tests/k6-basic-test.js
```

### 2. Sonuçları İzleyin

Test sonunda göreceksiniz:
```
✓ Pages status is 200
✓ Posts response time < 500ms

checks.........................: 95.50% ✓ 3820  ✗ 180
http_req_duration..............: avg=345ms p(95)=678ms
http_req_failed................: 2.74%
http_reqs......................: 4000   40.9/s
```

**Değerlendirme:**
- ✅ Checks >95% → İyi
- 🟡 P95 678ms → Optimizasyon gerekebilir
- ✅ Error rate 2.74% → Kabul edilebilir

---

## 🎯 Makefile ile Daha Kolay

Tüm komutlar Makefile'a eklendi!

```bash
# Setup - Tek komut
make perf-setup          # K6 ve araçları kur

# Test - Tek komut
make load-test           # Basic load test
make stress-test         # Stress test (dikkatli!)
make ab-test             # Hızlı Apache Bench test

# Database - Tek komut
make db-perf             # Performance index'lerini yükle

# Monitoring - Tek komut
make monitor             # Real-time metrics görüntüle

# Yardım
make perf-help           # Tüm performans komutları
```

---

## 📊 Test Senaryoları

### Senaryo 1: Başlangıç (5 dakika)
```bash
# Terminal 1: Uygulama
./mvnw spring-boot:run

# Terminal 2: Test
make load-test

# Sonuç: Baseline performans
```

### Senaryo 2: Monitoring (15 dakika)
```bash
# Terminal 1: Uygulama
./mvnw spring-boot:run

# Terminal 2: Real-time monitoring
make monitor

# Terminal 3: Test
make load-test

# Sonuç: Test sırasında metrics izle
```

### Senaryo 3: Limit Bulma (30 dakika)
```bash
# Terminal 1: Uygulama (performance mode)
make perf-mode

# Terminal 2: Monitoring
make monitor

# Terminal 3: Stress test
make stress-test

# Sonuç: Sistemin limitini öğren
```

---

## 🔧 Optimizasyon - Hemen Yapılacaklar

### 1. Database Index'lerini Yükle (2 dakika)
```bash
# Tek komutla:
make db-perf

# Veya manuel:
psql "postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:postgres}?sslmode=require&channel_binding=require" \
  -f src/main/resources/db-performance-indexes.sql
```

**Etki:** %80-90 sorgu hızlanması ⚡

### 2. Connection Pool Artır (1 dakika)
```properties
# application.properties (veya application-performance.properties kullanın)
spring.datasource.hikari.maximum-pool-size=50  # 10'dan 50'ye
```

**Etki:** Yüksek yük altında çöküş önlenir 🛡️

### 3. Pagination Ekle (10 dakika)
```java
// Örnek kod: OPTIMIZATION_EXAMPLES.md dosyasında
@GetMapping
public ResponseEntity<Page<DtoPost>> getAllPosts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    // ...
}
```

**Etki:** OutOfMemoryError önlenir 💾

---

## 📈 Beklenen İyileşmeler

### ÖNCE (Optimizasyon yok)
```
Response Time (p95):    2000ms    🔴
Throughput:             20 req/s   🔴
Max Users:              ~50        🔴
Error Rate:             10%        🔴
```

### SONRA (Index + Connection Pool + Pagination)
```
Response Time (p95):    <500ms    ✅
Throughput:             200 req/s  ✅
Max Users:              ~500       ✅
Error Rate:             <1%        ✅
```

**10x İyileşme!** 🚀

---

## 📚 Detaylı Dökümanlar

| Dosya | Açıklama |
|-------|----------|
| **LOAD_TEST_GUIDE.md** | Adım adım load test rehberi (hiç bilmeyen için) |
| **PERFORMANCE_ANALYSIS.md** | Tüm olası sorunlar ve çözümleri (11 problem detaylı) |
| **OPTIMIZATION_EXAMPLES.md** | Kopyala-yapıştır kod örnekleri |
| **db-performance-indexes.sql** | Hazır SQL index'leri |
| **application-performance.properties** | Hazır performans konfigürasyonu |

---

## 🎬 Video Tutorial (Varsayımsal)

```bash
# Adım 1: Setup (Terminal 1)
cd ~/Documents/huseyin_server/java_workspace/elly
make perf-setup

# Adım 2: Database Index'leri (Terminal 1)
make db-perf

# Adım 3: Uygulamayı Başlat (Terminal 1)
./mvnw spring-boot:run

# Adım 4: Monitoring (Terminal 2)
make monitor

# Adım 5: Test Çalıştır (Terminal 3)
make load-test

# Adım 6: Sonuçları Analiz Et
# Terminal 3'te çıktıyı incele
```

---

## ❓ Sık Sorulan Sorular

### Q: Test sırasında uygulama çöktü, ne yapmalıyım?
**A:** Normal! Stress test'in amacı bu. Çökme noktasını not edin ve optimizasyon yapın.

### Q: K6 çıktısını nasıl okuyorum?
**A:** 
- `checks`: %95 üzeri olmalı ✅
- `http_req_duration p(95)`: 500ms altı olmalı ✅
- `http_req_failed`: %5 altı olmalı ✅

### Q: Production'da test yapabilir miyim?
**A:** ❌ ASLA! Sadece test/staging ortamında test yapın.

### Q: Index'ler database'i yavaşlatır mı?
**A:** Write işlemlerini %5-10 yavaşlatır ama read işlemlerini 10-100x hızlandırır. Trade-off değer.

### Q: Hangi optimizasyonu önce yapmalıyım?
**A:** Öncelik sırası:
1. Database index'leri (en kolay, en etkili)
2. Connection pool artırma (5 dakika)
3. Pagination (OutOfMemory önler)
4. N+1 query çözümleri (biraz kod gerektirir)

---

## 🚨 Uyarılar

1. **Test ortamında çalışın!** Production'da test yapmayın.
2. **Monitoring şart!** Test sırasında mutlaka metrics izleyin.
3. **Tek tek optimize edin!** Her optimizasyonun etkisini ayrı ölçün.
4. **Backup alın!** Database optimizasyonları öncesi mutlaka backup alın.

---

## ✅ Checklist

Başlamadan önce kontrol edin:

- [ ] Java 21 kurulu mu? (`java -version`)
- [ ] PostgreSQL erişilebilir mi?
- [ ] K6 kurulu mu? (`k6 version`)
- [ ] Uygulama çalışıyor mu? (`curl localhost:8080/actuator/health`)
- [ ] Load test dosyaları var mı? (`ls load-tests/`)
- [ ] En az 4GB RAM boşta mı?

---

## 🎯 İlk Hedef

**Şimdi yapın:**
1. `make perf-setup` - Araçları kur (2 dakika)
2. `make db-perf` - Index'leri yükle (2 dakika)
3. `./mvnw spring-boot:run` - Uygulamayı başlat (30 saniye)
4. `make load-test` - Test çalıştır (5 dakika)

**Toplam süre: ~10 dakika** ⏱️

---

**Başarılar! 🚀**

Sorularınız için: [LOAD_TEST_GUIDE.md](LOAD_TEST_GUIDE.md)

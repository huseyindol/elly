# 📚 Elly CMS - Performans Dokümantasyonu Index

Bu dosya, tüm performans ve load testing dökümanlarına erişim için rehberinizdir.

---

## 🎯 Nereden Başlamalıyım?

### Hiç load test yapmadıysanız:
👉 **[QUICK_START_PERFORMANCE.md](QUICK_START_PERFORMANCE.md)** ← BURADAN BAŞLAYIN!

5 dakikada ilk testinizi çalıştırın!

---

## 📖 Tüm Dökümanlar

### 1️⃣ Hızlı Başlangıç
| Dosya | İçerik | Süre |
|-------|--------|------|
| **[QUICK_START_PERFORMANCE.md](QUICK_START_PERFORMANCE.md)** | 5 dakikada load test | ⏱️ 5 dk |
| **[Makefile](Makefile)** | Tüm komutlar (make perf-help) | ⏱️ 1 dk |

### 2️⃣ Detaylı Rehberler
| Dosya | İçerik | Süre |
|-------|--------|------|
| **[LOAD_TEST_GUIDE.md](LOAD_TEST_GUIDE.md)** | Adım adım test rehberi | ⏱️ 30 dk |
| **[PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md)** | 11 olası sorun + çözümleri | ⏱️ 45 dk |
| **[OPTIMIZATION_EXAMPLES.md](OPTIMIZATION_EXAMPLES.md)** | Kopyala-yapıştır kod örnekleri | ⏱️ 30 dk |

### 3️⃣ Özet ve Roadmap
| Dosya | İçerik | Süre |
|-------|--------|------|
| **[PERFORMANCE_SUMMARY.md](PERFORMANCE_SUMMARY.md)** | Genel özet ve action items | ⏱️ 15 dk |
| **[PERFORMANCE_ROADMAP.md](PERFORMANCE_ROADMAP.md)** | Görsel roadmap ve timeline | ⏱️ 10 dk |
| **[PERFORMANCE_INDEX.md](PERFORMANCE_INDEX.md)** | Bu dosya - tüm dökümanlar | ⏱️ 5 dk |

### 4️⃣ Test Dosyaları
| Dosya | İçerik | Kullanım |
|-------|--------|----------|
| **[load-tests/k6-basic-test.js](load-tests/k6-basic-test.js)** | Temel load test | `k6 run k6-basic-test.js` |
| **[load-tests/k6-stress-test.js](load-tests/k6-stress-test.js)** | Stres testi | `k6 run k6-stress-test.js` |
| **[load-tests/k6-write-test.js](load-tests/k6-write-test.js)** | Yazma operasyonları | `k6 run k6-write-test.js` |
| **[load-tests/README.md](load-tests/README.md)** | Test klasörü rehberi | - |

### 5️⃣ Konfigürasyon Dosyaları
| Dosya | İçerik | Kullanım |
|-------|--------|----------|
| **[src/main/resources/application-performance.properties](src/main/resources/application-performance.properties)** | Optimize edilmiş config | `--spring.profiles.active=performance` |
| **[src/main/resources/db-performance-indexes.sql](src/main/resources/db-performance-indexes.sql)** | Database index'leri | `make db-perf` |

---

## 🚀 Hızlı Erişim: Senaryolar

### Senaryo 1: "Load test nedir öğrenmek istiyorum"
1. [QUICK_START_PERFORMANCE.md](QUICK_START_PERFORMANCE.md) (5 dakika)
2. [LOAD_TEST_GUIDE.md](LOAD_TEST_GUIDE.md) (30 dakika)

### Senaryo 2: "Hemen test çalıştırmak istiyorum"
```bash
make perf-setup
make load-test
```
Detay: [QUICK_START_PERFORMANCE.md](QUICK_START_PERFORMANCE.md)

### Senaryo 3: "Performans problemlerim var, çözmek istiyorum"
1. [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md) - 11 problem + çözüm
2. [OPTIMIZATION_EXAMPLES.md](OPTIMIZATION_EXAMPLES.md) - Kod örnekleri

### Senaryo 4: "Optimizasyon yapmak istiyorum, nereden başlamalıyım?"
1. [PERFORMANCE_ROADMAP.md](PERFORMANCE_ROADMAP.md) - Timeline ve öncelikler
2. [OPTIMIZATION_EXAMPLES.md](OPTIMIZATION_EXAMPLES.md) - Direkt uygulanabilir kod

### Senaryo 5: "Sistemim çok yavaş, acil çözüm lazım!"
**3 Hızlı Çözüm:**
1. `make db-perf` (2 dakika) → %80 iyileşme
2. Connection pool artır (1 dakika) → Çöküş önler
3. Pagination ekle (10 dakika) → Memory önler

Detay: [QUICK_START_PERFORMANCE.md](QUICK_START_PERFORMANCE.md) → "Hemen Yapılacaklar"

---

## 🎓 Öğrenme Yolu (Sıfırdan Expert)

### Seviye 1: Başlangıç (1 gün)
- [ ] [QUICK_START_PERFORMANCE.md](QUICK_START_PERFORMANCE.md) oku
- [ ] K6 kur (`make perf-setup`)
- [ ] İlk load test'i çalıştır (`make load-test`)
- [ ] Sonuçları anlamayı öğren (p95, throughput, error rate)

### Seviye 2: Temel Optimizasyon (1 hafta)
- [ ] [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md) oku
- [ ] Database index'leri yükle (`make db-perf`)
- [ ] Connection pool artır
- [ ] Pagination ekle
- [ ] Test tekrarla ve karşılaştır

### Seviye 3: İleri Seviye (2-3 hafta)
- [ ] [OPTIMIZATION_EXAMPLES.md](OPTIMIZATION_EXAMPLES.md) uygula
- [ ] N+1 query problemlerini çöz (Entity Graph)
- [ ] DTO pattern kullan
- [ ] Query optimization
- [ ] Stress test çalıştır

### Seviye 4: Production Ready (4 hafta)
- [ ] Redis cache kurulumu
- [ ] Monitoring (Prometheus + Grafana)
- [ ] Alerting setup
- [ ] Capacity planning
- [ ] Production deployment stratejisi

---

## 📊 Olası Sorunlar - Hızlı Referans

| Problem | Çözüm | Dosya | Süre |
|---------|-------|-------|------|
| **Yavaş sorgular** | Database index'leri | [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md) #3 | 2 dk |
| **Connection pool tükeniyor** | Pool size artır | [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md) #2 | 1 dk |
| **N+1 query** | Entity Graph | [OPTIMIZATION_EXAMPLES.md](OPTIMIZATION_EXAMPLES.md) #1 | 30 dk |
| **OutOfMemoryError** | Pagination | [OPTIMIZATION_EXAMPLES.md](OPTIMIZATION_EXAMPLES.md) #2 | 10 dk |
| **JSON circular reference** | DTO pattern | [OPTIMIZATION_EXAMPLES.md](OPTIMIZATION_EXAMPLES.md) #3 | 30 dk |
| **Test nasıl çalıştırılır?** | K6 setup | [LOAD_TEST_GUIDE.md](LOAD_TEST_GUIDE.md) | 5 dk |
| **Sonuçları anlayamıyorum** | Metrik açıklaması | [LOAD_TEST_GUIDE.md](LOAD_TEST_GUIDE.md) #4 | 10 dk |
| **Monitoring nasıl yapılır?** | Real-time monitoring | [LOAD_TEST_GUIDE.md](LOAD_TEST_GUIDE.md) → Monitoring | 5 dk |
| **Cloud database yavaş** | Cache + Index | [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md) #7 | 1 gün |
| **File upload yavaş** | Async + S3 | [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md) #5 | 2 gün |

---

## 🛠️ Komutlar - Hızlı Referans

### Setup
```bash
make perf-setup          # K6 ve araçları kur
make db-perf             # Database index'lerini yükle
```

### Test
```bash
make load-test           # Basic load test (5 dakika)
make stress-test         # Stress test (13 dakika)
make write-test          # Write operations test
make ab-test             # Hızlı Apache Bench test
```

### Monitoring
```bash
make monitor             # Real-time metrics
make health              # Health check
make stats               # Resource usage
```

### Yardım
```bash
make help                # Tüm Docker komutları
make perf-help           # Performans komutları
```

Detay: [Makefile](Makefile) (tüm komutlar orada)

---

## 📈 Beklenen Sonuçlar

### Başlangıç (Optimizasyon öncesi)
```
Response Time (p95):    2000ms     🔴
Throughput:             20 req/s   🔴
Max Users:              ~50        🔴
Error Rate:             10%        🔴
Database Queries:       47/req     🔴
```

### Hedef (Optimizasyon sonrası)
```
Response Time (p95):    <500ms     ✅ (%75 iyileşme)
Throughput:             200 req/s  ✅ (10x artış)
Max Users:              500+       ✅ (10x artış)
Error Rate:             <1%        ✅ (%90 azalma)
Database Queries:       1/req      ✅ (%98 azalma)
```

**10x Performans İyileşmesi!** 🚀

---

## 🎯 Action Items - Bugün Yapılacaklar

Hiç load test yapmadıysanız, bugün bunları yapın:

1. **Setup** (5 dakika)
   ```bash
   cd ~/Documents/huseyin_server/java_workspace/elly
   make perf-setup
   ```
   
2. **Baseline Test** (5 dakika)
   ```bash
   # Terminal 1
   ./mvnw spring-boot:run
   
   # Terminal 2
   make load-test
   ```

3. **İlk Optimizasyon** (5 dakika)
   ```bash
   make db-perf
   ```

4. **Test Tekrarla** (5 dakika)
   ```bash
   make load-test
   # İyileşmeyi gör!
   ```

**Toplam: 20 dakika** ⏱️  
**Beklenen: %70-80 iyileşme** 🚀

---

## 🆘 Yardıma İhtiyacınız Var mı?

### "Nereden başlayacağımı bilmiyorum"
👉 [QUICK_START_PERFORMANCE.md](QUICK_START_PERFORMANCE.md)

### "Load test nasıl çalıştırılır?"
👉 [LOAD_TEST_GUIDE.md](LOAD_TEST_GUIDE.md)

### "Performans sorunum var"
👉 [PERFORMANCE_ANALYSIS.md](PERFORMANCE_ANALYSIS.md)

### "Kod örneği lazım"
👉 [OPTIMIZATION_EXAMPLES.md](OPTIMIZATION_EXAMPLES.md)

### "Genel bir özet istiyorum"
👉 [PERFORMANCE_SUMMARY.md](PERFORMANCE_SUMMARY.md)

### "Timeline ve roadmap görmek istiyorum"
👉 [PERFORMANCE_ROADMAP.md](PERFORMANCE_ROADMAP.md)

### "Komutları unuttum"
👉 `make perf-help`

---

## 📚 Ek Kaynaklar

### Proje Dökümanları
- [PROJECT.md](PROJECT.md) - Proje yapısı
- [DATABASE_INDEXES.md](DATABASE_INDEXES.md) - Mevcut index'ler
- [RATING_API.md](RATING_API.md) - Rating API
- [EXCEPTION_IMPLEMENTATION_SUMMARY.md](EXCEPTION_IMPLEMENTATION_SUMMARY.md) - Exception handling

### Dış Kaynaklar
- [K6 Documentation](https://k6.io/docs/)
- [Spring Boot Performance Tuning](https://spring.io/blog/2015/11/29/how-not-to-hate-spring-in-2016)
- [PostgreSQL Performance Tips](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)

---

## 🎯 Sonraki Adımlar

1. ✅ [QUICK_START_PERFORMANCE.md](QUICK_START_PERFORMANCE.md) okuyun
2. ✅ `make perf-setup` çalıştırın
3. ✅ İlk load test'i yapın
4. ✅ Database index'lerini yükleyin
5. ✅ Sonuçları karşılaştırın
6. ✅ [PERFORMANCE_ROADMAP.md](PERFORMANCE_ROADMAP.md) ile planlayın

---

## 🏆 Başarı Kriterleri

Optimizasyonlar tamamlandığında:

- [x] Load test çalışıyor ✅
- [x] Response time <500ms (p95) ✅
- [x] Error rate <%1 ✅
- [x] Throughput >100 req/s ✅
- [x] Database index'leri mevcut ✅
- [x] Pagination çalışıyor ✅
- [x] N+1 query çözülmüş ✅
- [x] Monitoring aktif ✅

---

**Hazırsınız! Hadi başlayalım! 🚀**

İlk adım: [QUICK_START_PERFORMANCE.md](QUICK_START_PERFORMANCE.md)

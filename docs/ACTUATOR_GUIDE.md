# 📊 Actuator Metrikleri Kullanım Kılavuzu

Bu kılavuz, Spring Boot Actuator metriklerini nasıl kontrol edeceğinizi ve görüntüleyeceğinizi açıklar.

---

## 🚀 Hızlı Başlangıç

### 1. Actuator Endpoint'lerini Kontrol Etme

#### Temel Kontroller

```bash
# Health check (uygulama çalışıyor mu?)
curl http://localhost:8080/actuator/health

# Tüm mevcut metrikleri listele
curl http://localhost:8080/actuator/metrics

# Tüm mevcut endpoint'leri listele
curl http://localhost:8080/actuator
```

---

## 📈 HTTP Request Metrikleri

### Tüm HTTP İsteklerini Görüntüleme

```bash
# Genel HTTP request metrikleri
curl http://localhost:8080/actuator/metrics/http.server.requests

# JSON formatında güzel görüntüleme
curl -s http://localhost:8080/actuator/metrics/http.server.requests | python3 -m json.tool
```

### Belirli Bir Endpoint İçin Metrikler

```bash
# Pages endpoint'i için (URL encoding ile)
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:%2Fapi%2Fv1%2Fpages%2F%7Bslug%7D"

# Daha kolay yöntem (curl --data-urlencode ile)
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=uri:/api/v1/pages/{slug}"

# Posts endpoint'i için
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=uri:/api/v1/posts"

# Auth endpoint'i için
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=uri:/api/v1/auth/login"
```

### Tüm Mevcut URI'leri Listeleme

```bash
# Hangi endpoint'lere istek gelmiş görmek için
curl -s "http://localhost:8080/actuator/metrics/http.server.requests" | \
  python3 -m json.tool | grep -A 30 '"uri"'
```

### Filtreleme Örnekleri

```bash
# Sadece GET istekleri
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=method:GET"

# Sadece başarılı istekler (200)
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=status:200"

# Hata veren istekler (4xx, 5xx)
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=status:404"

# Birden fazla filtre kombinasyonu
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=uri:/api/v1/pages/{slug}" \
  --data-urlencode "tag=method:GET" \
  --data-urlencode "tag=status:200"
```

---

## 🔍 Metrik Detayları

### Metrik İçeriği Açıklaması

Bir metrik response'u şu bilgileri içerir:

```json
{
  "name": "http.server.requests",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",        // Toplam istek sayısı
      "value": 150.0
    },
    {
      "statistic": "TOTAL_TIME",   // Toplam süre (saniye)
      "value": 12.5
    },
    {
      "statistic": "MAX",          // En uzun süre (saniye)
      "value": 0.5
    }
  ],
  "availableTags": [
    {
      "tag": "uri",
      "values": ["/api/v1/pages/{slug}", "/api/v1/posts", ...]
    },
    {
      "tag": "method",
      "values": ["GET", "POST", "PUT", "DELETE"]
    },
    {
      "tag": "status",
      "values": ["200", "404", "500", ...]
    }
  ]
}
```

### Önemli İstatistikler

- **COUNT**: Toplam istek sayısı
- **TOTAL_TIME**: Tüm isteklerin toplam süresi (saniye)
- **MAX**: En uzun süren istek (saniye)
- **MEAN**: Ortalama süre (TOTAL_TIME / COUNT)

---

## 💾 Database Connection Pool Metrikleri

### HikariCP Metrikleri

```bash
# Aktif connection sayısı
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Toplam connection sayısı
curl http://localhost:8080/actuator/metrics/hikaricp.connections

# Bekleyen connection sayısı (pool tükeniyor mu?)
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending

# Connection timeout sayısı
curl http://localhost:8080/actuator/metrics/hikaricp.connections.timeout

# Pool boyutları
curl http://localhost:8080/actuator/metrics/hikaricp.connections.max
curl http://localhost:8080/actuator/metrics/hikaricp.connections.min
```

---

## 🖥️ JVM ve Sistem Metrikleri

### JVM Memory

```bash
# Kullanılan memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Maximum memory
curl http://localhost:8080/actuator/metrics/jvm.memory.max

# Memory kullanım yüzdesi
curl http://localhost:8080/actuator/metrics/jvm.memory.usage
```

### CPU Kullanımı

```bash
# Sistem CPU kullanımı
curl http://localhost:8080/actuator/metrics/system.cpu.usage

# Process CPU kullanımı
curl http://localhost:8080/actuator/metrics/process.cpu.usage
```

### Thread Bilgileri

```bash
# Aktif thread sayısı
curl http://localhost:8080/actuator/metrics/jvm.threads.live

# Peak thread sayısı
curl http://localhost:8080/actuator/metrics/jvm.threads.peak
```

---

## 📊 Prometheus Formatında Metrikler

Prometheus ve Grafana ile entegrasyon için:

```bash
# Prometheus formatında tüm metrikler
curl http://localhost:8080/actuator/prometheus

# Sadece HTTP request metrikleri
curl http://localhost:8080/actuator/prometheus | grep "http_server_requests"

# Sadece HikariCP metrikleri
curl http://localhost:8080/actuator/prometheus | grep "hikaricp"
```

---

## 🌐 Tarayıcıdan Kullanım

### 1. Temel Endpoint'ler

Tarayıcınızda şu URL'leri açabilirsiniz:

```
# Health check
http://localhost:8080/actuator/health

# Tüm metrikleri listele
http://localhost:8080/actuator/metrics

# HTTP request metrikleri
http://localhost:8080/actuator/metrics/http.server.requests
```

### 2. Filtreleme (URL Encoding Gerekli)

Tarayıcıda özel karakterleri encode etmeniz gerekir:

```
# Pages endpoint için
http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:%2Fapi%2Fv1%2Fpages%2F%7Bslug%7D

# GET method için
http://localhost:8080/actuator/metrics/http.server.requests?tag=method:GET

# Status 200 için
http://localhost:8080/actuator/metrics/http.server.requests?tag=status:200
```

**URL Encoding Tablosu:**
- `/` → `%2F`
- `{` → `%7B`
- `}` → `%7D`
- `:` → `%3A`

---

## 🛠️ Pratik Kullanım Senaryoları

### Senaryo 1: Bir Endpoint'in Performansını Kontrol Etme

```bash
# 1. Önce endpoint'e bir istek atın (metrik oluşsun)
curl http://localhost:8080/api/v1/pages/test-page

# 2. Metrikleri kontrol edin
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=uri:/api/v1/pages/{slug}" | python3 -m json.tool

# 3. Ortalama süreyi hesaplayın
# TOTAL_TIME / COUNT = ortalama süre (saniye)
```

### Senaryo 2: Yavaş İstekleri Bulma

```bash
# MAX değeri yüksek olan endpoint'leri bulun
curl -s "http://localhost:8080/actuator/metrics/http.server.requests" | \
  python3 -m json.tool | grep -A 5 "MAX"
```

### Senaryo 3: Hata Oranını Kontrol Etme

```bash
# 404 hataları
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=status:404"

# 500 hataları
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=status:500"

# Tüm hatalar (4xx ve 5xx)
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=outcome:CLIENT_ERROR"
```

### Senaryo 4: Connection Pool Durumunu İzleme

```bash
# Pool doluyor mu kontrol edin
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.max

# Bekleyen connection var mı?
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending

# Timeout oluyor mu?
curl http://localhost:8080/actuator/metrics/hikaricp.connections.timeout
```

---

## 📝 Örnek Script'ler

### Tüm API Endpoint'lerini Listele

```bash
#!/bin/bash
echo "=== Mevcut API Endpoint'leri ==="
curl -s "http://localhost:8080/actuator/metrics/http.server.requests" | \
  python3 -m json.tool | \
  grep -A 50 '"uri"' | \
  grep -E '"/api/' | \
  sed 's/.*"\(.*\)".*/\1/' | \
  sort -u
```

### Endpoint Performans Özeti

```bash
#!/bin/bash
ENDPOINT="/api/v1/pages/{slug}"

echo "=== $ENDPOINT Performans Metrikleri ==="
curl -s -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=uri:$ENDPOINT" | python3 -m json.tool | \
  grep -E '"statistic"|"value"' | \
  head -6
```

### Connection Pool Durumu

```bash
#!/bin/bash
echo "=== HikariCP Connection Pool Durumu ==="
echo "Aktif: $(curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | python3 -c 'import sys, json; print(int(json.load(sys.stdin)["measurements"][0]["value"]))')"
echo "Max: $(curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.max | python3 -c 'import sys, json; print(int(json.load(sys.stdin)["measurements"][0]["value"]))')"
echo "Bekleyen: $(curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.pending | python3 -c 'import sys, json; print(int(json.load(sys.stdin)["measurements"][0]["value"]))')"
```

---

## 🎯 Sık Kullanılan Komutlar

```bash
# Hızlı health check
curl http://localhost:8080/actuator/health

# Tüm metrikleri listele
curl http://localhost:8080/actuator/metrics

# HTTP request metrikleri (genel)
curl http://localhost:8080/actuator/metrics/http.server.requests

# Belirli endpoint
curl -G "http://localhost:8080/actuator/metrics/http.server.requests" \
  --data-urlencode "tag=uri:/api/v1/pages/{slug}"

# Connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

---

## ⚠️ Önemli Notlar

1. **Metrik Oluşması**: Bir endpoint için metrik görmek için önce o endpoint'e istek atılmalı.

2. **Path Variable Formatı**: Spring Boot path variable'ları `{variableName}` formatında gösterir:
   - `/api/v1/pages/{slug}` ✅
   - `/api/v1/pages/*` ❌

3. **URL Encoding**: Tarayıcıda veya script'lerde özel karakterleri encode edin.

4. **Süre Birimi**: Tüm süreler **saniye** cinsindendir. Milisaniyeye çevirmek için 1000 ile çarpın.

5. **Metrik Güncelliği**: Metrikler gerçek zamanlıdır, her istekten sonra güncellenir.

---

## 🔗 İlgili Dosyalar

- `application.properties` - Actuator konfigürasyonu
- `RequestTimingInterceptor.java` - Request timing interceptor
- `PERFORMANCE_ANALYSIS.md` - Performans analizi dokümantasyonu

---

**Son Güncelleme**: 2024


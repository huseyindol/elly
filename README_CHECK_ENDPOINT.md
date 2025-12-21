# check-endpoint.sh Kullanım Kılavuzu

## Token ile Kullanım

### 1. Token Olmadan (Public Endpoint'ler)

```bash
# Metrik formatında (Spring Boot pattern)
./check-endpoint.sh "/api/v1/pages/{slug}"

# Gerçek path ile (script otomatik olarak pattern'e çevirir)
./check-endpoint.sh "/api/v1/pages/home"
```

### 2. Token ile (Authenticated Endpoint'ler)

```bash
# Token'ı ikinci parametre olarak verin
./check-endpoint.sh "/api/v1/pages/home" "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Metrik formatında da token kullanabilirsiniz
./check-endpoint.sh "/api/v1/pages/{slug}" "your-jwt-token"
```

## Token Nasıl Alınır?

### Login ile Token Alma

```bash
# Login yapın ve token'ı alın
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "your-username",
    "password": "your-password"
  }' | python3 -m json.tool

# Response'dan token'ı kopyalayın
# {
#   "data": {
#     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#     ...
#   }
# }
```

### Token'ı Değişkene Atama

```bash
# Token'ı bir değişkene atayın
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "your-username",
    "password": "your-password"
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['token'])")

# Token ile endpoint kontrolü
./check-endpoint.sh "/api/v1/pages/home" "$TOKEN"
```

## Örnekler

### Pages Endpoint'i

```bash
# Token ile
./check-endpoint.sh "/api/v1/pages/home" "your-token"

# Token olmadan (eğer public ise)
./check-endpoint.sh "/api/v1/pages/home"
```

### Posts Endpoint'i

```bash
./check-endpoint.sh "/api/v1/posts/1" "your-token"
```

### Components Endpoint'i

```bash
./check-endpoint.sh "/api/v1/components/1" "your-token"
```

## Script Nasıl Çalışır?

1. **Gerçek Path Verilirse**: 
   - Önce endpoint'e istek atar (token varsa Authorization header ekler)
   - Metrik oluşturur
   - Spring Boot pattern formatına çevirir (örn: `/api/v1/pages/home` → `/api/v1/pages/{slug}`)
   - Metrikleri gösterir

2. **Metrik Formatı Verilirse**:
   - Direkt metrikleri gösterir
   - Token varsa sadece bilgi amaçlı gösterir

## Çıktı Örneği

```
🔄 Endpoint'e istek atılıyor (metrik oluşturmak için)...
Status: 200

📝 Metrik URI: /api/v1/pages/{slug}
==========================================
📊 Endpoint Metrikleri: /api/v1/pages/home
🔐 Token kullanılıyor
==========================================

{
    "name": "http.server.requests",
    "baseUnit": "seconds",
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 1.0
        },
        ...
    ]
}

==========================================
📈 Özet:
==========================================
Toplam İstek Sayısı: 1
Toplam Süre: 0.002859732s (2ms)
En Uzun Süre: 0.0s (0ms)
Ortalama Süre: .0028s (2ms)
```

## Sorun Giderme

### "bad token" Hatası

- Token'ın doğru olduğundan emin olun
- Token'ın süresi dolmamış olmalı
- Token formatı: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` (Bearer prefix olmadan)

### 401 Unauthorized

- Endpoint authenticated ise token gerekli
- Token'ı ikinci parametre olarak verin

### 404 Not Found

- Endpoint path'inin doğru olduğundan emin olun
- Uygulamanın çalıştığından emin olun


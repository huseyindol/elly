# Fake/Geçersiz Access Token Oluşturucu

Bu script, test amaçlı geçersiz/eskimiş JWT access token'ları oluşturur.

## 📁 Dosya Konumu

```
scripts/generate-fake-token.py
```

## 🚀 Kullanım

### Yöntem 1: Python ile çalıştırma

```bash
python3 scripts/generate-fake-token.py
```

### Yöntem 2: Doğrudan çalıştırma (executable)

```bash
# İlk kez çalıştırırken executable yap (sadece bir kez)
chmod +x scripts/generate-fake-token.py

# Sonra direkt çalıştır
./scripts/generate-fake-token.py
```

## 📋 Çıktı

Script iki farklı formatda geçersiz token üretir:

1. **JWE Formatında Token** (Sisteminizde kullanılan format)
   - Format: `header.encrypted_key.iv.ciphertext.tag`
   - Daha gerçekçi görünür
   - Decrypt edilemez (geçersiz)

2. **Basit JWT Formatında Token**
   - Format: `header.payload.signature`
   - Daha kısa ve basit
   - Validate edilemez (geçersiz)

## 🧪 Test Örnekleri

### Senaryo 1: JWE Token ile Test

```bash
# Token'ı al
TOKEN=$(python3 scripts/generate-fake-token.py | grep -A 1 "1. JWE" | tail -1)

# Test isteği gönder
curl -X 'GET' \
  'http://localhost:8080/api/v1/pages/home' \
  -H 'accept: */*' \
  -H "Authorization: Bearer $TOKEN"
```

### Senaryo 2: Manuel Token Kullanımı

```bash
# Script'i çalıştır ve token'ı kopyala
python3 scripts/generate-fake-token.py

# Çıktıdaki token'ı kullan
curl -X 'GET' \
  'http://localhost:8080/api/v1/pages/home' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiAiZGlyIiwgImVuYyI6ICJBMjU2R0NNIn0...'
```

## ✅ Beklenen Sonuç

Geçersiz token ile istek gönderildiğinde, sistem **401 Unauthorized** döndürmelidir:

```json
{
  "result": false,
  "status": 401,
  "error": "Unauthorized",
  "errorCode": "BAD_CREDENTIALS",
  "message": "Invalid or expired token"
}
```

## 📝 Notlar

- Bu token'lar **sadece test amaçlıdır**
- Gerçek authentication için kullanılamaz
- Token'lar decrypt/validate edilemez (kasıtlı olarak geçersiz)
- Her çalıştırmada farklı token'lar üretilmez (aynı fake data kullanılır)

## 🔧 Özelleştirme

Script'i düzenleyerek:
- Farklı fake data'lar ekleyebilirsiniz
- Token formatını değiştirebilirsiniz
- Çıktı formatını özelleştirebilirsiniz

## 📚 İlgili Dosyalar

- `src/main/java/com/cms/config/JwtAuthenticationFilter.java` - Token validation
- `src/main/java/com/cms/util/JwtUtil.java` - JWT utility
- `src/main/java/com/cms/exception/GlobalExceptionHandler.java` - Error handling

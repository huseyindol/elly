# 🚀 Rating Sistemi Kurulum Rehberi

## ✅ Yapılanlar

Rating sistemi projenize başarıyla entegre edildi! Tüm dosyalar oluşturuldu ve projenizin mevcut mimarisiyle tam uyumlu.

### Oluşturulan Dosyalar:
```
src/main/java/com/cms/
├── entity/Rating.java                    ✅
├── repository/RatingRepository.java      ✅
├── service/
│   ├── IRatingService.java               ✅
│   └── impl/RatingService.java           ✅
├── dto/
│   ├── DtoRating.java                    ✅
│   ├── DtoRatingIU.java                  ✅
│   └── DtoRatingStats.java               ✅
├── mapper/RatingMapper.java              ✅
└── controller/
    ├── IRatingController.java            ✅
    └── impl/RatingController.java        ✅

Dokümantasyon:
├── RATING_API.md                         ✅
├── RATING_SETUP.md                       ✅ (bu dosya)
└── db-migration-ratings.sql              ✅
```

---

## 📋 Kurulum Adımları

### 1️⃣ Maven Build (Yapıldı ✅)

MapStruct mapper implementasyonları başarıyla oluşturuldu:
```bash
./mvnw clean compile
```

**Sonuç:** BUILD SUCCESS ✅

---

### 2️⃣ Veritabanı Tablosunu Oluştur

#### Seçenek A: Docker ile (Önerilen)

Eğer Docker kullanıyorsanız:

```bash
# PostgreSQL container'ına bağlan ve migration'ı çalıştır
docker exec -i elly-postgres psql -U postgres -d postgres < src/main/resources/db-migration-ratings.sql
```

#### Seçenek B: Yerel PostgreSQL

```bash
psql -U postgres -d postgres -f src/main/resources/db-migration-ratings.sql
```

#### Seçenek C: PgAdmin veya DBeaver

`src/main/resources/db-migration-ratings.sql` dosyasının içeriğini kopyalayıp SQL editöründe çalıştırın.

---

### 3️⃣ Uygulamayı Başlat

#### Docker ile:
```bash
docker-compose up -d
```

#### Yerel çalıştırma:
```bash
./mvnw spring-boot:run
```

---

## 🧪 Test Et

### 1. Swagger UI'da Test Et
```
http://localhost:8080/swagger-ui.html
```

`RatingController` bölümünden endpoint'leri test edebilirsiniz.

### 2. cURL ile Test Et

#### Rating Ekle:
```bash
curl -X POST http://localhost:8080/api/v1/ratings \
  -H "Content-Type: application/json" \
  -d '{
    "postId": 1,
    "rating": 5,
    "comment": "Harika bir yazı!"
  }'
```

**Beklenen Response:**
```json
{
  "result": true,
  "message": null,
  "data": {
    "id": 1,
    "post": { ... },
    "rating": 5,
    "comment": "Harika bir yazı!",
    "createdAt": "2025-12-09T...",
    "updatedAt": "2025-12-09T..."
  }
}
```

#### İstatistikleri Görüntüle:
```bash
curl http://localhost:8080/api/v1/ratings/stats/1
```

**Beklenen Response:**
```json
{
  "result": true,
  "message": null,
  "data": {
    "postId": 1,
    "averageRating": 5.0,
    "totalRatings": 1
  }
}
```

---

## 📊 API Endpoints

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| POST | `/api/v1/ratings` | Rating ekle/güncelle |
| GET | `/api/v1/ratings/{id}` | Rating getir |
| GET | `/api/v1/ratings/post/{postId}` | Post'un tüm rating'leri |
| GET | `/api/v1/ratings/stats/{postId}` | Post istatistikleri |

Detaylı kullanım için: **[RATING_API.md](RATING_API.md)**

---

## 🎯 Özellikler

### ✅ Implementasyonlar:
- ✅ 1-5 arası rating sistemi
- ✅ IP bazlı tekrar oy kontrolü (aynı IP sadece 1 kez oy verir)
- ✅ Tekrar oy verilirse güncelleme
- ✅ Opsiyonel yorum ekleme
- ✅ Ortalama rating hesaplama
- ✅ Toplam oy sayısı
- ✅ Validasyon (Jakarta Validation)
- ✅ Veritabanı constraint'leri
- ✅ Cascade delete (Post silinirse rating'ler de silinir)
- ✅ Performans için index'ler

---

## 🔍 Veritabanı Kontrolü

Migration'dan sonra tablo oluşturulmuş mu kontrol edin:

```sql
-- Tabloyu kontrol et
SELECT * FROM ratings;

-- İndeksleri kontrol et
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'ratings';

-- Constraint'leri kontrol et
SELECT conname, contype 
FROM pg_constraint 
WHERE conrelid = 'ratings'::regclass;
```

---

## 📖 Mimari

```
┌──────────────────────────────────────────────┐
│  RatingController                             │
│  - POST /ratings                              │
│  - GET /ratings/{id}                          │
│  - GET /ratings/post/{postId}                 │
│  - GET /ratings/stats/{postId}                │
└───────────────┬──────────────────────────────┘
                │
                ↓
┌──────────────────────────────────────────────┐
│  RatingService                                │
│  - saveRating() → Duplicate kontrolü          │
│  - getRatingById()                            │
│  - getRatingsByPostId()                       │
│  - getAverageRating()                         │
│  - getRatingCount()                           │
└───────────────┬──────────────────────────────┘
                │
                ↓
┌──────────────────────────────────────────────┐
│  RatingRepository (JPA)                       │
│  - findByPostId()                             │
│  - findByPostIdAndUserIdentifier()            │
│  - Custom queries (AVG, COUNT)                │
└───────────────┬──────────────────────────────┘
                │
                ↓
┌──────────────────────────────────────────────┐
│  PostgreSQL Database                          │
│  ratings table                                │
└──────────────────────────────────────────────┘
```

**DTO Mapping:** `RatingMapper` (MapStruct)

---

## 🐛 Sorun Giderme

### "Table ratings does not exist"
**Çözüm:** Migration script'ini çalıştırın (Adım 2)

### "Post not found"
**Çözüm:** Önce bir post oluşturun veya mevcut post ID'sini kullanın

### MapStruct implementation not found
**Çözüm:** Maven compile yapın:
```bash
./mvnw clean compile
```

### Docker container çalışmıyor
**Çözüm:** Container'ları yeniden başlatın:
```bash
docker-compose down
docker-compose up -d
```

---

## 📦 Veritabanı Yapısı

```sql
ratings (
    id              BIGSERIAL PRIMARY KEY
    post_id         BIGINT NOT NULL → posts(id)
    rating          INTEGER NOT NULL (1-5)
    user_identifier VARCHAR(255) NOT NULL
    comment         VARCHAR(500)
    created_at      TIMESTAMP
    updated_at      TIMESTAMP
    
    UNIQUE(user_identifier, post_id)
)
```

---

## 🎉 Tamamlandı!

Rating sistemi kullanıma hazır! 

**Sırada ne yapılabilir?**
- Frontend entegrasyonu (React/Vue/Angular)
- User authentication ile entegrasyon (IP yerine user ID)
- Rating dağılımı görselleştirme
- Email bildirimleri
- Moderasyon paneli

**Daha fazla bilgi için:**
- [RATING_API.md](RATING_API.md) - API Dokümantasyonu
- [Swagger UI](http://localhost:8080/swagger-ui.html) - İnteraktif API Test

---

## 📞 Destek

Herhangi bir sorun yaşarsanız:
1. Log'ları kontrol edin: `docker-compose logs app`
2. Veritabanı bağlantısını kontrol edin
3. Migration script'inin çalıştığını doğrulayın

**Başarılar!** 🚀


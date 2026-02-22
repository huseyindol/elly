# 🌟 Rating API Dokümantasyonu

## Genel Bakış
Rating sistemi, kullanıcıların post'lara 1-5 arası puan vermesine ve opsiyonel yorum eklemesine olanak sağlar.

## API Endpoints

### Base URL
```
/api/v1/ratings
```

---

## 1. Rating Ekle/Güncelle

**POST** `/api/v1/ratings`

Bir post'a rating ekler. Aynı kullanıcı (IP adresi ile tanımlanır) daha önce rating vermişse günceller.

### Request Body
```json
{
  "postId": 1,
  "rating": 5,
  "comment": "Harika bir yazı!"
}
```

### Validasyon Kuralları
- `postId`: Zorunlu (null olamaz)
- `rating`: Zorunlu, 1-5 arası olmalı
- `comment`: Opsiyonel (max 500 karakter)

### Response (Success)
```json
{
  "result": true,
  "message": null,
  "data": {
    "id": 1,
    "post": {
      "id": 1,
      "title": "Post Başlığı",
      "content": "Post içeriği...",
      "slug": "post-basligi",
      "status": true,
      "orderIndex": 1,
      "seoInfo": null
    },
    "rating": 5,
    "comment": "Harika bir yazı!",
    "createdAt": "2025-12-09T22:30:00",
    "updatedAt": "2025-12-09T22:30:00"
  }
}
```

### Response (Error)
```json
{
  "result": false,
  "message": "Post ID is required",
  "data": null
}
```

---

## 2. Rating ID ile Getir

**GET** `/api/v1/ratings/{id}`

Belirli bir rating'i ID'sine göre getirir.

### Path Parameters
- `id`: Rating ID (Long)

### Response
```json
{
  "result": true,
  "message": null,
  "data": {
    "id": 1,
    "post": { ... },
    "rating": 5,
    "comment": "Harika bir yazı!",
    "createdAt": "2025-12-09T22:30:00",
    "updatedAt": "2025-12-09T22:30:00"
  }
}
```

---

## 3. Post'a Ait Tüm Rating'leri Getir

**GET** `/api/v1/ratings/post/{postId}`

Belirli bir post'a ait tüm rating'leri listeler.

### Path Parameters
- `postId`: Post ID (Long)

### Response
```json
{
  "result": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "post": { ... },
      "rating": 5,
      "comment": "Harika!",
      "createdAt": "2025-12-09T22:30:00",
      "updatedAt": "2025-12-09T22:30:00"
    },
    {
      "id": 2,
      "post": { ... },
      "rating": 4,
      "comment": "Güzel yazı",
      "createdAt": "2025-12-09T22:35:00",
      "updatedAt": "2025-12-09T22:35:00"
    }
  ]
}
```

---

## 4. Post Rating İstatistikleri

**GET** `/api/v1/ratings/stats/{postId}`

Belirli bir post için ortalama rating ve toplam oy sayısını getirir.

### Path Parameters
- `postId`: Post ID (Long)

### Response
```json
{
  "result": true,
  "message": null,
  "data": {
    "postId": 1,
    "averageRating": 4.5,
    "totalRatings": 10
  }
}
```

---

## Özellikler

### ✅ Tekrar Oy Verme Kontrolü
- Aynı IP adresinden aynı post'a sadece **1 kez** oy verilebilir
- Tekrar oy verilirse, mevcut rating **güncellenir** (yeni kayıt oluşturulmaz)

### ✅ Veritabanı Constraints
- `rating` değeri 1-5 arası olmalı (CHECK constraint)
- `(user_identifier, post_id)` UNIQUE constraint
- Post silinirse, o post'a ait rating'ler de silinir (CASCADE)

### ✅ IP Bazlı Tanımlama
- User identifier olarak `HttpServletRequest.getRemoteAddr()` kullanılır
- Gelecekte user authentication eklenirse, user ID kullanılabilir

---

## Veritabanı Şeması

```sql
CREATE TABLE ratings (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    user_identifier VARCHAR(255) NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rating_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT uc_rating_user_post UNIQUE (user_identifier, post_id)
);
```

---

## Örnek Kullanım Senaryoları

### Scenario 1: İlk Kez Rating Ver
```bash
curl -X POST http://localhost:8080/api/v1/ratings \
  -H "Content-Type: application/json" \
  -d '{
    "postId": 1,
    "rating": 5,
    "comment": "Mükemmel!"
  }'
```

### Scenario 2: Mevcut Rating'i Güncelle
```bash
# Aynı IP'den tekrar istek atılırsa, rating güncellenir
curl -X POST http://localhost:8080/api/v1/ratings \
  -H "Content-Type: application/json" \
  -d '{
    "postId": 1,
    "rating": 4,
    "comment": "Güzel ama geliştirilebilir"
  }'
```

### Scenario 3: Post İstatistiklerini Görüntüle
```bash
curl http://localhost:8080/api/v1/ratings/stats/1
```

### Scenario 4: Tüm Rating'leri Listele
```bash
curl http://localhost:8080/api/v1/ratings/post/1
```

---

## Frontend Entegrasyonu Örneği

### React/JavaScript
```javascript
// Rating gönder
async function submitRating(postId, rating, comment) {
  const response = await fetch('/api/v1/ratings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ postId, rating, comment })
  });
  
  const result = await response.json();
  
  if (result.result) {
    console.log('Rating başarıyla kaydedildi:', result.data);
  } else {
    console.error('Hata:', result.message);
  }
}

// İstatistikleri getir
async function getRatingStats(postId) {
  const response = await fetch(`/api/v1/ratings/stats/${postId}`);
  const result = await response.json();
  
  if (result.result) {
    const { averageRating, totalRatings } = result.data;
    console.log(`Ortalama: ${averageRating} ⭐ (${totalRatings} oy)`);
  }
}
```

---

## Swagger/OpenAPI

Uygulamanız çalışırken şu adresten API dokümantasyonunu görüntüleyebilirsiniz:
```
http://localhost:8080/swagger-ui.html
```

---

## Veritabanı Migration

Uygulamayı çalıştırmadan önce veritabanında ratings tablosunu oluşturun:

```bash
psql -d your_database -f src/main/resources/db-migration-ratings.sql
```

veya Docker kullanıyorsanız:

```bash
docker exec -i postgres_container psql -U your_user -d your_database < src/main/resources/db-migration-ratings.sql
```

---

## Gelecek Geliştirmeler

- [ ] User authentication entegrasyonu (IP yerine user ID)
- [ ] Rating dağılımı (kaç kişi 5 yıldız verdi vb.)
- [ ] Spam koruması ve rate limiting
- [ ] Rating'i geri alma (DELETE endpoint)
- [ ] Moderasyon sistemi (yorum onaylama)
- [ ] Email bildirimleri (yeni rating geldiğinde)

---

## Sorun Giderme

### "Post not found" hatası
- Post ID'nin geçerli olduğundan emin olun
- Post'un silinmemiş olduğunu kontrol edin

### "Rating must be at least 1" hatası
- Rating değerinin 1-5 arası olduğundan emin olun

### Duplicate key violation
- Aynı IP'den aynı post'a tekrar oy verilmeye çalışılıyor olabilir
- Bu durumda backend otomatik güncelleme yapmalı

---

## Destek

Herhangi bir sorun veya soru için lütfen issue açın veya iletişime geçin.


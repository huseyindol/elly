# Elly CMS API Endpoints

Base URL: `http://localhost:8080`

---

## Authentication

### POST /api/v1/auth/register
Yeni kullanıcı kaydı.

**Body:**
```json
{
  "username": "string",      // zorunlu, min 3, max 50 karakter
  "email": "string",         // zorunlu, geçerli email
  "password": "string",      // zorunlu, min 6 karakter
  "firstName": "string",     // opsiyonel
  "lastName": "string"       // opsiyonel
}
```

---

### POST /api/v1/auth/login
Kullanıcı girişi.

**Body:**
```json
{
  "usernameOrEmail": "string",  // zorunlu
  "password": "string"          // zorunlu
}
```

**Response:**
```json
{
  "result": true,
  "data": {
    "token": "string",
    "refreshToken": "string",
    "type": "Bearer",
    "userId": 1,
    "username": "string",
    "email": "string",
    "userCode": "string",
    "expiredDate": 1735516528560
  }
}
```

---

### POST /api/v1/auth/refresh
Token yenileme.

**Body:**
```json
{
  "refreshToken": "string"  // zorunlu
}
```

---

## Pages

### GET /api/v1/pages/{slug}
Sayfa getir (slug ile).

**Path Parameters:**
- `slug`: Sayfa slug'ı

**Response:**
```json
{
  "result": true,
  "data": {
    "id": 1,
    "title": "string",
    "description": "string",
    "slug": "string",
    "status": true,
    "seoInfo": { ... },
    "components": [ ... ]
  }
}
```

---

### GET /api/v1/pages/list
Tüm sayfaları listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "title": "string",
      "description": "string",
      "slug": "string",
      "status": true,
      "seoInfo": { ... },
      "components": [ ... ]
    }
  ]
}
```

---

### GET /api/v1/pages/list/summary
Sayfa özetlerini listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "title": "string",
      "slug": "string",
      "status": true
    }
  ]
}
```

---

### GET /api/v1/pages/list/paged
Sayfalı sayfa listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "string",
        "description": "string",
        "slug": "string",
        "status": true,
        "seoInfo": { ... },
        "components": [ ... ]
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/pages/list/summary/paged
Sayfalı sayfa özet listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "string",
        "slug": "string",
        "status": true
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### POST /api/v1/pages
Sayfa oluştur.

**Body:**
```json
{
  "title": "string",
  "description": "string",
  "slug": "string",
  "status": true,
  "componentIds": [1, 2],    // opsiyonel
  "seoInfo": {
    "title": "string",
    "description": "string",
    "keywords": "string",
    "canonicalUrl": "string",
    "noIndex": false,
    "noFollow": false
  }
}
```

---

### PUT /api/v1/pages/{id}
Sayfa güncelle.

**Path Parameters:**
- `id`: Sayfa ID

**Body:** (POST ile aynı)

---

### DELETE /api/v1/pages/{id}
Sayfa sil.

**Path Parameters:**
- `id`: Sayfa ID

---

## Posts

### GET /api/v1/posts/{id}
Post getir.

**Path Parameters:**
- `id`: Post ID

---

### GET /api/v1/posts/list
Tüm postları listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "title": "string",
      "content": "string",
      "slug": "string",
      "status": true,
      "orderIndex": 1,
      "seoInfo": { ... }
    }
  ]
}
```

---

### GET /api/v1/posts/list/summary
Post özetlerini listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "title": "string",
      "slug": "string",
      "status": true,
      "orderIndex": 1
    }
  ]
}
```

---

### GET /api/v1/posts/list/paged
Sayfalı post listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "string",
        "content": "string",
        "slug": "string",
        "status": true,
        "orderIndex": 1,
        "seoInfo": { ... }
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/posts/list/summary/paged
Sayfalı post özet listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "string",
        "slug": "string",
        "status": true,
        "orderIndex": 1
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### POST /api/v1/posts
Post oluştur.

**Body:**
```json
{
  "title": "string",
  "content": "string",
  "slug": "string",
  "status": true,
  "orderIndex": 1,
  "seoInfo": {
    "title": "string",
    "description": "string",
    "keywords": "string",
    "canonicalUrl": "string",
    "noIndex": false,
    "noFollow": false
  }
}
```

---

### PUT /api/v1/posts/{id}
Post güncelle.

**Path Parameters:**
- `id`: Post ID

**Body:** (POST ile aynı)

---

### DELETE /api/v1/posts/{id}
Post sil.

**Path Parameters:**
- `id`: Post ID

---

## Comments

### GET /api/v1/comments/{id}
Yorum getir.

**Path Parameters:**
- `id`: Yorum ID

---

### GET /api/v1/comments/post/{postId}
Post'a ait yorumları getir.

**Path Parameters:**
- `postId`: Post ID

---

### POST /api/v1/comments
Yorum ekle.

**Body:**
```json
{
  "name": "string",
  "email": "string",           // zorunlu, geçerli email
  "content": "string",
  "postId": 1,                 // zorunlu
  "status": true,
  "parentCommentId": null      // alt yorum için parent ID
}
```

---

### DELETE /api/v1/comments/{id}
Yorum sil.

**Path Parameters:**
- `id`: Yorum ID

---

## Ratings

### GET /api/v1/ratings/{id}
Rating getir.

**Path Parameters:**
- `id`: Rating ID

---

### GET /api/v1/ratings/post/{postId}
Post'a ait ratingleri getir.

**Path Parameters:**
- `postId`: Post ID

---

### GET /api/v1/ratings/stats/{postId}
Post rating istatistikleri.

**Path Parameters:**
- `postId`: Post ID

**Response:**
```json
{
  "postId": 1,
  "averageRating": 4.5,
  "totalRatings": 10
}
```

---

### POST /api/v1/ratings
Rating ekle.

**Body:**
```json
{
  "postId": 1,        // zorunlu
  "rating": 5,        // zorunlu, 1-5 arası
  "comment": "string" // opsiyonel
}
```

---

## Banners

### GET /api/v1/banners/{id}
Banner getir.

**Path Parameters:**
- `id`: Banner ID

**Response:**
```json
{
  "result": true,
  "data": {
    "id": 1,
    "title": "string",
    "altText": "string",
    "images": {
      "desktop": "string",
      "tablet": "string",
      "mobile": "string"
    },
    "link": "string",
    "target": "_blank",
    "type": "string",
    "orderIndex": 1,
    "status": true,
    "subFolder": "string"
  }
}
```

---

### GET /api/v1/banners/list
Tüm bannerları listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "title": "string",
      "altText": "string",
      "images": {
        "desktop": "string",
        "tablet": "string",
        "mobile": "string"
      },
      "link": "string",
      "target": "_blank",
      "type": "string",
      "orderIndex": 1,
      "status": true,
      "subFolder": "string"
    }
  ]
}
```

---

### GET /api/v1/banners/list/{subFolder}
Belirli bir subFolder'daki bannerları listele.

**Path Parameters:**
- `subFolder`: Alt klasör adı

**Response:** (list ile aynı format)

---

### GET /api/v1/banners/list/summary
Banner özetlerini listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "title": "string",
      "status": true,
      "orderIndex": 1
    }
  ]
}
```

---

### GET /api/v1/banners/list/summary/{subFolder}
Belirli bir subFolder'daki banner özetlerini listele.

**Path Parameters:**
- `subFolder`: Alt klasör adı

**Response:** (summary ile aynı format)

---

### GET /api/v1/banners/sub-folders
Tüm mevcut subFolder listesini döndürür.

**Response:**
```json
{
  "result": true,
  "data": ["promo", "hero", "sidebar"]
}
```

---

### GET /api/v1/banners/list/paged
Sayfalı banner listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "string",
        "altText": "string",
        "images": { ... },
        "link": "string",
        "target": "_blank",
        "type": "string",
        "orderIndex": 1,
        "status": true,
        "subFolder": "string"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/banners/list/summary/paged
Sayfalı banner özet listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "string",
        "status": true,
        "orderIndex": 1
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### POST /api/v1/banners
Banner oluştur.

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `data`: JSON string (DtoBannerIU formatında, zorunlu)
- `desktop`: file (opsiyonel, desktop görsel)
- `tablet`: file (opsiyonel, tablet görsel)
- `mobile`: file (opsiyonel, mobil görsel)

**data JSON formatı:**
```json
{
  "title": "string",
  "altText": "string",
  "link": "string",
  "target": "_blank | _self",
  "type": "string",
  "orderIndex": 1,
  "status": true,
  "subFolder": "string",
  "images": {
    "desktop": "https://...",
    "tablet": "https://...",
    "mobile": "https://..."
  }
}
```

> **Not:** Dosya yüklemek istiyorsanız `desktop`, `tablet`, `mobile` form field'larını kullanın.
> Doğrudan URL göndermek istiyorsanız `data` JSON içindeki `images` objesini kullanın.

---

### PUT /api/v1/banners/{id}
Banner güncelle.

**Path Parameters:**
- `id`: Banner ID

**Content-Type:** `multipart/form-data`

**Form Fields:** (POST ile aynı)

---

### DELETE /api/v1/banners/{id}
Banner sil.

**Path Parameters:**
- `id`: Banner ID

---

## Components

### GET /api/v1/components/{id}
Component getir.

**Path Parameters:**
- `id`: Component ID

---

### GET /api/v1/components/list
Tüm componentleri listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "name": "string",
      "description": "string",
      "type": "BANNER | WIDGET | FORM",
      "content": "string",
      "orderIndex": 1,
      "status": true,
      "pages": [ ... ],
      "banners": [ ... ],
      "widgets": [ ... ],
      "forms": [ ... ]
    }
  ]
}
```

---

### GET /api/v1/components/list/summary
Component özetlerini listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "name": "string",
      "type": "BANNER | WIDGET | FORM",
      "status": true,
      "orderIndex": 1
    }
  ]
}
```

---

### GET /api/v1/components/list/paged
Sayfalı component listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "string",
        "description": "string",
        "type": "BANNER | WIDGET | FORM",
        "content": "string",
        "orderIndex": 1,
        "status": true,
        "pages": [ ... ],
        "banners": [ ... ],
        "widgets": [ ... ],
        "forms": [ ... ]
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/components/list/summary/paged
Sayfalı component özet listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "string",
        "type": "BANNER | WIDGET | FORM",
        "status": true,
        "orderIndex": 1
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### POST /api/v1/components
Component oluştur.

**Body:**
```json
{
  "name": "string",
  "description": "string",
  "type": "BANNER | WIDGET | FORM",
  "content": "string",
  "orderIndex": 1,
  "status": true,
  "pageIds": [1, 2],      // opsiyonel
  "bannerIds": [1],       // opsiyonel, sadece type=BANNER
  "widgetIds": [1],       // opsiyonel, sadece type=WIDGET
  "formIds": [1]          // opsiyonel, sadece type=FORM
}
```

> **Not:** `type` değerine göre sadece ilgili ID listesi gönderilebilir.
> BANNER → `bannerIds`, WIDGET → `widgetIds`, FORM → `formIds`.
> Yanlış tip kombinasyonu validation hatası döndürür.

---

### PUT /api/v1/components/{id}
Component güncelle.

**Path Parameters:**
- `id`: Component ID

**Body:** (POST ile aynı)

---

### DELETE /api/v1/components/{id}
Component sil.

**Path Parameters:**
- `id`: Component ID

---

## Widgets

### GET /api/v1/widgets/{id}
Widget getir.

**Path Parameters:**
- `id`: Widget ID

---

### GET /api/v1/widgets/list
Tüm widgetları listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "name": "string",
      "description": "string",
      "type": "BANNER | POST",
      "content": "string",
      "orderIndex": 1,
      "status": true,
      "banners": [ ... ],
      "posts": [ ... ]
    }
  ]
}
```

---

### GET /api/v1/widgets/list/summary
Widget özetlerini listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "name": "string",
      "type": "BANNER | POST",
      "status": true,
      "orderIndex": 1
    }
  ]
}
```

---

### GET /api/v1/widgets/list/paged
Sayfalı widget listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "string",
        "description": "string",
        "type": "BANNER | POST",
        "content": "string",
        "orderIndex": 1,
        "status": true,
        "banners": [ ... ],
        "posts": [ ... ]
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/widgets/list/summary/paged
Sayfalı widget özet listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "string",
        "type": "BANNER | POST",
        "status": true,
        "orderIndex": 1
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### POST /api/v1/widgets
Widget oluştur.

**Body:**
```json
{
  "name": "string",
  "description": "string",
  "type": "BANNER | POST",
  "content": "string",
  "orderIndex": 1,
  "status": true,
  "bannerIds": [1, 2],    // opsiyonel
  "postIds": [1, 2]       // opsiyonel
}
```

---

### PUT /api/v1/widgets/{id}
Widget güncelle.

**Path Parameters:**
- `id`: Widget ID

**Body:** (POST ile aynı)

---

### DELETE /api/v1/widgets/{id}
Widget sil.

**Path Parameters:**
- `id`: Widget ID

---

## Assets

### GET /api/v1/assets/id/{id}
Asset getir (ID ile).

**Path Parameters:**
- `id`: Asset ID

**Response:**
```json
{
  "result": true,
  "data": {
    "id": 1,
    "path": "/uploads/images/dosya.jpg",
    "type": "image/jpeg",
    "name": "dosya.jpg",
    "extension": "jpg",
    "subFolder": "images"
  }
}
```

---

### GET /api/v1/assets/{name}
İsme göre asset ara (partial match, case-insensitive).

**Path Parameters:**
- `name`: Aranacak dosya adı (kısmi eşleşme destekler)

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "path": "/uploads/images/dosya.jpg",
      "type": "image/jpeg",
      "name": "dosya.jpg",
      "extension": "jpg",
      "subFolder": "images"
    }
  ]
}
```

---

### GET /api/v1/assets/{name}/paged
İsme göre sayfalı asset ara (partial match, case-insensitive).

**Path Parameters:**
- `name`: Aranacak dosya adı (kısmi eşleşme destekler)

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "path": "/uploads/images/dosya.jpg",
        "type": "image/jpeg",
        "name": "dosya.jpg",
        "extension": "jpg",
        "subFolder": "images"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/assets/{subFolder}/{name}/paged
SubFolder ve isme göre sayfalı asset ara (partial match, case-insensitive).

**Path Parameters:**
- `subFolder`: Alt klasör adı
- `name`: Aranacak dosya adı (kısmi eşleşme destekler)

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "path": "/uploads/images/dosya.jpg",
        "type": "image/jpeg",
        "name": "dosya.jpg",
        "extension": "jpg",
        "subFolder": "images"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/assets/list
Tüm assetleri listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "path": "/uploads/images/dosya.jpg",
      "type": "image/jpeg",
      "name": "dosya.jpg",
      "extension": "jpg",
      "subFolder": "images"
    }
  ]
}
```

---

### GET /api/v1/assets/list/paged
Sayfalı asset listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "path": "/uploads/images/dosya.jpg",
        "type": "image/jpeg",
        "name": "dosya.jpg",
        "extension": "jpg",
        "subFolder": "images"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/assets/list/{subFolder}
Belirli bir subFolder'daki assetleri listele.

**Path Parameters:**
- `subFolder`: Alt klasör adı

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "path": "/uploads/images/dosya.jpg",
      "type": "image/jpeg",
      "name": "dosya.jpg",
      "extension": "jpg",
      "subFolder": "images"
    }
  ]
}
```

---

### GET /api/v1/assets/list/{subFolder}/paged
Belirli bir subFolder'daki assetleri sayfalı olarak listele.

**Path Parameters:**
- `subFolder`: Alt klasör adı

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "path": "/uploads/images/dosya.jpg",
        "type": "image/jpeg",
        "name": "dosya.jpg",
        "extension": "jpg",
        "subFolder": "images"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/assets/sub-folders
Tüm mevcut subFolder listesini döndürür.

**Response:**
```json
{
  "result": true,
  "data": ["images", "documents", "videos"]
}
```

---

### POST /api/v1/assets
Asset yükle.

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `subFolder`: string (opsiyonel, alt klasör)
- `file`: file (zorunlu)

**Response:**
```json
{
  "result": true,
  "data": {
    "id": 1,
    "path": "/uploads/images/dosya.jpg",
    "type": "image/jpeg",
    "name": "dosya.jpg",
    "extension": "jpg",
    "subFolder": "images"
  }
}
```

---

### POST /api/v1/assets/multi
Birden fazla asset yükle.

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `subFolder`: string (opsiyonel, alt klasör)
- `files`: file[] (zorunlu, birden fazla dosya)

**Örnek curl komutu:**
```bash
curl -X POST "http://localhost:8080/api/v1/assets/multi" \
  -H "Content-Type: multipart/form-data" \
  -F "subFolder=images" \
  -F "files=@dosya1.jpg" \
  -F "files=@dosya2.png" \
  -F "files=@dosya3.pdf"
```

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "path": "/uploads/images/dosya1.jpg",
      "type": "image/jpeg",
      "name": "dosya1.jpg",
      "extension": "jpg",
      "subFolder": "images"
    },
    {
      "id": 2,
      "path": "/uploads/images/dosya2.png",
      "type": "image/png",
      "name": "dosya2.png",
      "extension": "png",
      "subFolder": "images"
    }
  ]
}
```

---

### PUT /api/v1/assets/{id}
Asset güncelle.

**Path Parameters:**
- `id`: Asset ID

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file`: file (zorunlu)

---

### DELETE /api/v1/assets/{id}
Asset sil.

**Path Parameters:**
- `id`: Asset ID

---

## Forms

### POST /api/v1/forms
Form tanımı oluştur.

**Body:**
```json
{
  "title": "string",
  "version": 1,
  "active": true,
  "schema": {
    "config": {
      "layout": "vertical",
      "submitLabel": "Gönder"
    },
    "fields": [
      {
        "id": "name",
        "type": "text",
        "label": "Ad Soyad",
        "required": true,
        "validation": {
          "min": 2,
          "max": 100,
          "pattern": null
        },
        "condition": null
      },
      {
        "id": "email",
        "type": "text",
        "label": "E-posta",
        "required": true,
        "validation": {
          "min": null,
          "max": null,
          "pattern": "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"
        },
        "condition": null
      },
      {
        "id": "age",
        "type": "number",
        "label": "Yaş",
        "required": false,
        "validation": {
          "min": 18,
          "max": 120,
          "pattern": null
        },
        "condition": null
      },
      {
        "id": "company",
        "type": "text",
        "label": "Şirket",
        "required": false,
        "validation": null,
        "condition": {
          "field": "age",
          "operator": "GT",
          "value": 18
        }
      }
    ]
  }
}
```

> **Schema Yapısı:**
> - `config`: Form genel ayarları (layout, styling vb.) — serbest key-value
> - `fields`: Alan tanımlarının listesi
>   - `id`: Benzersiz alan tanımlayıcı
>   - `type`: Alan tipi (`text`, `number`, `select` vb.)
>   - `label`: Görüntüleme etiketi
>   - `required`: Zorunlu alan mı
>   - `validation`: Validasyon kuralları (`min`, `max`, `pattern`)
>   - `condition`: Koşullu görünürlük kuralı (`field`, `operator`, `value`)
>     - `operator` değerleri: `EQUALS`, `NOT_EQUALS`, `GT`, `LT`

**Response:**
```json
{
  "result": true,
  "data": {
    "id": 1,
    "title": "string",
    "version": 1,
    "schema": { ... },
    "active": true,
    "createdAt": "2026-02-14T00:00:00.000+00:00",
    "updatedAt": "2026-02-14T00:00:00.000+00:00"
  }
}
```

---

### PUT /api/v1/forms/{id}
Form tanımı güncelle.

**Path Parameters:**
- `id`: Form Definition ID

**Body:** (POST ile aynı)

---

### GET /api/v1/forms/{id}
Form tanımı getir (ID ile).

**Path Parameters:**
- `id`: Form Definition ID

**Response:**
```json
{
  "result": true,
  "data": {
    "id": 1,
    "title": "string",
    "version": 1,
    "schema": {
      "config": { ... },
      "fields": [ ... ]
    },
    "active": true,
    "createdAt": "2026-02-14T00:00:00.000+00:00",
    "updatedAt": "2026-02-14T00:00:00.000+00:00"
  }
}
```

---

### GET /api/v1/forms/list
Tüm form tanımlarını listele.

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "title": "string",
      "version": 1,
      "schema": { ... },
      "active": true,
      "createdAt": "2026-02-14T00:00:00.000+00:00",
      "updatedAt": "2026-02-14T00:00:00.000+00:00"
    }
  ]
}
```

---

### GET /api/v1/forms/list/active
Sadece aktif form tanımlarını listele.

**Response:** (list ile aynı format, sadece `active: true` olanlar)

---

### GET /api/v1/forms/list/paged
Sayfalı form tanımı listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "id,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "string",
        "version": 1,
        "schema": { ... },
        "active": true,
        "createdAt": "2026-02-14T00:00:00.000+00:00",
        "updatedAt": "2026-02-14T00:00:00.000+00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### DELETE /api/v1/forms/{id}
Form tanımı sil.

**Path Parameters:**
- `id`: Form Definition ID

---

### POST /api/v1/forms/{formId}/submit
Forma yanıt gönder (submission).

**Path Parameters:**
- `formId`: Form Definition ID

**Body:**
```json
{
  "payload": {
    "name": "Ahmet Yılmaz",
    "email": "ahmet@example.com",
    "age": 25,
    "company": "Acme Corp"
  }
}
```

> **Not:** `payload` içindeki key'ler form schema'daki field `id`'leri ile eşleşmelidir.
> Sunucu tarafında validasyon ve sanitizasyon uygulanır.

**Response:**
```json
{
  "result": true,
  "data": {
    "id": 1,
    "formDefinitionId": 1,
    "formTitle": "string",
    "payload": {
      "name": "Ahmet Yılmaz",
      "email": "ahmet@example.com",
      "age": 25,
      "company": "Acme Corp"
    },
    "submittedAt": "2026-02-14T00:00:00",
    "createdAt": "2026-02-14T00:00:00.000+00:00"
  }
}
```

---

### GET /api/v1/forms/{formId}/submissions
Belirli bir forma ait tüm yanıtları listele.

**Path Parameters:**
- `formId`: Form Definition ID

**Response:**
```json
{
  "result": true,
  "data": [
    {
      "id": 1,
      "formDefinitionId": 1,
      "formTitle": "string",
      "payload": { ... },
      "submittedAt": "2026-02-14T00:00:00",
      "createdAt": "2026-02-14T00:00:00.000+00:00"
    }
  ]
}
```

---

### GET /api/v1/forms/{formId}/submissions/paged
Belirli bir forma ait yanıtları sayfalı listele.

**Path Parameters:**
- `formId`: Form Definition ID

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "submittedAt,desc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": 1,
        "formDefinitionId": 1,
        "formTitle": "string",
        "payload": { ... },
        "submittedAt": "2026-02-14T00:00:00",
        "createdAt": "2026-02-14T00:00:00.000+00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### GET /api/v1/forms/submissions/{submissionId}
Tekil form yanıtı getir (submission ID ile).

**Path Parameters:**
- `submissionId`: Submission ID

**Response:**
```json
{
  "result": true,
  "data": {
    "id": 1,
    "formDefinitionId": 1,
    "formTitle": "string",
    "payload": { ... },
    "submittedAt": "2026-02-14T00:00:00",
    "createdAt": "2026-02-14T00:00:00.000+00:00"
  }
}
```

---

### GET /api/v1/forms/{formId}/submissions/count
Belirli bir forma ait toplam yanıt sayısını getir.

**Path Parameters:**
- `formId`: Form Definition ID

**Response:**
```json
{
  "result": true,
  "data": 42
}
```

---

## CMS Contents

Esnek, sektörden bağımsız içerik yapıları için JSONB tabanlı Headless CMS endpointleri.
`metadata` alanı `Map<String, Object>` yapısında olup, her içerik tipi için farklı veri yapıları saklayabilir.

> **Not:** `id` alanı UUID formatındadır (örn: `"550e8400-e29b-41d4-a716-446655440000"`).

### GET /api/v1/contents/section/{sectionKey}
Belirli bir section'a ait aktif içerikleri getir (sortOrder'a göre sıralı).

**Path Parameters:**
- `sectionKey`: Section anahtarı (örn: "homepage_hero", "resume_experience", "services_grid")

**Response:**
{
  "result": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "basicInfo": {
        "id": "660e8400-f39b-41d4-a716-446655441111",
        "sectionKey": "services_grid",
        "title": "Dental Implants",
        "description": "Professional dental implant services",
        "isActive": true,
        "sortOrder": 1,
        "createdAt": "2026-02-17T00:00:00.000+00:00",
        "updatedAt": "2026-02-17T00:00:00.000+00:00"
      },
      "contentType": "treatment_card",
      "metadata": {
        "icon": "Syringe",
        "desc": "Permanent solution for missing teeth"
      },
      "createdAt": "2026-02-17T00:00:00.000+00:00",
      "updatedAt": "2026-02-17T00:00:00.000+00:00"
    }
  ]
}
```

---

### GET /api/v1/contents/{id}
Tekil içerik getir (UUID ile).

**Path Parameters:**
- `id`: İçerik UUID'si

**Response:**
{
  "result": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "basicInfo": {
      "id": "660e8400-f39b-41d4-a716-446655441111",
      "sectionKey": "resume_experience",
      "title": "Senior Developer",
      "description": "Full-stack development role",
      "isActive": true,
      "sortOrder": 1,
      "createdAt": "2026-02-17T00:00:00.000+00:00",
      "updatedAt": "2026-02-17T00:00:00.000+00:00"
    },
    "contentType": "experience_item",
    "metadata": {
      "company": "Acme Corp",
      "period": "2022-2025",
      "technologies": ["Java", "Spring Boot", "PostgreSQL"],
      "achievements": ["Led team of 5", "Improved performance by 40%"]
    },
    "createdAt": "2026-02-17T00:00:00.000+00:00",
    "updatedAt": "2026-02-17T00:00:00.000+00:00"
  }
}
```

---

### GET /api/v1/contents/list
Tüm içerikleri listele.

**Response:**
{
  "result": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "basicInfo": {
        "id": "660e8400-f39b-41d4-a716-446655441111",
        "sectionKey": "string",
        "title": "string",
        "description": "string",
        "isActive": true,
        "sortOrder": 1,
        "createdAt": "2026-02-17T00:00:00.000+00:00",
        "updatedAt": "2026-02-17T00:00:00.000+00:00"
      },
      "contentType": "string",
      "metadata": { ... },
      "createdAt": "2026-02-17T00:00:00.000+00:00",
      "updatedAt": "2026-02-17T00:00:00.000+00:00"
    }
  ]
}
```

---

### GET /api/v1/contents/list/paged
Sayfalı içerik listesi.

**Query Parameters:**
- `page`: Sayfa numarası (default: 0)
- `size`: Sayfa başına kayıt sayısı (default: 10)
- `sort`: Sıralama alanı ve yönü (default: "sortOrder,asc")

**Response:**
```json
{
  "result": true,
  "data": {
    "content": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "basicInfo": {
          "id": "660e8400-f39b-41d4-a716-446655441111",
          "sectionKey": "string",
          "title": "string",
          "description": "string",
          "isActive": true,
          "sortOrder": 1,
          "createdAt": "2026-02-17T00:00:00.000+00:00",
          "updatedAt": "2026-02-17T00:00:00.000+00:00"
        },
        "contentType": "string",
        "metadata": { ... },
        "createdAt": "2026-02-17T00:00:00.000+00:00",
        "updatedAt": "2026-02-17T00:00:00.000+00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### POST /api/v1/contents
İçerik oluştur.

**Body:**
{
  "basicInfoId": "660e8400-f39b-41d4-a716-446655441111", // opsiyonel, mevcut gruba eklemek için (biri zorunlu)
  "basicInfo": {                                        // opsiyonel, yeni grup yaratmak için (biri zorunlu)
    "sectionKey": "string",
    "title": "string",
    "description": "string",
    "isActive": true,
    "sortOrder": 1
  },
  "contentType": "string",     // zorunlu
  "metadata": {                // opsiyonel, serbest key-value
    "icon": "Syringe",
    "desc": "Permanent solution for missing teeth"
  }
}
```

> **metadata Örnekleri:**
>
> Diş Hekimi Hizmet Kartı:
> ```json
> { "icon": "Syringe", "desc": "Permanent solution for missing teeth" }
> ```
>
> Yazılımcı Deneyim Kartı:
> ```json
> {
>   "company": "Acme Corp",
>   "period": "2022-2025",
>   "technologies": ["Java", "Spring Boot"],
>   "achievements": ["Led team of 5"]
> }
> ```
>
> Testimonial:
> ```json
> { "name": "Ahmet Y.", "rating": 5, "comment": "Harika hizmet!" }
> ```

**Response:**
```json
{
  "result": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "basicInfo": { ... },
    "contentType": "string",
    "metadata": { ... },
    "createdAt": "2026-02-17T00:00:00.000+00:00",
    "updatedAt": "2026-02-17T00:00:00.000+00:00"
  }
}
```

---

### PUT /api/v1/contents/{id}
İçerik güncelle.

**Path Parameters:**
- `id`: İçerik UUID'si

**Body:** (POST ile aynı)

---

### DELETE /api/v1/contents/{id}
İçerik sil.

**Path Parameters:**
- `id`: İçerik UUID'si

---

## Response Format

Tüm API'ler şu formatta döner:

**Başarılı:**
```json
{
  "result": true,
  "data": { ... }
}
```

**Hata:**
```json
{
  "result": false,
  "status": 401,
  "error": "Unauthorized",
  "errorCode": "AUTHENTICATION_REQUIRED",
  "message": "Authentication required"
}
```

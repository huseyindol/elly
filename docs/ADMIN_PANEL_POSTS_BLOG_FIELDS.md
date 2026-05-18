# Admin Panel — Posts Yeni Blog Alanları

**Hedef:** Elly Admin Panel'deki post yönetim sayfasına backend'e eklenen 6 yeni blog
alanını ekle: `description`, `category`, `coverImage`, `publishedAt`, `author`, `readingTime`.

**Stack:** Next.js 16 App Router, React 19, TypeScript 5.9, Tailwind CSS 4, shadcn/ui, Bun.
API çağrıları `fetcher` utility ile yapılır (token otomatik eklenir, Authorization header'ı elle ekleme).

---

## Backend'den Gelen Yeni Alanlar

`DtoPost` ve `DtoPostIU` güncellendi — şu 6 alan eklendi:

```typescript
description: string | null      // kısa açıklama (listing kartı için)
category: string | null         // kategori
coverImage: string | null       // kapak görseli URL
publishedAt: string | null      // ISO date string ("2025-01-15T00:00:00.000Z")
author: string | null           // yazar adı
readingTime: string | null      // "5 dk okuma"
```

---

## 1. Tip Güncellemesi

Projedeki `Post` tip tanımlarına (muhtemelen `src/types/` veya servis dosyasının yanında) 6 alanı ekle:

```typescript
// Mevcut alanlara ek olarak:
description: string | null
category: string | null
coverImage: string | null
publishedAt: string | null   // ISO string
author: string | null
readingTime: string | null
```

---

## 2. Post Servisi

Mevcut post servis fonksiyonlarında request body'ye yeni alanları dahil et.
`fetcher` kullanımını değiştirme, sadece payload'a yeni alanları ekle.

---

## 3. Form Güncellemesi (Create + Edit)

Post form bileşenine aşağıdaki alanları ekle. Mevcut form yapısını (Input, Textarea,
Label, shadcn bileşenleri, layout) koru — sadece yeni alanları uygun bir yere ekle
(örneğin "SEO" bölümünden önce "Blog Bilgileri" başlığı altında).

### Alan tablosu

| Alan | Bileşen | Label | Notlar |
|------|---------|-------|--------|
| `description` | `<Textarea rows={3}>` | Kısa Açıklama | Listing kartında görünür |
| `category` | `<Input>` | Kategori | Serbest metin |
| `coverImage` | `<Input>` | Kapak Görseli URL | Tam URL veya `/assets/...` |
| `publishedAt` | `<Input type="date">` | Yayın Tarihi | Dönüşüm gerekli (aşağıya bak) |
| `author` | `<Input>` | Yazar | |
| `readingTime` | `<Input>` | Okuma Süresi | Örn. "5 dk okuma" |

### `publishedAt` dönüşümü

```typescript
// ISO string → <input type="date"> value (YYYY-MM-DD)
const toDateInput = (iso: string | null): string =>
  iso ? iso.split('T')[0] : ''

// <input type="date"> value → ISO string (API'ya gönderilirken)
const toIso = (val: string): string | null =>
  val ? new Date(val).toISOString() : null
```

---

## 4. Listing Güncellemesi

Posts listesindeki her satıra / karta şunları ekle:

- **`category`** → küçük `<Badge variant="secondary">` veya renkli chip
- **`author`** → muted text (`text-muted-foreground text-sm`)
- **`publishedAt`** → `new Date(publishedAt).toLocaleDateString('tr-TR')` formatında

Kart/satır alan yoksa sadece `category` badge yeterli; `author` ve `publishedAt`
tooltip veya genişletilmiş view'da gösterilebilir.

---

## 5. API Endpoint Referansı

```
GET    /api/v1/posts/list             → Tüm liste (content dahil)
GET    /api/v1/posts/list/summary     → Özet liste (content yok, listing için)
GET    /api/v1/posts/slug/{slug}      → Slug ile getir
GET    /api/v1/posts/{id}             → ID ile getir
POST   /api/v1/posts                  → Tek post oluştur
POST   /api/v1/posts/bulk             → Toplu oluştur (body: DtoPostIU[])
PUT    /api/v1/posts/{id}             → Güncelle
DELETE /api/v1/posts/{id}             → Sil
```

Response wrapper: `{ result: boolean, message?: string, data: T }`

---

## 6. Gerekli Permission

`posts:create`, `posts:update`, `posts:delete`, `posts:read`

Mevcut `PermissionGate` / `@PreAuthorize` kullanımlarını değiştirme.

---

## 7. Doğrulama Kriterleri

1. Form açılınca yeni 6 alan görünüyor
2. Kayıt sonrası tüm alanlar API'dan geri dönüyor (edit modda dolu geliyor)
3. Edit modda `publishedAt` tarih input'unda doğru görünüyor
4. Listing'de `category` badge ve `author` görünüyor
5. `bun run build` veya `bun dev` TypeScript hatası yok (`any` kullanma)

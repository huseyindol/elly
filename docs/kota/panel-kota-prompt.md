# Panel — Tenant Depolama Kotası UI + İçerik Tenant Bağlamı

> **Backend HAZIR ve deploy edildi.** Bu dosya **elly-admin-panel** agent'ı içindir.
> İki parça:
> - **A) İçerik tenant bağlamı:** Panel içerik upload'ları (assets/banners/posts) **seçili
>   tenant'a** gitsin (`X-Tenant-Id`) → dosyalar o tenant'ın klasörüne yazılır ve **o tenant'ın
>   kotasına** sayılır. (Şu an panel içerik isteklerinde X-Tenant-Id GÖNDERMİYOR → her şey
>   `basedb`'ye gidiyor.)
> - **B) Kota UI:** Kullanım çubuğu + limit ayarı + kota dolunca (HTTP 413) net hata mesajı.

---

## Ortak — Backend sözleşmesi

### X-Tenant-Id
Chat servislerinde zaten var olan `tenantHeader(tenantId)` → `{ 'X-Tenant-Id': tenantId }`
deseni içerik servislerine de uygulanacak. Header **TC grubun/tenant'ın id'si** (ör. `tenant1`).
Header yoksa backend `basedb` bağlamında çalışır.

### Quota API (hepsi `X-Tenant-Id` context'inde; yanıt `{ result, data }`)
| İşlem | Method + Path | Auth | Body | data |
|---|---|---|---|---|
| Kullanım | `GET /api/v1/storage/quota` | authenticated | — | `StorageQuota` |
| Limit ayarla | `PUT /api/v1/storage/quota/limit` | SUPER_ADMIN | `{ limitBytes }` | `StorageQuota` |
| Yeniden hesapla | `POST /api/v1/storage/quota/recompute` | SUPER_ADMIN | — | `StorageQuota` |

```ts
interface StorageQuota {
  tenantId: string
  usedBytes: number
  limitBytes: number    // etkin limit (override yoksa varsayılan 3GB)
  usedPercent: number   // 0-100
}
```

### Kota aşımı (upload)
Dosya yükleme kotayı aşarsa backend **HTTP 413** döner:
```json
{ "result": false, "status": 413, "errorCode": "STORAGE_QUOTA_EXCEEDED", "message": "Depolama kotası aşıldı (tenant1): 2.9 GB + 200 MB > 3.0 GB" }
```

---

## A) İçerik tenant bağlamı (PROMPT)

````
elly-admin-panel'de içerik yönetimini (assets, banners, posts...) "seçili tenant" bağlamına
bağla: bir tenant seçilince ilgili içerik istekleri X-Tenant-Id ile o tenant'a gitsin.
Böylece dosyalar o tenant'ın klasörüne/kotasına yazılır. Backend hazır; sadece header gelsin.

## Görev
1. **Tenant seçici (content scope):**
   - Mevcut tenant listesini kullan (chat/mail-accounts/users zaten tenant kavramı kullanıyor;
     aynı kaynağı/endpoint'i kullan). Yoksa tenant listesi endpoint'inden çek.
   - Seçili tenant'ı bir store/context'te tut (ör. `useContentTenant` / Zustand). Varsayılan:
     boş/basedb. Header'da: seçili ise `X-Tenant-Id: <tenant>`, değilse gönderme.
   - Üst barda küçük bir "İçerik Tenant'ı: [tenant1 ▼]" seçici (içerik sayfalarında görünür).

2. **İçerik servislerine X-Tenant-Id ekle:**
   - `assets.services.ts`, `banners.services.ts` (ve içerik upload'ı olan diğerleri: posts,
     pages, components...) — `fetcher` çağrılarına `headers: tenantHeader(selectedTenant)` ekle.
   - `tenantHeader` chat'te var; ortak bir util'e taşı veya yeniden kullan.
   - GET/list istekleri de aynı tenant'ı göndermeli (o tenant'ın içeriğini göster).

3. **Tenant değişince** içerik listelerini invalidate et (TanStack Query: queryKey'e tenant'ı ekle).

## Önemli
- Tenant seçili değilken davranış AYNI kalır (basedb içeriği). Geriye dönük uyumlu.
- Bu, panelin içeriği tenant başına yönetmesini sağlar (entity'ler zaten TenantContext'e göre
  doğru DB'ye gidiyor; eksik olan tek şey header'dı).

## Doğrulama
- Tenant1 seç → asset yükle → backend assets/t/tenant1/... altına yazar; GET /storage/quota
  (X-Tenant-Id: tenant1) used_bytes artmış görünür.
- Tenant değiştir → liste o tenant'ın içeriğini gösterir.
````

---

## B) Kota UI + 413 (PROMPT)

````
elly-admin-panel'e tenant depolama kotası göstergesi + limit ayarı + kota-dolu hata mesajı ekle.
Backend: GET/PUT/POST /api/v1/storage/quota* (X-Tenant-Id context'inde). Tipler ve 413 sözleşmesi
yukarıda.

## Görev
1. `src/app/_services/storage.services.ts`:
   - `getQuota(tenantId?)` → GET /api/v1/storage/quota (tenantHeader)
   - `setQuotaLimit(limitBytes, tenantId?)` → PUT /api/v1/storage/quota/limit
   - `recomputeQuota(tenantId?)` → POST /api/v1/storage/quota/recompute
2. `src/app/_hooks/useStorageQuota.ts` (TanStack Query, queryKey'e tenant'ı ekle).
3. **Kullanım çubuğu** (içerik/assets sayfasında veya ayarlar):
   - `usedBytes / limitBytes` → progress bar + "X.X GB / Y.Y GB (%Z)". Byte→insan-okur format helper'ı.
   - `usedPercent > 80` → turuncu, `> 95` → kırmızı.
4. **Limit ayarı** (yalnız SUPER_ADMIN — `usePermission`/role ile gate):
   - GB cinsinden input → `limitBytes = gb * 1024^3` → `setQuotaLimit`. Başarıda quota'yı invalidate et.
   - "Yeniden hesapla" butonu → `recomputeQuota` (drift onarımı).
5. **413 yakalama:** Tüm dosya upload servislerinde (assets/banners...) yanıt `status === 413` /
   `errorCode === 'STORAGE_QUOTA_EXCEEDED'` ise: toast/uyarı → backend `message`'ını göster
   ("Depolama kotası doldu — X / Y. Dosya yüklenemedi."). Upload'ı sessiz başarısız bırakma.

## Doğrulama
- Kullanım çubuğu seçili tenant'ın gerçek kullanımını gösterir.
- Kota dolunca yükleme → 413 → net hata mesajı (sessiz fail değil).
- Admin limiti değiştirince çubuk/limit güncellenir; "yeniden hesapla" gerçek boyutu çeker.
````

---

## Notlar (senin için — agent'a verme)
- **A** daha büyük parça (tenant seçici + içerik servislerini header'la donatma). İstersen önce
  sadece **assets + banners** (upload-yoğun) ile başla, diğer içerik tipleri sonra.
- **B** bağımsız çalışır (tenant seçici olmasa da mevcut context'in kotasını gösterir).
- Backend tarafı tamamen hazır; panel sadece header + UI ekliyor.
- İstersen bu iki parçayı ben de doğrudan panel projesinde uygulayabilirim (prompt yerine).

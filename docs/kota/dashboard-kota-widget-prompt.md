# Dashboard — Depolama Kotası Widget'ı (Panel Prompt)

> **Backend HAZIR ve canlı.** Bu dosya **elly-admin-panel** agent'ı içindir.
> Hedef: Dashboard'a, her tenant'ın **ne kadar depolama kullandığını / ne kadar kaldığını**
> gösteren şık bir kart ekle (kullanım çubukları + yüzde + kalan).

## Backend sözleşmesi

`GET /api/v1/storage/quota` — **mevcut tenant** (X-Tenant-Id) bağlamında çalışır. Auth: authenticated.

```ts
interface StorageQuota {
  tenantId: string
  usedBytes: number
  limitBytes: number    // etkin limit (varsayılan 3GB)
  usedPercent: number   // 0-100
}
// Yanıt: { result: true, data: StorageQuota }
```

**Tüm tenant'lar için:** her tenant'a `X-Tenant-Id: <tenant>` header'ı ile ayrı çağrı yap;
`basedb` için header gönderme (varsayılan bağlam). Tenant listesi: panelin mevcut tenant
kaynağını kullan (chat/users tenant geçişinin kullandığı liste; yoksa sabit
`['basedb','tenant1','tenant2']`).

---

````
elly-admin-panel dashboard'ına "Depolama Kullanımı" kartı ekle: her tenant'ın kullandığı/
kalan depolama alanını çubuk + yüzde ile göster. Backend hazır (sözleşme yukarıda).

## Görev
1. `src/app/_services/storage.services.ts`:
   - `getQuota(tenantId?: string)` → GET `/api/v1/storage/quota`, tenantId varsa
     `{ 'X-Tenant-Id': tenantId }` header'ı ekle (chat'teki `tenantHeader` deseni). `.data` döndür.

2. `src/app/_hooks/useStorageQuotas.ts` (TanStack Query):
   - Tenant listesi (`['basedb','tenant1','tenant2']` veya mevcut tenant kaynağı) üzerinden
     **paralel** `getQuota(t)` (`useQueries` veya `Promise.all`). `StorageQuota[]` döndür.
   - `staleTime` ~60sn. Hata olan tenant'ı atla (kısmi göster).

3. `formatBytes(bytes)` helper: `1.0 GB`, `512 MB` gibi insan-okur format (1024 tabanı).

4. Dashboard kartı (`src/app/(baseLayout)/dashboard/...` içine, mevcut grid'e bir kart):
   - Başlık: "Depolama Kullanımı" + disk ikonu.
   - **Üstte özet:** toplam kullanılan / toplam limit + genel yüzde (Σ used / Σ limit).
   - **Her tenant için bir satır:**
     - Tenant adı (ör. "tenant1", basedb için "Genel/Admin").
     - Yatay **progress bar** (`usedPercent`).
     - Sağda: `formatBytes(usedBytes)` / `formatBytes(limitBytes)` · **%`usedPercent`**.
     - Altında küçük: "Kalan: `formatBytes(limitBytes - usedBytes)`".
   - **Renk eşikleri:** %0-70 yeşil, %70-90 turuncu/amber, %90+ kırmızı (bar + yüzde rengi).
   - `usedBytes=0` durumunu da düzgün göster (boş bar, "0 B / 3.0 GB").
   - Yükleniyor → skeleton; hata → küçük "yüklenemedi" notu.

## UI ipuçları
- shadcn/Tailwind kullanılıyorsa: `Card` + `Progress` bileşenleri. Progress yoksa basit
  `<div>` bar (width = `usedPercent%`).
- Kartı dashboard grid'inde 1 kolon (mobil tam genişlik) yer kaplayacak şekilde koy.
- Sayıları 1 ondalık göster (`2.9 GB`, `%96.7`).

## Doğrulama
- Dashboard'da kart görünür; her tenant için kullanılan/limit/kalan + yüzde doğru.
- Bir tenant'a dosya yükleyince (veya `recompute` sonrası) değer artar.
- %90+ tenant kırmızı görünür.
- Tenant erişilemezse o satır atlanır, kart yine render olur.
````

---

## Notlar (senin için — agent'a verme)
- Bu widget **salt-okunur** (sadece gösterim) — limit ayarı/recompute ayrı (`panel-kota-prompt.md` B bölümü).
- SUPER_ADMIN tüm tenant'ları görür; tek-tenant kullanıcıya sadece kendi tenant'ını göstermek
  istersen tenant listesini role/permission'a göre filtrele (opsiyonel).
- N tenant = N paralel istek; tenant sayısı azken sorun değil. Çok artarsa backend'e tek seferde
  "tüm tenant kotaları" endpoint'i eklenebilir (ileride).

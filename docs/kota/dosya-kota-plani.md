# Tenant Bazlı Dosya Depolama — Klasör İzolasyonu + Kota Planı

> Amaç: Her tenant'ın yüklediği dosyalar kendi klasöründe tutulsun; tenant başına
> **depolama kotası** (byte limiti) uygulansın. Kota aşımında upload reddedilsin.

## 0. Mevcut durum (kod incelemesi)

| Konu | Durum |
|------|-------|
| Depolama | PVC `elly-assets-pvc` **5Gi**, `local-path`, **RWO**, `/app/assets` |
| Serve | `WebConfig`: `/assets/**` → `file:assets/` |
| Yazım | `FileService.saveImage` → `assets/images/{sub}/{uuid.ext}` · `saveFile` → `assets/{sub}/{name}` |
| Tenant farkındalığı | **YOK** — hepsi ortak `assets/` altında |
| Yükleyiciler | chat (`"chat"`), banner (`images/{sub}/...`), assets (`{subFolder}`) |
| Tenant context | Upload anında `TenantContext.getTenantId()` mevcut |
| Kota | **YOK** |
| Max dosya | `spring.servlet.multipart.max-file-size=10MB` |

⚠️ **PVC RWO + 5Gi:** Tek pod yazabilir (burst 2. pod aynı RWO PVC'yi mount edemez — zaten kapalı). Toplam bütçe 5Gi; tenant kotaları toplamı bunu aşmamalı (ya da PVC büyütülmeli).

---

## 1. Klasör yapısı (Faz 1 — tenant izolasyonu)

Yeni yüklemeler tenant kökü altına:
```
assets/
├── t/
│   ├── basedb/         ← admin/ortak (TenantContext null)
│   │   ├── images/{subfolder}/{uuid.ext}
│   │   └── files/{subfolder}/{name}
│   ├── tenant1/
│   │   ├── images/...
│   │   └── files/...
│   └── tenant2/ ...
├── images/...          ← LEGACY (eski kayıtlar, dokunulmaz)
└── chat/ ...           ← LEGACY
```
- Tenant segment: `TenantContext.getTenantId()` (null → `basedb`). **Path-traversal koruması:** sadece `[a-zA-Z0-9_-]`, gerisi `_`.
- DB'de saklanan yol da yeni yapıyı içerir (`assets/t/{tenant}/...`). `WebConfig` `/assets/**`'i serve ettiği için ek ayar gerekmez.
- **Kota ölçümü kolaylaşır:** bir tenant'ın kullanımı = `assets/t/{tenant}/` klasörünün boyutu.

### Legacy strateji
Eski dosyalar `assets/images/...`, `assets/chat/...` altında kalır; DB'deki yolları aynen çalışır (WebConfig hepsini serve eder). **Migration zorunlu değil.** İstenirse Faz 4'te taşınır (dosya + DB yolu güncelleme). Kota hesabına legacy dahil edilmez (yalnız `t/{tenant}/`), ya da reconcile job ile bir kez "basedb"ye sayılır.

---

## 2. Kota modeli (Faz 2-3)

### Tablo — `storage_quota` (HER DB'de: basedb + tenant1 + tenant2)
Kota verisi **ilgili tenant'ın kendi DB'sinde** tutulur → enforcement upload anındaki (tenant) context'te çalışır, **cross-DB / OSIV sorunu yok**.

| alan | tip | açıklama |
|------|-----|----------|
| tenant_id | varchar PK | "tenant1" / "basedb" |
| limit_bytes | bigint | kota (0/null → varsayılan config) |
| used_bytes | bigint | güncel kullanım (sayaç) |
| updated_at | timestamptz | |

> Neden per-tenant-DB? Upload `TenantContext=tenant`'ta çalışır; sayaç sorgusu aynı DB'de
> olunca cross-DB switch (ve OSIV bağlantı pin sorunu) yaşanmaz. Admin tüm tenant'ları görmek
> isterse `inTenantContext(tenantId, ...)` döngüsüyle her DB'den okur (mevcut pattern).

### Varsayılan limit
`application.properties`: `app.storage.default-quota-bytes=1073741824` (1 GB). Tenant override tabloda.

### Kullanım takibi (used_bytes)
- **Sayaç (hızlı):** upload sonrası `+= size`, delete'te `-= size`.
- **Drift'e karşı reconcile:** `assets/t/{tenant}/` klasörünü gezip gerçek boyutu hesaplayan job/endpoint → `used_bytes` düzeltir. (Sayaç başarısız/manuel değişimde sapabilir.)

### Enforcement (upload akışı)
```
upload isteği (TenantContext=tenant)
  → FileService.save*: quota = storageQuotaService.check(tenant, file.size)
       used + file.size > limit ?  → QuotaExceededException (HTTP 413)
  → dosyayı yaz
  → storageQuotaService.addUsage(tenant, file.size)
```
Delete: `removeUsage(tenant, size)` (silinen dosyanın boyutu — silmeden önce `Files.size` ile alınır).

---

## 3. API (admin)

| Method | Path | Yetki | İş |
|--------|------|-------|-----|
| GET | `/api/v1/storage/quota` (+ `/tenant/{tid}/quota`) | authenticated | Kullanım/limit — hedef tenant URL path'inde, yoksa basedb |
| GET | `/api/v1/admin/tenants/{tenantId}/storage-quota` | SUPER_ADMIN/ADMIN | Bir tenant'ın kotası |
| PUT | `/api/v1/admin/tenants/{tenantId}/storage-quota` | SUPER_ADMIN | Limit ayarla `{ limitBytes }` |
| POST | `/api/v1/admin/tenants/{tenantId}/storage-quota/recompute` | SUPER_ADMIN | Klasörden gerçek kullanımı yeniden hesapla |

Yanıt: `{ tenantId, limitBytes, usedBytes, usedPercent, fileCount? }`.

---

## 4. Fazlar

- **Faz 1 — Tenant klasörleri (BU COMMIT):** `FileService` `assets/t/{tenant}/...` yazsın. Legacy dokunulmaz. Şema değişikliği yok → güvenli.
- **Faz 2 — Kota şeması + servis:** `StorageQuota` entity + repo + `StorageQuotaService` (check/add/remove/recompute) + migration (3 DB) + `QuotaExceededException` + `GlobalExceptionHandler` (413) + config.
- **Faz 3 — Enforcement:** `FileService.save*` içine quota check + addUsage; delete'te removeUsage. Reconcile endpoint.
- **Faz 4 — Admin API + Panel:** kota görüntüleme/ayarlama endpoint'leri + panel UI (kullanım çubuğu, limit ayarı) + 413 hatası UX. (Ayrı panel prompt'u.)
- **Faz 5 (ops.) — Legacy migration:** eski dosyaları `t/{tenant}/`'a taşı + DB yollarını güncelle (riskli, opsiyonel).

## 5. Edge case / riskler
- **Path traversal:** tenant + subfolder sanitize (`[^a-zA-Z0-9_-]`→`_`).
- **PVC RWO:** burst pod uploads'a erişemez → burst açılırsa object storage (S3/GCS) veya RWX PVC gerekir. Şimdilik tek pod.
- **5Gi toplam:** Σ tenant limitleri ≤ 5Gi olmalı, yoksa PVC dolar (tüm tenant'lar etkilenir). PVC büyütme = `storage: 10Gi` + `kubectl apply` (local-path genişletme destekliyorsa).
- **Sayaç drift:** reconcile job ile periyodik düzeltme.
- **Eşzamanlılık:** aynı tenant'tan paralel upload → `used_bytes` güncellemesi atomik olmalı (`UPDATE ... SET used_bytes = used_bytes + :n` veya satır kilidi).
- **Delete boyutu:** silmeden önce `Files.size()`; dosya yoksa 0.

---

## 6. Karar özeti
- Klasör: `assets/t/{tenant}/{images|files}/{subfolder}/...`
- Kota tablosu: **per-tenant DB** (`storage_quota`), enforcement aynı context'te.
- Varsayılan 1GB, override edilebilir.
- Sayaç + reconcile.
- Legacy: dokunma (opsiyonel migration sonra).

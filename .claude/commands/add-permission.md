---
name: add-permission
description: Yeni bir permission ekler — PermissionConstants sabiti, DataInitializer'da role atamasi, @PreAuthorize ornek kullanim
---

# Yeni Permission Ekleme

Kullanici "$ARGUMENTS" formatinda permission istedi (orn: "reports:export" veya "contents:publish").

## Adimlar

1. **`com.cms.config.PermissionConstants`** sinifina yeni sabit ekle:
   - Modul basligi yoksa `// =============== MODUL_ADI ===============` yorumlu blok olustur
   - Sabit isimlendirme: modul + islem (orn: `REPORTS_EXPORT = "reports:export"`)

2. **`com.cms.config.DataInitializer.initializeRoles()`** metodunda role atamasini guncelle:
   - SUPER_ADMIN otomatik tum permission'lari alir (degisiklik gerekmiyor)
   - ADMIN'e atanmamasini istiyorsan `if (!p.getName().startsWith("..."))` filtresi ekle
   - EDITOR'a vermek istiyorsan `editorModules` Set'ine modul adini ekle (orn: `"REPORTS"`)
   - VIEWER yalnizca `:read` permission'larini alir (otomatik)
   - **Idempotent sync:** Mevcut kullanicilar uygulama yeniden baslaginca permission'i otomatik alir

3. **Controller'da kullanim ornegi goster:**
   ```java
   @PreAuthorize("hasAuthority('reports:export')")
   public ResponseEntity<...> exportReport(...) { ... }
   ```

4. **Kontrol et:**
   - `./mvnw compile` ile derleme hatasi yok
   - Frontend'in `permission-store.ts`'sine de eklemesi gerekecegini hatirlat (yeni permission frontend menu/buton kontrolunde de kullanilacaksa)

## Onemli Notlar

- Format: `modul:islem` (kucuk harf, iki nokta ayrac)
- `:read`, `:create`, `:update`, `:delete`, `:manage` standart islemler — yeni islem turu eklerken tutarli ol
- `DataInitializer` her uygulama basinda `syncRole()` ile rollere eksik permission'lari otomatik ekler
- Hic kullanilmadigi halde sabit eklersen `DataInitializer` yine de DB'ye yazar — gercekten gerekli olduguna emin ol
- PermissionConstants degisiklikleri Redis auth cache'ini etkiler — `auth:user:*` key'lerinin TTL'i (30dk) bitince yeni izinler aktif olur

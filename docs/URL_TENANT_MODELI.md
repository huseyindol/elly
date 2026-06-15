# URL-Tenant Modeli (Multi-Tenant Yönlendirme)

> **Tek kural:** Kimlik (kim olduğun) **her zaman** JWT'de taşınır; hedef tenant
> (hangi DB'ye gidileceği) **her zaman** URL path'inde taşınır.
> `X-Tenant-Id` header'ı ve "tenant-switch token" **KULLANILMAZ**.

Bu doküman, admin'in **başka bir tenant adına** yaptığı işlemlerin (tenant chat —
TC, depolama kotası) hangi tenant DB'sine gideceğinin nasıl belirlendiğini ve bu
modele neden geçtiğimizi anlatır. Backend (`elly`) commit `5e94674`, panel
(`elly-admin-panel`) commit `d708cb2`.

---

## 1. Neden bu model? (kararın hikâyesi)

Üç aşamadan geçtik:

1. **`X-Tenant-Id` header (eski).** Çalışıyordu ama tenant bilgisini güvensiz raw
   header'dan okumak [`MULTI_TENANCY.md`](./MULTI_TENANCY.md)'deki "tenant bilgisi
   JWT claim'inden gelmeli" ilkesine aykırıydı. Ayrıca **yalnız bu birkaç servis
   için** ekstra bir header kuralı olması, ileri dönük review'u zorlaştırıyordu.
   Bu yüzden header desteği kaldırıldı.

2. **Tenant-switch token (kırık ara çözüm).** Header kalkınca TC/kota için tenant
   taşıyacak bir mekanizma gerekti; `POST /api/v1/tenants/token` ile **kimliksiz**
   bir JWT (`tenantId` + `type=tenant`, kullanıcı yok) üretildi. Bu token
   `isAuthenticated()` isteyen endpoint'lerde reddediliyordu → **TC sekmesi kırıldı**.
   Admin aslında kendi kimliğiyle yazabilmeliydi; sorun kimlik değil, **DB seçimiydi**.

3. **URL-tenant (final).** Hedef tenant'ı URL path'ine koyduk. Bu kalıp zaten
   sistemde vardı:
   - WebSocket: `/app/tenant-chat/{tid}/{groupId}/send`
   - Public (anonim) REST: `/api/v1/public/{tid}/...`
   - Admin tenant-users: `/api/v1/admin/tenants/{tid}/users`

   Artık chat/kota da aynı kalıbı kullanıyor → **chat'e özel istisna kalmadı**,
   kural tek ve uniform: *kimlik JWT'de, hedef tenant URL'de.*

---

## 2. Endpoint kalıbı

| Bağlam | AC / basedb | TC / hedef tenant |
|--------|-------------|-------------------|
| Chat REST | `/api/v1/chat/...` | `/api/v1/chat/tenant/{tid}/...` |
| Storage kota | `/api/v1/storage/quota` | `/api/v1/storage/tenant/{tid}/quota` |
| WebSocket (send/typing/read) | `/app/chat/{groupId}/...` | `/app/tenant-chat/{tid}/{groupId}/...` |
| Public visitor chat (anonim) | — | `/api/v1/public/{tid}/tenant-chat/...` |

Kimlik tüm bu çağrılarda admin'in **kendi** `Authorization: Bearer <JWT>`'sidir
(panel cookie'den otomatik ekler). URL'deki `{tid}` yalnızca **hangi DB** sorusunu
yanıtlar.

---

## 3. Güvenlik garantileri

- URL-tenant kalıbı **yalnız `loginSource = admin`** kimliğiyle çalışır. Tenant
  kullanıcısı URL'ye başka bir tenant yazarak oraya **sıçrayamaz** (kendi JWT
  claim'indeki tenant'a kilitli kalır).
- Bilinmeyen tenant → `400 Bad Request` (PublicApiFilter ile aynı davranış).
- Admin kimliği her zaman **basedb**'den yüklenir (`JwtAuthenticationFilter`),
  veri işlemleri ise URL'deki tenant DB'sinde olur → kimlik ve veri ayrışması net.

---

## 4. Backend'de (`elly`) ne değişti

- **`JwtTenantFilter`** (`com.cms.config`): `^/api/v1/(chat|storage)/tenant/{tid}/...`
  kalıbını yakalar; yalnız admin kimliğiyle `TenantContext`'i o tenant'a set eder,
  bilinmeyen tenant'ta 400 döner. Diğer her şey eskisi gibi (JWT claim / basedb).
- **Controller'lar** çift `@RequestMapping` aldı (`{"/...", "/tenant/{tid}/..."}`)
  → eski AC yolları korundu, TC için yeni varyant eklendi. **Migration gerekmedi.**
  - `ChatGroupController`, `ChatHistoryController`, `StorageQuotaController`.
- **Silinen ölü kod (switch-token zinciri):**
  - `AuthController` `GET /api/v1/auth/public-token/{tenantId}`
  - `TenantController` `POST /api/v1/tenants/token` *(/list korundu)*
  - `JwtUtil.generateTenantToken`, `IAuthService/AuthService.getPublicToken`
  - `DtoTenantTokenResponse`

---

## 5. Panel'de (`elly-admin-panel`) ne değişti

- `chat.services` → `chatBase(tid)` = `/api/v1/chat/tenant/{tid}`;
  `storage.services` → `quotaBase(tid)` = `/api/v1/storage/tenant/{tid}/quota`.
- `tcAuth` / `tenantToken` parametreleri, `X-Tenant-Id` (`tenantHeader`) ve
  `fetcher`'daki `overrideAuth` **tamamen kaldırıldı**.
- `chat-ws-store`'dan `activeTenantToken` state'i + `getTenantTokenService` çağrısı
  silindi (WS destination/topic tenant-aware kalmaya devam ediyor).
- `uploadChatFileService` artık `tenantId` alır → TC dosyaları doğru tenant
  klasörüne/kotasına yazılır.
- Silinen dosyalar: `_services/tenant.services.ts`, `utils/tenantHeader.ts`.

---

## 6. Yeni TC/kota endpoint'i eklerken

1. Controller metodunu çift map'le: `@GetMapping({"/x", "/tenant/{tenantId}/x"})`.
   `{tenantId}` path değişkenini bind etmene gerek yok; filtre URI'den okur.
2. Panel servisinde base'i `chatBase(tid)` / `quotaBase(tid)` üzerinden kur.
3. **Asla** `X-Tenant-Id` header'ı veya ayrı bir tenant token'ı ekleme — admin'in
   normal JWT'si + URL'deki `{tid}` yeterli.

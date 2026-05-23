# Elly Admin Panel — Mail Hesabı Yönetimi

**Stack:** Next.js 16 App Router, React 19, TypeScript 5.9, Tailwind CSS 4, shadcn/ui, Bun.
API çağrıları `fetcher` utility ile yapılır (token otomatik eklenir).

---

## API Referansı

```
GET    /api/v1/mail-accounts                  → Tüm hesaplar
GET    /api/v1/mail-accounts?tenantId=tenant1 → Belirli tenant'ın hesapları
GET    /api/v1/mail-accounts/active           → Tüm aktif hesaplar
GET    /api/v1/mail-accounts/active?tenantId=tenant1 → Tenant'ın aktif hesapları
GET    /api/v1/mail-accounts/{id}             → Hesap detayı
POST   /api/v1/mail-accounts                  → Yeni hesap
PUT    /api/v1/mail-accounts/{id}             → Güncelle
DELETE /api/v1/mail-accounts/{id}             → Sil
POST   /api/v1/mail-accounts/{id}/test        → Test maili gönder
POST   /api/v1/mail-accounts/{id}/verify      → Sadece SMTP bağlantısını doğrula (mail göndermez)
```

**Yetki:** `mail:create`, `mail:read`, `mail:update`, `mail:delete`

---

## DTO Yapıları

### DtoMailAccountResponse
```typescript
interface MailAccountResponse {
  id: number
  name: string
  fromAddress: string
  smtpHost: string
  smtpPort: number
  smtpUsername: string
  active: boolean
  tenantId: string | null   // hangi tenant'a ait
  isPrimary: boolean        // tenant'ın ana gönderim hesabı
  createdAt: string
  updatedAt: string
  // smtpPassword: ASLA DÖNMEZ
}
```

### DtoMailAccountRequest (POST/PUT body)
```typescript
interface MailAccountRequest {
  name: string              // zorunlu
  fromAddress: string       // zorunlu, email formatında
  smtpHost: string          // zorunlu (ör. smtp.gmail.com)
  smtpPort: number          // zorunlu, 1-65535 (587=STARTTLS, 465=SSL)
  smtpUsername: string      // zorunlu
  smtpPassword?: string     // oluştururken zorunlu; güncellerken boş bırakılırsa mevcut şifre korunur
  active?: boolean          // varsayılan: true
  tenantId?: string         // ör. "tenant1", "tenant2"
  isPrimary?: boolean       // varsayılan: false; true yapılınca aynı tenant'ın eskisi otomatik false olur
}
```

**Response wrapper:** `{ result: boolean, message?: string, data: T }`

---

## isPrimary Davranışı

- Her tenant için **en fazla bir** hesap `isPrimary: true` olabilir.
- Yeni bir hesap `isPrimary: true` ile oluşturulunca ya da güncellenince,
  aynı `tenantId`'ye sahip önceki primary hesap otomatik olarak `false` yapılır.
- Ayrı bir "varsayılan yap" endpoint'i **yoktur** — create/update sırasında `isPrimary` set edilir.
- Sistem e-postaları (kayıt doğrulama vb.) `isPrimary=true` olan hesaptan gönderilir.

---

## 1. Servis Fonksiyonları

`src/app/_services/mail-account.services.ts` (mevcut servisi güncelle veya oluştur):

```typescript
import { fetcher } from '@/utils/services/fetcher'

interface BaseResponse<T> {
  result: boolean
  message?: string
  data: T
}

export interface MailAccountResponse {
  id: number
  name: string
  fromAddress: string
  smtpHost: string
  smtpPort: number
  smtpUsername: string
  active: boolean
  tenantId: string | null
  isPrimary: boolean
  createdAt: string
  updatedAt: string
}

export interface MailAccountRequest {
  name: string
  fromAddress: string
  smtpHost: string
  smtpPort: number
  smtpUsername: string
  smtpPassword?: string
  active?: boolean
  tenantId?: string
  isPrimary?: boolean
}

export const getMailAccountsService = async (tenantId?: string): Promise<MailAccountResponse[]> => {
  const params = tenantId ? `?tenantId=${tenantId}` : ''
  const res = await fetcher<BaseResponse<MailAccountResponse[]>>(`/api/v1/mail-accounts${params}`)
  if (!res.result) throw new Error(res.message ?? 'Yüklenemedi')
  return res.data
}

export const getMailAccountService = async (id: number): Promise<MailAccountResponse> => {
  const res = await fetcher<BaseResponse<MailAccountResponse>>(`/api/v1/mail-accounts/${id}`)
  if (!res.result) throw new Error(res.message ?? 'Yüklenemedi')
  return res.data
}

export const createMailAccountService = async (data: MailAccountRequest): Promise<MailAccountResponse> => {
  const res = await fetcher<BaseResponse<MailAccountResponse>>('/api/v1/mail-accounts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.result) throw new Error(res.message ?? 'Oluşturulamadı')
  return res.data
}

export const updateMailAccountService = async (id: number, data: MailAccountRequest): Promise<MailAccountResponse> => {
  const res = await fetcher<BaseResponse<MailAccountResponse>>(`/api/v1/mail-accounts/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.result) throw new Error(res.message ?? 'Güncellenemedi')
  return res.data
}

export const deleteMailAccountService = async (id: number): Promise<void> => {
  const res = await fetcher<BaseResponse<boolean>>(`/api/v1/mail-accounts/${id}`, {
    method: 'DELETE',
  })
  if (!res.result) throw new Error(res.message ?? 'Silinemedi')
}

export const testMailAccountService = async (id: number, testTo: string): Promise<string> => {
  const res = await fetcher<BaseResponse<string>>(`/api/v1/mail-accounts/${id}/test`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ testTo }),
  })
  if (!res.result) throw new Error(res.message ?? 'Test başarısız')
  return res.data
}

export const verifyMailAccountService = async (id: number): Promise<string> => {
  const res = await fetcher<BaseResponse<string>>(`/api/v1/mail-accounts/${id}/verify`, {
    method: 'POST',
  })
  if (!res.result) throw new Error(res.message ?? 'Doğrulama başarısız')
  return res.data
}
```

---

## 2. Liste Sayfası

`src/app/(baseLayout)/mail-accounts/page.tsx`:

- Üst toolbar: tenant filtresi (dropdown: Tümü / tenant1 / tenant2), "Yeni Hesap" butonu
- Hesaplar kart veya tablo formatında listelenir
- Her satır/kart:
  - Hesap adı
  - `fromAddress` (gönderici adresi)
  - `smtpHost:smtpPort`
  - **Tenant rozeti** — `tenantId` değeri badge olarak gösterilir (yoksa "Atanmamış")
  - **Ana Hesap rozeti** — `isPrimary: true` ise yeşil "Ana Hesap" badge'i
  - Aktif/Pasif durumu
  - Eylemler: Düzenle | Sil | Test Et | Bağlantı Doğrula
- `isPrimary: true` olan hesap görsel olarak öne çıkarılmalı (border, rozet)
- Pasif hesaplar soluklaştırılmalı

**tenant filtresi akışı:**
```
State: selectedTenantId = ''
getMailAccountsService(selectedTenantId || undefined)
tenant dropdown değişince → servis yeniden çağrılır
```

---

## 3. Hesap Oluşturma / Düzenleme Formu

Tek bileşen: `MailAccountFormDialog.tsx` (yeni ve düzenleme için ortak dialog).

**Form alanları:**

| Alan | Tip | Zorunlu | Not |
|------|-----|---------|-----|
| Hesap Adı | text | ✓ | |
| Gönderici Adresi (From) | email | ✓ | From header'da görünür |
| SMTP Host | text | ✓ | smtp.gmail.com |
| SMTP Port | number | ✓ | 587 veya 465 |
| SMTP Kullanıcı Adı | text | ✓ | |
| SMTP Şifre | password | Oluştururken ✓ | Güncellerken boş = değiştirme |
| Tenant | select | — | tenant1 / tenant2 / boş |
| Ana Hesap | switch/checkbox | — | `isPrimary` — tenant başına tek |
| Aktif | switch | — | |

**isPrimary switch uyarısı:**
`isPrimary` toggle açılınca küçük bir uyarı göster:
> "Bu tenant'ın mevcut ana hesabı varsa otomatik olarak değiştirilecek."

**Şifre alanı placeholder:**
- Yeni: "SMTP şifrenizi girin"
- Düzenleme: "Değiştirmek için yeni şifre girin (boş bırakırsanız mevcut şifre korunur)"

**Kaydet sonrası:** Listeyi yenile, `isPrimary` değiştiyse diğer kartların rozetini güncelle.

---

## 4. Test Modalı

`SmtpTestModal.tsx`:

```typescript
// Açılınca:
// - testTo: string input (email)
// - "Test Gönder" → POST /api/v1/mail-accounts/{id}/test
// - Başarı: "Test maili gönderildi → {testTo}" (yeşil)
// - Hata: backend'den gelen message (kırmızı)
```

---

## 5. Doğrulama Kriterleri

1. Liste yüklenince hesaplar görünür; tenant filtresi çalışır
2. `isPrimary: true` olan hesap "Ana Hesap" rozetiyle belirtilir
3. Yeni hesap `isPrimary: true` oluşturulunca aynı tenant'ın eski ana hesabının rozeti kalkar (liste yenilenir)
4. Düzenleme formunda şifre boş bırakılınca mevcut şifre korunur
5. Test modalından mail gönderilir, başarı/hata mesajı gösterilir
6. `bun dev` veya `bun run build` TypeScript hatası yok

---

## Güvenlik Notları

- `smtpPassword` response'da **hiçbir zaman dönmez** — edit formunda boş başlar
- Gmail için App Password gereklidir (16 karakter, boşluksuz)
  > "Google Hesabı → Güvenlik → 2 Adımlı Doğrulama → Uygulama Şifreleri"
- `isPrimary` flag server-side enforced — frontend'de ek doğrulama gerekmez

---

## Backend Referans

- **Repo:** `huseyindol/elly` — `main` branch
- `src/main/java/com/cms/entity/MailAccount.java`
- `src/main/java/com/cms/controller/impl/MailAccountController.java`
- `src/main/java/com/cms/service/impl/MailAccountService.java`
- Migration: `src/main/resources/migration/db-migration-mail-accounts-tenantid.sql` (basedb'de çalıştır)

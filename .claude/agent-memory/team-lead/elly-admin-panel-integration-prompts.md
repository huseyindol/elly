# elly-admin-panel Entegrasyon Prompt'lari

> Bu doküman, elly-admin-panel (Next.js) repo'sunda Cursor / Claude Code / başka bir AI agent ile çalışırken **birebir kopyala-yapıştır** edilebilecek prompt'ları içerir. Her bölüm self-contained — agent önceki sohbeti görmez, bu yüzden tek prompt tüm bağlamı taşır.

## Kullanım

1. elly-admin-panel projesini bir agent'ın olduğu editörde aç (Cursor veya `claude` CLI).
2. Aşağıdaki bölümlerden istediğini **tek seferde** prompt olarak ver.
3. Agent'ın değişikliklerini review et, gerekirse sonraki promptu çalıştır.

Prompt sırası (baştan sona):
- **Prompt 0:** Keşif (proje stack'i tespit)
- **Prompt 1:** Ortak altyapı (http client, types, permission hook)
- **Prompt 2:** Email Templates sayfası (v4 feature)
- **Prompt 3:** RabbitMQ yönetim sayfası
- **Prompt 4:** Email Logs sayfası (v3 retry için)
- **Prompt 5:** Mail Accounts sayfası (SMTP profil CRUD + test/verify — v2 DB-based)
- **Prompt 6:** Forms sayfası (FormDefinition CRUD + Submissions + Mail+Form v4 — opsiyonel bildirim, çoklu alıcı)

Her prompt'un bağlamı **aynı CMS API**'ye dayanır — endpoint listeleri her promptta tekrar veriliyor ki agent diğer dokümanlara bakmak zorunda kalmasın.

---

## Prompt 0 — Proje Keşfi (ilk adım)

Bu prompt agent'a projenin stack'ini ve dosya yapısını inceletir. Sonuç sende kalır ve sonraki prompt'larda "X kütüphanesi ile yap" diyebilirsin.

```
Bu elly-admin-panel Next.js projesinin mevcut stack'ini ve dosya organizasyonunu
analiz et. Kod yazma, sadece araştır ve rapor et. Şu sorulara cevap ver:

1. Next.js versiyonu (13/14/15) ve App Router mu Pages Router mu?
2. TypeScript kullanılıyor mu? Strict mode açık mı?
3. UI kütüphanesi: shadcn/ui, MUI, Chakra, Ant Design, Mantine — hangisi?
4. Form yönetimi: react-hook-form, formik, native? Validation: zod, yup?
5. Data fetching: TanStack Query, SWR, axios, native fetch — hangisi?
6. State yönetimi: Zustand, Redux, Jotai, Context — hangisi?
7. Auth akışı nasıl işliyor? JWT nerede tutuluyor (cookie / localStorage / store)?
8. Mevcut admin sayfaları nerede? (`app/admin/*` veya `pages/admin/*`)
9. Mevcut bir API client / http wrapper var mı? (`lib/api/` veya `services/`)
10. Tailwind kullanılıyor mu? shadcn/ui kuruluysa hangi bileşenler mevcut?
11. Toast/notification sistemi var mı? (sonner, react-hot-toast, react-toastify)
12. Monaco editor, code editor, WYSIWYG gibi bir editor bileşeni halihazirda
    kuruluyor mu?
13. `.env` dosyasında CMS API base URL tanımlı mı? (NEXT_PUBLIC_*)

Raporu şu formatta ver:
- Stack özeti (tablo)
- Mevcut bağımlılıklar listesi (`package.json`'dan ilgili olanlar)
- Auth akışı 3-5 satırda nasıl işliyor
- Önerilen konum: yeni admin sayfaları nereye eklenmeli (mevcut pattern'e uyumlu)
- Eksik olup eklenmesi gerekecek kütüphaneler (varsa)

Rapor altında 400 kelime.
```

---

## Prompt 1 — Ortak Altyapı (http client + types + permission hook)

Prompt 0 sonrası stack'ı öğrendikten sonra bunu çalıştır. Altyapı olmadan feature sayfaları yazılamaz.

```
elly-admin-panel projesine CMS API entegrasyonu için ortak altyapı ekle.

## Bağlam

Bu panel, elly CMS API'sini (Spring Boot) tüketiyor. CMS tüm yanıtlarını
`RootEntityResponse<T>` wrapper'ı ile döner:

```json
{ "result": true, "message": null, "data": { ... } }
```

Hata durumunda:
```json
{ "result": false, "status": 400, "error": "...", "errorCode": "VALIDATION_ERROR", "message": "..." }
```

Auth: JWT Bearer token. CMS `/api/v1/auth/login` ile alınır, HttpOnly cookie
olarak saklanıyor (`elly-jwt` adıyla) veya auth store'da tutuluyor — projenin
mevcut patterns'ine bakarak uygun yeri seç. CMS ayrıca JWT claim'inde kullanıcının
`permissions: string[]` listesini taşıyor.

## Görev

Aşağıdaki dosyaları oluştur (projenin mevcut stack'ine göre import path ve
folder convention'larını adapte et, ama dosya amaçları aşağıdakilerle aynı olsun):

### 1. `lib/api/http.ts` (veya mevcut http wrapper'ı genişlet)

Özellikler:
- Base URL: `process.env.NEXT_PUBLIC_ELLY_API_URL`
- JWT token'ı cookie'den (server) veya auth store'dan (client) otomatik çeker
- `RootEntityResponse<T>` wrapper'ını otomatik açar, `data` döner
- `result: false` ise `ApiError(message, errorCode, status)` fırlatır
- Metot imzaları: `http.get<T>(path, opts?)`, `http.post<T>`, `http.put<T>`, `http.delete<T>`
- `opts.searchParams: Record<string, unknown>` ve `opts.json: unknown` destekler
- Cache: server component çağrıları `cache: 'no-store'` ile yapılsın
- `ApiError` export edilsin (try/catch'te kullanmak için)

### 2. `types/cms.ts` (veya projeye uygun konum)

CMS endpoint'lerinin döndüğü DTO tiplerini tanımla:

```typescript
export interface EmailTemplate {
  id?: number;
  tenantId: string | null;
  templateKey: string;
  subject: string;
  htmlBody: string;
  description?: string | null;
  active: boolean;
  version: number;
  optimisticLockVersion: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface EmailLog {
  id: number;
  recipient: string;
  subject: string;
  templateName: string;
  status: 'PENDING' | 'SENT' | 'FAILED';
  retryCount: number;
  createdAt: string;
  sentAt: string | null;
}

export interface RabbitOverview {
  rabbitmqVersion: string | null;
  erlangVersion: string | null;
  clusterName: string | null;
  totalMessages: number | null;
  totalConsumers: number | null;
  queueCount: number | null;
  exchangeCount: number | null;
  connectionCount: number | null;
  channelCount: number | null;
}

export interface RabbitQueue {
  name: string;
  vhost: string;
  messages: number | null;
  messagesReady: number | null;
  messagesUnacknowledged: number | null;
  consumers: number | null;
  state: string | null;
  arguments: Record<string, unknown>;
  policy: string | null;
  durable: boolean | null;
  autoDelete: boolean | null;
  exclusive: boolean | null;
}

export interface RabbitMessage {
  payload: string;
  payloadEncoding: 'string' | 'base64';
  properties: Record<string, unknown>;
  messageCount: number | null;
  routingKey: string;
  redelivered: boolean;
  exchange: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;       // current page
  size: number;
}
```

### 3. `lib/auth/permissions.ts` — Permission kontrol yardımcıları

İki fonksiyon:

- **Server-side** `requirePermission(permission: string): Promise<void>` —
  Server Component / Server Action / Route Handler içinde kullanılır.
  JWT cookie'sini okur, `permissions` claim'ini kontrol eder, yoksa
  `redirect('/login')` veya `/403`.

- **Client-side** `usePermission(permission: string): boolean` hook'u —
  Auth store'dan (projenin mevcut store'u) permission listesini okur,
  boolean döner. UI'da buton disabled kontrolü için.

### 4. `.env.local.example` güncelle

```
NEXT_PUBLIC_ELLY_API_URL=https://api.huseyindol.com
```

## Kısıtlar

- Projenin mevcut import alias'larını kullan (`@/lib/...` vs `~/lib/...`)
- Mevcut toast kütüphanesi varsa onu kullan, yoksa kurma — agent kararı çağır
- TanStack Query kurulu değilse, data fetching katmanını hook'lar olmadan
  yaz (sonraki prompt'larda eklenecek)
- Auth store'un nasıl çalıştığını Prompt 0 raporundan öğrendin, onu referans al

## Doğrulama

- `npm run build` hatasız geçmeli
- TypeScript strict mode uyarı vermemeli
- `console.log` bırakma
- README veya basit bir `lib/api/README.md` yaz (kullanım örneği)
```

---

## Prompt 2 — Email Templates Sayfası (v4 Feature)

**Ön koşul:** Prompt 1 tamamlandı (http client + types hazır). CMS v4 backend deploy edildi — endpoint'ler aktif.

```
elly-admin-panel'e "Email Templates" admin sayfası ekle. Bu sayfa, CMS'te
veritabanında saklanan Thymeleaf email template'lerini panel'den yönetmeyi sağlar.

## Bağlam

CMS endpoint'leri:

| Method | Path | Permission | Açıklama |
|---|---|---|---|
| GET  | `/api/v1/email-templates?page=0&size=20` | `email_templates:read` | Liste (paginated) |
| GET  | `/api/v1/email-templates/{key}` | `email_templates:read` | Detay |
| POST | `/api/v1/email-templates` | `email_templates:manage` | Oluştur |
| PUT  | `/api/v1/email-templates/{key}` | `email_templates:manage` | Güncelle |
| DELETE | `/api/v1/email-templates/{key}` | `email_templates:manage` | Sil (soft) |
| POST | `/api/v1/email-templates/{key}/preview` | `email_templates:read` | Dummy data ile render |

Request/Response tipleri: `EmailTemplate` (zaten `types/cms.ts`'de tanımlı).

**Mevcut seed template'leri (DB'de hazır):**
| templateKey | Subject | Değişkenler |
|-------------|---------|-------------|
| `form-notification` | `Yeni Form Gönderimi: [[${formTitle}]]` | `formTitle`, `submittedAt`, `formFields` |
| `welcome` | `Hoş Geldin, [[${userName}]]!` | `userName`, `dashboardUrl` |
| `password-reset` | `Şifre Sıfırlama Talebiniz` | `userName`, `resetUrl`, `resetCode`, `expiresIn` |

Preview endpoint'i şu body bekler:
```json
{ "data": { "userName": "Ahmet", "link": "https://..." } }
```

Response:
```json
{ "html": "<html>...", "subject": "Render edilmiş subject" }
```

## Görev

### Dosyalar

```
app/(admin)/admin/email-templates/
├── page.tsx                         # Liste sayfası
├── loading.tsx                      # Skeleton
├── new/
│   └── page.tsx                     # Oluşturma formu
├── [key]/
│   ├── page.tsx                     # Edit sayfası
│   └── _components/
│       ├── TemplateForm.tsx
│       ├── MonacoBodyEditor.tsx
│       └── PreviewPanel.tsx
└── _components/
    ├── TemplateListTable.tsx
    └── DeleteConfirmDialog.tsx

lib/api/email-templates.ts           # API client fonksiyonlari
lib/hooks/email-templates/
├── useEmailTemplates.ts             # useQuery list
├── useEmailTemplate.ts              # useQuery detail
└── useTemplateMutations.ts          # create/update/delete/preview
```

### Özellikler

**Liste sayfası (`/admin/email-templates`)**
- Server Component, `requirePermission('email_templates:read')`
- Tablo: templateKey (mono font), subject, active (badge), updatedAt, actions
- "Yeni Template" butonu (sağ üst) — `rabbit:manage` değil, `email_templates:manage`
- Satır tıklanınca `/admin/email-templates/{key}` edit sayfasına git
- Her satırda "Delete" butonu — permission'a göre disabled
- TanStack Query `staleTime: 30_000`

**Oluşturma formu (`/new`)**
- react-hook-form + zod schema
- Alanlar: templateKey (regex `^[a-z0-9-]+$`), subject, description, active checkbox, htmlBody
- htmlBody için Monaco editor (HTML mode, VS dark theme, 500px yükseklik)
- Monaco `dynamic(() => import('@monaco-editor/react'), { ssr: false })`
- "Preview" butonu (form submit etmez) → panel açar, dummy JSON girilir, iframe'de render
- "Kaydet" → `POST /api/v1/email-templates` → başarılıysa edit sayfasına redirect

**Edit sayfası (`/[key]`)**
- URL'den `key` alır, `useEmailTemplate(key)` ile fetch
- `TemplateForm` bileşenini `defaultValues` ile doldurur
- `optimisticLockVersion` hidden field — PUT body'sinde gönderilir
- 409 Conflict (OptimisticLockException) → toast: "Başka biri güncellemiş, yenile"
- Delete butonu (sağ üst, destructive variant) — confirm dialog + success redirect

**Preview Panel**
- Sol: JSON textarea (dummy data, default: `{"userName": "Ahmet"}`)
- Sağ: iframe `sandbox=""` `srcDoc={html}` (XSS koruması)
- "Render et" butonu → `POST /preview` → iframe güncellenir
- Response'taki `subject` de gösterilsin (iframe üstünde)

**Delete Confirm Dialog**
- shadcn Dialog (veya projenin modal bileşeni)
- "Onaylamak için templateKey'i yaz: **welcome**"
- Input değeri eşleşince "Sil" butonu aktif olur

### TanStack Query Hook'ları

Query key'leri:
```typescript
export const emailTemplatesKeys = {
  all: ['email-templates'] as const,
  list: (params?: Record<string, unknown>) => [...emailTemplatesKeys.all, 'list', params] as const,
  detail: (key: string) => [...emailTemplatesKeys.all, 'detail', key] as const,
};
```

Mutations `onSuccess`'te `invalidateQueries({ queryKey: emailTemplatesKeys.all })` çağır.

### Navigasyon

Admin sidebar'a (mevcutsa) "Email Templates" linki ekle.
İkon: envelope / mail (lucide veya projedeki ikon kitaplığı).

### Doğrulama

- `npm run build` hatasız
- Strict TS, lint temiz
- Sayfa aç → liste gelsin → detay aç → preview çalışsın
- Permission testi: `email_templates:read` yok → `/403` redirect
- Monaco editor SSR hatası vermemeli (dynamic import kontrolü)

Kısıt: Monaco kurulu değilse `npm install @monaco-editor/react` kur.
Yoksa `react-hook-form` ve `zod` ile formu yaz, form kütüphanesi zaten kurulu
olabilir — Prompt 0 raporundan teyit et.
```

---

## Prompt 3 — RabbitMQ Yönetim Sayfası

```
elly-admin-panel'e "RabbitMQ Yönetimi" admin sayfası ekle. Bu sayfa, CMS'in
proxy'lediği RabbitMQ management API endpoint'lerini kullanır — panel doğrudan
:15672'ye bağlanmaz.

## Bağlam

CMS endpoint'leri:

| Method | Path | Permission | Açıklama |
|---|---|---|---|
| GET  | `/api/v1/admin/rabbit/overview` | `rabbit:read` | Broker özeti |
| GET  | `/api/v1/admin/rabbit/queues` | `rabbit:read` | Tüm queue'lar |
| GET  | `/api/v1/admin/rabbit/queues/{name}` | `rabbit:read` | Tek queue detay |
| GET  | `/api/v1/admin/rabbit/queues/{name}/messages?count=10` | `rabbit:read` | Peek |
| POST | `/api/v1/admin/rabbit/queues/{name}/purge` | `rabbit:manage` | Tümünü sil |
| POST | `/api/v1/admin/rabbit/queues/{name}/republish` | `rabbit:manage` | Yeniden publish |

Tipler `types/cms.ts`'de: `RabbitOverview`, `RabbitQueue`, `RabbitMessage`.

Republish body:
```json
{ "targetQueue": "email-queue", "payload": "...", "contentType": "application/json" }
```

Peek her zaman `ackmode=ack_requeue_true` ile yapılır (backend tarafında) —
mesajlar queue'dan silinmez, sadece görüntülenir.

## Görev

### Dosyalar

```
app/(admin)/admin/infrastructure/rabbitmq/
├── page.tsx                         # Dashboard
├── loading.tsx
└── _components/
    ├── OverviewCard.tsx
    ├── QueueTable.tsx
    ├── QueueDetailSheet.tsx         # shadcn Sheet (yandan kayar drawer)
    ├── MessageList.tsx
    ├── DestructiveConfirmDialog.tsx # Queue adı yaz-onayla
    └── RepublishDialog.tsx

lib/api/rabbit-admin.ts
lib/hooks/rabbit/
├── useRabbitOverview.ts
├── useRabbitQueues.ts
├── useQueueMessages.ts
└── useRabbitMutations.ts
```

### Özellikler

**Ana sayfa (`/admin/infrastructure/rabbitmq`)**
- Server Component, `requirePermission('rabbit:read')`
- `export const dynamic = 'force-dynamic'`
- 3 bölüm:
  1. Overview Card (versiyonlar, toplamlar)
  2. Queue tablosu
  3. Seçili queue drawer'ı (Sheet)
- `HydrationBoundary` ile prefetch (overview + queues)

**OverviewCard**
- shadcn Card
- 4 stat grid: Toplam Mesaj / Consumer / Queue / Exchange
- Başlık: "RabbitMQ {version} · {clusterName}"
- Auto-refresh 10 sn

**QueueTable**
- TanStack Table veya shadcn Table (hangisi mevcut)
- Kolonlar: Queue (mono), Ready, Unacked, Consumer, State (badge), Actions
- Satır click → QueueDetailSheet açılır
- Actions kolonu:
  - "Purge" butonu (destructive variant)
  - `disabled={!canManage || messages === 0}` (`usePermission('rabbit:manage')`)
  - Click → DestructiveConfirmDialog
- `refetchInterval: 5_000` (canlı görünüm)
- State "running" → default badge, "idle" → secondary badge

**QueueDetailSheet**
- shadcn Sheet, `side="right"`, `className="w-[640px]"`
- İçerik:
  - Queue adı (mono, büyük)
  - Key-value listesi: state, durable, messages, consumers
  - Arguments JSON pre'de göster (varsa)
  - MessageList bileşeni

**MessageList**
- "Son 10 mesajı göster" butonu (pattern: `enabled: false` → butona basınca `refetch()`)
- Her mesaj `<details>` içinde:
  - Summary: index + routingKey + redelivered badge
  - Expanded: payload (JSON formatlı pretty-print, try/catch), properties
- Max 10 mesaj, count parametresi buton'un yanında input olabilir

**DestructiveConfirmDialog (reusable)**
- Props: `expectedText, title, description, onConfirm`
- Input'a `expectedText` yazılana kadar "Onayla" butonu disabled
- "İptal" butonu her zaman aktif
- Dialog kapandığında input sıfırlanır

**RepublishDialog**
- Input: targetQueue (default: "email-queue"), payload (textarea, preview'den kopyalanabilir)
- Submit → `POST /republish` → toast

### TanStack Query

```typescript
export const rabbitKeys = {
  all: ['rabbit'] as const,
  overview: () => [...rabbitKeys.all, 'overview'] as const,
  queues: () => [...rabbitKeys.all, 'queues'] as const,
  queue: (name: string) => [...rabbitKeys.all, 'queue', name] as const,
  messages: (name: string) => [...rabbitKeys.all, 'messages', name] as const,
};
```

Mutations invalidate `rabbitKeys.all`.

### Edge Case'ler

- `state === null` → Badge "—"
- `messages === 0` → Purge disabled
- 503 BrokerUnavailable → toast + sayfa üstünde "RabbitMQ şu an erişilemez" alert
- `rabbit:read` var, `rabbit:manage` yok → tüm destructive butonlar disabled (tooltip: "Yetki yok")

### Navigasyon

Admin sidebar'a "Infrastructure" sub-menu ekle (yoksa):
- Infrastructure
  - RabbitMQ
  - (ileride: Redis, Postgres durumu)

İkon: database / server / activity (lucide).

### Doğrulama

- Permission yok → /403
- Queue listesi geliyor → drawer açılıyor → peek mesaj getiriyor
- Purge modal "queue adını yaz" UX'i çalışıyor
- `npm run build` hatasız
```

---

## Prompt 4 — Email Logs Sayfası (v3 retry için)

```
elly-admin-panel'e "Email Logs" admin sayfası ekle. Gönderilen/kuyrukta bekleyen/
başarısız mail kayıtlarını listeler, FAILED olanları tek tıkla retry eder.

## Bağlam

Auth: **JWT Bearer token** — `Authorization: Bearer <token>` header'ı yeterli.
`X-API-KEY` header'ı GEREKMEZ ve BEKLENMİYOR (backend'de kaldırıldı).

CMS endpoint'leri:

| Method | Path | Permission | Açıklama |
|---|---|---|---|
| GET  | `/api/v1/emails?status=&page=0&size=20` | `emails:read` | Paginated list, opsiyonel status |
| POST | `/api/v1/emails/{id}/retry` | `emails:retry` | FAILED/PENDING → PENDING + re-publish |
| GET  | `/api/v1/emails/templates` | `emails:read` | Classpath template listesi |

Tip `types/cms.ts`'de: `EmailLog`, `Page<T>`.

Retry kuralları:
- SENT retry edilemez → 400 VALIDATION_ERROR
- FAILED veya PENDING retry edilebilir
- retry başarılıysa status PENDING'e döner, retryCount=0, errorMessage=null

## Görev

### Dosyalar

```
app/(admin)/admin/email-logs/
├── page.tsx                         # Liste + filter
├── loading.tsx
└── _components/
    ├── EmailLogTable.tsx
    ├── StatusFilter.tsx             # "Tümü | PENDING | SENT | FAILED"
    ├── EmailLogDetailSheet.tsx      # Tam detay + error message
    └── RetryButton.tsx

lib/api/emails.ts
lib/hooks/emails/
├── useEmailLogs.ts                  # useQuery list (status + pagination)
└── useRetryEmail.ts                 # useMutation retry
```

### Özellikler

**Liste sayfası (`/admin/email-logs`)**
- Server Component, `requirePermission('emails:read')`
- URL search params: `?status=FAILED&page=0`
- Filter toggle group: Tümü / PENDING / SENT / FAILED
- Filtre değişince URL güncellenir (`router.push`), ilgili query refetch
- Tablo kolonları:
  - ID
  - Recipient (mail adresi)
  - Subject (truncated)
  - Template (badge)
  - Status (renkli badge: PENDING=yellow, SENT=green, FAILED=red)
  - Retry Count
  - Created (relative time — "3 dk önce")
  - Actions
- Satır click → EmailLogDetailSheet
- Actions:
  - "Retry" butonu (SENT için disabled)
  - `disabled={!canRetry || status === 'SENT'}`

**EmailLogDetailSheet**
- shadcn Sheet, right side
- İçerik:
  - Recipient, Subject, Template
  - Status badge (büyük)
  - Retry Count
  - Created At / Sent At (absolute + relative)
  - Error Message (varsa) — kırmızı box, mono font, wrap
  - "Retry" butonu (permission'a göre)

**RetryButton**
- Mutation: `POST /api/v1/emails/{id}/retry`
- `onSuccess` → toast "Mail yeniden kuyruğa alındı" + invalidate list
- `onError` (400) → toast "SENT mail retry edilemez"
- Loading state: "Retry ediliyor..."

**Pagination**
- Alt: "Sayfa 1/5 · Toplam 87 mail" + prev/next butonlar
- Veya shadcn Pagination bileşeni (mevcutsa)

### Query Keys

```typescript
export const emailLogsKeys = {
  all: ['email-logs'] as const,
  list: (params?: { status?: string; page?: number; size?: number }) =>
    [...emailLogsKeys.all, 'list', params] as const,
};
```

### Opsiyonel: Bulk Actions

Başlangıçta atla — ilerisinde çoklu seçim + "Retry Selected" eklenebilir.
Şimdi sadece satır-bazlı retry.

### Doğrulama

- FAILED filtresi → sadece failed kayıtlar
- Retry → toast + liste güncellenir (PENDING'e döner)
- SENT satırda retry butonu disabled
- Permission kontrolü çalışıyor
- Pagination URL'e yansıyor (refresh sonrası kaybolmasın)
```

---

## Prompt 5 — Mail Accounts Sayfası (SMTP profil CRUD, v2 DB-based)

**Ön koşul:** Prompt 1 tamamlandı. Bu endpoint'ler **CMS'te canlı** — bugün deploy edildi, test edebilirsin.

```
elly-admin-panel'e "Mail Accounts" admin sayfası ekle. Her tenant, kendi
veritabanında birden fazla SMTP hesabı (ör. info@, sales@, support@) tutabilir.
Bu sayfa onları CRUD eder + test/verify aksiyonları sağlar.

## Bağlam — v2 DB-based Mail Architecture

Mail+Form v2'de artık `application.properties`'te SMTP host/port/username/password
YOK. Her tenant kendi `mail_accounts` tablosunda kayıtlı hesaplarını yönetir.
`smtpPassword` alanı DB'de AES-256-CBC ile şifreli saklanır ve **API response'unda
hiçbir zaman dönmez** (write-only).

Form oluştururken admin bu listeden bir hesap seçer (`senderMailAccountId` FK).
Pasif hesaplar form'da sender olarak seçilemez.

## CMS Endpoint'leri

| Method | Path | Permission | Açıklama |
|---|---|---|---|
| GET    | `/api/v1/mail-accounts` | `mail:read` | Tüm hesaplar (array) |
| GET    | `/api/v1/mail-accounts/active` | `mail:read` | Sadece `active=true` — Form dropdown'unda kullanılır |
| GET    | `/api/v1/mail-accounts/{id}` | `mail:read` | Detay |
| POST   | `/api/v1/mail-accounts` | `mail:create` | Yeni hesap (201 Created) |
| PUT    | `/api/v1/mail-accounts/{id}` | `mail:update` | Güncelle (password boşsa korunur) |
| DELETE | `/api/v1/mail-accounts/{id}` | `mail:delete` | Sil |
| POST   | `/api/v1/mail-accounts/{id}/test` | `mail:create` | Body: `{testTo: string}` — gerçek mail gönderir |
| POST   | `/api/v1/mail-accounts/{id}/verify` | `mail:read` | Mail göndermeden SMTP bağlantısını test eder |

## Request/Response Tipleri

`types/cms.ts` dosyasına (yoksa) şu tipleri ekle:

```typescript
export interface MailAccount {
  id: number;
  name: string;               // Panel'de gösterilen ad (ör. "Satış Hesabı")
  fromAddress: string;        // From header adresi (sales@firma.com)
  smtpHost: string;           // smtp.gmail.com, smtp.office365.com vs.
  smtpPort: number;           // 587 (STARTTLS), 465 (SSL), 25 (plaintext)
  smtpUsername: string;       // genellikle email adresi
  active: boolean;
  createdAt: string;
  updatedAt: string;
  // NOT: smtpPassword response'ta YOK — sadece create/update isteğinde body'de
}

export interface MailAccountRequest {
  name: string;
  fromAddress: string;
  smtpHost: string;
  smtpPort: number;
  smtpUsername: string;
  smtpPassword?: string;  // Update'te boş/null ise mevcut şifre korunur
  active?: boolean;       // default: true
}

export interface MailTestRequest {
  testTo: string;  // Test mailinin gideceği hedef adres
}
```

## Görev

### Dosyalar

```
app/(admin)/admin/mail-accounts/
├── page.tsx                         # Liste sayfası
├── loading.tsx
├── new/
│   └── page.tsx                     # Oluşturma formu
├── [id]/
│   ├── page.tsx                     # Edit sayfası
│   └── _components/
│       ├── MailAccountForm.tsx      # Create + Edit ortak
│       ├── TestEmailDialog.tsx      # testTo alıp POST /test
│       └── VerifyConnectionButton.tsx
└── _components/
    ├── MailAccountListTable.tsx
    ├── ActiveBadge.tsx              # active=true → green, false → gray
    └── DeleteConfirmDialog.tsx

lib/api/mail-accounts.ts
lib/hooks/mail-accounts/
├── useMailAccounts.ts               # useQuery list (all)
├── useActiveMailAccounts.ts         # useQuery active only — form dropdown'u kullanır
├── useMailAccount.ts                # useQuery detail
└── useMailAccountMutations.ts       # create/update/delete/test/verify
```

### Özellikler

**Liste sayfası (`/admin/mail-accounts`)**
- Server Component, `requirePermission('mail:read')`
- Tablo kolonları:
  - Ad (name, bold)
  - From Adresi (fromAddress, mono font)
  - SMTP Host:Port (ör. `smtp.gmail.com:587`)
  - Kullanıcı Adı (smtpUsername)
  - Durum (ActiveBadge)
  - Güncelleme (relative time)
  - Actions
- Sağ üst: "Yeni Hesap" butonu (`mail:create` permission kontrolü)
- Her satırda:
  - Tıklanınca `/admin/mail-accounts/{id}` edit sayfasına gider
  - "Test Mail" butonu → TestEmailDialog açar
  - "Verify" butonu → quick SMTP ping (toast ile sonuç)
  - "Sil" butonu → DeleteConfirmDialog
- TanStack Query `staleTime: 60_000`
- Empty state: "Henüz mail hesabı yok. İlk hesabı eklemek için..."

**Oluşturma formu (`/new`)**
- react-hook-form + zod:
  ```typescript
  const schema = z.object({
    name: z.string().min(1).max(255),
    fromAddress: z.string().email(),
    smtpHost: z.string().min(1).max(255),
    smtpPort: z.number().int().min(1).max(65535),
    smtpUsername: z.string().min(1).max(255),
    smtpPassword: z.string().min(1, 'Yeni hesap için şifre zorunludur'),
    active: z.boolean().default(true),
  });
  ```
- Alan hint'leri:
  - SMTP Port yanında: "587 (STARTTLS, önerilen), 465 (SSL/TLS), 25 (plaintext)"
  - SMTP Username yanında: "Çoğu sağlayıcıda e-posta adresinizle aynı"
  - Gmail için küçük info bubble: "App Password kullanın (2FA gerekir, normal şifre çalışmaz)"
- "Kaydet" → `POST /api/v1/mail-accounts` → 201 → edit sayfasına redirect
- "Kaydet ve Test Et" → önce POST, sonra TestEmailDialog otomatik açılır

**Edit sayfası (`/[id]`)**
- URL'den `id` alır, `useMailAccount(id)` ile fetch
- Form doldurur; **smtpPassword alanı boş gelir** — placeholder: "Değiştirmek için yeni şifre yaz, boş bırakırsan mevcut korunur"
- Password zod schema update'te optional: `smtpPassword: z.string().optional()`
- Sağ üst actions: Test | Verify | Sil (destructive)
- Kaydet → `PUT /api/v1/mail-accounts/{id}`
- Boş password → body'den smtpPassword field'ını **çıkar** (undefined olsa bile JSON'da yok olsun)

**TestEmailDialog**
- shadcn Dialog
- Input: "Test maili gideceği adres" (email validation)
- "Gönder" butonu → `POST /api/v1/mail-accounts/{id}/test` body `{testTo}`
- Loading: "SMTP bağlantısı kuruluyor, mail gönderiliyor..." (5-10sn sürebilir)
- Success: toast "Test maili gönderildi → {testTo}"
- Error: toast error ile response mesajı (SMTP auth failure, port kapalı vs.)

**VerifyConnectionButton**
- Tek tıkla `POST /api/v1/mail-accounts/{id}/verify` (mail göndermez, sadece SMTP handshake)
- Loading state: spinner + "Doğrulanıyor..."
- Success: toast "SMTP bağlantısı başarılı"
- Error: toast error — response mesajı detaylı gösterilsin (kullanıcı şifresini düzeltsin)

**DeleteConfirmDialog**
- "Onaylamak için hesap adını yaz: **Satış Hesabı**"
- Input eşleşince "Sil" butonu aktif
- Delete sonrası toast + liste sayfasına redirect
- Uyarı mesajı: "Bu hesabı kullanan formlar 422 dönmeye başlayacak. Önce formları başka hesaba geçir."

### Query Keys

```typescript
export const mailAccountsKeys = {
  all: ['mail-accounts'] as const,
  list: () => [...mailAccountsKeys.all, 'list'] as const,
  active: () => [...mailAccountsKeys.all, 'active'] as const,
  detail: (id: number) => [...mailAccountsKeys.all, 'detail', id] as const,
};
```

Invalidation kuralları:
- Create/Update/Delete → `mailAccountsKeys.all` invalidate (hem list hem active tazelenir)
- Test/Verify → invalidate **etme** (hesap verisi değişmiyor)

### Güvenlik Notları

- **smtpPassword'u UI'da hiçbir zaman render etme** — response'ta zaten yok, ama yine de ekranda gösterilmesin
- Form validation öncesi `fromAddress` ve `smtpUsername`'in aynı domain olup olmadığını **sadece uyarı** olarak göster (hard block değil — Gmail relay gibi senaryolarda farklı olabilir)
- Permission yok → `/403` (requirePermission helper'ı yapsın)

### Doğrulama

- Create → 201 → edit sayfasına yönlenir, password alanı boş
- Update → password boş → 200, değişmez; password dolu → 200, günceller
- Active=false yapılan hesap, formlar sayfasındaki dropdown'da görünmez (Prompt 6 ile bağlantılı)
- Test button: gerçek mail gönderir (dikkat: test adrese gerçek mail gider)
- Verify button: mail göndermez, hızlı döner (< 5sn)
- `npm run build` hatasız
```

---

## Prompt 6 — Forms Sayfası (FormDefinition + Submissions + Mail+Form v4 — opsiyonel bildirim)

**Ön koşul:** Prompt 1 + **Prompt 5 tamamlanmalı** (form sender dropdown'u aktif mail hesaplarına bağlı). Bu endpoint'ler **CMS'te canlı**.

```
elly-admin-panel'e "Forms" admin sayfası ekle. FormDefinition CRUD + submission
görüntüleme. Mail+Form v4 ile bildirim **opsiyonel**: form submit edildiğinde
mail gönderilebilir veya tamamen devre dışı bırakılabilir. Buna göre form tanım
formu, bildirim alanlarını switch'e bağlı conditional required olarak göstermelidir.

## Bağlam — Mail+Form v4 Entegrasyonu (bildirim opsiyonel)

Form oluşturulurken bildirim alanları:

1. **notificationEnabled** (boolean, **default true**) — Mail gönderilsin mi?
   - `true` (varsayılan): submit sonrası bildirim maili tetiklenir → sender/recipient zorunlu.
   - `false`: form submit yine 200 döner, sadece DB'ye kaydedilir, mail tetiklenmez,
     sender/recipient null/boş kalabilir.

2. **senderMailAccountId** (Long | null) — Hangi MailAccount'tan gönderilecek.
   - `notificationEnabled=true` iken **zorunlu**, pasif hesap seçilirse 422.
   - `notificationEnabled=false` iken null bırakılabilir.

3. **recipientEmail** (string | null, max 1000) — Bildirim alıcı adres(ler).
   - `notificationEnabled=true` iken **zorunlu** ve format validate edilir.
   - **v3 itibarıyla çoklu alıcı destekli**: virgülle ayrılmış email listesi
     (`a@x.com, b@y.com`). Tek adres de olabilir.
   - `notificationEnabled=false` iken null/boş bırakılabilir.

4. **notificationSubject** (string | null, max 255) — Mail konusu.
   - Boş bırakılırsa backend default: `"Yeni form gonderimi: {form.title}"`.

**Backend dogrulamasi (FormDefinitionService.validateNotificationConfig):**
- `notificationEnabled` null gelirse default true atanır.
- Disabled iken: hiç validation yapılmaz.
- Enabled iken: sender atanmış ve aktif olmalı, recipient boş olmamalı,
  format `^email(,email)*$` ile uyumlu olmalı. Aksi halde 422.

Akış (enabled iken): Submit → FormSubmission kaydı + EmailLog(PENDING) +
RabbitMQ'ya job → EmailQueueService consumer → `senderMailAccount`'tan
AES-decrypt → Gmail SMTP. Disabled iken yalnızca FormSubmission kaydı.

## CMS Endpoint'leri — FormDefinition

| Method | Path | Permission | Açıklama |
|---|---|---|---|
| POST   | `/api/v1/forms` | `forms:create` | Yeni form tanımı |
| PUT    | `/api/v1/forms/{id}` | `forms:update` | Güncelle |
| GET    | `/api/v1/forms/{id}` | `forms:read` | Detay |
| GET    | `/api/v1/forms/list` | `forms:read` | Tüm formlar (array) |
| GET    | `/api/v1/forms/list/active` | `forms:read` | Sadece `active=true` |
| GET    | `/api/v1/forms/list/paged?page=0&size=10&sort=id,asc` | `forms:read` | Paginated |
| DELETE | `/api/v1/forms/{id}` | `forms:delete` | Sil |

## CMS Endpoint'leri — Submissions

| Method | Path | Permission | Açıklama |
|---|---|---|---|
| POST   | `/api/v1/forms/{formId}/submit` | `forms:create` | Test amaçlı panelden submit |
| GET    | `/api/v1/forms/{formId}/submissions` | `forms:read` | Tüm gönderimler |
| GET    | `/api/v1/forms/{formId}/submissions/paged?page&size&sort` | `forms:read` | Paginated |
| GET    | `/api/v1/forms/submissions/{submissionId}` | `forms:read` | Gönderim detayı |
| GET    | `/api/v1/forms/{formId}/submissions/count` | `forms:read` | Sayı |

## Request/Response Tipleri

`types/cms.ts`'e ekle (yoksa):

```typescript
// ===== FormSchema yapıları =====

export type ConditionOperator = 'EQUALS' | 'NOT_EQUALS' | 'GT' | 'LT';

export interface FormValidationRule {
  min?: number | null;
  max?: number | null;
  pattern?: string | null;  // regex
}

export interface FormConditionRule {
  field: string;             // başka bir field.id
  operator: ConditionOperator;
  value: unknown;
}

export interface FormFieldOption {
  label: string;
  value: unknown;
}

export interface FormFieldDefinition {
  id: string;                // unique field ID
  stepId?: string | null;    // multi-step form için step'e bağlanır
  type: string;              // 'text' | 'email' | 'number' | 'select' | 'radio' | 'checkbox' | 'textarea' | ...
  label: string;
  required?: boolean;
  validation?: FormValidationRule | null;
  condition?: FormConditionRule | null;  // conditional visibility
  options?: FormFieldOption[];           // select/radio/checkbox için
}

export interface FormStepDefinition {
  id: string;
  title: string;
  description?: string;
}

export interface FormSchema {
  config?: Record<string, unknown>;  // layout, styling, vs.
  fields: FormFieldDefinition[];
  steps?: FormStepDefinition[];      // wizard form'lar için
}

// ===== FormDefinition =====

export interface FormDefinition {
  id: number;
  title: string;
  version: number | null;
  schema: FormSchema;
  active: boolean;

  // Mail+Form v4 alanları — bildirim opsiyonel
  senderMailAccountId: number | null;       // notificationEnabled=false iken null olabilir
  senderMailAccountName: string | null;     // readonly — UI display
  senderFromAddress: string | null;          // readonly — UI display
  recipientEmail: string | null;             // virgülle ayrılmış email listesi de olabilir
  notificationSubject: string | null;
  notificationEnabled: boolean;

  createdAt: string;
  updatedAt: string;
}

export interface FormDefinitionRequest {
  title: string;
  version?: number;
  schema: FormSchema;
  active?: boolean;
  // Mail+Form v4: senderMailAccountId ve recipientEmail conditional required —
  // notificationEnabled=true (default) iken zorunlu, false iken opsiyonel.
  senderMailAccountId?: number | null;
  recipientEmail?: string | null;            // tek adres veya "a@x.com, b@y.com"
  notificationSubject?: string;
  notificationEnabled?: boolean;             // default true
}

// ===== FormSubmission =====

export interface FormSubmission {
  id: number;
  formDefinitionId: number;
  formTitle: string;
  payload: Record<string, unknown>;  // kullanıcı cevapları
  submittedAt: string;
  createdAt: string;
}
```

## Görev

### Dosyalar

```
app/(admin)/admin/forms/
├── page.tsx                         # Liste sayfası (paginated)
├── loading.tsx
├── new/
│   └── page.tsx                     # Yeni form
├── [id]/
│   ├── page.tsx                     # Edit + Tab'li görünüm (Tanım | Gönderimler)
│   └── _components/
│       ├── FormDefinitionForm.tsx   # Create + Edit ortak
│       ├── NotificationSection.tsx  # sender/recipient/subject/enabled bloğu
│       ├── SenderMailAccountSelect.tsx  # /mail-accounts/active'den dropdown
│       ├── SchemaEditor.tsx         # Monaco JSON editor + validate
│       ├── SchemaPreview.tsx        # Alan sayısı özeti
│       ├── SubmissionsTab.tsx
│       ├── SubmissionDetailSheet.tsx
│       └── TestSubmitDialog.tsx     # Admin'in test submit yapması için
└── _components/
    ├── FormListTable.tsx
    └── DeleteConfirmDialog.tsx

lib/api/forms.ts
lib/hooks/forms/
├── useForms.ts                      # paged list
├── useForm.ts                       # detay
├── useFormMutations.ts              # create/update/delete
├── useFormSubmissions.ts            # paged submissions
└── useTestSubmit.ts                 # test amaçlı submit
```

### Özellikler

**Liste sayfası (`/admin/forms`)**
- Server Component, `requirePermission('forms:read')`
- URL search params: `?page=0&size=10&sort=id,asc`
- Tablo kolonları:
  - ID
  - Başlık (title)
  - Versiyon
  - Bildirim (notificationEnabled → "Açık/Kapalı" badge — Kapalı ise muted/gri)
  - Gönderici (senderMailAccountName veya `—` ; bildirim kapalıysa muted)
  - Alıcı (recipientEmail mono font; çoklu adres varsa "a@x.com +2" şeklinde
    kısalt, hover'da tooltip ile tam liste; null ise `—`)
  - Durum (active badge)
  - Güncelleme
  - Actions: edit | sil
- Sağ üst: "Yeni Form" butonu
- `useForms` hook'u `/list/paged` endpoint'ini çağırır
- Empty state: "Henüz form yok"

**Oluşturma formu (`/new`)**
- react-hook-form + zod (v4 — conditional required):

  ```typescript
  // Tek/çoklu email: "a@b.com" veya "a@b.com, c@d.com"
  const RECIPIENT_REGEX =
    /^\s*[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}(\s*,\s*[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})*\s*$/;

  const schema = z.object({
    title: z.string().min(1).max(255),
    version: z.number().int().optional(),
    schema: z.object({
      config: z.record(z.unknown()).optional(),
      fields: z.array(z.object({
        id: z.string().min(1),
        type: z.string().min(1),
        label: z.string().min(1),
        required: z.boolean().optional(),
        // ...
      })).min(1, 'En az 1 alan olmalı'),
      steps: z.array(z.object({ id: z.string(), title: z.string() })).optional(),
    }),
    active: z.boolean().default(true),
    // Conditional required — superRefine içinde notificationEnabled'a göre kontrol
    senderMailAccountId: z.number().int().positive().nullable().optional(),
    recipientEmail: z.string().max(1000).nullable().optional(),
    notificationSubject: z.string().max(255).optional(),
    notificationEnabled: z.boolean().default(true),
  }).superRefine((data, ctx) => {
    // notificationEnabled undefined ise default true kabul et
    const enabled = data.notificationEnabled !== false;
    if (!enabled) return; // Bildirim kapalı — sender/recipient opsiyonel.

    if (!data.senderMailAccountId) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['senderMailAccountId'],
        message: 'Bildirim açıkken bir gönderici seç',
      });
    }
    const recipient = data.recipientEmail?.trim() ?? '';
    if (!recipient) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['recipientEmail'],
        message: 'Bildirim açıkken alıcı zorunludur',
      });
    } else if (!RECIPIENT_REGEX.test(recipient)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['recipientEmail'],
        message: 'Geçersiz e-posta formatı. Birden fazla için virgülle ayır.',
      });
    }
  });
  ```

**FormDefinitionForm layout** — iki sütun:

Sol sütun (form tanımı):
- Başlık
- Versiyon (opsiyonel)
- Aktif toggle
- SchemaEditor (Monaco JSON, yükseklik 500px)

Sağ sütun — **NotificationSection** (kart içinde, "Bildirim Ayarları" başlığı):

- **`notificationEnabled` switch en üstte, default ON** — diğer alanların
  enabled/disabled durumunu kontrol eder.
  - OFF durumunda kart muted background ve içerideki sender/recipient/subject
    inputları **disabled** olur (değerler korunur, sadece düzenlenemez).
    Üstte info banner: "ℹ Bildirim kapalı — submit sonrası mail gönderilmeyecek,
    yalnızca FormSubmission DB'ye kaydedilecek. Bu alanlar opsiyonel."
  - ON durumunda alanlar required işaretiyle (kırmızı yıldız) gösterilir.

- `SenderMailAccountSelect` — `useActiveMailAccounts()` hook'undan dropdown
  - Opsiyonlar: `{id} • {name} ({fromAddress})`
  - **Yalnızca notificationEnabled=true iken** validation aktif. False iken
    placeholder "—" gösterilir, seçim opsiyoneldir.
  - Eğer notificationEnabled=true ve hiç aktif hesap yoksa empty state:
    > ⚠ "Bildirim göndermek için önce aktif bir Mail Account oluştur."
    > [Yeni Mail Account Oluştur] butonu → `/admin/mail-accounts/new`
    > veya alternatif: "Bildirimi devre dışı bırak" → switch'i OFF yapar
  - Seçim değişince UI altında preview: "Bu hesaptan gönderilecek: sales@firma.com"

- `recipientEmail` — text input, **çoklu adres destekli**
  - Placeholder: `notifications@firma.com, alt@firma.com`
  - Helper text: "Birden fazla adres için araya virgül koy (a@x.com, b@y.com)"
  - notificationEnabled=true iken zorunlu; format zod superRefine içinde kontrol
  - notificationEnabled=false iken disabled, boş bırakılabilir
  - Görsel ipucu: alanın altında badge ile "Tahmini alıcı sayısı: 3" göster
    (virgüle göre split edip count) — kullanıcının yanlışlıkla noktalı virgül
    kullanmasını fark etmesine yardımcı olur

- `notificationSubject` — text input (her durumda opsiyonel)
  - Placeholder: `"Yeni form gönderimi: {title}"` (literal)
  - Helper text: "Boş bırakırsan default kullanılır: Yeni form gönderimi: [form başlığı]"

**SchemaEditor**
- Monaco JSON mode, dynamic import (`ssr: false`)
- "Format" butonu (Shift+Alt+F trigger)
- Validate butonu → zod ile schema parse, hataları alta listele
- Sağ altta: "Alan sayısı: 5, Step sayısı: 2, Zorunlu: 3" sayaçları
- **Visual form builder v5'e ertelendi** — şimdi raw JSON editor yeterli
- Sample JSON (New sayfası default):
  ```json
  {
    "config": {},
    "fields": [
      {
        "id": "fullName",
        "type": "text",
        "label": "Ad Soyad",
        "required": true,
        "validation": { "min": 2, "max": 100 }
      },
      {
        "id": "email",
        "type": "email",
        "label": "E-posta",
        "required": true
      },
      {
        "id": "country",
        "type": "select",
        "label": "Ülke",
        "required": true,
        "options": [
          { "label": "Türkiye", "value": "TR" },
          { "label": "Almanya", "value": "DE" }
        ]
      }
    ],
    "steps": []
  }
  ```

**Edit sayfası (`/[id]`)** — Tab'li yapı

- Tab 1: **Tanım** → `FormDefinitionForm` (aynı create form'u)
- Tab 2: **Gönderimler** → `SubmissionsTab`
- Tab 3: **Test** → `TestSubmitDialog` tetikleyicisi

**SubmissionsTab**
- Server Component, paged list `/api/v1/forms/{formId}/submissions/paged`
- Üstte sayaç: `useQuery('submissions-count', formId)` → "Toplam 87 gönderim"
- Tablo:
  - ID | Gönderim Zamanı (relative + absolute tooltip) | Payload özet (ilk 3 field) | Actions: detay
- Satır tıklanınca `SubmissionDetailSheet` açılır

**SubmissionDetailSheet**
- shadcn Sheet, right side (büyük — md:max-w-2xl)
- İçerik:
  - Başlık: `{formTitle} - Gönderim #{id}`
  - Gönderim zamanı (absolute + relative)
  - **Payload tablosu** — field.label → value (schema'dan label'ı eşle)
    - type === 'checkbox' ise virgül ile birleştir
    - null/empty → "—"
  - Alt: raw JSON göster/gizle toggle

**TestSubmitDialog**
- Admin panelinden test amaçlı form submit — gerçek bildirim maili tetikler
- Form schema'dan dinamik alan üret:
  - type === 'text' → Input
  - type === 'email' → Input email
  - type === 'number' → Input number
  - type === 'select' → Select with options
  - type === 'textarea' → Textarea
  - type === 'checkbox' → Checkbox group
  - required alanlar kırmızı yıldızlı
- "Test Submit" butonu → `POST /api/v1/forms/{formId}/submit` body `{payload: {...}}`
- Success: toast "Gönderildi. Bildirim maili kuyrukta — Email Logs'tan takip edebilirsin"
- Modal içinde "Email Logs'a git →" link'i (`/admin/email-logs?status=PENDING`)
- Uyarı: "Bu gerçek bir mail tetikler — `recipientEmail` adresine mail gider."

**DeleteConfirmDialog**
- "Form silindiğinde mevcut gönderimler de silinir (cascade)"
- "Onaylamak için form başlığını yaz: **{title}**"

### Query Keys

```typescript
export const formsKeys = {
  all: ['forms'] as const,
  lists: () => [...formsKeys.all, 'list'] as const,
  paged: (params: { page: number; size: number; sort: string }) =>
    [...formsKeys.lists(), 'paged', params] as const,
  active: () => [...formsKeys.all, 'active'] as const,
  detail: (id: number) => [...formsKeys.all, 'detail', id] as const,
  submissions: (formId: number) => [...formsKeys.all, 'submissions', formId] as const,
  submissionsPaged: (formId: number, params: unknown) =>
    [...formsKeys.submissions(formId), 'paged', params] as const,
  submissionDetail: (submissionId: number) =>
    [...formsKeys.all, 'submission', submissionId] as const,
  submissionsCount: (formId: number) =>
    [...formsKeys.submissions(formId), 'count'] as const,
};
```

### Önemli UX kuralları

1. **SenderMailAccountSelect** her zaman `/mail-accounts/active` kullansın
   (tüm liste değil). Pasif hesaplar dropdown'da görünmesin.

2. **notificationEnabled switch'i tek karar noktası** —
   bildirim alanlarının zorunluluğunu kontrol eder:
   - ON (default): senderMailAccountId + recipientEmail required, format validate
   - OFF: ikisi de opsiyonel; inputlar disabled görünür (değer korunur ki
     kullanıcı tekrar ON yaparsa veriyi kaybetmesin)
   - Backend de aynı semantik: `notificationEnabled=false` iken DB'de null kabul edilir.

3. **Çoklu alıcı gösterimi** — `recipientEmail` virgülle ayrılmış string olarak
   tutuluyor. Liste tablosunda çok adres varsa "a@x.com **+2**" şeklinde kısalt
   ve hover'da tooltip ile tam liste göster. Form input'unda comma-split badge ile
   tahmini alıcı sayısı gösterilebilir.

4. **422 Unprocessable Entity** olası sebepler ve toast mesajları:
   - "notificationEnabled=true ise senderMailAccountId zorunludur" → "Gönderici seç veya bildirimi kapat"
   - "Secilen mail hesabi aktif degil" → "Hesap pasif, başka birini seç veya aktifleştir" + edit link
   - "notificationEnabled=true ise recipientEmail zorunludur" → "Alıcı zorunlu veya bildirimi kapat"
   - "Gecersiz e-posta formati" → "Format hatası — `a@b.com, c@d.com` şeklinde gir"

5. **Optimistic invalidation:** Form update sonrası `formsKeys.all` ve
   `formsKeys.detail(id)` invalidate; submit sonrası `submissions` ve
   `submissionsCount` invalidate.

### Doğrulama

- **notificationEnabled=true (default) + sender/recipient dolu** → form oluşur,
  submit'te EmailLog PENDING'e düşer, Email Logs sayfasında görünür
- **notificationEnabled=false + sender/recipient boş** → form oluşur (200),
  submit edildiğinde **EmailLog oluşmaz**, sadece FormSubmission kaydı atar
- **notificationEnabled=true ama sender boş** → 422 toast "Gönderici seç veya bildirimi kapat"
- **Çoklu alıcı (`a@x.com, b@y.com`)** → form oluşur, submit'te tek EmailLog
  oluşur ama mail her iki adrese gider (backend `helper.setTo(addresses[])`)
- **Pasif hesap seçildi** → 422 + UX yönlendirme link'i
- **notificationEnabled toggle ON→OFF→ON** → input değerleri korunur (sadece disabled görünüm)
- Aktif mail hesabı yokken bildirim açık form oluşturma → empty-state + yönlendirme
- Submissions tab → sayaç ve tablo eş zamanlı gelir
- Permission 403 → /403 redirect
- `npm run build` hatasız
```

---

## Prompt'ları Kullanırken İpuçları

1. **Prompt 0'ı mutlaka ilk çalıştır** — agent'ın stack'i bilmeden yazdığı
   kod çoğunlukla yanlış olur (MUI projeye shadcn eklemek vb.).

2. **Her prompt'tan sonra `npm run build` çalıştır** — bir sonraki prompt
   hatalı hale başlamasın.

3. **Permission seed'i unutma:** CMS DB'sinde bu permission'lar SUPER_ADMIN
   rolüne bağlı olmalı. Aksi halde panel kullanıcısı "403" görür ve agent
   kodunun hatası sanabilir. Gerekli SQL:

   ```sql
   INSERT INTO permissions (name) VALUES
     -- v4 templates
     ('email_templates:read'), ('email_templates:manage'),
     -- v3 retry + list
     ('emails:read'), ('emails:retry'),
     -- RabbitMQ admin
     ('rabbit:read'), ('rabbit:manage'),
     -- Mail accounts (v2 DB-based) — Prompt 5
     ('mail:create'), ('mail:read'), ('mail:update'), ('mail:delete'),
     -- Forms + submissions — Prompt 6
     ('forms:create'), ('forms:read'), ('forms:update'), ('forms:delete')
   ON CONFLICT (name) DO NOTHING;

   INSERT INTO role_permissions (role_id, permission_id)
   SELECT r.id, p.id
   FROM roles r
   CROSS JOIN permissions p
   WHERE r.name = 'SUPER_ADMIN'
     AND p.name IN (
       'email_templates:read', 'email_templates:manage',
       'emails:read', 'emails:retry',
       'rabbit:read', 'rabbit:manage',
       'mail:create', 'mail:read', 'mail:update', 'mail:delete',
       'forms:create', 'forms:read', 'forms:update', 'forms:delete'
     )
   ON CONFLICT DO NOTHING;
   ```

   Her tenant DB'sinde bu migration'ı çalıştır.

4. **Backend hazır — tüm endpoint'ler canlı (2026-04-23 itibarıyla):**
   - Prompt 2 — Email Templates (`email_templates:read`, `email_templates:manage`) — v4 DB-hosted, 3 seed template (`form-notification`, `welcome`, `password-reset`) bootstrap runner ile idempotent gelir
   - Prompt 3 — RabbitMQ yönetim (`rabbit:read`, `rabbit:manage`)
   - Prompt 4 — Email Logs (`emails:read`, `emails:retry`) — auth: JWT Bearer only, **`X-API-KEY` header'ı yok** (legacy `ApiKeyFilter` kaldırıldı)
   - Prompt 5 — Mail Accounts (`mail:read/create/update/delete`)
   - Prompt 6 — Forms (`forms:read/create/update/delete`)

   Panel agent'ı 404 ile karşılaşırsa path'i kontrol et (`/api/v1/...` prefix'i şart) — backend eksikliği değil, path yazım hatası ihtimali yüksek.

5. **Stack uyuşmazlığı:** Projen shadcn/ui değil MUI kullanıyorsa, Prompt
   sonuna şunu ekle: *"Yukarıdaki örnek kodlar shadcn/ui varsayımıyla yazıldı.
   Projemiz MUI kullanıyor, aynı feature'ı MUI bileşenleri (Dialog, Drawer,
   Table, DataGrid) ile implement et."*

6. **Agent'ın çıktısını review et:** AI'lar bazen endpoint path'ini uyduruyor
   — `/api/emails` vs `/api/v1/emails` gibi. Bu dokümandaki path'lerin
   birebir aynı olduğunu kontrol et.

---

## Sonraki v5 — Planlanan (henüz CMS'te yok)

Aşağıdakiler **şu an API'de yok**, sadece fikir — eğer panel tarafında da
ileride yapmak istersen CMS'te endpoint açılması gerekir:

- **Bulk requeue:** `POST /api/v1/admin/rabbit/queues/{source}/requeue-all?target={target}`
  (şimdi peek + tek tek republish ile N çağrı yapılıyor)
- **Template versiyonlama:** `email_template_revisions` tablosu + "geri al" UI
- **Template A/B testing:** aynı `key` için 2 variant + weighted routing
- **Audit log:** kim ne zaman hangi template'i/queue'yu değiştirdi

Bu iterasyonlarda CMS'e önce endpoint ekleyip, sonra bu doküman stilinde
yeni bir prompt yaz.

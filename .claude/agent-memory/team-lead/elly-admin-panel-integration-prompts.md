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

**Ön koşul:** Prompt 1 tamamlandı (http client + types hazır). CMS tarafında v4
endpoint'leri deploy edildi (backend henüz yazılmadıysa bu prompt'u bekleyebilirsin).

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
     ('email_templates:read'), ('email_templates:manage'),
     ('emails:retry'),
     ('rabbit:read'), ('rabbit:manage')
   ON CONFLICT (name) DO NOTHING;

   INSERT INTO role_permissions (role_id, permission_id)
   SELECT r.id, p.id
   FROM roles r
   CROSS JOIN permissions p
   WHERE r.name = 'SUPER_ADMIN'
     AND p.name IN (
       'email_templates:read', 'email_templates:manage',
       'emails:retry', 'rabbit:read', 'rabbit:manage'
     )
   ON CONFLICT DO NOTHING;
   ```

   Her tenant DB'sinde bu migration'ı çalıştır.

4. **v4 backend henüz yazılmadıysa:** Prompt 2 (Email Templates) çalışmaz —
   endpoint 404 döner. Önce elly repo'sunda v4 endpoint'lerini yazdırmalısın.
   Prompt 3 (RabbitMQ) ve Prompt 4 (Email Logs) **hazır** — bugün deploy
   edildi, çalışır.

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

# v4 Mail Mimarisi — Template Hosting Kararı

> Kullanıcı isteği: "elly-admin-panel projesinde template hazır edip buradan POST ile bilgileri elly-admin-panel'e gönderelim response'ta html döneyim buradan mail gönderimini yapalım." CMS'in yük almasını istemiyor — şu an sadece API barındırıyor.

## TL;DR — Karar

**Terimler:**
- **"CMS"** = bu elly repo'su (Spring Boot API). CLAUDE.md'de de "multi-tenant CMS" diye geçiyor.
- **"Panel"** = elly-admin-panel (Next.js UI). CMS'i tüketen yönetim arayüzü.
- **"CMS hafif kalsın"** = kullanıcının sözü: *"bu proje sadece api barındırıyor ve ağırlaşmasını yük olmasını istemiyorum."* API projesi yeni sorumluluk almasın.

**3 seçenek değerlendirildi:**

| # | Yaklaşım | Kim Önerdi | Sonuç |
|---|---|---|---|
| A | Her mailde CMS → Panel HTTP render → HTML → CMS SMTP | Kullanıcı | ❌ Reddedildi |
| B | Panel S3/Git'e push, CMS pull | Ben (alternatif) | ⚠️ Olur ama ekstra altyapı |
| **C** | **Template'ler CMS DB'sinde, Panel sadece CRUD UI** | **Ben (öneri)** | **✅ Seçildi** |

**A neden reddedildi:**
1. Her mailde network hop (latency + consumer başına HTTP çağrısı)
2. Panel down → mail gönderimi durur (tek arıza noktası)
3. Yük CMS'ten panel'e taşır, azaltmaz
4. Service-to-service auth (mTLS/JWT + rate limit) — kaçmak istenen karmaşıklık

**C neden "hafif kalmayı" karşılıyor:**
- CMS runtime'da panel'e HTTP çağrısı atmaz (cross-service latency = 0)
- CMS'e eklenen: 1 tablo + 1 servis. PostgreSQL + Redis + Thymeleaf zaten var → **yeni teknoloji katmanı yok**
- Template dosyası repo'dan çıkar (asıl hedef); panel admin UI'dan düzenlenir, DB'ye yazılır
- CMS DB'den (Redis cache'li) okur, Thymeleaf ile yerel render eder → p99 latency aynı kalır

Detay analiz aşağıda.

---

---

## Tarihçe

- **v1/v2:** Thymeleaf template'leri CMS içinde, `src/main/resources/templates/emails/*.html` altında. 3 template var: `welcome`, `notification`, `form-notification`.
- **Sorun:** Template'i değiştirmek için kod deploy gerekiyor. İçerik yönetimi geliştirici işi, operasyon/pazarlama ekibi kendi başına düzenleyemiyor.
- **v3:** Retry endpoint eklendi (bu iterasyon). Template işine dokunulmadı.

---

## Tasarım Seçenekleri

Kullanıcının önerisi (A) yanında iki alternatifi de masaya yatırıyoruz çünkü "her mail'de network hop" ve "tek arıza noktası" problemleri çözülebilir.

### Opsiyon A — Panel On-Demand Render (kullanıcının önerisi)

```
elly CMS ─────────► elly-admin-panel ─────► (Thymeleaf/handlebars) ─────► HTML
    ▲                   │
    └─── SMTP ◄─── HTML response
```

**Akış:**
1. `EmailQueueService` consumer (RabbitMQ'da) mail işlerken `POST https://panel.../api/render` yapar.
2. Panel (Next.js / Nuxt) template'i render eder, HTML döner.
3. CMS HTML'i `JavaMailSender` ile gönderir.

**Artı:**
- Template'ler panel repo'sunda yaşar; tek kaynak.
- CMS içinde template dosyası/servis yok — kullanıcının istediği hafiflik.
- Panel ekibi template'i değiştirir, CMS deploy olmaz.

**Eksi:**
- **Her mailde network hop.** p99 latency artar, retry senaryoları karmaşıklaşır (hem SMTP hem panel tarafında hata olabilir).
- **Tek arıza noktası.** Panel down olursa CMS mail gönderemez. Consumer queue birikir.
- **Sıkı coupling.** CMS service-to-service auth (mTLS/JWT-signed) + rate limit + timeout çemberi kurmak zorunda. Bu tam da kullanıcının "CMS hafif kalsın" hedefine ters.
- Panel Next.js/SSR ise render maliyeti (cold start, memory) CMS'i değil panel'i ağırlaştırır — kullanıcı yükü bir yerden bir yere taşımış olur.

### Opsiyon B — Panel Push to CDN / Git (hafif sync)

```
elly-admin-panel ──► PUT s3://templates/welcome.html (or git push)
                                   │
                                   ▼
                     elly CMS ──► önyüklemede + TTL cache ile pull
                                   │
                                   ▼
                      Thymeleaf local render ──► SMTP
```

**Akış:**
1. Panel UI'da template düzenleyen kullanıcı kaydet der → panel S3'e (veya git repo'ya) push eder.
2. CMS S3'ü 5 dk cache ile okur (veya git webhook ile invalidate).
3. Mail gönderirken CMS yerelinden okur, Thymeleaf ile render eder.

**Artı:**
- Runtime'da cross-service call **yok**.
- CMS down scenario'su yok.
- CDN edge cache'lenebilir.

**Eksi:**
- Yeni altyapı (S3 bucket + IAM + sync job) gerekir.
- Template değişikliği ~5 dk gecikir (cache TTL).
- Preview/staging template'leri için ek logic.

### Opsiyon C — Template Registry CMS DB'de, Panel CRUD (**ÖNERİ**)

```
elly-admin-panel ──HTTP──► POST /api/v1/email-templates (CMS)
                                    │
                                    ▼
                         PostgreSQL email_templates tablosu
                                    │
                                    ▼ (Redis cache, TTL 10dk)
                         Thymeleaf engine ──► render ──► SMTP
```

**Akış:**
1. Panel UI'da admin `welcome` template'ini düzenler → `PUT /api/v1/email-templates/welcome` çağırır.
2. CMS DB'ye yazar, `@CacheEvict("emailTemplates")` ile cache temizler.
3. `EmailQueueService` consumer render anında `templateRegistry.load("welcome")` ile DB'den çeker (Redis cache), Thymeleaf ile render eder.
4. Classpath fallback: DB'de yoksa `templates/emails/{name}.html` kullanılır (eski davranış).

**Artı:**
- CMS runtime cross-service call yapmaz → v2 performans profiline aynı kalır.
- Panel sadece CRUD UI sağlar; render mantığı CMS'in sorumluluğunda (zaten Thymeleaf entegre).
- Redis cache ile DB round-trip sadece cache miss'de.
- Multi-tenant template destek — her tenant kendi override'ını tutabilir (tenant_id kolon).
- Classpath fallback ile **zero-downtime migration**: mevcut 3 template çalışmaya devam eder, DB'de override edilene kadar.
- Template versiyonlama (`email_template_revisions` tablosu) kolay eklenir.
- Panel "bu template'i kullananlar" listesi için CMS'ten `GET /email-templates/{key}/usage` endpoint'i sorabilir.

**Eksi:**
- Template içeriği CMS DB'sinde yaşar (ama bu sadece text, CMS iş mantığı değil — CMS'e yük binmez).
- Panel template düzenleme UI'sini yazmak zorunda (ama bu her opsiyon için gerekli).

---

## Karar: **Opsiyon C (Template Registry)**

Neden A değil: Her mailde network hop + tek arıza noktası + cross-service auth ekleme — kullanıcının hafiflik hedefine ters. Yükü CMS'ten panel'e taşımak yük azaltmaz, sadece yer değiştirir.

Neden B değil: Yeni altyapı (S3/IAM) gerekiyor, gelecekte template versiyonlama/audit için ekstra iş. CMS'imizde zaten PostgreSQL + Redis var, onu kullanalım.

---

## Uygulama Planı (v4 Iteration)

### Backend (elly CMS)

**1. Yeni Entity: `EmailTemplate`**

```java
@Entity
@Table(name = "email_templates", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "template_key"}))
public class EmailTemplate extends BaseEntity {
    @Column(name = "tenant_id", nullable = true)  // null = global (tüm tenantlar için)
    private String tenantId;

    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;    // "welcome", "form-notification", vs.

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;        // Thymeleaf expression: "Hoşgeldin [[${userName}]]"

    @Column(name = "html_body", nullable = false, columnDefinition = "TEXT")
    private String htmlBody;       // Tam Thymeleaf template

    @Column(name = "description", length = 500)
    private String description;    // Admin UI için

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "version", nullable = false)
    private Integer version;       // Optimistic lock / audit

    @Version
    private Long optimisticLockVersion;
}
```

**2. Yeni Servisler**

- `IEmailTemplateService` — CRUD + `loadByKey(key)` (tenant-aware, cacheable)
- `EmailTemplateRenderer` — Thymeleaf wrapper, önce DB'den çeker, yoksa classpath fallback
- Mevcut `EmailQueueService` değişir: `templateEngine.process(...)` yerine `templateRenderer.render(key, model)`

**3. Yeni Endpoint'ler**

| Method | Path | Permission | Açıklama |
|---|---|---|---|
| GET | `/api/v1/email-templates` | `email_templates:read` | Liste (paginated, tenant filtered) |
| GET | `/api/v1/email-templates/{key}` | `email_templates:read` | Detay |
| POST | `/api/v1/email-templates` | `email_templates:manage` | Oluştur |
| PUT | `/api/v1/email-templates/{key}` | `email_templates:manage` | Güncelle |
| DELETE | `/api/v1/email-templates/{key}` | `email_templates:manage` | Sil (soft delete) |
| POST | `/api/v1/email-templates/{key}/preview` | `email_templates:read` | Dummy data ile render preview döndür |

**4. Permission'lar**

`PermissionConstants.java`'ya ekle:
```java
public static final String EMAIL_TEMPLATES_READ = "email_templates:read";
public static final String EMAIL_TEMPLATES_MANAGE = "email_templates:manage";
```

**5. Cache Stratejisi**

- `@Cacheable(value = "emailTemplates", key = "#tenantId + '::' + #key")` — TTL 10dk.
- Tenant-aware prefix'i Elly'nin mevcut Redis config'i otomatik halleder.
- Güncelleme/silme: `@CacheEvict(value = "emailTemplates", allEntries = true)`.

**6. Migration**

```sql
CREATE TABLE email_templates (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64),
    template_key VARCHAR(100) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    html_body TEXT NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    optimistic_lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_email_templates_tenant_key UNIQUE (tenant_id, template_key)
);

CREATE INDEX idx_email_templates_key ON email_templates(template_key);
CREATE INDEX idx_email_templates_active ON email_templates(active);
```

**7. Bootstrap / Seed**

İlk deploy'da classpath'taki 3 template'i global (tenant_id=NULL) olarak insert et. Script:

```java
@Component
@RequiredArgsConstructor
public class EmailTemplateBootstrapRunner implements ApplicationRunner {
    private final IEmailTemplateService service;

    @Override
    public void run(ApplicationArguments args) {
        seed("welcome", "Hoşgeldin", classpathLoad("welcome.html"));
        seed("notification", "Bildirim", classpathLoad("notification.html"));
        seed("form-notification", "Yeni Form Gönderimi", classpathLoad("form-notification.html"));
    }

    private void seed(String key, String subject, String body) {
        if (!service.existsByKey(null, key)) {
            service.createGlobal(key, subject, body, "Classpath'ten seed edildi.");
        }
    }
}
```

### Frontend — elly-admin-panel (Next.js App Router)

> **Varsayılan stack:** Next.js 14+ (App Router), TypeScript, TanStack Query v5, shadcn/ui (Radix + Tailwind), react-hook-form + zod, Monaco Editor (`@monaco-editor/react`), sonner (toast). Farklı bir stack (Pages Router, SWR, MUI vb.) kullanılıyorsa bu örnekler kolay adapte edilir.

#### Dosya Yapısı

```
elly-admin-panel/
├─ app/
│  └─ (admin)/
│     └─ admin/
│        └─ email-templates/
│           ├─ page.tsx                         # Liste (Server Component)
│           ├─ loading.tsx                      # Skeleton (Suspense boundary)
│           ├─ new/
│           │  └─ page.tsx                      # Yeni template formu
│           ├─ [key]/
│           │  ├─ page.tsx                      # Edit sayfası
│           │  └─ _components/
│           │     ├─ TemplateForm.tsx           # Client, react-hook-form
│           │     ├─ MonacoBodyEditor.tsx       # Client, dynamic import
│           │     └─ PreviewPanel.tsx           # Client, iframe srcDoc
│           └─ _components/
│              ├─ TemplateListTable.tsx         # Client, TanStack Table
│              └─ DeleteConfirmDialog.tsx       # shadcn Dialog
│
├─ lib/
│  ├─ api/
│  │  ├─ http.ts                                # fetch wrapper (JWT + RootEntityResponse unwrap)
│  │  └─ email-templates.ts                     # Endpoint fonksiyonları
│  ├─ hooks/
│  │  └─ email-templates/
│  │     ├─ useEmailTemplates.ts                # useQuery list
│  │     ├─ useEmailTemplate.ts                 # useQuery detay
│  │     └─ useTemplateMutations.ts             # create/update/delete mutations
│  └─ auth/
│     └─ require-permission.ts                  # Server-side permission check
│
└─ types/
   └─ email-template.ts                         # DTO type tanımları
```

#### 1. Tip Tanımları — `types/email-template.ts`

```typescript
import { z } from 'zod';

export const emailTemplateSchema = z.object({
  id: z.number().optional(),
  tenantId: z.string().nullable(),
  templateKey: z.string().min(1).max(100).regex(/^[a-z0-9-]+$/,
    'Küçük harf, rakam ve tire kullan'),
  subject: z.string().min(1).max(255),
  htmlBody: z.string().min(1),
  description: z.string().max(500).nullable().optional(),
  active: z.boolean().default(true),
  version: z.number().default(1),
  optimisticLockVersion: z.number().default(0),
  createdAt: z.string().optional(),
  updatedAt: z.string().optional(),
});

export type EmailTemplate = z.infer<typeof emailTemplateSchema>;
export type EmailTemplateInput = Omit<EmailTemplate, 'id' | 'createdAt' | 'updatedAt'>;
```

#### 2. API Client — `lib/api/email-templates.ts`

```typescript
import { http } from './http';
import type { EmailTemplate, EmailTemplateInput } from '@/types/email-template';

export interface PreviewRequest {
  data: Record<string, unknown>;
}

export interface PreviewResponse {
  html: string;
  subject: string;
}

export const emailTemplatesApi = {
  list: (params?: { page?: number; size?: number; active?: boolean }) =>
    http.get<EmailTemplate[]>('/api/v1/email-templates', { searchParams: params }),

  get: (key: string) =>
    http.get<EmailTemplate>(`/api/v1/email-templates/${key}`),

  create: (body: EmailTemplateInput) =>
    http.post<EmailTemplate>('/api/v1/email-templates', { json: body }),

  update: (key: string, body: EmailTemplateInput) =>
    http.put<EmailTemplate>(`/api/v1/email-templates/${key}`, { json: body }),

  remove: (key: string) =>
    http.delete<void>(`/api/v1/email-templates/${key}`),

  preview: (key: string, body: PreviewRequest) =>
    http.post<PreviewResponse>(`/api/v1/email-templates/${key}/preview`, { json: body }),
};
```

`http` wrapper CMS'in `RootEntityResponse<T>` sarmalayıcısını (`{ result, message, data }`) açar, `data` döner; `result=false` ise `ApiError` fırlatır.

#### 3. TanStack Query Hooks — `lib/hooks/email-templates/`

```typescript
// useEmailTemplates.ts
'use client';
import { useQuery } from '@tanstack/react-query';
import { emailTemplatesApi } from '@/lib/api/email-templates';

export const emailTemplatesKeys = {
  all: ['email-templates'] as const,
  list: (params?: Record<string, unknown>) => [...emailTemplatesKeys.all, 'list', params] as const,
  detail: (key: string) => [...emailTemplatesKeys.all, 'detail', key] as const,
};

export function useEmailTemplates(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: emailTemplatesKeys.list(params),
    queryFn: () => emailTemplatesApi.list(params),
    staleTime: 30_000,
  });
}
```

```typescript
// useTemplateMutations.ts
'use client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { emailTemplatesApi } from '@/lib/api/email-templates';
import { emailTemplatesKeys } from './useEmailTemplates';
import type { EmailTemplateInput } from '@/types/email-template';

export function useCreateTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: EmailTemplateInput) => emailTemplatesApi.create(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: emailTemplatesKeys.all });
      toast.success('Template oluşturuldu');
    },
    onError: (e: Error) => toast.error(e.message),
  });
}

export function useUpdateTemplate(key: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: EmailTemplateInput) => emailTemplatesApi.update(key, body),
    onSuccess: (data) => {
      qc.setQueryData(emailTemplatesKeys.detail(key), data);
      qc.invalidateQueries({ queryKey: emailTemplatesKeys.list() });
      toast.success('Kaydedildi');
    },
    onError: (e: Error) => {
      if (e.message.includes('OptimisticLock')) {
        toast.error('Bu template başka biri tarafından güncellenmiş — yeniden yükle');
      } else {
        toast.error(e.message);
      }
    },
  });
}

export function useDeleteTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (key: string) => emailTemplatesApi.remove(key),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: emailTemplatesKeys.all });
      toast.success('Silindi');
    },
  });
}
```

#### 4. Liste Sayfası — `app/(admin)/admin/email-templates/page.tsx`

Server Component → permission kontrolü + TanStack Query için `HydrationBoundary` ile prefetch:

```tsx
import { HydrationBoundary, QueryClient, dehydrate } from '@tanstack/react-query';
import { requirePermission } from '@/lib/auth/require-permission';
import { emailTemplatesApi } from '@/lib/api/email-templates';
import { TemplateListTable } from './_components/TemplateListTable';
import { Button } from '@/components/ui/button';
import Link from 'next/link';

export default async function EmailTemplatesPage() {
  await requirePermission('email_templates:read');

  const qc = new QueryClient();
  await qc.prefetchQuery({
    queryKey: ['email-templates', 'list', undefined],
    queryFn: () => emailTemplatesApi.list(),
  });

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Email Templates</h1>
        <Button asChild>
          <Link href="/admin/email-templates/new">Yeni Template</Link>
        </Button>
      </div>
      <HydrationBoundary state={dehydrate(qc)}>
        <TemplateListTable />
      </HydrationBoundary>
    </div>
  );
}
```

#### 5. Monaco Editor Bileşeni — Client, dynamic import

```tsx
// _components/MonacoBodyEditor.tsx
'use client';
import dynamic from 'next/dynamic';
import type { OnChange } from '@monaco-editor/react';

const Monaco = dynamic(() => import('@monaco-editor/react'), {
  ssr: false,
  loading: () => <div className="h-96 animate-pulse bg-muted rounded" />,
});

export function MonacoBodyEditor({
  value,
  onChange,
}: {
  value: string;
  onChange: OnChange;
}) {
  return (
    <Monaco
      height="500px"
      language="html"
      value={value}
      onChange={onChange}
      options={{
        minimap: { enabled: false },
        wordWrap: 'on',
        fontSize: 13,
        scrollBeyondLastLine: false,
        automaticLayout: true,
      }}
      theme="vs-dark"
    />
  );
}
```

Thymeleaf syntax için özel tokenizer eklemek istenirse `monaco.languages.register({ id: 'thymeleaf' })` + `setMonarchTokensProvider` ile extend edilebilir; başlangıçta `html` yeterli.

#### 6. Preview Panel — `iframe srcDoc` pattern

```tsx
// _components/PreviewPanel.tsx
'use client';
import { useMutation } from '@tanstack/react-query';
import { emailTemplatesApi } from '@/lib/api/email-templates';
import { Textarea } from '@/components/ui/textarea';
import { Button } from '@/components/ui/button';
import { useState } from 'react';

export function PreviewPanel({ templateKey }: { templateKey: string }) {
  const [payload, setPayload] = useState('{\n  "userName": "Ahmet",\n  "link": "https://..."\n}');
  const [html, setHtml] = useState('');

  const preview = useMutation({
    mutationFn: () =>
      emailTemplatesApi.preview(templateKey, { data: JSON.parse(payload) }),
    onSuccess: (res) => setHtml(res.html),
  });

  return (
    <div className="grid grid-cols-2 gap-4">
      <div className="space-y-2">
        <label className="text-sm font-medium">Dummy Data (JSON)</label>
        <Textarea
          value={payload}
          onChange={(e) => setPayload(e.target.value)}
          className="font-mono text-xs h-80"
        />
        <Button onClick={() => preview.mutate()} disabled={preview.isPending}>
          {preview.isPending ? 'Render ediliyor…' : 'Preview'}
        </Button>
      </div>
      <div className="border rounded">
        <iframe
          sandbox=""
          srcDoc={html}
          className="w-full h-80"
          title="Template preview"
        />
      </div>
    </div>
  );
}
```

`sandbox=""` → iframe içindeki script/form çalışmaz (XSS güvenliği). `srcDoc` ile API response'undaki HTML'i doğrudan render eder.

#### 7. Server-Side Permission Guard — `lib/auth/require-permission.ts`

```typescript
import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import { decodeJwt } from 'jose';

interface JwtPayload {
  permissions?: string[];
  [k: string]: unknown;
}

export async function requirePermission(permission: string) {
  const token = cookies().get('elly-jwt')?.value;
  if (!token) redirect('/login');

  try {
    const payload = decodeJwt<JwtPayload>(token);
    if (!payload.permissions?.includes(permission)) {
      redirect('/403');
    }
  } catch {
    redirect('/login');
  }
}
```

Client tarafında buton gizleme için paralel bir `usePermission()` hook'u — aynı cookie'den JWT okuyup decode eder (veya auth store'da permission listesi tutulur).

#### 8. HTTP Wrapper — `lib/api/http.ts` (iskelet)

```typescript
import { cookies } from 'next/headers';

const BASE_URL = process.env.NEXT_PUBLIC_ELLY_API_URL ?? 'https://api.huseyindol.com';

interface Options {
  json?: unknown;
  searchParams?: Record<string, unknown>;
}

async function request<T>(method: string, path: string, opts: Options = {}): Promise<T> {
  const url = new URL(path, BASE_URL);
  if (opts.searchParams) {
    Object.entries(opts.searchParams).forEach(([k, v]) => {
      if (v !== undefined && v !== null) url.searchParams.set(k, String(v));
    });
  }

  // Server component'te cookies(), client component'te document.cookie veya auth store
  const token = typeof window === 'undefined'
    ? cookies().get('elly-jwt')?.value
    : getClientToken();

  const res = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: opts.json ? JSON.stringify(opts.json) : undefined,
    cache: 'no-store',
  });

  const body = await res.json().catch(() => null);
  if (!res.ok || body?.result === false) {
    throw new ApiError(body?.message ?? res.statusText, body?.errorCode, res.status);
  }
  return body.data as T;
}

export const http = {
  get: <T>(path: string, opts?: Options) => request<T>('GET', path, opts),
  post: <T>(path: string, opts?: Options) => request<T>('POST', path, opts),
  put: <T>(path: string, opts?: Options) => request<T>('PUT', path, opts),
  delete: <T>(path: string, opts?: Options) => request<T>('DELETE', path, opts),
};

export class ApiError extends Error {
  constructor(message: string, public errorCode?: string, public status?: number) {
    super(message);
  }
}
function getClientToken(): string | undefined { /* auth store veya cookie okur */ return undefined; }
```

#### 9. Environment

```env
# .env.local
NEXT_PUBLIC_ELLY_API_URL=https://api.huseyindol.com
```

Server-side'da cookie okunduğu için `NEXT_PUBLIC_` olmayan ek secret gerekmez — JWT HttpOnly cookie'de tutulur, tarayıcı otomatik gönderir.

### Dependencies (CMS)

Zaten var: Thymeleaf, Spring Data JPA, Spring Cache (Redis).
**Yeni:** `org.jsoup:jsoup` (opsiyonel — template editor preview sanitization için).

### Test Stratejisi

- Unit: `EmailTemplateRendererTest` — DB hit, cache hit, classpath fallback, bozuk Thymeleaf syntax hata yakalama.
- Integration: Mevcut `EmailQueueServiceTest`'e yeni scenario ekle: DB-based template + fallback.
- Manuel: Panel'den edit → preview → form submit → gelen mail içeriği panel'deki content ile eşleşmeli.

---

## Göç Yolu (Zero-downtime)

**v3 → v4 migrasyonu için sıra:**

1. DB migration'ı uygula (yeni tablo ekle, mevcut flow'a dokunma).
2. Backend deploy: yeni entity/servis/endpoint'ler ekli, `EmailQueueService` classpath-first → DB-second okur (ilk iterasyonda backward compatible).
3. Bootstrap runner çalışır, 3 template global olarak seed edilir.
4. Panel deploy: template yönetim UI canlı.
5. Admin template'leri DB'de düzenler. CMS DB-first'e geçmek için config flag: `app.mail.template-source=db` (default) / `classpath`.
6. Sonraki release'de classpath template'ler silinir (opsiyonel).

**Rollback:** `app.mail.template-source=classpath` yaparak eski davranışa döner.

---

## Açık Sorular / v5'e Ertelenenler

- **Template versiyonlama / audit log** — Şu an sadece `optimistic_lock_version` var. Kim ne zaman değiştirdi tablosu ayrı iterasyon.
- **A/B testing** — Aynı `key` için iki variant + weighted routing. v5.
- **Template import/export** — ZIP olarak dışa aktarma (staging → prod migration). v5.
- **Rich editor (WYSIWYG)** — Şu an Monaco/HTML source düşünülüyor, ileride GrapeJS tarzı visual editor.
- **Layout/partial support** — Thymeleaf fragment desteği (header/footer'ı ayrı template'te tut). Backend hazır, sadece bootstrap + UI'a eklenecek.

---

## Özet

Kullanıcının "CMS hafif kalsın" hedefi **Opsiyon C ile tam karşılanır:**
- CMS runtime'da dış servise çağrı yapmaz (latency + tek arıza noktası yok).
- Template yönetimi panel'de (kullanıcı deneyimi istenilen yerde).
- CMS'e eklenen yük: 1 tablo + 1 servis + 1 renderer wrapper. Bu zaten mevcut altyapı ile (JPA + Redis + Thymeleaf) komposite edilir, yeni teknoloji katmanı **yok**.

Opsiyon A'nın tek avantajı "template dosyası CMS repo'sunda olmaması" idi — C de bunu DB ile sağlıyor, üstüne runtime coupling olmadan.

**Karar:** v4'e Opsiyon C ile başla. Bootstrap runner ile geri uyumluluk sağla, gerekirse classpath fallback kalmaya devam etsin.

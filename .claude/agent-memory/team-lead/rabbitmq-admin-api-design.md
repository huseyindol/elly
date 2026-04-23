# RabbitMQ Admin API — Panel Entegrasyon Tasarımı

> Kullanıcı isteği: "RabbitMQ management UI yapmanı istiyorum ama bunu diğer repodaki elly-admin-panel projesinde yapmak istiyorum çünkü bu proje sadece api barındırıyor ve ağırlaşmasını yük olmasını istemiyorum." Panel repo erişimi yok → CMS'te gerekli API'leri hazırla + panel tarafı entegrasyon dokümanı.

---

## Genel Strateji: CMS = Thin Proxy

CMS **kendi üzerinde yeni bir queue management implement etmez.** Zaten RabbitMQ'nun resmi management plugin'i (port 15672) var; o HTTP REST API sunuyor. CMS sadece:

1. **Proxy katmanı:** Admin JWT token ile gelen panel çağrısını RabbitMQ Management API'ye forward eder.
2. **RBAC koruması:** Sadece `SUPER_ADMIN` / `rabbitmq:manage` yetkisi olan kullanıcılar erişebilir.
3. **Tenant-agnostik:** Queue'lar tenant'a göre bölünmemiş (email-queue tek, mesaj içinde tenantId var), o yüzden bu endpoint global scope.

Böylece panel **direkt RabbitMQ Management API'ye gitmez** (credential leak riski + network topology). Panel → CMS JWT → CMS → internal RabbitMQ 15672 → response.

```
[Panel]  ──JWT auth──►  [CMS /api/v1/admin/rabbit/*]  ──Basic auth──►  [RabbitMQ :15672/api/*]
```

### Neden Doğrudan Panel → RabbitMQ Değil?

- **Credential hygiene:** RABBITMQ_USER/RABBITMQ_PASSWORD secret'i CMS pod'ununda — panel'e secret geçmenin anlamı yok.
- **Network izolasyonu:** RabbitMQ ClusterIP service, sadece cluster içinden erişilir. Panel external, dışarıya expose etmek için yeni ingress + auth katmanı lazım → karmaşıklık.
- **RBAC tekillik:** Elly'nin mevcut `@PreAuthorize` sistemi tek yerde, Panel ayrı bir permission modeli kurmasın.
- **Audit trail:** CMS'te request logging zaten var; "kim hangi queue'yu purge etti" otomatik loglanır.

---

## Backend API (CMS'te Yazılacak)

### Yeni Paket: `com.cms.controller.admin` + `com.cms.service.admin`

### 1. Permission

`PermissionConstants.java`'ya ekle:
```java
// =============== RABBITMQ ADMIN ===============
public static final String RABBIT_READ = "rabbit:read";      // queue/message listeleme
public static final String RABBIT_MANAGE = "rabbit:manage";  // purge, republish, delete
```

Role → permission mapping (DB seed):
- `SUPER_ADMIN` → her ikisi
- `ADMIN` → sadece `rabbit:read` (destructive action yok)
- diğerleri → hiç

### 2. Endpoint'ler

Base path: `/api/v1/admin/rabbit`

| Method | Path | Permission | Açıklama |
|---|---|---|---|
| GET | `/overview` | `rabbit:read` | Broker durumu (toplam queue sayısı, mesaj, consumer) |
| GET | `/queues` | `rabbit:read` | Tüm queue'ların listesi + sayaçlar |
| GET | `/queues/{name}` | `rabbit:read` | Tek queue detay |
| GET | `/queues/{name}/messages?count=10` | `rabbit:read` | Kuyruktaki ilk N mesajı **peek** (ack etmez) |
| POST | `/queues/{name}/purge` | `rabbit:manage` | Tüm mesajları sil |
| DELETE | `/queues/{name}/messages/{deliveryTag}` | `rabbit:manage` | Tek mesajı sil |
| POST | `/queues/{name}/republish` | `rabbit:manage` | DLQ→ana kuyruğa yeniden publish (payload ile) |
| GET | `/exchanges` | `rabbit:read` | Exchange listesi |
| GET | `/bindings` | `rabbit:read` | Binding listesi |

### 3. Response DTO'ları

```java
// DtoRabbitQueue.java
@Data
@Builder
public class DtoRabbitQueue {
    private String name;
    private String vhost;
    private Long messages;            // toplam mesaj (ready + unacked)
    private Long messagesReady;
    private Long messagesUnacknowledged;
    private Integer consumers;
    private String state;             // "running" | "idle" | ...
    private Map<String, Object> arguments;  // x-dead-letter-exchange, x-message-ttl, vs.
    private String policy;
}

// DtoRabbitMessage.java
@Data
@Builder
public class DtoRabbitMessage {
    private String payload;                   // JSON string (consumer'ın EmailMessage'ı)
    private String payloadEncoding;           // "string" | "base64"
    private Map<String, Object> properties;   // headers, content_type, message_id, timestamp, vs.
    private Long messageCount;                // queue'da kalan toplam
    private String routingKey;
    private Boolean redelivered;
    private String exchange;
}

// DtoRabbitOverview.java
@Data
@Builder
public class DtoRabbitOverview {
    private String rabbitmqVersion;
    private String erlangVersion;
    private String clusterName;
    private Long totalMessages;
    private Long totalConsumers;
    private Integer queueCount;
    private Integer exchangeCount;
}
```

### 4. Servis Katmanı

**`IRabbitAdminService`** (mevcut `I*Service` pattern'i):

```java
public interface IRabbitAdminService {
    DtoRabbitOverview getOverview();
    List<DtoRabbitQueue> listQueues();
    DtoRabbitQueue getQueue(String name);
    List<DtoRabbitMessage> peekMessages(String queueName, int count);
    void purgeQueue(String queueName);
    void republishMessage(String queueName, String targetQueueName, String payload);
}
```

**Impl: İki yaklaşım — karar bekliyor**

#### Yaklaşım A: Spring AMQP `RabbitAdmin` (native, sadece management işlemleri)

```java
@Service
@RequiredArgsConstructor
public class RabbitAdminService implements IRabbitAdminService {
    private final RabbitAdmin rabbitAdmin;        // Spring AMQP
    private final RabbitTemplate rabbitTemplate;  // peek için

    public void purgeQueue(String name) {
        rabbitAdmin.purgeQueue(name, true);  // no-wait=true
    }
    // ...
}
```

**Artı:** Credential CMS'te zaten var (ConnectionFactory), ekstra config yok.
**Eksi:** `RabbitAdmin` peek/consumer count gibi detayları vermiyor — sadece declare/delete/purge.

#### Yaklaşım B: HTTP Management API (REST) — **ÖNERİ**

Port 15672'deki management plugin'in `/api/*` endpoint'leri her şeyi döner. CMS `WebClient` ile HTTP çağrı yapar.

```java
@Service
@RequiredArgsConstructor
public class RabbitMgmtClient {
    private final WebClient webClient;  // baseUrl=http://rabbitmq:15672/api, basicAuth=${RABBITMQ_USER}:${RABBITMQ_PASSWORD}

    public Mono<List<Map<String, Object>>> listQueues() {
        return webClient.get()
            .uri("/queues/%2F")  // %2F = default vhost "/"
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList();
    }

    public Mono<Void> purge(String queue) {
        return webClient.delete()
            .uri("/queues/%2F/{q}/contents", queue)
            .retrieve()
            .bodyToMono(Void.class);
    }

    public Mono<List<Map<String, Object>>> peek(String queue, int count) {
        Map<String, Object> body = Map.of(
            "count", count,
            "ackmode", "ack_requeue_true",  // ack sonrası tekrar kuyruğa koy = peek
            "encoding", "auto",
            "truncate", 50000
        );
        return webClient.post()
            .uri("/queues/%2F/{q}/get", queue)
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList();
    }
}
```

**Artı:** Tüm metrikleri alır (consumers, rates, memory), peek gerçek payload'ı döner, exchange/binding/overview hepsi aynı API'den.
**Eksi:** Yeni bağımlılık (Spring WebFlux sadece `WebClient` için, aslında bulk dependency değil — `spring-boot-starter-webflux` ama sadece WebClient'i kullanmak için `spring-webflux` zaten Spring Boot 3'te core'da).

**Karar: Yaklaşım B** — management API panel UI için zengin, A ile sadece purge'yi karşılayabiliyoruz.

### 5. Config

`application.properties`:
```properties
# Management API base URL (ClusterIP DNS)
rabbitmq.mgmt.url=${RABBITMQ_MGMT_URL:http://rabbitmq:15672/api}
rabbitmq.mgmt.vhost=${RABBITMQ_VHOST:/}
# Kullanıcı/şifre RABBITMQ_USER/RABBITMQ_PASSWORD secret'ten alınıyor (zaten var)
rabbitmq.mgmt.connect-timeout-ms=2000
rabbitmq.mgmt.read-timeout-ms=5000
```

`k8s/1-configmap.yaml`:
```yaml
RABBITMQ_MGMT_URL: "http://rabbitmq-management:15672/api"
RABBITMQ_VHOST: "/"
```

**Not:** `rabbitmq-management` service zaten var (`k8s/2e-rabbitmq.yaml:106`). Ekstra K8s kaynağı gerekmez.

### 6. Örnek Controller (RestController şeması)

```java
@RestController
@RequestMapping("/api/v1/admin/rabbit")
@RequiredArgsConstructor
public class RabbitAdminController implements IRabbitAdminController {

    private final IRabbitAdminService service;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_READ + "')")
    @Override
    public RootEntityResponse<DtoRabbitOverview> getOverview() {
        return RootEntityResponse.ok(service.getOverview());
    }

    @GetMapping("/queues")
    @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_READ + "')")
    @Override
    public RootEntityResponse<List<DtoRabbitQueue>> listQueues() {
        return RootEntityResponse.ok(service.listQueues());
    }

    @GetMapping("/queues/{name}/messages")
    @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_READ + "')")
    @Override
    public RootEntityResponse<List<DtoRabbitMessage>> peekMessages(
            @PathVariable String name,
            @RequestParam(defaultValue = "10") int count) {
        return RootEntityResponse.ok(service.peekMessages(name, count));
    }

    @PostMapping("/queues/{name}/purge")
    @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_MANAGE + "')")
    @Override
    public RootEntityResponse<Void> purgeQueue(@PathVariable String name) {
        service.purgeQueue(name);
        return RootEntityResponse.ok(null);
    }

    @PostMapping("/queues/{source}/republish")
    @PreAuthorize("hasAuthority('" + PermissionConstants.RABBIT_MANAGE + "')")
    @Override
    public RootEntityResponse<Void> republish(
            @PathVariable String source,
            @RequestBody RepublishRequest req) {
        service.republishMessage(source, req.getTargetQueue(), req.getPayload());
        return RootEntityResponse.ok(null);
    }
}
```

### 7. Güvenlik Notları

- **Destructive endpoint'ler (`purge`, `delete`, `republish`) idempotent değil.** CSRF koruması: admin API'ler zaten JWT ile çalışıyor, session-based değil → CSRF problemi yok.
- **Rate limit:** Panel UI kullanıcı tıklamasına bağlı → saniyede 5 request limiti yeterli. Mevcut filter varsa ekle, yoksa Bucket4j düşün (v5 iterasyonu).
- **Audit log:** Her `rabbit:manage` çağrısı için `AuditLogService.log(user, action, details)` (varsa; yoksa en azından `log.warn()` ile).
- **Panel direkt 15672'ye erişmesin.** K8s NetworkPolicy ile `rabbitmq` service'e sadece `elly` namespace pod'larından trafik gelsin. Bu zaten cluster default davranışı, ama dokümante edelim.

### 8. Hata Senaryoları

| Durum | HTTP | Message |
|---|---|---|
| Queue yok | 404 | "Queue 'xyz' bulunamadı" |
| Management API timeout | 503 | "RabbitMQ management servisine ulaşılamadı" |
| Auth hatası (CMS↔RabbitMQ) | 500 | "RabbitMQ credential hatası — admin'e bildirin" |
| Purge başarısız (queue in use) | 409 | "Queue şu an consumer tarafından kullanılıyor" |

`GlobalExceptionHandler`'a yeni case eklemek yerine `WebClientResponseException`'ı map eden `@ExceptionHandler` ekle.

---

## Frontend — elly-admin-panel (Next.js App Router)

> **Varsayılan stack:** Next.js 14+ (App Router), TypeScript, TanStack Query v5, shadcn/ui (Radix + Tailwind), TanStack Table, react-hook-form + zod, sonner (toast). Pages Router veya farklı UI kütüphanesi varsa parçalar birebir adapte edilebilir.

### Dosya Yapısı

```
elly-admin-panel/
├─ app/
│  └─ (admin)/
│     └─ admin/
│        └─ infrastructure/
│           └─ rabbitmq/
│              ├─ page.tsx                     # Server Component — prefetch + guard
│              ├─ loading.tsx                  # Skeleton
│              └─ _components/
│                 ├─ OverviewCard.tsx          # Client — broker özeti
│                 ├─ QueueTable.tsx            # Client — TanStack Table
│                 ├─ QueueDetailSheet.tsx      # Client — shadcn Sheet (yandan drawer)
│                 ├─ MessageList.tsx           # Peek edilmiş mesajları listeler
│                 ├─ DestructiveConfirmDialog.tsx  # Queue adı yaz-onayla
│                 └─ RepublishDialog.tsx       # DLQ → hedef queue modalı
│
├─ lib/
│  ├─ api/
│  │  └─ rabbit-admin.ts                       # Endpoint fonksiyonları
│  ├─ hooks/
│  │  └─ rabbit/
│  │     ├─ useRabbitOverview.ts
│  │     ├─ useRabbitQueues.ts
│  │     ├─ useQueueMessages.ts
│  │     └─ useRabbitMutations.ts              # purge / republish
│  └─ auth/
│     └─ permissions.ts                        # usePermission / requirePermission
│
└─ types/
   └─ rabbit.ts                                 # DTO type tanımları
```

### 1. Tipler — `types/rabbit.ts`

CMS'in dönen yapısına birebir mapping (sadece alanları listeliyorum, `RootEntityResponse` wrapper açıldıktan sonraki hal):

```typescript
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
  state: string | null;            // "running" | "idle" | ...
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

export interface RepublishRequest {
  targetQueue: string;
  payload: string;
  contentType?: string;
}
```

### 2. API Client — `lib/api/rabbit-admin.ts`

```typescript
import { http } from './http';
import type { RabbitOverview, RabbitQueue, RabbitMessage, RepublishRequest } from '@/types/rabbit';

export const rabbitAdminApi = {
  overview: () =>
    http.get<RabbitOverview>('/api/v1/admin/rabbit/overview'),

  listQueues: () =>
    http.get<RabbitQueue[]>('/api/v1/admin/rabbit/queues'),

  getQueue: (name: string) =>
    http.get<RabbitQueue>(`/api/v1/admin/rabbit/queues/${encodeURIComponent(name)}`),

  peekMessages: (name: string, count = 10) =>
    http.get<RabbitMessage[]>(
      `/api/v1/admin/rabbit/queues/${encodeURIComponent(name)}/messages`,
      { searchParams: { count } },
    ),

  purgeQueue: (name: string) =>
    http.post<void>(`/api/v1/admin/rabbit/queues/${encodeURIComponent(name)}/purge`),

  republish: (sourceQueue: string, body: RepublishRequest) =>
    http.post<void>(
      `/api/v1/admin/rabbit/queues/${encodeURIComponent(sourceQueue)}/republish`,
      { json: body },
    ),
};
```

### 3. TanStack Query Hooks — `lib/hooks/rabbit/`

```typescript
// useRabbitQueues.ts
'use client';
import { useQuery } from '@tanstack/react-query';
import { rabbitAdminApi } from '@/lib/api/rabbit-admin';

export const rabbitKeys = {
  all: ['rabbit'] as const,
  overview: () => [...rabbitKeys.all, 'overview'] as const,
  queues: () => [...rabbitKeys.all, 'queues'] as const,
  queue: (name: string) => [...rabbitKeys.all, 'queue', name] as const,
  messages: (name: string) => [...rabbitKeys.all, 'messages', name] as const,
};

export function useRabbitQueues() {
  return useQuery({
    queryKey: rabbitKeys.queues(),
    queryFn: () => rabbitAdminApi.listQueues(),
    refetchInterval: 5_000,        // canlı görünüm — her 5 saniyede yenile
    staleTime: 2_000,
  });
}

export function useRabbitOverview() {
  return useQuery({
    queryKey: rabbitKeys.overview(),
    queryFn: () => rabbitAdminApi.overview(),
    refetchInterval: 10_000,
  });
}
```

```typescript
// useRabbitMutations.ts
'use client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { rabbitAdminApi } from '@/lib/api/rabbit-admin';
import { rabbitKeys } from './useRabbitQueues';
import type { RepublishRequest } from '@/types/rabbit';

export function usePurgeQueue() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => rabbitAdminApi.purgeQueue(name),
    onSuccess: (_, name) => {
      qc.invalidateQueries({ queryKey: rabbitKeys.all });
      toast.success(`Queue "${name}" purge edildi`);
    },
    onError: (e: Error) => toast.error(e.message),
  });
}

export function useRepublish(sourceQueue: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: RepublishRequest) => rabbitAdminApi.republish(sourceQueue, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: rabbitKeys.all });
      toast.success('Mesaj yeniden publish edildi');
    },
    onError: (e: Error) => toast.error(e.message),
  });
}
```

### 4. Sayfa — `app/(admin)/admin/infrastructure/rabbitmq/page.tsx`

Server Component: permission guard + prefetch. UI kısmı client child'lere bırakılır.

```tsx
import { HydrationBoundary, QueryClient, dehydrate } from '@tanstack/react-query';
import { requirePermission } from '@/lib/auth/permissions';
import { rabbitAdminApi } from '@/lib/api/rabbit-admin';
import { OverviewCard } from './_components/OverviewCard';
import { QueueTable } from './_components/QueueTable';

export const dynamic = 'force-dynamic';  // JWT cookie'si her request'te okunsun

export default async function RabbitMqPage() {
  await requirePermission('rabbit:read');

  const qc = new QueryClient();
  await Promise.all([
    qc.prefetchQuery({ queryKey: ['rabbit', 'overview'],  queryFn: () => rabbitAdminApi.overview() }),
    qc.prefetchQuery({ queryKey: ['rabbit', 'queues'],    queryFn: () => rabbitAdminApi.listQueues() }),
  ]);

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-semibold">RabbitMQ Yönetimi</h1>
      <HydrationBoundary state={dehydrate(qc)}>
        <OverviewCard />
        <QueueTable />
      </HydrationBoundary>
    </div>
  );
}
```

### 5. Overview Card — shadcn `Card`

```tsx
// _components/OverviewCard.tsx
'use client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useRabbitOverview } from '@/lib/hooks/rabbit/useRabbitQueues';
import { Skeleton } from '@/components/ui/skeleton';

export function OverviewCard() {
  const { data, isLoading } = useRabbitOverview();

  if (isLoading) return <Skeleton className="h-28 w-full" />;
  if (!data) return null;

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          RabbitMQ {data.rabbitmqVersion} · <span className="text-muted-foreground">{data.clusterName}</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
        <Stat label="Toplam Mesaj"   value={data.totalMessages} />
        <Stat label="Consumer"       value={data.totalConsumers} />
        <Stat label="Queue"          value={data.queueCount} />
        <Stat label="Exchange"       value={data.exchangeCount} />
      </CardContent>
    </Card>
  );
}

function Stat({ label, value }: { label: string; value: number | null }) {
  return (
    <div>
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="text-2xl font-semibold tabular-nums">{value ?? '—'}</div>
    </div>
  );
}
```

### 6. Queue Tablosu — TanStack Table + shadcn

```tsx
// _components/QueueTable.tsx
'use client';
import { useState } from 'react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useRabbitQueues } from '@/lib/hooks/rabbit/useRabbitQueues';
import { usePermission } from '@/lib/auth/permissions';
import { QueueDetailSheet } from './QueueDetailSheet';
import { DestructiveConfirmDialog } from './DestructiveConfirmDialog';
import { usePurgeQueue } from '@/lib/hooks/rabbit/useRabbitMutations';
import type { RabbitQueue } from '@/types/rabbit';

export function QueueTable() {
  const { data: queues = [], isLoading } = useRabbitQueues();
  const canManage = usePermission('rabbit:manage');
  const purge = usePurgeQueue();
  const [selected, setSelected] = useState<RabbitQueue | null>(null);
  const [purgeTarget, setPurgeTarget] = useState<RabbitQueue | null>(null);

  return (
    <>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Queue</TableHead>
            <TableHead className="text-right">Ready</TableHead>
            <TableHead className="text-right">Unacked</TableHead>
            <TableHead className="text-right">Consumer</TableHead>
            <TableHead>State</TableHead>
            <TableHead className="w-32">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading && (
            <TableRow><TableCell colSpan={6}>Yükleniyor…</TableCell></TableRow>
          )}
          {queues.map((q) => (
            <TableRow key={q.name} className="cursor-pointer" onClick={() => setSelected(q)}>
              <TableCell className="font-mono text-sm">{q.name}</TableCell>
              <TableCell className="text-right tabular-nums">{q.messagesReady ?? 0}</TableCell>
              <TableCell className="text-right tabular-nums">{q.messagesUnacknowledged ?? 0}</TableCell>
              <TableCell className="text-right tabular-nums">{q.consumers ?? 0}</TableCell>
              <TableCell>
                <Badge variant={q.state === 'running' ? 'default' : 'secondary'}>
                  {q.state ?? '—'}
                </Badge>
              </TableCell>
              <TableCell onClick={(e) => e.stopPropagation()}>
                <Button
                  size="sm"
                  variant="destructive"
                  disabled={!canManage || (q.messages ?? 0) === 0}
                  onClick={() => setPurgeTarget(q)}
                >
                  Purge
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <QueueDetailSheet
        queue={selected}
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
      />

      <DestructiveConfirmDialog
        open={!!purgeTarget}
        onOpenChange={(o) => !o && setPurgeTarget(null)}
        expectedText={purgeTarget?.name ?? ''}
        title="Queue Purge"
        description={purgeTarget && `"${purgeTarget.name}" kuyruğundaki ${purgeTarget.messages ?? 0} mesaj kalıcı olarak silinecek.`}
        onConfirm={() => {
          if (purgeTarget) purge.mutate(purgeTarget.name);
          setPurgeTarget(null);
        }}
      />
    </>
  );
}
```

### 7. Destructive Confirm Dialog — "queue adını yaz" UX

```tsx
// _components/DestructiveConfirmDialog.tsx
'use client';
import { useState } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  expectedText: string;
  title: string;
  description?: React.ReactNode;
  onConfirm: () => void;
}

export function DestructiveConfirmDialog({
  open, onOpenChange, expectedText, title, description, onConfirm,
}: Props) {
  const [value, setValue] = useState('');
  const matches = value.trim() === expectedText;

  return (
    <Dialog open={open} onOpenChange={(o) => { onOpenChange(o); if (!o) setValue(''); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="text-destructive">⚠️ {title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <p className="text-sm">
            Onaylamak için queue adını yaz: <code className="font-mono font-semibold">{expectedText}</code>
          </p>
          <Input
            autoFocus
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder={expectedText}
          />
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>İptal</Button>
          <Button variant="destructive" disabled={!matches} onClick={onConfirm}>
            Onayla
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

### 8. Queue Detail Sheet + Message List

```tsx
// _components/QueueDetailSheet.tsx
'use client';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { MessageList } from './MessageList';
import type { RabbitQueue } from '@/types/rabbit';

interface Props {
  queue: RabbitQueue | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function QueueDetailSheet({ queue, open, onOpenChange }: Props) {
  if (!queue) return null;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[640px] sm:max-w-none overflow-y-auto">
        <SheetHeader>
          <SheetTitle className="font-mono">{queue.name}</SheetTitle>
        </SheetHeader>

        <section className="my-4 space-y-1 text-sm">
          <KV k="State"    v={queue.state} />
          <KV k="Durable"  v={String(queue.durable)} />
          <KV k="Messages" v={String(queue.messages ?? 0)} />
          <KV k="Consumers" v={String(queue.consumers ?? 0)} />
        </section>

        {queue.arguments && Object.keys(queue.arguments).length > 0 && (
          <section className="my-4">
            <h3 className="text-sm font-semibold mb-2">Arguments</h3>
            <pre className="bg-muted p-3 rounded text-xs font-mono overflow-x-auto">
              {JSON.stringify(queue.arguments, null, 2)}
            </pre>
          </section>
        )}

        <MessageList queueName={queue.name} />
      </SheetContent>
    </Sheet>
  );
}

function KV({ k, v }: { k: string; v: string | null }) {
  return <div><span className="text-muted-foreground w-24 inline-block">{k}</span>{v ?? '—'}</div>;
}
```

```tsx
// _components/MessageList.tsx
'use client';
import { useQuery } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { rabbitAdminApi } from '@/lib/api/rabbit-admin';

export function MessageList({ queueName }: { queueName: string }) {
  const { data, isFetching, refetch } = useQuery({
    queryKey: ['rabbit', 'messages', queueName],
    queryFn: () => rabbitAdminApi.peekMessages(queueName, 10),
    enabled: false,                // sadece butona basınca yükle (peek destructive hissedilmesin)
  });

  return (
    <section className="my-4 space-y-2">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">Son Mesajlar (peek)</h3>
        <Button size="sm" variant="outline" onClick={() => refetch()} disabled={isFetching}>
          {isFetching ? 'Yükleniyor…' : (data ? 'Yenile' : 'Göster')}
        </Button>
      </div>
      {data && data.length === 0 && (
        <p className="text-sm text-muted-foreground">Kuyrukta mesaj yok.</p>
      )}
      {data?.map((m, i) => (
        <details key={i} className="border rounded p-2 text-xs">
          <summary className="cursor-pointer font-mono">
            #{i + 1} — {m.routingKey} {m.redelivered && <span className="text-amber-600">(redelivered)</span>}
          </summary>
          <pre className="mt-2 overflow-x-auto bg-muted p-2 rounded">{tryFormatJson(m.payload)}</pre>
          <details className="mt-1">
            <summary className="text-muted-foreground">properties</summary>
            <pre className="overflow-x-auto bg-muted p-2 rounded mt-1">{JSON.stringify(m.properties, null, 2)}</pre>
          </details>
        </details>
      ))}
    </section>
  );
}

function tryFormatJson(s: string): string {
  try { return JSON.stringify(JSON.parse(s), null, 2); } catch { return s; }
}
```

### 9. Permission Hook — `lib/auth/permissions.ts`

```typescript
// Server-side (Server Component / Server Action / Route Handler)
'use server';
import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import { decodeJwt } from 'jose';

interface JwtPayload { permissions?: string[]; [k: string]: unknown; }

export async function requirePermission(permission: string) {
  const token = cookies().get('elly-jwt')?.value;
  if (!token) redirect('/login');
  try {
    const p = decodeJwt<JwtPayload>(token);
    if (!p.permissions?.includes(permission)) redirect('/403');
  } catch { redirect('/login'); }
}
```

```typescript
// Client-side — auth store'dan permission listesi okur (Zustand/Jotai/Redux — projeniz neyse)
'use client';
import { useAuthStore } from '@/lib/auth/store';   // proje-özel

export function usePermission(required: string): boolean {
  const perms = useAuthStore((s) => s.permissions);
  return perms.includes(required);
}
```

Rol bazlı UI:
- `rabbit:read` yoksa → `requirePermission` server'da `/403`'e yönlendirir; UI hiç render olmaz.
- `rabbit:read` var, `rabbit:manage` yoksa → sayfa görünür ama Purge/Requeue butonları `disabled={!canManage}`.

### 10. Route Segment Config

Queue listesi canlı veri olduğu için:

```tsx
// app/(admin)/admin/infrastructure/rabbitmq/page.tsx üstünde:
export const dynamic = 'force-dynamic';
export const revalidate = 0;
```

Canlı refetch TanStack Query'nin `refetchInterval`'ı ile yapılır (yukarıdaki hook'larda 5-10 sn).

### 11. Özel Akış: "DLQ'dan Tümünü Requeue"

**Yaklaşım 1 (basit, kısa vadede):** Panel DLQ'yu peek eder, her mesaj için CMS'in `POST /republish` endpoint'ine tek tek çağrı yapar. `Promise.allSettled` ile paralel — 10-20 mesaj için yeterli.

```typescript
async function requeueAll(sourceQueue: string, targetQueue: string) {
  const msgs = await rabbitAdminApi.peekMessages(sourceQueue, 100);
  const results = await Promise.allSettled(
    msgs.map((m) => rabbitAdminApi.republish(sourceQueue, {
      targetQueue,
      payload: m.payload,
      contentType: m.properties['content_type'] as string | undefined,
    })),
  );
  const failed = results.filter((r) => r.status === 'rejected').length;
  if (failed > 0) toast.warning(`${failed} mesaj tekrar publish edilemedi`);
  else toast.success(`${msgs.length} mesaj kuyruğa geri alındı`);
}
```

**Yaklaşım 2 (100+ mesaj için):** CMS'e bulk endpoint ekle — `POST /api/v1/admin/rabbit/queues/{source}/requeue-all?target={target}&limit=500`. Backend iterate eder, tek HTTP çağrısı. v5 kuyruğunda.

### 12. Environment

```env
# .env.local
NEXT_PUBLIC_ELLY_API_URL=https://api.huseyindol.com
```

JWT HttpOnly cookie olarak CMS tarafından set edildiği için tarayıcı otomatik gönderir; ek header yönetimi gerekmez. Server Component'lerde `cookies()` ile okunur.

---

## v3 Retry Endpoint'i ile Çakışma

Bu iterasyonda eklenen `POST /api/v1/emails/{id}/retry` **mantıksal seviyede** FAILED EmailLog'ları reset+republish eder. Yani:

- **"Tek mail retry et":** Panel > Email Log > satır > "Retry" → `POST /emails/{id}/retry` (app-level, transaction + status update).
- **"Queue'yu yönet":** Panel > RabbitMQ > Queue > "Purge" → `POST /admin/rabbit/queues/{name}/purge` (broker-level).

İki endpoint farklı amaç için — bunları UI'da karıştırma. Email Log sayfası mail-level, RabbitMQ sayfası broker-level.

---

## Uygulama Checklist (Bir Sonraki İterasyon)

### CMS
- [ ] `PermissionConstants.RABBIT_READ` + `RABBIT_MANAGE` ekle
- [ ] `rabbitmq-admin-permissions.sql` migration — SUPER_ADMIN rolüne permission bağla
- [ ] `RabbitMgmtClient` (WebClient base) — config + basic auth
- [ ] `IRabbitAdminService` + `RabbitAdminService`
- [ ] `IRabbitAdminController` + `RabbitAdminController`
- [ ] DTO'lar: `DtoRabbitQueue`, `DtoRabbitMessage`, `DtoRabbitOverview`
- [ ] `RepublishRequest` (requestBody wrapper)
- [ ] `@ExceptionHandler` for `WebClientResponseException`
- [ ] Integration test: wiremock ile 15672 mock, listQueues/purge senaryoları
- [ ] K8s configmap: `RABBITMQ_MGMT_URL` env

### Panel (elly-admin-panel repo'sunda)
- [ ] `/admin/infrastructure/rabbitmq` route
- [ ] Overview card component
- [ ] QueueTable component (tanstack-table önerisi)
- [ ] QueueDetailDrawer + MessageList
- [ ] DestructiveConfirmModal (queue adı yazma + onay)
- [ ] API client (`rabbit-admin.ts`)
- [ ] Permission-aware button rendering
- [ ] i18n (tr/en) string'leri

### Dokümantasyon
- [ ] `docs/ADMIN_API.md`'ye Rabbit section ekle
- [ ] Panel README'ye entegrasyon adımları

---

## Kısa Özet

- **CMS:** thin proxy — RabbitMQ management HTTP API'yi (`:15672/api/*`) forward eder, JWT/permission ile korur.
- **Panel:** UI sadece CMS'in `/api/v1/admin/rabbit/*` endpoint'lerini çağırır, RabbitMQ credential'ı panel'e hiç sızmaz.
- **CMS'e eklenecek yük:** 1 WebClient, 1 servis, 1 controller + DTO'lar. Runtime'da her page load için 1-2 HTTP call; broker üzerinde ekstra load yok (management plugin zaten ayakta).
- **v3 retry ile karışmaz:** biri app-level (EmailLog), diğeri broker-level (queue).

Panel repo'suna erişim açıldığında UI implementasyonu bu dokümana göre yapılabilir; şimdilik CMS backend'ini hazır hale getirmek yeterli.

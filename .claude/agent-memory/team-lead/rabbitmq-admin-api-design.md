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

## Frontend (Panel Tarafında Yapılacak)

Bu repo ayrı — nasıl entegre edeceğin rehberi.

### Sayfa: `/admin/infrastructure/rabbitmq`

#### 1. Overview Card

- Broker versiyon, cluster adı
- Toplam mesaj / consumer / queue sayısı
- Son 5 dk mesaj akış grafiği (opsiyonel, `messages_delivered_details.rate`'ten hesaplanabilir)

```
┌─────────────────────────────┐
│ RabbitMQ 3.13 · cluster-1   │
│ 42 messages · 3 consumers   │
│ 4 queues · 3 exchanges      │
└─────────────────────────────┘
```

#### 2. Queue Listesi Tablosu

| Name | Ready | Unacked | Consumers | State | Actions |
|---|---|---|---|---|---|
| email-queue | 0 | 0 | 1 | running | [Peek] |
| email-retry-queue | 0 | 0 | 0 | idle | [Peek] [Purge] |
| email-dead-letter-queue | 2 | 0 | 0 | idle | [Peek] [Purge] [Requeue All] |

Satır tıklanınca drawer açılır:
- Queue detay (arguments: x-dead-letter-exchange, x-message-ttl, vs.)
- "Son 10 mesajı göster" butonu → tablo
  - Message ID, Payload (JSON viewer), Timestamp, Headers
  - Her satırda "Requeue" ve "Delete" butonları

#### 3. Destructive Actions — Confirm Modal

Purge / Requeue / Delete butonları tıklanınca:
```
┌────────────────────────────────────────┐
│ ⚠️  email-dead-letter-queue            │
│                                        │
│ Bu işlem 2 mesajı kalıcı olarak       │
│ silecek. Emin misiniz?                 │
│                                        │
│ Queue adını yazın: [___________]       │
│                                        │
│              [İptal]  [Onayla]         │
└────────────────────────────────────────┘
```

Queue adını yazdırmak = "oops önlemi" (GitHub repo delete UX'i).

#### 4. API Client (panel tarafında)

```typescript
// panel/src/api/rabbit-admin.ts
export const rabbitAdminApi = {
  overview: () => http.get('/api/v1/admin/rabbit/overview'),
  listQueues: () => http.get('/api/v1/admin/rabbit/queues'),
  getQueue: (name: string) => http.get(`/api/v1/admin/rabbit/queues/${name}`),
  peekMessages: (name: string, count = 10) =>
    http.get(`/api/v1/admin/rabbit/queues/${name}/messages`, { params: { count } }),
  purgeQueue: (name: string) =>
    http.post(`/api/v1/admin/rabbit/queues/${name}/purge`),
  republish: (source: string, targetQueue: string, payload: string) =>
    http.post(`/api/v1/admin/rabbit/queues/${source}/republish`, { targetQueue, payload }),
};
```

#### 5. Rol Bazlı UI

- Kullanıcı `rabbit:read` yetkisine sahipse → sayfa görünür (read-only mode).
- `rabbit:manage` yetkisi varsa → Purge/Delete/Requeue butonları aktif; yoksa disabled.
- Permission kontrolü zaten mevcut JWT decode + permission store üzerinden.

#### 6. Özel Akış: "DLQ'dan email-queue'ya Requeue All"

DLQ'daki tüm FAILED mesajları toplu yeniden göndermek için:
```
DLQ detay drawer → "Tümünü Requeue" butonu → confirm modal
```

Backend bu işlem için özel endpoint: `POST /api/v1/admin/rabbit/queues/email-dead-letter-queue/requeue-all?target=email-queue`

Alternatif: Panel DLQ'yu peek ile okur, tek tek `republish` çağırır (N+1 problem). Bulk endpoint daha doğru.

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

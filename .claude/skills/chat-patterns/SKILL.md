---
name: chat-patterns
description: Elly chat sisteminin pattern'leri — WebSocket+STOMP konfigürasyonu, rol bazlı görünürlük (visibilityLevel), davet hiyerarşisi, guest chat, real-time broadcast topic'leri, multi-tenant chat routing, WebRTC arama sinyalleşmesi. Chat endpoint ekleme, WebSocket topic tanımlama, mesaj akışı debug veya rol/davet kuralı değişikliğinde otomatik aktif et.
version: 2.0.0
---

# Elly Chat Sistemi — Pattern'ler

## Mimari Özet — AC=TC tek model (basedb YOK)

```
Frontend (panel / tenant site / website widget)
   ├── REST: /api/v1/chat/**  (+ /tenant/{tid}/... alias'ları)
   └── WebSocket: ${API}/ws  (SockJS+STOMP, CONNECT'te Authorization: Bearer)
        ↓
Backend
   ├── REST Controller (ChatGroupController, ChatHistoryController, VisitorChatController)
   ├── WebSocket Controller (ChatWebSocketController, CallSignalController)
   ├── STOMP Channel Interceptor (ChatWebSocketSecurityConfig)
   └── Service (ChatGroupService, ChatMessageService, ChatGroupTenantRouter)
        ↓
Her TENANT DB'sinde 5 tablo (chat_groups, chat_group_members, chat_messages,
chat_message_reads, chat_message_edits) + call_logs
Redis (rate limit, typing TTL, presence TTL)
```

**Kritik kural (AC=TC):** Ayrı bir "admin chat DB'si" YOK. Her chat grubu ait olduğu
TENANT DB'sinde yaşar; admin sohbeti = tenantId'si adminin kendi tenant'ı olan grup.
Eski "chat tabloları sadece basedb'de" modeli d4ad211/e948ead ile kaldırıldı.

## Multi-tenant Routing — Chat
- **REST:** Tenant JWT `tenantId` claim'inden gelir (JwtTenantFilter). Chat path'i için
  basedb-forcing YOKTUR. Endpoint'lerin `/api/v1/chat/tenant/{tid}/...` alias'ları vardır
  (frontend URL şeması); DB routing yine JWT claim + `ChatGroupTenantRouter` ile yapılır.
- **WS CONNECT:** token `tenantId` claim'i ZORUNLU (admin/tenant/guest) — yoksa CONNECT reddedilir.
- **`ChatGroupTenantRouter.useGroupDatabase(group)`:** context'i grubun tenantId'sine sabitler;
  cross-tenant tarama kaldırıldı.
- **STOMP handler'larında:** TenantContext try/finally ile set/clear — ZORUNLU.
- Yeni grup/DM oluştururken `tenantId` DAİMA set edilir (`createGroup`/`getOrCreateDm`
  TenantContext'ten doldurur). tenant_id NULL grup canlı yayın alamaz (legacy fallback var ama
  yeni kayıt üretmemeli).

## Rol Seviyesi Tablosu
| Rol | Level |
|---|---|
| SUPER_ADMIN | 4 |
| ADMIN | 3 |
| EDITOR | 2 |
| VIEWER / TENANT (veya bilinmeyen) | 1 |

Kullanıcının **en yüksek** rolünün level'ı döner.

## visibilityLevel Kuralları
| Grup oluşturanın rolü | `visibilityLevel` | Görüntüleyebilenler |
|---|---|---|
| VIEWER | 1 | Herkes |
| EDITOR | 2 | EDITOR, ADMIN, SUPER_ADMIN |
| ADMIN | 3 | ADMIN, SUPER_ADMIN |
| SUPER_ADMIN | 4 | Yok (sadece davet edilenler) |
| DM (her durumda) | 4 | Sadece iki taraf |

İstisna: public TC grubu (`tenantId` dolu + `visitorAccess=true`) → tüm panel rolleri okur/yazar,
görünürlük/üyelik kuralı atlanır.

⚠️ **LEFT JOIN ON embedded-id kullanma** — Hibernate 6 güvenilmez. Üyelik kontrolü için `EXISTS` subquery kullan.

## Davet Hiyerarşisi
Davet eden, KENDİ SEVİYESİ ve altındaki kullanıcıları davet edebilir (admin→admin dahil);
SUPER_ADMIN (level 4) herkesi:
```java
if (requesterLevel < 4 && targetLevel > requesterLevel) {
  throw new ForbiddenException(...);
}
```
| Davet eden | Davet edebileceği |
|---|---|
| VIEWER | — (chat:manage yok, controller'da bloklanır) |
| EDITOR | EDITOR, VIEWER |
| ADMIN | ADMIN, EDITOR, VIEWER |
| SUPER_ADMIN | Herkes |

**E-posta ile davet:** `POST /groups/{id}/members/by-email` (+ `/tenant/{tid}/...` alias)
— e-posta grubun tenant DB'sinde aranır, bulunmazsa 404. `@PreAuthorize("chat:manage")`.

TENANT rollü kullanıcılar `/api/v1/chat/**`'e erişemez (chat:read yok) —
`/api/v1/tenant-chat/**` (VisitorChatController) kullanır.

## Guest Chat (website widget)
- Token: `POST /api/v1/public/{tid}/auth/guest-token` — `tenantId` + `sessionId` ZORUNLU
  (guest'in DB kullanıcısı yok; hangi tenant DB'sine yazılacağı yalnız claim'den bilinir).
  `loginSource=website`, `chat:guest` authority.
- Yazma kapısı TEK: grup `tenantId` dolu + `visitorAccess=true` (`checkGuestWriteAccess`).
- Ban: grubun tenant DB'sinde, sessionId VEYA visitorId ile; `/bans` topic'ine broadcast.

## Bildirim (CHAT_MESSAGE)
- YALNIZ tenant tarafından gelen mesajlarda üretilir: guest, visitor veya TENANT rollü gönderen.
- Alıcılar: o tenant DB'sindeki aktif SUPER_ADMIN + ADMIN (EDITOR dahil DEĞİL).
- Panel rolü mesaj gönderirse bildirim YOK (spam önlemi). Fire-and-forget — hata mesaj akışını bozmaz.
- İzolasyon tenant DB'sinin kendisiyle sağlanır (managedTenants konsepti kaldırıldı).

## STOMP Topic Şeması

**Server → Client (subscribe):**
| Topic | Payload | Tetiklenme |
|---|---|---|
| `/topic/tenant/{tid}/group/{groupId}` | `DtoChatMessage` | Yeni mesaj |
| `/topic/tenant/{tid}/group/{groupId}/typing` | `DtoChatTyping` | Yazıyor sinyali |
| `/topic/tenant/{tid}/group/{groupId}/read` | `DtoChatRead` | Okundu |
| `/topic/tenant/{tid}/group/{groupId}/bans` | `DtoChatBanEvent` | Ban/unban |
| `/topic/groups/new` | `DtoChatGroup` | Grup oluşturulunca |
| `/topic/groups/deleted` | `groupId` (string) | Grup silinince |
| `/topic/tenant/{tid}/user/{userId}/membership` | membership event | Davet/çıkarma (banner dahil) |
| `/topic/tenant/{tid}/user/{userId}/groups/joined` · `/groups/removed` | `DtoChatGroup` · membership event | Gruba eklendi / çıkarıldı |
| `/topic/presence` | `DtoChatPresence` | Online/offline |
| `/topic/tenant/{tid}/user/{userId}/rtc` | `DtoCallSignal` | WebRTC sinyali (ESAS yol) |
| `/topic/rtc-client/{clientRef}/rtc` | `DtoCallSignal` | Tenant caller'ın kendi sinyalleri |
| `/user/queue/notifications` · `/notifications/unread-count` · `/membership` · `/groups/joined` · `/groups/removed` · `/rtc` · `/chat-errors` | ilgili payload | Oturuma özel; teslimat sunucuda tenant'lı anahtarla — **ama bugün hiçbiri teslim edilmiyor, bkz. not** |

(`/topic/group/{id}` legacy — yeni kod tenant-aware `ChatTopics` üretimini kullanmalı.
`/topic/user/{id}/…` tenant'sız eski kullanıcı konusudur — **GEÇİŞ**: yalnız panel oturumlarında (`loginSource=admin`)
aboneliğe ve yayına açık, istemciler tenant'lı konuya geçince kaldırılır.)

**Kullanıcı hedefli kuyruk (`/user/queue/**`) bugün teslim etmiyor.** `WebSocketConfig` basit aracı
`enableSimpleBroker("/topic", "/user")` ile kurulu; kullanıcı hedefli çözümleme `/queue/<ad>-user<oturumKimliği>`
hedefi üretir, o hiçbir aracı önekine uymaz ve yayın atılır (2026-11-24 canlı doğrulama: okunmamış sayısı itmesi
200 dönüyor, kare gelmiyor). Panel/mobil bu yüzden anlık bildirim itmeyi WS'ten almıyor. Kuyruğu açmak
(`"/queue"` öneki) ayrı bir iş: admin aynı üyelik/çağrı sinyallerini hem konudan hem kuyruktan dinler → tekillik şart.

**Kullanıcı konuları neden tenant'lı:** kullanıcı **numaraları** ve **adları** her tenant veritabanına
özeldir (iki tenant'ta da `admin` olabilir). Konu adı tenant'sız kalırsa başka tenant'taki aynı numaralı
kullanıcı o konuya abone olup çağrı sinyallerini alabilir; kullanıcı hedefli kuyruk (`convertAndSendToUser`)
kullanıcı adına bakar, anahtar tenant'sız olursa bildirim başka tenant'taki aynı adlı oturuma düşer.
Bu yüzden: konular `ChatTopics.userRtcTopic(tenantId, userId)` vb. ile üretilir; kuyruk gönderimlerinde
anahtar `ChatTopics.userKey(tenantId, username)` olur (WS oturumu da bu anahtarla kurulur);
`/rtc/{callId}/*` katılımcı denetimi tenant'ı da karşılaştırır.

**Client → Server (publish):**
| Destination | İşlem |
|---|---|
| `/app/tenant-chat/{tid}/{groupId}/send` | Mesaj kaydet + broadcast (guest destekli, TERCİH) |
| `/app/chat/{groupId}/send`, `/typing`, `/read` | Legacy — ileride kaldırılacak |
| `/app/rtc/call` | Panel-içi arama başlat |
| `/app/tenant-rtc/{tid}/call` | Tenant destek araması (ring-all) |
| `/app/rtc/{callId}/{answer,reject,cancel,hangup,sdp,ice}` | Çağrı yaşam döngüsü + SDP/ICE relay |

## WebRTC Arama (Faz 1)
- Backend yalnız sinyalleşme (SDP/ICE opak relay) + orkestrasyon; medya P2P. ICE sunucu listesi:
  `GET /api/v1/notifications/ice-servers` (`TURN_URL` / `TURN_USERNAME` / `TURN_CREDENTIAL` env'leri; prod'da coturn — `k8s/2f-coturn.yaml`).
- Panel-içi: hiyerarşi kuralı (kendi seviyesi ve altı; aynı chat grubunun üyeleriyse ATLANIR).
- Ring-all: tenant destek çağrısı tüm online panel kullanıcılarını çaldırır; ilk cevaplayan
  atomik kazanır (CALL_TAKEN diğerlerine).
- Durum in-memory (`CallSessionService`) — TEK instance varsayımı; 30sn ring timeout; busy/presence kontrolü.
- Çevrimiçi kaydı: `chat:presence:<tenantId>:<userId>` (Redis; kullanıcı numaraları tenant'a özeldir). Ömür
  300 sn, instance'ın açık WS oturumları için 60 sn'de bir tazelenir (`ChatPresenceService.refreshLiveSessions`) —
  tazeleme yoksa 5 dakikadan uzun bağlantılar "çevrimdışı" görünür ve aranamaz.
- `call_logs` her tenant DB'sinde (migration: `db-migration-call-logs.sql`).

## REST Endpoint Pattern'i
`@RequestMapping` annotation'ı **impl class'ta** olmalı (Spring 6 interface annotation'ları detect etmiyor):
```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")   // ← impl'de
public class ChatGroupController implements IChatGroupController {

  @Override
  @PostMapping({"/groups", "/tenant/{tenantId}/groups"})   // ← impl'de, tenant alias'lı
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatGroup>> createGroup(...) { ... }
}
```

## SecurityConfig — WebSocket Path
```java
// ❌ Bu çalışmaz (MvcRequestMatcher, SockJS handler'ı bulamaz)
.requestMatchers("/ws/**").permitAll()

// ✅ Doğru
.requestMatchers(AntPathRequestMatcher.antMatcher("/ws/**")).permitAll()
```

## PostgreSQL Cursor Pagination
❌ JPQL'de `LIMIT :limit` Spring Data ile çalışmaz → `Pageable.of(0, limit)` kullan.

❌ `(:before IS NULL OR createdAt < :before)` ile null tarih parametresi → PostgreSQL
`could not determine data type of parameter` hatası.

✅ İki ayrı query method:
```java
List<ChatMessage> findByGroupId(UUID groupId, Pageable pageable);
List<ChatMessage> findByGroupIdBefore(UUID groupId, Date before, Pageable pageable);
```
Service'te `if (before == null) ... else ...` ile seç.

## DataSource & Schema Validation
Production'da `JPA_DDL_AUTO=validate`. Yeni kolon eklerken:
1. Entity field ekle
2. İlgili migration SQL'ine `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` yaz
3. **HER tenant DB'sine** (tenant1 + tenant2) manuel çalıştır — chat tabloları her tenant'ta!
4. Ardından pod'u sil → yeni image ayağa kalkar

Aksi halde `SchemaManagementException` → CrashLoopBackOff.

## Frontend Notları
- JWT **JWE encrypted** — `atob` decode etmez, içerik şifreli.
- Rol bilgisi için `GET /api/v1/users/me/permissions` çağrılır.
- WebSocket: `@stomp/stompjs` + `sockjs-client`, CONNECT header `Authorization: Bearer <token>`.
- Admin gönderen adı mesaja YAZARKEN denormalize edilir (`senderDisplayName`) — okumada
  cross-DB lookup yapılmaz.

## Yeni Topic Eklerken Checklist
1. **Backend:** topic üretimini `ChatTopics`'e ekle (tenant-aware!), `messagingTemplate.convertAndSend(...)`
2. **WebSocketConfig:** Yeni `/app/...` destination ise `@MessageMapping` ekle
3. **Frontend store:** `onConnect` içinde `client.subscribe('/topic/...', handler)`
4. **changelog.md:** Topic tablosuna ekle

## Yeni Chat Endpoint Eklerken Checklist
1. Interface'de metot tanımı (Swagger için)
2. Impl class'ta: `@RequestMapping("/api/v1/chat")` class seviyesinde; metoda mapping
   (+ `/tenant/{tid}/...` alias) ve `@PreAuthorize`; `ResponseEntity<RootEntityResponse<T>>` dönüş
3. Grup üzerinden iş yapılıyorsa `ChatGroupTenantRouter.useGroupDatabase(group)` ile
   context'i grubun DB'sine sabitle
4. Üyelik kontrolü gerekirse `groupService.checkAccess(groupId, userId)` çağır

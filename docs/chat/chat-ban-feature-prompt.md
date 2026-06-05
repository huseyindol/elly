# Chat Ban (TC yazma engeli) — Frontend Entegrasyon Prompt'ları

> **Backend HAZIR** (commit `c1837a9`). Banlı guest/visitor sohbeti **görür ama yazamaz**
> (gönderim backend'de `CHAT_BANNED` ile reddedilir). Bu dosya **iki ayrı agent prompt'u**
> içerir: **A) Admin panel** (ban/unban UI) ve **B) Tenant website / widget** (banlıysa input kilit).
>
> Ban yapanlar: **SUPER_ADMIN / ADMIN / EDITOR** (`chat:manage`). Yalnızca **TC** (tenant chat) grupları.

---

## Ortak — Backend sözleşmesi (her iki agent bilsin)

### REST (admin JWT + `X-Tenant-Id` = TC grubun tenant'ı)
| İşlem        | Method + Path                                                   | Auth                  | Body / Query                          | data           |
| ------------ | -------------------------------------------------------------- | --------------------- | ------------------------------------- | -------------- |
| Banla        | `POST /api/v1/chat/groups/{groupId}/bans`                      | `chat:manage` (EDITOR+) | `{ sessionId?, visitorId?, reason? }` | `DtoChatBan`   |
| Ban kaldır   | `DELETE /api/v1/chat/groups/{groupId}/bans?sessionId=&visitorId=` | `chat:manage`       | query                                 | 204            |
| Ban listesi  | `GET /api/v1/chat/groups/{groupId}/bans`                       | `chat:read`           | —                                     | `DtoChatBan[]` |

- **Tam olarak BİR hedef**: `sessionId` (GUEST mesajından) **VEYA** `visitorId` (VISITOR mesajından).
- ADMIN mesajları banlanamaz (ban hedefi yok).
- Yanıt wrapper'lı: `{ "result": true, "data": <T> }`.

```ts
interface DtoChatBan {
  id: string
  groupId: string
  sessionId: string | null   // guest ban hedefi
  visitorId: number | null   // visitor ban hedefi
  bannedByUserId: number
  bannedByUsername: string | null
  reason: string | null
  createdAt: string
}
```

### WebSocket — ban/unban canlı olayı
- SUBSCRIBE: `/topic/tenant/{tenantId}/group/{groupId}/bans`
- payload:
```ts
interface ChatBanEvent {
  action: 'BANNED' | 'UNBANNED'
  groupId: string
  sessionId: string | null
  visitorId: number | null
  byUsername: string | null
}
```

### Enforcement (backend zaten yapıyor — frontend sadece UX)
- Banlı guest/visitor mesaj **gönderince** backend reddeder (`errorCode = CHAT_BANNED`).
- **Okuma / subscribe SERBEST** — banlı kişi akışı görmeye devam eder.

---

## A) PANEL PROMPT — ban/unban yönetimi

````
elly-admin-panel'de TC (tenant chat) sohbetine ban/unban özelliği ekle. chat:manage olan
kullanıcı (SUPER_ADMIN/ADMIN/EDITOR) bir GUEST/VISITOR mesajının yanındaki menüden
"Banla" / "Ban kaldır" yapabilsin; banlı gönderenler işaretli görünsün. Banlı kişi sohbeti
görmeye devam eder ama yazamaz (backend zorlar).

## Bağlam
- Backend hazır. Sözleşme: bu dosyanın "Ortak — Backend sözleşmesi" bölümü.
- Panel TC istekleri zaten `X-Tenant-Id` header'ı ile gidiyor (mevcut `tenantHeader(tenantId)`).
- Ban hedefi MESAJDAN gelir: GUEST → `msg.sessionId`, VISITOR → `msg.visitorId`.
  (Panel ChatMessage tipinde `sessionId: string | null` ve `visitorId: number | null` yoksa ekle.)

## Görev
1. `src/app/_services/chat.services.ts`'e ekle:
   - `banUser(groupId, body: { sessionId?: string; visitorId?: number; reason?: string }, tenantId?)`
     → POST `/api/v1/chat/groups/${groupId}/bans` (tenantHeader)
   - `unbanUser(groupId, target: { sessionId?: string; visitorId?: number }, tenantId?)`
     → DELETE `/api/v1/chat/groups/${groupId}/bans?` + query (tenantHeader)
   - `listBans(groupId, tenantId?)` → GET `/api/v1/chat/groups/${groupId}/bans` (tenantHeader)
2. Aktif grupta ban listesini tut: açılışta `listBans` → bir `Set<string|number>` (banned
   sessionId/visitorId). chat-ws-store'da `bannedKeys` state'i.
3. chat-ws-store `attachActiveGroupSubs`/`onConnect`'te `/topic/tenant/{tid}/group/{gid}/bans`
   abone ol → `BANNED` → key ekle, `UNBANNED` → key çıkar (mesaj listesine DOKUNMA).
4. Mesaj balonu (`MessageBubble` benzeri): kendi olmayan GUEST/VISITOR mesajında bir "⋯" menü:
   - `hasPermission('chat:manage')` ise: banlı değilse **"Banla"** (opsiyonel reason modalı),
     banlıysa **"Ban kaldır"**.
   - Banlı gönderenin balonunda küçük **"banlı"** rozeti.
   - ADMIN mesajında menü gösterme.
5. Ban/unban sonrası: optimistic `bannedKeys` güncelle; WS event zaten teyit eder.

## Önemli
- Ban hedefi tekildir: GUEST mesajı → `{ sessionId }`, VISITOR mesajı → `{ visitorId }`.
- Sadece `chat:manage` ban/unban yapar; `chat:read` ban listesini görür (rozet için).
- AC (admin chat, tenantId yok) gruplarında ban YOK — buton sadece TC'de (tenantId dolu) görünsün.

## Doğrulama
- chat:manage kullanıcı bir guest mesajını "Banla" → guest widget'ta input ANINDA kilitlenir.
- Panelde o gönderen "banlı" rozetiyle görünür; "Ban kaldır" → guest tekrar yazabilir.
- chat:read (VIEWER) kullanıcı ban/unban butonlarını GÖRMEZ (sadece rozet).
- AC sohbetinde ban menüsü çıkmaz.
````

---

## B) TENANT / WIDGET PROMPT — banlıysa input kilit

> Tenant website (Next.js `useGuestChat`) VEYA Lit widget (`<elly-chat>`) — hangisini
> kullanıyorsan ona uygula. Mantık aynı.

````
Tenant chat (anonim guest) tarafında: banlanan ziyaretçi sohbeti GÖRMEYE devam eder ama
YAZAMAZ. WS ban olayını dinle; kendi sessionId'in banlanınca composer'ı kilitle.

## Bağlam
- Backend hazır. Sözleşme: bu dosyanın "Ortak — Backend sözleşmesi" bölümü.
- Guest kendi kimliğini `mySessionId` ile biliyor (per-device localStorage; guest-token
  yanıtındaki sessionId). "Kendi mesajım" eşleşmesi zaten bununla yapılıyor.

## Görev
1. WS'te mevcut mesaj/typing aboneliklerinin yanına ban aboneliği ekle:
   `client.subscribe('/topic/tenant/{tenantId}/group/{groupId}/bans', handleBan)`
2. `handleBan(event: ChatBanEvent)`:
   - `event.sessionId && event.sessionId === mySessionId`:
     - `action === 'BANNED'`  → `banned = true`
     - `action === 'UNBANNED'`→ `banned = false`
   - (Kayıtlı VISITOR akışı kullanıyorsan `event.visitorId === myVisitorId` ile aynı mantık.)
3. `banned` state'ini dışarı ver (hook return / Lit @state).
4. Composer: `banned` ise input + gönder butonu **disabled**, placeholder/banner:
   "Yazma yetkiniz kaldırıldı. Mesajları görmeye devam edebilirsiniz."
5. Mesaj akışı + history DEĞİŞMEZ (banlı kişi okumaya devam eder).

## Edge — reconnect / sayfa yenileme
Ban olayı yalnızca ban ANINDA yayınlanır; banlı ziyaretçi sayfayı yenilerse event almaz.
MVP davranışı: ziyaretçi yazıp gönderince backend reddeder (STOMP `onStompError` →
`errorCode/CHAT_BANNED` veya mesaj echo'su gelmez). Bu durumda da `banned = true` yap.
(İleri seviye: backend WS CONNECT'te guest ban durumunu push etsin — ayrı geliştirme.)

## Doğrulama
- Panelden bu ziyaretçi banlanınca → widget composer'ı ANINDA kilitlenir + banner görünür.
- Akış görünmeye devam eder; admin yeni mesaj atınca ziyaretçi görür ama yazamaz.
- Ban kaldırılınca → composer tekrar açılır.
- (Edge) banlı ziyaretçi reconnect olup yazmayı denerse mesaj gitmez; UI banned'a düşer.
````

---

## Notlar
- Backend deploy ÖNCESİ `chat_bans` tablosu **basedb + tenant1 + tenant2**'de oluşmalı
  (prod `ddl-auto=validate`). Migration: `db-migration-chat-bans.sql`.
- Ban per-group'tur (bir destek grubu). Aynı ziyaretçi farklı grupta banlı olmayabilir.
- visitorId akışı (kayıtlı tenant user) opsiyoneldir; ana senaryo anonim **guest (sessionId)**.

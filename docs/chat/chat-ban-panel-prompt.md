# Chat Ban (TC) — PANEL Entegrasyon Prompt'u

> **Backend HAZIR** (commit `c1837a9`). Bu dosya **admin panel** agent'ı içindir:
> `chat:manage` olan kullanıcı (**SUPER_ADMIN / ADMIN / EDITOR**) bir TC (tenant chat)
> sohbetinde guest/visitor'ı **banlar / ban kaldırır**; banlı kişi sohbeti **görür ama yazamaz**.
>
> Tenant website / widget tarafı (banlıysa input kilit) **ayrı dosyada**:
> [`chat-ban-tenant-prompt.md`](./chat-ban-tenant-prompt.md)

## Backend sözleşmesi

### REST (admin JWT + `X-Tenant-Id` = TC grubun tenant'ı — panel zaten ekliyor)
| İşlem       | Method + Path                                                    | Auth                    | Body / Query                          | data           |
| ----------- | --------------------------------------------------------------- | ----------------------- | ------------------------------------- | -------------- |
| Banla       | `POST /api/v1/chat/groups/{groupId}/bans`                       | `chat:manage` (EDITOR+) | `{ sessionId?, visitorId?, reason? }` | `DtoChatBan`   |
| Ban kaldır  | `DELETE /api/v1/chat/groups/{groupId}/bans?sessionId=&visitorId=` | `chat:manage`         | query                                 | 204            |
| Ban listesi | `GET /api/v1/chat/groups/{groupId}/bans`                        | `chat:read`             | —                                     | `DtoChatBan[]` |

- **Tam olarak BİR hedef**: `sessionId` (GUEST mesajından) **VEYA** `visitorId` (VISITOR mesajından).
- ADMIN mesajları banlanamaz. AC (tenantId yok) gruplarında ban YOK.
- Yanıt wrapper'lı: `{ "result": true, "data": <T> }`.

```ts
interface DtoChatBan {
  id: string; groupId: string
  sessionId: string | null   // guest ban hedefi
  visitorId: number | null   // visitor ban hedefi
  bannedByUserId: number; bannedByUsername: string | null
  reason: string | null; createdAt: string
}
```

### WebSocket — ban/unban canlı olayı (panel ban listesini tazeler)
- SUBSCRIBE: `/topic/tenant/{tenantId}/group/{groupId}/bans`
```ts
interface ChatBanEvent {
  action: 'BANNED' | 'UNBANNED'
  groupId: string; sessionId: string | null; visitorId: number | null; byUsername: string | null
}
```

---

````
elly-admin-panel'de TC (tenant chat) sohbetine ban/unban özelliği ekle. chat:manage olan
kullanıcı (SUPER_ADMIN/ADMIN/EDITOR) bir GUEST/VISITOR mesajının yanındaki menüden
"Banla" / "Ban kaldır" yapabilsin; banlı gönderenler işaretli görünsün. Banlı kişi sohbeti
görmeye devam eder ama yazamaz (backend zorlar). Sözleşme: yukarıdaki "Backend sözleşmesi".

## Görev
1. `src/app/_services/chat.services.ts`'e ekle (X-Tenant-Id = mevcut `tenantHeader(tenantId)`):
   - `banUser(groupId, body: { sessionId?: string; visitorId?: number; reason?: string }, tenantId?)`
     → POST `/api/v1/chat/groups/${groupId}/bans`
   - `unbanUser(groupId, target: { sessionId?: string; visitorId?: number }, tenantId?)`
     → DELETE `/api/v1/chat/groups/${groupId}/bans?` + query
   - `listBans(groupId, tenantId?)` → GET `/api/v1/chat/groups/${groupId}/bans` → DtoChatBan[]
2. Ban hedefi MESAJDAN gelir: GUEST → `msg.sessionId`, VISITOR → `msg.visitorId`.
   (Panel `ChatMessage` tipinde `sessionId: string | null` ve `visitorId: number | null` yoksa ekle.)
3. Aktif grupta ban durumu: açılışta `listBans` → `bannedKeys` Set<string|number> (sessionId/visitorId).
4. chat-ws-store onConnect/aktif-grup aboneliklerine `/topic/tenant/{tid}/group/{gid}/bans` ekle →
   `BANNED` key ekle, `UNBANNED` key çıkar (mesaj listesine DOKUNMA).
5. Mesaj balonu (kendi olmayan GUEST/VISITOR): "⋯" menü →
   - `hasPermission('chat:manage')` ise: banlı değilse **Banla** (opsiyonel reason), banlıysa **Ban kaldır**.
   - Banlı gönderende küçük **"banlı"** rozeti.
   - ADMIN mesajında ve AC grubunda (tenantId yok) menü gösterme.
6. Yalnızca `chat:manage` ban/unban yapar; `chat:read` ban listesini görür (rozet için).

## Doğrulama
- chat:manage kullanıcı bir guest mesajını "Banla" → guest tarafında input ANINDA kilitlenir (WS).
- Panelde o gönderen "banlı" rozetiyle görünür; "Ban kaldır" → guest tekrar yazabilir.
- VIEWER (chat:read) ban/unban butonlarını GÖRMEZ (sadece rozet). AC'de ban menüsü çıkmaz.
````

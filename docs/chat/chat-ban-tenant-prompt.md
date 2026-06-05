# Chat Ban (TC) — TENANT / WIDGET Entegrasyon Prompt'u

> **Backend HAZIR** (commit `c1837a9`). Bu dosya **tenant website / widget** agent'ı içindir
> (Next.js `useGuestChat` veya Lit `<elly-chat>`): **banlanan ziyaretçi sohbeti GÖRMEYE
> devam eder ama YAZAMAZ** — kendi sessionId'i banlanınca composer kilitlenir.
>
> Ban'ı YAPAN admin panel tarafı **ayrı dosyada**:
> [`chat-ban-panel-prompt.md`](./chat-ban-panel-prompt.md)

## Backend sözleşmesi (tenant'ın gördüğü yüzey)

Tenant/widget **ban yapmaz**, yalnızca **ban olayını dinler** ve enforcement'a uyar.

### WebSocket — ban/unban canlı olayı
- SUBSCRIBE: `/topic/tenant/{tenantId}/group/{groupId}/bans`  (mevcut mesaj/typing abonelikleriyle aynı bağlantı)
```ts
interface ChatBanEvent {
  action: 'BANNED' | 'UNBANNED'
  groupId: string
  sessionId: string | null   // guest hedefi
  visitorId: number | null   // (kayıtlı visitor akışı kullanıyorsan)
  byUsername: string | null
}
```

### Enforcement (backend zaten yapıyor)
- Banlı ziyaretçi mesaj **gönderince** backend reddeder (`errorCode = CHAT_BANNED`).
- **Okuma / subscribe SERBEST** — banlı ziyaretçi akışı görmeye devam eder.

---

````
Tenant chat (anonim guest) tarafında: banlanan ziyaretçi sohbeti GÖRMEYE devam eder ama
YAZAMAZ. WS ban olayını dinle; kendi sessionId'in banlanınca composer'ı kilitle. Sözleşme:
yukarıdaki "Backend sözleşmesi". (Next.js useGuestChat ya da Lit <elly-chat> — hangisini
kullanıyorsan ona uygula; mantık aynı.)

## Bağlam
- Guest kendi kimliğini `mySessionId` ile biliyor (per-device localStorage; guest-token
  yanıtındaki sessionId). "Kendi mesajım" eşleşmesi zaten bununla yapılıyor.

## Görev
1. WS'te mevcut mesaj/typing aboneliklerinin yanına ban aboneliği ekle:
   `client.subscribe('/topic/tenant/{tenantId}/group/{groupId}/bans', handleBan)`
2. `handleBan(event: ChatBanEvent)`:
   - `event.sessionId && event.sessionId === mySessionId`:
     - `action === 'BANNED'`   → `banned = true`
     - `action === 'UNBANNED'` → `banned = false`
   - (Kayıtlı VISITOR akışı kullanıyorsan `event.visitorId === myVisitorId` ile aynı mantık.)
3. `banned` state'ini dışarı ver (hook return / Lit @state).
4. Composer: `banned` ise input + gönder butonu **disabled**, placeholder/banner:
   "Yazma yetkiniz kaldırıldı. Mesajları görmeye devam edebilirsiniz."
5. Mesaj akışı + history DEĞİŞMEZ (banlı kişi okumaya devam eder).

## Edge — reconnect / sayfa yenileme
Ban olayı yalnızca ban ANINDA yayınlanır; banlı ziyaretçi sayfayı yenilerse o olayı almaz.
MVP davranışı: ziyaretçi yazıp gönderince backend reddeder (STOMP `onStompError` →
`CHAT_BANNED`, ya da mesaj echo'su gelmez). Bu durumda da `banned = true` yap.
(İleri seviye: backend WS CONNECT'te guest ban durumunu push etsin — ayrı geliştirme.)

## Doğrulama
- Panelden bu ziyaretçi banlanınca → composer ANINDA kilitlenir + banner görünür.
- Akış görünmeye devam eder; admin yeni mesaj atınca ziyaretçi görür ama yazamaz.
- Ban kaldırılınca → composer tekrar açılır.
- (Edge) banlı ziyaretçi reconnect olup yazmayı denerse mesaj gitmez; UI `banned`'a düşer.
````

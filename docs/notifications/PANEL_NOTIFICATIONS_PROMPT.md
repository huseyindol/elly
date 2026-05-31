# Panel Prompt — Bildirim (Notification) UI'ı

> Bu dosya **elly-admin-panel projesinde** bir AI agent'a birebir kopyala-yapıştır
> edilecek prompt'tur. Agent backend repoyu görmez; tüm API/WS sözleşmesi burada.
> Backend HAZIR ve deploy edildi.

---

````
elly-admin-panel (Next.js, App Router) projesine "Notification" (bildirim) UI'ı ekle.
Backend HAZIR ve deploy edilmiş durumda; aşağıdaki REST + WebSocket sözleşmesini tüket.
Amaç: Header'daki zil ikonunu gerçek, kalıcı bildirimlerle beslemek — okunmamış sayısı
rozet olarak, dropdown'da son bildirimler, bir bildirime tıklayınca ilgili sayfaya gidip
okundu işaretlemek.

## Bağlam — panel mevcut mimari (varsayımlar)
- Next.js App Router. API client zaten var (axios/fetch) ve admin JWT'yi Authorization:
  Bearer olarak ekliyor. Base URL: process.env.NEXT_PUBLIC_API.
- Tüm REST yanıtları RootEntityResponse<T> ile sarılı: { result, message, data }.
- Veri çekme TanStack Query ile.
- Chat için zaten bir STOMP/SockJS client var (örn. chat-ws-store): `${NEXT_PUBLIC_API}/ws`
  endpoint'ine CONNECT header `Authorization: Bearer <adminJWT>` ile bağlanıyor.
  Bildirimler AYNI WS bağlantısını kullanır — yeni bir bağlantı AÇMA, sadece abonelik ekle.

## REST API (auth: admin JWT Bearer; kullanıcı yalnız KENDİ bildirimlerini görür)

| Method | Path                                    | data |
|--------|-----------------------------------------|------|
| GET    | /api/v1/notifications?page=&size=&unread= | Page<Notification> |
| GET    | /api/v1/notifications/unread-count      | { count: number } |
| POST   | /api/v1/notifications/{id}/read         | Notification (read=true) |
| POST   | /api/v1/notifications/read-all          | { updated: number } |
| DELETE | /api/v1/notifications/{id}              | null |

Önemli kurallar:
- **X-Tenant-Id GÖNDERME.** Bildirimler basedb'de tutulur; backend bu path'i otomatik
  basedb'ye sabitler. Tenant header'ı gereksiz (gönderilse de yok sayılır).
- Sayfalama Spring Pageable: `?page=0&size=20` (0 tabanlı). Sıralama backend'de
  createdAt DESC olarak sabit — `sort` göndermene gerek yok.
- `unread=true` → yalnız okunmamışları döndürür. Parametre yoksa hepsi.
- read/read-all/delete sonrası backend WS ile güncel unread-count'u da yayınlar (aşağıya bak).

### Örnek — GET /api/v1/notifications?size=2
```json
{
  "result": true,
  "message": null,
  "data": {
    "content": [
      {
        "id": 12,
        "userId": 3,
        "type": "FORM_SUBMISSION",
        "title": "Yeni form gonderimi",
        "message": "İletişim formuna yeni gönderim",
        "link": "/forms/8/submissions",
        "read": false,
        "tenantId": "tenant1",
        "metadata": { "formId": 8, "submissionId": 144 },
        "createdAt": "2026-05-31T10:22:00.000+00:00"
      }
    ],
    "totalElements": 1, "totalPages": 1, "number": 0, "size": 2
  }
}
```
### Örnek — GET /api/v1/notifications/unread-count
```json
{ "result": true, "message": null, "data": { "count": 4 } }
```

## Veri Modeli (TypeScript)
```ts
export type NotificationType =
  | 'FORM_SUBMISSION'
  | 'EMAIL_FAILED'
  | 'CHAT_MESSAGE'
  | 'USER_REGISTERED'
  | 'MAIL_VERIFY_FAILED'
  | 'SYSTEM'

export interface Notification {
  id: number
  userId: number
  type: NotificationType
  title: string
  message: string
  link: string | null            // panelde gidilecek yol — doğrudan router.push(link)
  read: boolean
  tenantId: string | null        // bilgi amaçlı (olay hangi tenant'tan geldi)
  metadata: Record<string, unknown> | null
  createdAt: string              // ISO tarih
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number   // mevcut sayfa (0 tabanlı)
  size: number
}
```
- `link` alanı backend'den hazır gelir (örn. "/forms/8/submissions", "/email-logs",
  "/users", "/mail-accounts"). Panelde tip→yol eşlemesi YAPMA; doğrudan `notification.link`
  kullan (null ise tıklama no-op).

## WebSocket — gerçek zamanlı push
- Endpoint: `${NEXT_PUBLIC_API}/ws` (SockJS + STOMP), CONNECT header
  `Authorization: Bearer <adminJWT>`. (Chat ile AYNI bağlantı — onu yeniden kullan.)
- Abonelikler (Spring "user destination" — bağlı admin'e özeldir, userId GEREKMEZ):
  - `/user/queue/notifications` → payload: TAM Notification nesnesi (yeni bildirim).
  - `/user/queue/notifications/unread-count` → payload: `{ count: number }`
    (okundu/okundu-hepsi/sil/yeni bildirim olaylarında backend yayınlar).
- Davranış:
  - `/user/queue/notifications` geldiğinde: dropdown listesinin BAŞINA ekle + rozeti artır
    (veya unread-count query'sini invalidate et).
  - `/user/queue/notifications/unread-count` geldiğinde: rozet sayısını bununla güncelle
    (kaynak-doğruluk budur).

## Yapılacak dosyalar
- `src/types/notification.ts` — yukarıdaki tipler (+ RootEntityResponse zaten varsa onu kullan).
- `src/app/_services/notifications.services.ts`:
  - `listNotifications(params: { page?: number; size?: number; unread?: boolean })`
  - `getUnreadCount(): Promise<number>`
  - `markRead(id: number): Promise<Notification>`
  - `markAllRead(): Promise<{ updated: number }>`
  - `removeNotification(id: number): Promise<void>`
  (Mevcut API client'ı kullan; RootEntityResponse'tan `.data`'yı aç.)
- `src/app/_hooks/useNotifications.ts` — TanStack Query:
  - `useUnreadCount()` → `getUnreadCount`, `refetchInterval` (örn. 60sn) fallback + WS event'iyle
    invalidate/`setQueryData`.
  - `useNotificationList()` → dropdown için ilk ~10 (`size: 10`).
  - mutation'lar: markRead / markAllRead / remove → başarıda ilgili query'leri invalidate et.
- WS aboneliği: mevcut STOMP client'a (chat-ws-store benzeri) iki `/user/queue/...` aboneliği ekle;
  gelen event'lerde query cache'i güncelle.
- `src/components/.../Header.tsx` (zil ikonu): statik "5" rozetini KALDIR →
  - gerçek `unreadCount` rozeti (0 ise gizle, 99+ kıst).
  - dropdown: son bildirimler (title, message, göreli zaman, okundu/okunmadı vurgusu).
  - bir bildirime tıkla → `markRead(id)` + `router.push(notification.link)` (link varsa).
  - "Tümünü okundu işaretle" butonu → `markAllRead()` → rozet 0.
  - (opsiyonel) tek bildirimi sil → `removeNotification(id)`.

## Kabul kriterleri
- [ ] Zil rozeti gerçek okunmamış sayısını gösteriyor (REST'ten yüklenir, WS'le canlı güncellenir).
- [ ] Dropdown son bildirimleri listeliyor; okunmuş/okunmamış görsel ayrımı var.
- [ ] Bir bildirime tıklayınca ilgili sayfaya gidiyor VE okundu oluyor, rozet düşüyor.
- [ ] "Tümünü okundu" çalışıyor, rozet 0 oluyor.
- [ ] Yeni bildirim WS ile sayfa yenilemeden rozete/dropdown'a anında yansıyor.
- [ ] Yalnız kendi bildirimleri görünüyor (backend zorluyor; panel ekstra bir şey yapmaz).
- [ ] X-Tenant-Id gönderilmiyor; WS için yeni bağlantı açılmıyor (chat bağlantısı yeniden kullanılıyor).
````

---

## Notlar (panel agent'ına vermeden, senin için)

- Bu prompt'taki dosya yolları panelin mevcut yapısına göre uyarlanabilir (agent kendi yapısına
  oturtsun). Önemli olan: types + services + hook + WS aboneliği + Header zil ikonu.
- WS'i mevcut chat STOMP client'ına eklemek en temizi (tek bağlantı). Agent ayrı client açmak
  isterse de çalışır ama gereksiz.
- Backend canlı olmadan UI'ı kurabilir; entegrasyon testi backend deploy'undan sonra yapılır.

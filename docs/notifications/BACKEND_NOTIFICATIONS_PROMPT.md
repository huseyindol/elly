# Backend Prompt — Bildirim (Notification) Domaini

> Bu dosya **backend (Spring Boot CMS) projesinde** bir AI agent'a birebir
> kopyala-yapıştır edilecek prompt'tur. Agent bu repoyu görmez; tüm bağlam
> burada. Amaç: elly-admin-panel header'ındaki bildirim (zil) ikonunu gerçek,
> kalıcı bildirimlerle beslemek.

---

````
elly CMS (Spring Boot) projesine bir "Notification" (bildirim) domaini ekle.
Admin panel header'ındaki zil ikonu bu API'yi tüketecek: okunmamış sayısı rozet
olarak gösterilecek, dropdown'da son bildirimler listelenecek, tıklayınca ilgili
sayfaya gidilecek ve okundu işaretlenecek.

## Bağlam — mevcut mimari (varsayımlar)

- Tüm REST yanıtları RootEntityResponse<T> ile sarılı: { result, message, data }.
- Auth: JWT Bearer. Kullanıcı id'si JWT'den (basedb users.id) çözülüyor.
- Çok kiracılı (multi-tenant): basedb + her tenant'ın kendi DB'si. X-Tenant-Id
  header'ı ile tenant context'i belirleniyor; header yoksa basedb kullanılıyor.
- Gerçek zamanlı: STOMP üzerinden SockJS, endpoint ${API}/ws. Kişisel kuyruklar
  için Spring "user destination" kullanılıyor (örn. /user/queue/...).

## Bildirimler kime ait?

Bildirimler **admin kullanıcısına** aittir ve **basedb**'de saklanır (tenant
DB'sinde değil). Bir bildirimin hedefi her zaman bir basedb users.id'dir.
(Tenant kaynaklı olaylar da ilgili admin'lere basedb bildirimi olarak düşer.)

## Veri Modeli — notifications (basedb)

| alan         | tip          | açıklama                                                        |
|--------------|--------------|----------------------------------------------------------------|
| id           | bigint PK    |                                                                |
| user_id      | bigint FK    | hedef admin (basedb users.id)                                  |
| type         | varchar      | enum (aşağıda)                                                 |
| title        | varchar      | kısa başlık (örn. "Yeni form gönderimi")                       |
| message      | varchar      | detay metni (sanitize edilmiş, düz metin)                      |
| link         | varchar null | panelde gidilecek yol (örn. "/forms/{id}/submissions")         |
| read         | boolean      | default false                                                  |
| tenant_id    | varchar null | olay bir tenant'tan geldiyse referans (sadece bilgi amaçlı)    |
| metadata     | jsonb null   | serbest ek veri (örn. { formId, submissionId })                |
| created_at   | timestamptz  |                                                                |

### NotificationType enum

- FORM_SUBMISSION   — yeni form gönderimi
- EMAIL_FAILED      — e-posta gönderimi başarısız (FAILED)
- CHAT_MESSAGE      — kullanıcının üye olduğu grupta yeni mesaj / mention
- USER_REGISTERED   — yeni kullanıcı kaydı
- MAIL_VERIFY_FAILED— SMTP doğrulaması başarısız
- SYSTEM            — genel/sistem bildirimi

## REST Endpoint'leri (auth: admin JWT; kullanıcı kendi bildirimlerini görür)

| Method | Path                                   | Body | data |
|--------|----------------------------------------|------|------|
| GET    | /api/v1/notifications?page=&size=&unread= | —    | Page<Notification> (cursor/offset paged) |
| GET    | /api/v1/notifications/unread-count     | —    | { count: number } |
| POST   | /api/v1/notifications/{id}/read        | —    | Notification (read=true) |
| POST   | /api/v1/notifications/read-all         | —    | { updated: number } |
| DELETE | /api/v1/notifications/{id}             | —    | null (204/200) |

- `unread=true` filtresi yalnızca okunmamışları döndürür.
- Sıralama: created_at DESC.
- Tüm uçlar **yalnızca** JWT'deki kullanıcıya ait kayıtlarda çalışır (başkasının
  bildirimine erişim/okuma/silme 403/404).

## WebSocket — gerçek zamanlı push

- Yeni bildirim oluşunca hedef kullanıcıya STOMP user destination ile yayınla:
  - Destination: `/user/queue/notifications`
  - Payload: tam Notification nesnesi (REST ile aynı şekil).
- (Opsiyonel) okunmamış sayacı değişiminde: `/user/queue/notifications/unread-count`
  → `{ count }`. Panel isterse bunu dinler; yoksa REST count'u yeniler.

## Bildirim üreten olaylar (event → notification)

Aşağıdaki mevcut akışlara hook ekle; ilgili admin(ler)e bildirim yaz + WS push et:

1. **Form gönderimi** (public form submit başarıyla kaydedilince)
   → type=FORM_SUBMISSION, title="Yeni form gönderimi",
     message="{formName} formuna yeni gönderim", link="/forms/{formId}/submissions",
     metadata={ formId, submissionId, tenantId }. Hedef: formu yöneten admin(ler)
     / ADMIN+ roller.
2. **E-posta FAILED** (email log status FAILED'a düşünce)
   → type=EMAIL_FAILED, link="/email-logs", metadata={ emailLogId }.
3. **Chat mesajı / mention** (kullanıcının üye olduğu grupta yeni mesaj)
   → type=CHAT_MESSAGE, link="/chat", metadata={ groupId, messageId }.
   (Not: panel zaten WS unread tutuyor; bu bildirim "offline'dayken kaçırılan"
    için kalıcı kayıt sağlar. İstersen sadece mention'da üret.)
4. **Yeni kullanıcı kaydı** → type=USER_REGISTERED, link="/users",
   hedef: SUPER_ADMIN/ADMIN.
5. **SMTP verify başarısız** → type=MAIL_VERIFY_FAILED, link="/mail-accounts".

Her tür için bildirim üretimini **idempotent ve hatalara dayanıklı** yap:
bildirim yazımı asıl iş akışını (form kaydı, mail gönderimi) bloklamamalı/
patlatmamalı (try/catch + log).

## JSON örnekleri

GET /api/v1/notifications?size=2
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
        "title": "Yeni form gönderimi",
        "message": "İletişim formuna yeni gönderim",
        "link": "/forms/8/submissions",
        "read": false,
        "tenantId": "tenant1",
        "metadata": { "formId": 8, "submissionId": 144 },
        "createdAt": "2026-05-31T10:22:00Z"
      }
    ],
    "totalElements": 1, "totalPages": 1, "number": 0, "size": 2
  }
}
````

GET /api/v1/notifications/unread-count → `{ "result": true, "data": { "count": 4 } }`

## Güvenlik / kısıtlar

- Sadece kendi bildirimleri (JWT user_id eşleşmesi). Başkasının id'siyle read/delete → 403.
- message/title düz metin; HTML strip et (stored-XSS önle).
- Rate limit / index: (user_id, read, created_at) üzerinde index.
- Eski bildirimler için (opsiyonel) retention: N günden eski okunmuşları temizleyen job.

## Permission seed (gerekiyorsa)

Bildirimler kullanıcının kendine ait olduğundan özel permission gerekmez; ancak
panelde menü/erişim için bir `notifications:read` permission'ı tanımlamak
istersen SUPER_ADMIN dahil tüm rollere ver.

## Kabul kriterleri

- [ ] notifications tablosu + migration (basedb).
- [ ] 5 REST endpoint çalışıyor, RootEntityResponse sarmalı, sadece kendi kayıtları.
- [ ] GET unread-count doğru sayıyı döndürüyor.
- [ ] read / read-all kalıcı güncelliyor.
- [ ] En az FORM_SUBMISSION ve EMAIL_FAILED olayları bildirim üretiyor + WS push.
- [ ] WS /user/queue/notifications canlı bildirim gönderiyor (token'lı STOMP CONNECT).
- [ ] Bildirim üretimi asıl iş akışını bloklamıyor (hata izole).

```

---

## Panel tarafı (bu prompt onaylanınca yapılacak — referans)

Backend hazır olduğunda panelde eklenecekler (özet):

- `src/types/notification.ts` — `Notification`, `NotificationType`, `Page<Notification>`.
- `src/app/_services/notifications.services.ts` — list / unreadCount / markRead / markAllRead / delete.
- `src/app/_hooks/useNotifications.ts` — TanStack Query (unread-count `refetchInterval` veya WS).
- WS: `chat-ws-store` benzeri ya da mevcut STOMP client'a `/user/queue/notifications` aboneliği.
- `Header.tsx` zil ikonu: statik "5" yerine gerçek `unreadCount` rozeti + dropdown (son bildirimler, tıkla→`link`, okundu işaretle).
```

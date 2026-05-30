# elly Tenant Website — Prompt'ları

> Bu dosya **tenant website projesi** (public site) içindir — admin panel DEĞİL.
> Panel prompt'ları ayrı dosyada: `elly-admin-panel-integration-prompts.md`.
> Her prompt kendi içinde yeterli bağlamı taşır; agent'a doğrudan verilebilir.

## Genel bağlam (tüm prompt'larda geçerli)

- **Proje:** tenant website (Next.js), elly CMS API'sini tüketir. Tek tenant'a hizmet eder.
- **API base:** `process.env.NEXT_PUBLIC_ELLY_API_URL` (örn. `https://api.huseyindol.com`)
- **Tenant:** `process.env.NEXT_PUBLIC_TENANT_ID` (örn. `tenant1`)
- **Public REST düzeni:** Tüm public (login'siz) çağrılar şu prefix ile gider:
  ```
  /api/v1/public/{tenantId}/...
  ```
  Backend `PublicApiFilter` bu yolu yakalar → tenantId'yi path'ten alıp doğru tenant DB'sine
  yönlendirir, anonim erişim verir, sonra `/api/v1/...`'e rewrite eder. **Yeni public çağrı
  eklerken hep bu prefix kullanılır** (login de böyle: `/api/v1/public/{tenantId}/auth/login`).
- **WebSocket:** API **kökündeki** `/ws` (SockJS + STOMP) — `/public` prefix'ine tabi DEĞİL.
  ⚠️ WS host **çıplak host** olmalı (`https://api.huseyindol.com`); REST base'i (`.../api/v1/public`)
  KULLANMA. Yanlışsa SockJS `/api/v1/public/ws/info`'ya gider → "Unknown tenant: ws" (400) → WS
  bağlanamaz → input pasif kalır. Güvenli yol: `new URL(NEXT_PUBLIC_ELLY_API_URL).origin`.
- **Yanıt zarfı:** Tüm REST yanıtları `{ result, message, data }`. `data`'yı unwrap et.

---

## Prompt 1 — Anonim Guest Chat Widget

**Bu prompt'u tenant website projesinde çalıştır.**

> Hedef: Siteye giren **anonim ziyaretçi** (login YOK) sağ alttaki butona tıklar →
> görünen ad girer → tenant'ın "ziyaretçi erişimine açık" destek **gruplarının listesini**
> görür → bir grubu seçer → o grubun geçmişini + canlı mesajlaşmasını yapar. Admin panel
> tarafı (Prompt 7+9) aynı konuşmayı görür ve yanıtlar. **Grup yoksa** anlamsız boş bir
> ekran yerine "şu an aktif destek yok" gösterilir.

### Akış (4 adım)

```
1. Ad gir         → POST /api/v1/public/{tenantId}/auth/guest-token  body { displayName }
                     → { token, expiresIn, displayName, tenantId }   (token = WS kimliği)
2. Grup listesi   → GET  /api/v1/public/{tenantId}/tenant-chat/groups
                     → visitorAccess=true gruplar (anonim, token gerekmez)
3. Grup seç       → GET  /api/v1/public/{tenantId}/tenant-chat/groups/{groupId}/messages
                     → geçmiş (anonim) + WS CONNECT/SUBSCRIBE/SEND
4. Sohbet         → canlı mesaj akışı
```

### Endpoint'ler — guest-reachable yüzey

| Tür | Adres | Auth | Not |
|---|---|---|---|
| REST POST | `/api/v1/public/{tenantId}/auth/guest-token` | yok | body `{ displayName }` → guest JWT (≈1 saat) |
| REST GET | `/api/v1/public/{tenantId}/tenant-chat/groups` | yok (anonim) | visitorAccess=true gruplar |
| REST GET | `/api/v1/public/{tenantId}/tenant-chat/groups/{groupId}/messages?before=&limit=50` | yok (anonim) | geçmiş (cursor) |
| WS CONNECT | `${API}/ws` | header `Authorization: Bearer {guestToken}` | SockJS+STOMP |
| WS SUBSCRIBE | `/topic/tenant/{tenantId}/group/{groupId}` | connect ile | canlı mesaj |
| WS SEND | `/app/tenant-chat/{tenantId}/{groupId}/send` | connect ile | body `{ content }` |

### Önemli kurallar

- **Guest mesaj gönderimi YALNIZCA WebSocket** ile. Guest'in REST mesaj POST'u yoktur
  (backend `/tenant-chat/groups/{id}/messages` POST'unu guest'e 405 ile kapatır).
- **Grup listesi + geçmiş anonim public GET** — token gerekmez. Token sadece WS içindir.
- **typing / read yok** — backend guest için bunları no-op yapar; bu event'leri gönderme.
- **Grup yoksa** "şu an aktif destek yok" göster; boş ChatView açma.
- **Token süreli** (≈1 saat). STOMP error / disconnect olursa yeni token al + reconnect.
- **XSS:** `senderUsername` ve `content` düz metin olarak bas (`{value}`); asla
  `dangerouslySetInnerHTML`. Backend zaten sanitize ediyor, ama client tekrar açmasın.

### Tipler

```typescript
export interface GuestTokenResponse {
  token: string;
  expiresIn: number;     // saniye
  displayName: string;
  tenantId: string;
}

export interface ChatGroup {
  id: string;
  name: string | null;
  description: string | null;
  type: 'GROUP' | 'DM';
  visibilityLevel: number;
  tenantId: string;
  visitorAccess: boolean;     // bu listede hep true
  createdAt: string;
  updatedAt: string;
}

export type ChatSenderType = 'ADMIN' | 'VISITOR' | 'GUEST';

export interface ChatMessage {
  id: string;
  groupId: string;
  senderType: ChatSenderType;
  senderId: number | null;       // ADMIN ise dolu; GUEST/VISITOR'da null olabilir
  visitorId: number | null;      // VISITOR ise dolu
  senderUsername: string;        // backend pre-resolved (GUEST = display name) — direkt göster
  content: string;
  contentType: 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM';
  fileUrl: string | null;
  parentId: string | null;
  deleted: boolean;
  editedAt: string | null;
  createdAt: string;
}
```

### Dosya yapısı

```
components/chat/
├── ChatWidget.tsx           # Floating buton + panel state
├── ChatPanel.tsx            # Adım yönetimi: ad → liste → sohbet
├── GuestNameGate.tsx        # Ad formu → guest-token al
├── GroupList.tsx            # visitorAccess grupları (boşsa "aktif destek yok")
├── ChatView.tsx             # Geçmiş + canlı mesaj + composer
├── MessageBubble.tsx        # ADMIN / VISITOR / GUEST rozetli mesaj
├── ChatComposer.tsx         # Input + gönder
└── _hooks/
    ├── useGuestToken.ts     # POST guest-token (sessionStorage)
    ├── useVisitorGroups.ts  # GET groups (public, anonim)
    └── useGuestChat.ts      # geçmiş GET + WS connect/subscribe/send

lib/api/publicApi.ts         # /api/v1/public/{tenantId} fetch helper (RootEntityResponse unwrap)
```

### lib/api/publicApi.ts

```typescript
const API = process.env.NEXT_PUBLIC_ELLY_API_URL!;
const TENANT_ID = process.env.NEXT_PUBLIC_TENANT_ID!;
const BASE = `${API}/api/v1/public/${TENANT_ID}`;

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
    cache: 'no-store',
  });
  const body = await res.json();
  if (!body.result) throw new Error(body.message || `HTTP ${res.status}`);
  return body.data as T;
}

export const publicApi = {
  // tenantId path'ten gelir; body'de göndermeye gerek yok
  guestToken: (displayName: string) =>
    request<GuestTokenResponse>('/auth/guest-token', {
      method: 'POST',
      body: JSON.stringify({ displayName }),
    }),
  listGroups: () => request<ChatGroup[]>('/tenant-chat/groups'),
  getHistory: (groupId: string, before?: string, limit = 50) => {
    const p = new URLSearchParams();
    if (before) p.set('before', before);
    p.set('limit', String(limit));
    return request<ChatMessage[]>(`/tenant-chat/groups/${groupId}/messages?${p}`);
  },
};
```

### _hooks/useGuestToken.ts

```typescript
'use client';
import { useState, useCallback } from 'react';
import { publicApi } from '@/lib/api/publicApi';

const SS_KEY = 'elly_guest';

export function useGuestToken() {
  const [token, setToken] = useState<string | null>(() =>
    typeof window !== 'undefined' ? sessionStorage.getItem(SS_KEY + '_token') : null);
  const [displayName, setDisplayName] = useState<string | null>(() =>
    typeof window !== 'undefined' ? sessionStorage.getItem(SS_KEY + '_name') : null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const start = useCallback(async (name: string) => {
    setLoading(true); setError(null);
    try {
      const res = await publicApi.guestToken(name.trim());
      sessionStorage.setItem(SS_KEY + '_token', res.token);
      sessionStorage.setItem(SS_KEY + '_name', res.displayName);
      setToken(res.token); setDisplayName(res.displayName);
    } catch (e: any) {
      setError(e?.message || 'Bağlanılamadı'); throw e;
    } finally { setLoading(false); }
  }, []);

  const reset = useCallback(() => {
    sessionStorage.removeItem(SS_KEY + '_token');
    sessionStorage.removeItem(SS_KEY + '_name');
    setToken(null); setDisplayName(null);
  }, []);

  return { token, displayName, loading, error, start, reset };
}
```

### _hooks/useVisitorGroups.ts

```typescript
'use client';
import { useEffect, useState } from 'react';
import { publicApi } from '@/lib/api/publicApi';
import type { ChatGroup } from '@/types/chat';

export function useVisitorGroups() {
  const [groups, setGroups] = useState<ChatGroup[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    let alive = true;
    publicApi.listGroups()
      .then((g) => { if (alive) setGroups(g); })
      .catch((e) => { if (alive) setError(e.message); });
    return () => { alive = false; };
  }, []);
  return { groups, error, loading: groups === null && !error };
}
```

### _hooks/useGuestChat.ts — geçmiş + canlı + gönder

```typescript
'use client';
import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { publicApi } from '@/lib/api/publicApi';
import type { ChatMessage } from '@/types/chat';

// WS host = API origin (scheme+host). NEXT_PUBLIC_ELLY_API_URL'i ÇIPLAK host gir; kazara
// REST base'i (.../api/v1/public) girilse bile origin'e indiriyoruz — aksi halde SockJS
// /api/v1/public/ws/info'ya gider → PublicApiFilter "Unknown tenant: ws" (400) → WS bağlanamaz.
const WS_HOST = new URL(process.env.NEXT_PUBLIC_ELLY_API_URL ?? 'https://api.huseyindol.com').origin;
const TENANT_ID = process.env.NEXT_PUBLIC_TENANT_ID!;

export function useGuestChat(token: string, groupId: string) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  const upsert = useCallback((msg: ChatMessage) => {
    setMessages((prev) => (prev.some((m) => m.id === msg.id) ? prev : [...prev, msg]));
  }, []);

  // 1) Geçmiş (anonim public GET)
  useEffect(() => {
    let alive = true;
    publicApi.getHistory(groupId).then((h) => { if (alive) setMessages(h); }).catch(() => {});
    return () => { alive = false; };
  }, [groupId]);

  // 2) Canlı (WS + guest token)
  useEffect(() => {
    if (!token) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_HOST}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/tenant/${TENANT_ID}/group/${groupId}`, (frame) => {
          try { upsert(JSON.parse(frame.body) as ChatMessage); } catch {}
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (f) => console.error('STOMP error', f.headers, f.body),
    });
    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); clientRef.current = null; };
  }, [token, groupId, upsert]);

  const sendMessage = useCallback((content: string) => {
    const c = clientRef.current;
    if (!c || !c.connected || !content.trim()) return;
    c.publish({
      destination: `/app/tenant-chat/${TENANT_ID}/${groupId}/send`,
      body: JSON.stringify({ content: content.trim() }),
    });
    // Optimistic ekleme YAPMA — mesaj aynı topic'ten server id'siyle geri döner.
  }, [groupId]);

  return { messages, connected, sendMessage };
}
```

### Bileşenler

```typescript
// ChatWidget.tsx
'use client';
import { useState } from 'react';
import { ChatPanel } from './ChatPanel';

export function ChatWidget() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button onClick={() => setOpen((o) => !o)} aria-label="Destek"
        className="fixed bottom-6 right-6 z-50 h-14 w-14 rounded-full bg-blue-600 text-white shadow-lg">💬</button>
      {open && <ChatPanel onClose={() => setOpen(false)} />}
    </>
  );
}
```

```typescript
// ChatPanel.tsx — adım yönetimi: ad → liste → sohbet
'use client';
import { useState } from 'react';
import { useGuestToken } from './_hooks/useGuestToken';
import { GuestNameGate } from './GuestNameGate';
import { GroupList } from './GroupList';
import { ChatView } from './ChatView';
import type { ChatGroup } from '@/types/chat';

export function ChatPanel({ onClose }: { onClose: () => void }) {
  const guest = useGuestToken();
  const [group, setGroup] = useState<ChatGroup | null>(null);

  return (
    <div className="fixed bottom-24 right-6 w-96 h-[520px] bg-white shadow-xl rounded-lg flex flex-col">
      <header className="p-3 border-b flex items-center justify-between">
        <span className="font-semibold">Destek</span>
        <button onClick={group ? () => setGroup(null) : onClose}>{group ? '←' : '×'}</button>
      </header>
      <div className="flex-1 overflow-hidden">
        {!guest.token ? (
          <GuestNameGate onSubmit={guest.start} loading={guest.loading} error={guest.error} />
        ) : !group ? (
          <GroupList onSelect={setGroup} />
        ) : (
          <ChatView token={guest.token} myName={guest.displayName} group={group} />
        )}
      </div>
    </div>
  );
}
```

```typescript
// GuestNameGate.tsx
'use client';
import { useState, FormEvent } from 'react';

export function GuestNameGate({ onSubmit, loading, error }: {
  onSubmit: (name: string) => Promise<void>; loading: boolean; error: string | null;
}) {
  const [name, setName] = useState('');
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (name.trim().length < 2 || loading) return;
    try { await onSubmit(name.trim()); } catch {}
  };
  return (
    <form onSubmit={submit} className="p-4 flex flex-col gap-3">
      <p className="text-sm text-gray-600">Sohbete başlamak için adınızı girin.</p>
      <input value={name} onChange={(e) => setName(e.target.value)} maxLength={80}
        placeholder="Adınız" className="border rounded px-3 py-2 text-sm" />
      {error && <span className="text-xs text-red-600">{error}</span>}
      <button type="submit" disabled={name.trim().length < 2 || loading}
        className="bg-blue-600 text-white rounded px-3 py-2 text-sm disabled:opacity-50">
        {loading ? 'Bağlanıyor…' : 'Başla'}
      </button>
    </form>
  );
}
```

```typescript
// GroupList.tsx — boşsa "aktif destek yok"
'use client';
import { useVisitorGroups } from './_hooks/useVisitorGroups';
import type { ChatGroup } from '@/types/chat';

export function GroupList({ onSelect }: { onSelect: (g: ChatGroup) => void }) {
  const { groups, loading, error } = useVisitorGroups();
  if (loading) return <p className="p-4 text-sm text-gray-500">Yükleniyor…</p>;
  if (error) return <p className="p-4 text-sm text-red-600">{error}</p>;
  if (!groups || groups.length === 0)
    return <p className="p-4 text-sm text-gray-500">Şu an aktif bir destek konuşması yok.</p>;
  return (
    <ul className="divide-y overflow-y-auto h-full">
      {groups.map((g) => (
        <li key={g.id}>
          <button onClick={() => onSelect(g)} className="w-full text-left p-3 hover:bg-gray-50">
            <div className="font-medium text-sm">{g.name || 'Destek'}</div>
            {g.description && <div className="text-xs text-gray-500">{g.description}</div>}
          </button>
        </li>
      ))}
    </ul>
  );
}
```

```typescript
// ChatView.tsx
'use client';
import { useEffect, useRef } from 'react';
import { useGuestChat } from './_hooks/useGuestChat';
import { MessageBubble } from './MessageBubble';
import { ChatComposer } from './ChatComposer';
import type { ChatGroup } from '@/types/chat';

export function ChatView({ token, myName, group }: {
  token: string; myName: string | null; group: ChatGroup;
}) {
  const { messages, connected, sendMessage } = useGuestChat(token, group.id);
  const bottomRef = useRef<HTMLDivElement>(null);
  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages.length]);

  return (
    <div className="flex flex-col h-full">
      {!connected && <div className="text-xs text-amber-600 px-3 py-1">Bağlanıyor…</div>}
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        {messages.length === 0 && <p className="text-xs text-gray-400">İlk mesajı siz yazın 👋</p>}
        {messages.map((m) => (
          <MessageBubble key={m.id} message={m}
            isOwn={m.senderType === 'GUEST' && m.senderUsername === myName} />
        ))}
        <div ref={bottomRef} />
      </div>
      <ChatComposer onSubmit={(c) => { sendMessage(c); return Promise.resolve(); }} />
    </div>
  );
}
```

```typescript
// MessageBubble.tsx — ADMIN / VISITOR / GUEST
import type { ChatMessage } from '@/types/chat';

export function MessageBubble({ message, isOwn }: { message: ChatMessage; isOwn: boolean }) {
  const isAdmin = message.senderType === 'ADMIN';
  return (
    <div className={isOwn ? 'flex justify-end' : 'flex justify-start'}>
      <div>
        <div className="flex items-center gap-2 text-xs mb-1">
          {/* düz metin — dangerouslySetInnerHTML kullanma (XSS) */}
          <span className="font-semibold">{message.senderUsername}</span>
          {isAdmin && <span className="bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded text-[10px]">Destek</span>}
          <time className="text-gray-500">
            {new Date(message.createdAt).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })}
          </time>
        </div>
        <div className={isOwn
          ? 'bg-blue-600 text-white rounded-lg px-3 py-2 max-w-[80%]'
          : 'bg-gray-100 text-gray-900 rounded-lg px-3 py-2 max-w-[80%]'}>
          {message.deleted ? '[silinmiş mesaj]' : message.content}
        </div>
      </div>
    </div>
  );
}
```

```typescript
// ChatComposer.tsx
'use client';
import { useState, FormEvent } from 'react';

export function ChatComposer({ onSubmit }: { onSubmit: (content: string) => Promise<unknown> }) {
  const [content, setContent] = useState('');
  const [sending, setSending] = useState(false);
  const handle = async (e: FormEvent) => {
    e.preventDefault();
    if (!content.trim() || sending) return;
    setSending(true);
    try { await onSubmit(content.trim()); setContent(''); } finally { setSending(false); }
  };
  return (
    <form onSubmit={handle} className="border-t p-2 flex gap-2">
      <input value={content} onChange={(e) => setContent(e.target.value)} maxLength={4000}
        placeholder="Mesajınızı yazın…" disabled={sending}
        className="flex-1 border rounded px-3 py-2 text-sm" />
      <button type="submit" disabled={!content.trim() || sending}
        className="bg-blue-600 text-white px-3 py-2 rounded disabled:opacity-50">Gönder</button>
    </form>
  );
}
```

### Layout'a ekleme

```typescript
// app/layout.tsx — login şartı YOK (anonim ziyaretçi)
<body>{children}<ChatWidget /></body>
```

### .env

```
NEXT_PUBLIC_ELLY_API_URL=https://api.huseyindol.com   # ÇIPLAK host (WS host'u) — /api/v1/public EKLEME
NEXT_PUBLIC_TENANT_ID=tenant1
```
(Sabit grup id'sine gerek YOK — grup listesi runtime'da çekilir.)

### Kütüphaneler

```bash
npm i @stomp/stompjs sockjs-client
npm i -D @types/sockjs-client
```

### Doğrulama

- Widget login OLMADAN görünür (herkese).
- Tıkla → ad gir (2+ karakter) → "Başla" → guest-token alınır (POST .../auth/guest-token, 200).
- Grup listesi gelir (GET .../tenant-chat/groups). **Grup yoksa** "aktif destek yok" yazısı (boş ChatView değil).
- Grup seç → geçmiş yüklenir (GET .../messages) + "Bağlanıyor…" sonra kaybolur (WS connected).
- Mesaj yaz → birkaç yüz ms içinde aynı mesaj topic'ten geri gelir (echo).
- Admin panelden (Prompt 7+9) bu gruba yazınca widget'ta "Destek" rozetiyle anında görünür.
- `xhr_send` çağrıları **204** döner — bu SockJS'in normal davranışı (hata değil).
- Panel kapanıp açılınca geçmiş yeniden GET ile gelir (token sessionStorage'da kalır).
- `npm run build` hatasız.

### Notlar

- `senderType: 'GUEST'` + `senderUsername === myName` → "benim mesajım" (sağ/mavi). Aynı grupta
  aynı isimli birden çok guest olursa kusursuz değil; tek-ziyaretçi/destek senaryosu için yeterli.
- Token süresi dolarsa STOMP error gelir → `guest.reset()` çağırıp yeniden ad iste (veya sessiz
  yeni token + reconnect).
- Bu akış **mevcut backend ile çalışır** — yeni endpoint gerektirmez. Liste/geçmiş anonim public
  GET; gönderim guest WS. Guest-token `/api/v1/public/{tenantId}/auth/guest-token` üzerinden
  (PublicApiFilter allowlist'inde).

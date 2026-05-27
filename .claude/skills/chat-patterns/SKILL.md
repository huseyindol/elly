---
name: chat-patterns
description: Elly chat sisteminin pattern'leri — WebSocket+STOMP konfigürasyonu, rol bazlı görünürlük (visibilityLevel), davet hiyerarşisi, real-time broadcast topic'leri, multi-tenant chat routing, dosya yükleme. Chat endpoint ekleme, WebSocket topic tanımlama, mesaj akışı debug veya rol/davet kuralı değişikliğinde otomatik aktif et.
version: 1.0.0
---

# Elly Chat Sistemi — Pattern'ler

## Mimari Özet

```
Frontend (admin panel)
   ├── REST: /api/v1/chat/**  (fetcher utility, JWT cookie)
   └── WebSocket: ${API}/ws  (SockJS+STOMP, CONNECT'te Bearer header)
        ↓
Backend
   ├── REST Controller (ChatGroupController, ChatHistoryController)
   ├── WebSocket Controller (ChatWebSocketController)
   ├── STOMP Channel Interceptor (ChatWebSocketSecurityConfig)
   └── Service (ChatGroupService, ChatMessageService)
        ↓
PostgreSQL `basedb` (chat_groups, chat_group_members, chat_messages, ...)
Redis (rate limit, typing TTL, presence TTL)
```

**Kritik kural:** Chat tabloları SADECE `basedb`'de — tenant DB'lerinde yok.

## Multi-tenant Routing — Chat Özel Davranışı

`JwtTenantFilter.resolveTenantId()` `/api/v1/chat/` path'lerini zorla `basedb`'ye yönlendirir:

```java
if (path.startsWith("/api/v1/chat/")) {
  log.debug("Chat path, forcing basedb: {}", path);
  return null;  // null → defaultDataSource (basedb)
}
```

`ChatWebSocketSecurityConfig` STOMP CONNECT'te `TenantContext.setTenantId(null)` yapar.

`ChatWebSocketController`'da HER handler'da:
```java
TenantContext.setTenantId(null);
try {
  // ... işlem
} finally {
  TenantContext.clear();
}
```

## Rol Seviyesi Tablosu

`ChatGroupService.getUserRoleLevel(userId)`:
| Rol | Level |
|---|---|
| SUPER_ADMIN | 4 |
| ADMIN | 3 |
| EDITOR | 2 |
| VIEWER (veya bilinmeyen) | 1 |

Kullanıcının **en yüksek** rolünün level'ı döner.

## visibilityLevel Kuralları

| Grup oluşturanın rolü | `visibilityLevel` set | Görüntüleyebilenler |
|---|---|---|
| VIEWER | 1 | Herkes |
| EDITOR | 2 | EDITOR, ADMIN, SUPER_ADMIN |
| ADMIN | 3 | ADMIN, SUPER_ADMIN |
| SUPER_ADMIN | 4 | Yok (sadece davet edilenler) |
| DM (her durumda) | 4 | Sadece iki taraf |

**`getMyGroups()` JPQL pattern:**
```jpql
SELECT g FROM ChatGroup g
WHERE g.visibilityLevel <= :roleLevel
   OR EXISTS (
       SELECT 1 FROM ChatGroupMember m
       WHERE m.id.groupId = g.id AND m.id.userId = :userId
   )
ORDER BY g.updatedAt DESC
```

⚠️ **LEFT JOIN ON embedded-id kullanma** — Hibernate 6 güvenilmez. Üyelik kontrolü için `EXISTS` subquery kullan.

## Davet Hiyerarşisi

`addMember()` enforcement:
```java
int requesterLevel = getUserRoleLevel(requesterId);
int targetLevel = getUserRoleLevel(targetUserId);
if (requesterLevel < 4 && targetLevel >= requesterLevel) {
  throw new ForbiddenException("You can only invite users with a lower role than yours");
}
```

| Davet eden | Davet edebileceği |
|---|---|
| VIEWER | — (chat:manage yetkisi yok, controller seviyesinde bloklanır) |
| EDITOR | VIEWER |
| ADMIN | VIEWER, EDITOR |
| SUPER_ADMIN | Herkes |

## STOMP Topic Şeması

**Server → Client (subscribe):**
| Topic | Payload | Tetiklenme |
|---|---|---|
| `/topic/groups/new` | `DtoChatGroup` | Grup oluşturulunca |
| `/topic/groups/deleted` | `groupId` (string) | Grup silinince |
| `/topic/user/{userId}/groups/joined` | `DtoChatGroup` | Kullanıcı davet edilince |
| `/topic/group/{groupId}` | `DtoChatMessage` | Yeni mesaj |
| `/topic/group/{groupId}/typing` | `DtoChatTyping` | Yazıyor sinyali |
| `/topic/group/{groupId}/read` | `DtoChatRead` | Okundu |
| `/topic/presence` | `DtoChatPresence` | Online/offline |

**Client → Server (publish):**
| Destination | Payload | İşlem |
|---|---|---|
| `/app/chat/{groupId}/send` | JSON `{content, contentType, parentId?}` | Mesaj kaydet + broadcast |
| `/app/chat/{groupId}/typing` | `""` | Yazıyor sinyali yay |
| `/app/chat/{groupId}/read` | `""` | Grubu okundu işaretle |

## REST Endpoint Pattern'i

`@RequestMapping` annotation'ı **impl class'ta** olmalı (Spring 6 interface annotation'ları detect etmiyor):

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")   // ← impl'de
public class ChatGroupController implements IChatGroupController {

  @Override
  @PostMapping("/groups")          // ← impl'de
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatGroup>> createGroup(...) {
    ...
  }
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

❌ `(:before IS NULL OR createdAt < :before)` ile null tarih parametresi → PostgreSQL `could not determine data type of parameter` hatası.

✅ İki ayrı query method:
```java
List<ChatMessage> findByGroupId(UUID groupId, Pageable pageable);
List<ChatMessage> findByGroupIdBefore(UUID groupId, Date before, Pageable pageable);
```

Service'te `if (before == null) ... else ...` ile seç.

## DataSource & Schema Validation

Production'da `JPA_DDL_AUTO=validate`. Yeni kolon eklerken:
1. Entity field ekle
2. `db-migration-chat.sql`'e `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` yaz
3. Production DB'ye manuel `ALTER TABLE` çalıştır
4. Ardından pod'u sil → yeni image ayağa kalkar

Aksi halde `SchemaManagementException` → CrashLoopBackOff.

## Frontend Notları (Admin Panel)

- JWT **JWE encrypted** — `atob` decode etmez, içerik şifreli.
- Rol bilgisi için `GET /api/v1/users/me/permissions` çağrılır.
- WebSocket: `@stomp/stompjs` + `sockjs-client`, `Authorization: Bearer ${cookie.accessToken}` CONNECT header.
- Tek bir client multiple grup subscribe edebilir (sidebar unread badge için).

## Yeni Topic Eklerken Checklist

1. **Backend:** `messagingTemplate.convertAndSend("/topic/...", payload)` — uygun service/controller'da
2. **WebSocketConfig:** Yeni `/app/...` destination ise `@MessageMapping` ekle
3. **Frontend store:** `onConnect` içinde `client.subscribe('/topic/...', handler)`
4. **changelog.md:** Topic tablosuna ekle

## Yeni Chat Endpoint Eklerken Checklist

1. Interface'de metot tanımı (Swagger için)
2. Impl class'ta:
   - `@RestController`, `@RequestMapping("/api/v1/chat")` class seviyesinde
   - Metoda `@PostMapping`/`@GetMapping`/... ve `@PreAuthorize`
   - `ResponseEntity<RootEntityResponse<T>>` dönüş tipi
3. Service'te tenant routing'e gerek yok — `JwtTenantFilter` zaten basedb'ye yönlendiriyor
4. Üyelik kontrolü gerekirse `groupService.checkAccess(groupId, userId)` çağır

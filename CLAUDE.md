# Elly CMS — Claude Proje Bağlami

## Git Kurallari

- **Her zaman `main` branch'te calis.** Feature branch acma; degisiklikleri dogrudan `main`'e commit et ve push'la.
- Bunu her oturumda hatirlatmana gerek yok — bu kural kalicidir.

## Proje Yapisi Kurallari — KESIN KURAL

Bu kurallar tartismasizdir. Degistirmeden once kullanici ile mutlaka konusulmalidir.

### Paket yapisi (layer-based — degistirilemez)

```
com.cms.controller/       ← tum IController interface'leri
com.cms.controller.impl/  ← tum Controller implementasyonlari (@RestController)
com.cms.service/          ← tum IService interface'leri + yardimci @Service siniflari
com.cms.service.impl/     ← tum Service implementasyonlari
com.cms.repository/       ← tum Repository'ler (JpaRepository)
com.cms.entity/           ← tum @Entity siniflari + enum'lar
com.cms.dto/              ← tum DTO siniflari (Dto* prefix zorunlu)
com.cms.mapper/           ← tum MapStruct @Mapper siniflari
com.cms.config/           ← tum @Configuration, Filter, SecurityConfig siniflari
com.cms.exception/        ← tum exception siniflari
com.cms.util/             ← yardimci utility siniflari
```

**YASAK:** `com.cms.<modul>/` seklinde feature-package olusturma. Yeni bir ozellik (chat, blog, vb.) eklendiginde dosyalar yukardaki katmanlara dagitilir; ayri bir alt paket altinda toplanmaz.

### Yapiya aykiri bir degisiklik gormeden once dur

Eger mevcut kodun bu kurala uymadigi gorulurse (orn. feature-package mevcut) kullaniciya bildir ve duzeltme oneri sun — sessizce devam etme.

### Kontrol listesi — her yeni sinif icin

- [ ] Package adi `com.cms.<katman>` seklinde mi? (feature sub-package degil)
- [ ] Controller interface mi yoksa impl mi? (`IXxx` → controller/, `Xxx` → controller/impl/)
- [ ] DTO sinifi `Dto` prefix'i ile mi basliyor?
- [ ] `@RequestMapping` impl class'ta mi? (interface'te degil — Spring 6 kuralı)
- [ ] Mevcut benzer bir sinif var mi? Tekrar etme.

## Cursor ve diger agent araclari

Coklu agent rolleri, is akislari ve dosya haritasi icin repo kokundeki **[`AGENTS.md`](./AGENTS.md)** dosyasina bak. Cursor odakli kisa workflow'lar **[`.agents/workflows/`](./.agents/workflows/)** altinda; uzun tanimlar bu repoda **`.claude/agents/`** icinde kalir (tek kaynak).

## Proje Ozeti
Spring Boot 3.5.7 tabanli multi-tenant CMS. Java 21, PostgreSQL (database-per-tenant), Redis cache, RabbitMQ, JWT auth (JWE-encrypted) + istege bagli 2FA (TOTP), WebRTC arama sinyallesme (Faz 1). OAuth2 kaldirildi (gerekirse tekrar eklenir).

## Mimari
- **Pattern:** Monolith, katmanli mimari
- **Katmanlar:** IController -> Controller -> IService -> Service -> Repository -> Entity
- **DTO mapping:** MapStruct (`@Mapper(componentModel = "spring")`)
- **Response wrapper:** `RootEntityResponse<T>` — tum API yanitlari bu wrapper ile doner
- **Boilerplate:** Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`)
- **Exception:** `BaseException` hiyerarsisi + `GlobalExceptionHandler` (@ControllerAdvice)

## Multi-Tenancy
- **Strateji:** Database-per-tenant. **basedb YOK** — tamamen tenant-scoped mimari
- **Tenant'lar:** `tenant1`, `tenant2` (DEFAULT_TENANT yalniz boot/seed hedefi, request fallback DEGIL)
- **Routing:** `DataSourceConfig` -> `TenantRoutingDataSource` (boot sonrasi strict: context bos -> `TenantRequiredException`)
- **Context:** JWT `tenantId` claim'i veya `/api/v1/public/{tid}` URL'i; `TenantContext.setTenantId()`
- **Filtreleme:** `JwtTenantFilter` -> her request'te tenant context kurulur
- **ONEMLI:** `@Transactional` public auth endpoint'lerinde context CONTROLLER'da (transaction acilmadan ONCE) kurulmali — login/register/refresh/verify/mfa-verify ornekleri

## Kimlik Dogrulama & RBAC
- **Panel login:** `POST /api/v1/auth/login` + body `{ usernameOrEmail, password, tenantId, loginType:"admin" }` — panel rolu (SUPER_ADMIN/ADMIN/EDITOR/VIEWER) zorunlu
- **Tenant login:** `POST /api/v1/public/{tid}/auth/login` + `loginType:"tenant"` — TENANT rolu zorunlu (simetrik ayrim: panel kullanicisi tenant site'a giremez, tersi de)
- **Register:** public `/auth/register` -> TENANT rolu atar; admin user yalniz panel'den `POST /api/v1/users` (SUPER_ADMIN) ile olusur
- **JWT:** Access + refresh token (JWE-encrypted), `loginSource` ve `tenantId` claim'leri; refresh body-based, tenant refresh token claim'inden cozulur
- **Cookie max-age:** JWT surelerinden turetilir (JWT_EXPIRATION/JWT_REFRESH_EXPIRATION tek kaynak)
- **Util:** `JwtUtil.generateToken(user, loginSource, tenantId)`
- **RBAC:** User -> ManyToMany -> Role -> ManyToMany -> Permission
- **Method Security:** `@EnableMethodSecurity` + `@PreAuthorize` ile method-level authorization
- **Roller:** Panel: SUPER_ADMIN, ADMIN, EDITOR, VIEWER; Tenant-site: TENANT (izinsiz "tag" rol, MUNHASIR — baska rolle birlesemez) + 40+ permission (`PermissionConstants.java`)
- **Hiyerarsi guard:** SUPER_ADMIN=4, ADMIN=3, EDITOR=2, VIEWER/TENANT=1 — kimse kendi seviyesinden yuksek rol atayamaz/kullanici yaratamaz (`UserService.createUser`, `RoleService.assignRolesToUser`)
- **2FA (TOTP):** Istege bagli, Google Authenticator uyumlu (RFC 6238, harici kutuphanesiz `TotpService`). Aktifse login iki adimli: parola -> `mfaToken` (5dk) -> `POST /api/v1/auth/mfa/verify` + TOTP kodu -> tam JWT. Setup/status/disable: `/api/v1/auth/mfa/**` (`MfaController`). Secret AES sifreli saklanir (`User.mfa_*` kolonlari)
- **Token okuma:** SADECE `Authorization: Bearer` header (cookie'den token OKUNMAZ; login'de HttpOnly cookie yine YAZILIR — BFF/SSR icin)
- **Auth Cache:** Redis `auth:user:{username}` TTL=30dk (her request'te 3 SQL yerine 1 Redis GET)

## Mail Sistemi
- **Spring Mail auto-config EXCLUDE** — `MailSenderAutoConfiguration` EllyApplication'da exclude (pod crash onlemi)
- **Tenant-based SMTP:** Her tenant kendi Gmail hesabini `mail_accounts` tablosuna tanimlar (AES-256 sifreli)
- **Akis:** `POST /api/v1/emails/send` -> EmailLog(PENDING) -> RabbitMQ email-queue -> `EmailQueueService` consumer -> `TenantMailSenderFactory` -> Gmail SMTP
- **Retry:** email-retry-queue (TTL=30sn) -> tekrar email-queue (max 3 kez), asildiktan sonra FAILED + DLQ
- **Verify:** `POST /api/v1/mail-accounts/{id}/verify` — mail gondermeden SMTP dogrulamasi

## Cache
- **Redis** ile `@EnableCaching`, varsayilan TTL=10dk
- **Tenant izolasyonu otomatik:** `computePrefixWith` -> `{tenantId}::{cacheName}::{key}`
- **Graceful fallback:** `GracefulCacheErrorHandler` — Redis down ise DB'den okur, uygulama cokmez
- **Pattern:** Okuma `@Cacheable`, yazma/silme `@CacheEvict(allEntries = true)`

## Mesaj Kuyrugu
- **RabbitMQ** ile asenkron mail gonderimi
- **Queue'lar:** email-queue, email-retry-queue (TTL=30sn), email-dead-letter-queue
- **Serialization:** Jackson2JsonMessageConverter
- **Consumer'da TenantContext:** Baslangicta `setTenantId()`, finally'de `clear()` — ZORUNLU

## Chat Sistemi
- **AC=TC tek model:** Her chat grubu ait oldugu TENANT DB'sinde yasar (basedb yok); 5 tablo her tenant'ta (chat_groups, chat_group_members, chat_messages, chat_message_reads, chat_message_edits)
- **Routing:** Tenant, WS CONNECT'te token `tenantId` claim'inden; REST'te `/api/v1/chat/tenant/{tid}/...` URL'inden
- **WebSocket:** `${API}/ws` (SockJS+STOMP), CONNECT'te `Authorization: Bearer` zorunlu (guest token'la anonim de baglanir)
- **Permission:** `chat:read` (tum kullanicilar), `chat:manage` (EDITOR+)
- **Visibility:** VIEWER=1 (herkese acik) → EDITOR=2 → ADMIN=3 → SUPER_ADMIN=4 (gizli), DM her zaman 4
- **Davet hiyerarsisi:** Davet eden, KENDI SEVIYESI ve altindaki kullanicilari davet edebilir (admin→admin dahil); SUPER_ADMIN herkesi
- **Topic'ler:** `/topic/tenant/{tid}/group/{groupId}` (mesaj/typing/read/bans), `/topic/groups/new`, `/topic/groups/deleted`, `/topic/user/{userId}/membership`, `/topic/presence`
- **Guest chat:** `POST /auth/guest-token` (tenantId + sessionId ZORUNLU, `loginSource=website`); guest yazma kapisi: grup `tenantId` dolu + `visitorAccess=true`
- **Bildirim:** Tenant tarafindan gelen mesajlar (guest/visitor/TENANT rollu) o tenant'in SUPER_ADMIN+ADMIN'lerine `CHAT_MESSAGE` bildirimi uretir; panel rolu gonderirse bildirim YOK (spam onlemi)
- **Detay:** `chat-patterns` skill'i acilir; query/topic ornekleri orada

## WebRTC Arama (Faz 1 — sinyallesme)
- **Kapsam:** Backend yalniz SINYALLESME (SDP/ICE relay) + cagri orkestrasyonu; medya P2P, backend gormez. STUN/TURN backend'de YOK (client sorumlulugu)
- **Iki mod:** (1) panel-ici admin→admin (hiyerarsi kurali; ayni chat grubunun uyeleriyse hiyerarsi ATLANIR), (2) tenant destek cagrisi = ring-all (tum online panel kullanicilari calar, ilk cevaplayan kazanir, digerlerine CALL_TAKEN)
- **Client gonderir:** `/app/rtc/call`, `/app/tenant-rtc/{tid}/call`, `/app/rtc/{callId}/{answer,reject,cancel,hangup,sdp,ice}`
- **Client abone olur:** `/topic/user/{userId}/rtc` (ESAS yol), `/user/queue/rtc` (yedek); tenant caller ayrica `/topic/rtc-client/{clientRef}/rtc` (userId bilmeden abone olmak icin)
- **Durum:** In-memory registry (`CallSessionService`, ConcurrentHashMap) — TEK instance varsayimi, yatay olcekleme icin Faz 2 (Redis + broker relay) gerekir. 30sn ring timeout, busy/presence kontrolu
- **call_logs:** Her tenant DB'sinde metadata (`CallLog`, status: RINGING→ANSWERED/REJECTED/CANCELLED/MISSED, ANSWERED→COMPLETED); migration `db-migration-call-logs.sql` her tenant'a uygulanmali. Henuz okuyan REST endpoint yok (sonraki faz)

## Build & Calistirma
```bash
./mvnw clean package -DskipTests   # Build
./mvnw spring-boot:run              # Lokal calistir
make build && make up               # Docker ile
./mvnw test                         # Testler
```

## K8s Deployment
```bash
kubectl apply -f k8s/               # Tum kaynaklari uygula
kubectl rollout restart deployment/elly-app -n elly  # Yeniden baslat
```

## Veritabani
- PostgreSQL multi-DB: `elly_tenant1`, `elly_tenant2` (basedb kaldirildi — her tenant kendi DB'sinde)
- Migrations: `src/main/resources/migration/` altindaki `.sql` dosyalari — HER tenant DB'sine uygulanir (prod `JPA_DDL_AUTO=validate`, deploy oncesi calistirilmali)
- Backup: `make backup` / `make restore`

## Kod Standartlari
- Yeni ozellik eklerken IService + Service + IController + Controller + DTO + Mapper pattern'ini takip et
- Her endpoint `RootEntityResponse<T>` dondurmeli
- Cache invalidation: `@CacheEvict(value = "...", allEntries = true)` kullan
- Exception: `BaseException` alt siniflarini kullan, `GlobalExceptionHandler` mevcut
- Multi-tenant sorgularda her zaman tenant context'e gore filtrele
- `@Autowired` field injection kullanma, constructor injection kullan (`@RequiredArgsConstructor`)
- Entity'yi dogrudan controller'dan dondurme, MapStruct mapper kullan
- RabbitMQ consumer'da TenantContext set/clear zorunlu

## Skill Sistemi
Proje-spesifik bilgi birikimi `.claude/skills/` altinda tutulur. Agent'lar gorev basinda ilgili skill'leri okur.

| Skill | Konu |
|-------|------|
| `elly-project-mastery` | Proje durumu, mimari kararlar, oturum surekliligi |
| `elly-conventions` | Paket yapisi, katman kurallari, zorunlu pattern'ler |
| `multitenant-routing` | TenantContext, DataSource routing, async kullanimi |
| `spring-security-patterns` | JWT, 2FA (TOTP), SecurityConfig, filter sirasi |
| `redis-cache-patterns` | Cache key, TTL, invalidation, graceful fallback |
| `rabbitmq-patterns` | Queue/exchange ekleme, consumer pattern, retry/DLQ |
| `error-handling-patterns` | BaseException hiyerarsisi, GlobalExceptionHandler |
| `chat-patterns` | WebSocket+STOMP, visibilityLevel, davet hiyerarsisi, topic semasi |
| `mail-smoke-test` | Mail gonderim akisini end-to-end dogrulama playbook'u |
| `dev-session-tracker` | Uzun gorevlerde ilerleme takibi, changelog |
| `karpathy-guidelines` | Davranissal kurallar: overengineering'i onle, surgical degisim, goal-driven loop |

## Agent Teams
Agent Teams aktif (`CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`). Buyuk gorevlerde paralel calisma icin takim kurulabilir.

**Mevcut Agent'lar:**
| Agent | Rol | Model | Araclar |
|-------|-----|-------|---------|
| `team-lead` | Orchestrator, gorev dagitimi | opus | Agent, Read, Glob, Grep, Write, Edit, Bash |
| `java-architect` | Mimari tasarim, Spring Boot | sonnet | Read, Glob, Grep, Write, Edit, Agent(code-reviewer) |
| `code-reviewer` | Kod kalitesi, pattern uyumu | sonnet | Read, Glob, Grep |
| `devops-engineer` | K8s, Docker, CI/CD | sonnet | Read, Glob, Grep, Bash, Write, Edit |
| `security-guard` | Guvenlik analizi | sonnet | Read, Glob, Grep |
| `product-advisor` | Ozellik tartismasi, piyasa arastirmasi, yalakalik yapmadan elesti | sonnet | Read, Glob, Grep, WebSearch, WebFetch, Bash |

**Kullanim:**
```
# Takim baslatma
"Bu ozelligi agent team ile gelistir: java-architect tasarimi yapsin, code-reviewer kontrol etsin"

# Tek agent cagirma
@"java-architect (agent)" bu entity tasarimini incele
```

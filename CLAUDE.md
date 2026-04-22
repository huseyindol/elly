# Elly CMS — Claude Proje Bağlami

## Cursor ve diger agent araclari

Coklu agent rolleri, is akislari ve dosya haritasi icin repo kokundeki **[`AGENTS.md`](./AGENTS.md)** dosyasina bak. Cursor odakli kisa workflow'lar **[`.agents/workflows/`](./.agents/workflows/)** altinda; uzun tanimlar bu repoda **`.claude/agents/`** icinde kalir (tek kaynak).

## Proje Ozeti
Spring Boot 3.5.7 tabanli multi-tenant CMS. Java 21, PostgreSQL (database-per-tenant), Redis cache, RabbitMQ, JWT + OAuth2 auth.

## Mimari
- **Pattern:** Monolith, katmanli mimari
- **Katmanlar:** IController -> Controller -> IService -> Service -> Repository -> Entity
- **DTO mapping:** MapStruct (`@Mapper(componentModel = "spring")`)
- **Response wrapper:** `RootEntityResponse<T>` — tum API yanitlari bu wrapper ile doner
- **Boilerplate:** Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`)
- **Exception:** `BaseException` hiyerarsisi + `GlobalExceptionHandler` (@ControllerAdvice)

## Multi-Tenancy
- **Strateji:** Database-per-tenant
- **Tenant'lar:** `basedb` (varsayilan), `tenant1`, `tenant2`
- **Routing:** `DataSourceConfig` -> `TenantDataSourceRouter`
- **Context:** JWT claim'den `tenantId` alinir, `TenantContext.setCurrentTenant()` ile set edilir
- **Filtreleme:** `JwtTenantFilter` -> her request'te tenant context kurulur

## Kimlik Dogrulama & RBAC
- **Admin login:** `POST /api/auth/admin/login` -> `AdminLoginInterceptor`
- **Tenant login:** `POST /api/auth/login` -> `TenantLoginInterceptor`
- **OAuth2:** Google, Facebook, GitHub -> `OAuth2AuthenticationSuccessHandler`
- **JWT:** Access token + refresh token, `loginSource` ve `tenantId` claim'leri icerir
- **Util:** `JwtUtil.generateToken(user, loginSource, tenantId)`
- **RBAC:** User -> ManyToMany -> Role -> ManyToMany -> Permission
- **Method Security:** `@EnableMethodSecurity` + `@PreAuthorize` ile method-level authorization
- **Roller:** SUPER_ADMIN, ADMIN, EDITOR, VIEWER + 40+ permission (`PermissionConstants.java`)
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
- PostgreSQL multi-DB: `elly_basedb`, `elly_tenant1`, `elly_tenant2`
- Migrations: `src/main/resources/` altindaki `.sql` dosyalari
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
| `spring-security-patterns` | JWT, OAuth2, SecurityConfig, filter sirasi |
| `redis-cache-patterns` | Cache key, TTL, invalidation, graceful fallback |
| `rabbitmq-patterns` | Queue/exchange ekleme, consumer pattern, retry/DLQ |
| `error-handling-patterns` | BaseException hiyerarsisi, GlobalExceptionHandler |
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

**Kullanim:**
```
# Takim baslatma
"Bu ozelligi agent team ile gelistir: java-architect tasarimi yapsin, code-reviewer kontrol etsin"

# Tek agent cagirma
@"java-architect (agent)" bu entity tasarimini incele
```

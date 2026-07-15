---
name: multitenant-routing
description: Multi-tenant veri yönlendirme, tenant context yönetimi ve tenant izolasyonu. Tenant routing, DataSource değiştirme, tenant-specific query veya tenant context hataları ile ilgili konularda otomatik aktif et.
version: 2.0.0
---

# Elly Multi-Tenant Routing

## Mimari — tamamen tenant-scoped (basedb YOK)
- **Strateji:** Database-per-tenant (her tenant ayrı PostgreSQL DB'si)
- **Tenant'lar:** `tenant1`, `tenant2` — merkezi/varsayılan bir "basedb" YOKTUR (d4ad211'de kaldırıldı)
- **DataSource Routing:** `TenantRoutingDataSource extends AbstractRoutingDataSource`
- **Context Holder:** `TenantContext` (ThreadLocal tabanlı)
- **Strict routing:** Boot bitince (`RoutingStrictModeActivator`, ApplicationReadyEvent) strict mode aktifleşir: context boşsa sessiz fallback YERİNE `TenantRequiredException` (errorCode=TENANT_REQUIRED) fırlar
- **default-tenant** (`app.tenants.default-tenant`, varsayılan `tenant1`): YALNIZ boot/Hibernate dialect tespiti ve seed hedefi. Request fallback DEĞİLDİR. Config'te tanımlı değilse `DataSourceConfig` ilk datasource'a düşer (boot çökmesin diye)

## Request Akışı
```
HTTP Request
  → JwtTenantFilter (@Order(1))
      → Authorization: Bearer JWT'den tenantId claim'ini çıkar
      → TenantContext.setTenantId(tenantId)
      (public istekler shouldNotFilter ile atlanır)
  → PublicApiFilter (anonim akış)
      → /api/v1/public/{tid}/... path'inden tenant çözer, path'i rewrite eder
      → GET açık + allowlist'li POST (forms submit, auth register/login/guest-token)
  → TenantRoutingDataSource.determineCurrentLookupKey()
      → TenantContext.getTenantId() döndürür
      → context boş + strict mode → TenantRequiredException
  → Doğru PostgreSQL DB'sine bağlan
  → İşlem tamamlanınca TenantContext.clear()  ← finally bloğunda!
```

## Tenant Nereden Çözülür? (tek kural)
- **Authenticated:** JWT `tenantId` claim'i (ZORUNLU — claim yoksa JwtAuthenticationFilter TENANT_REQUIRED/400 döner)
- **Anonim:** `/api/v1/public/{tid}/...` URL'i (PublicApiFilter)
- **WS CONNECT:** token `tenantId` claim'i (admin/tenant/guest hepsi için zorunlu)
- **URL-tenant (admin):** `/api/v1/chat/tenant/{tid}/...` gibi path'ler — yalnız `loginSource=admin`
- X-Tenant-Id header ve tenant-switch token DESTEKLENMEZ

## Kritik Notlar

**@Transactional auth endpoint'lerinde (login/register/refresh/verify/mfa-verify):**
TenantContext, `@Transactional` servis çağrısından ÖNCE CONTROLLER'da kurulmalı — aksi halde
transaction begin aşamasında routing yanlış hedefe düşer (commit 602af2a, ddc573b).

**@Async / RabbitMQ consumer kullanımında:**
Async method'larda ThreadLocal kaybolur. Tenant'ı manuel aktar:
```java
String tenantId = TenantContext.getTenantId();
CompletableFuture.runAsync(() -> {
    TenantContext.setTenantId(tenantId);
    try { /* iş */ } finally { TenantContext.clear(); }
});
```

**Cache:** Tenant izolasyonu otomatik — `computePrefixWith` → `{tenantId}::{cacheName}::{key}`.
Ek olarak key'e tenantId gömmek gerekmiyor.

**Test'lerde:**
```java
@BeforeEach
void setUp() {
    TenantContext.setTenantId("tenant1");
}
@AfterEach
void tearDown() {
    TenantContext.clear();
}
```

## Kullanıcı Nerede Saklanır?
```
tenant1  → tenant1'in TÜM kullanıcıları (panel adminleri + site kullanıcıları)
tenant2  → tenant2'nin TÜM kullanıcıları
```
Panel/tenant ayrımı DB ile değil ROL ile yapılır: panel rolleri (SUPER_ADMIN/ADMIN/EDITOR/VIEWER)
vs TENANT rolü (münhasır). Login simetrik: admin login panel rolü ister, tenant login TENANT rolü ister.
Cross-tenant kullanıcı yönetimi YOKTUR — `TenantUserService`/`TenantController` silindi (1e0c44c, 6e4b506);
panel yalnız kendi JWT context'indeki tenant'ı yönetir.

## Seed (DataInitializer)
- Varsayılan KAPALI: `app.startup.rbac-seed=false`
- Her tenant AYRI transaction'da seed edilir (TransactionTemplate) — tek `@Transactional`
  bağlantıyı ilk tenant'a pinler, diğer tenant'lara yazamaz
- Her tenant'a env-tabanlı ilk SUPER_ADMIN: SEED_ADMIN_USER/PASSWORD/EMAIL (prod'da MUTLAKA override)

## Hata Senaryoları
- Context boş (boot sonrası) → `TenantRequiredException` (400, errorCode=TENANT_REQUIRED) — fallback YOK
- Bilinmeyen tenant adı (URL'de) → 400 Bad Request (PublicApiFilter / JwtTenantFilter)
- Transaction içinde tenant değiştirme → **Tehlikeli**, kaçın (bağlantı transaction başında pinlenir)

## Geçici Context Switch Pattern (aynı istekte farklı tenant DB'si)

Nadiren gerekir (örn. `ChatGroupTenantRouter.useGroupDatabase` grubu kendi tenant'ına sabitler):

```java
String currentTenant = TenantContext.getTenantId();
try {
  TenantContext.setTenantId(targetTenant);
  // ... hedef tenant DB'sinde iş
} finally {
  TenantContext.setTenantId(currentTenant);   // ZORUNLU restore — clear() değil
}
```

**Kural:** `finally` bloğu mutlaka restore etmeli. `TenantContext.clear()` kullanma — JwtTenantFilter
outer finally'de zaten clear ediyor, içeride clear edersen önceki context'i kaybedersin.
Ayrıca `@Transactional` sınırı içinde switch işe yaramaz — switch, transaction AÇILMADAN önce yapılmalı
(örn. ayrı bean + REQUIRES_NEW veya transaction dışı kod yolu).

## Migration Kapsamı — Entity vs DB Eşleşmesi (kritik)

Bir entity'ye yeni alan eklendiğinde kolon **TÜM tenant DB'lerinde** (tenant1 + tenant2 + gelecekte
eklenenler) olmalıdır.

**Neden:** Prod `hbm2ddl.auto=validate` tek DB'yi doğrular ama runtime'da Hibernate her tenant
context'inde aynı SQL'i üretir. Bir tenant'ta kolon eksikse ilk istek `column does not exist` → 500.

**Çözüm:** `src/main/resources/migration/` altındaki SQL'ler deploy öncesi HER tenant DB'sinde çalıştırılır.

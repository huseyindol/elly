---
name: multitenant-routing
description: Multi-tenant veri yönlendirme, tenant context yönetimi ve tenant izolasyonu. Tenant routing, DataSource değiştirme, tenant-specific query veya tenant context hataları ile ilgili konularda otomatik aktif et.
version: 1.0.0
---

# Elly Multi-Tenant Routing

## Mimari
- **Strateji:** Database-per-tenant (her tenant ayrı PostgreSQL DB'si)
- **Tenant'lar:** `basedb` (varsayılan), `tenant1`, `tenant2`
- **DataSource Routing:** `TenantDataSourceRouter extends AbstractRoutingDataSource`
- **Context Holder:** `TenantContext` (ThreadLocal tabanlı)

## Request Akışı
```
HTTP Request
  → JwtTenantFilter
      → JWT'den tenantId claim'ini çıkar
      → TenantContext.setCurrentTenant(tenantId)
  → TenantDataSourceRouter.determineCurrentLookupKey()
      → TenantContext.getCurrentTenant() döndürür
  → Doğru PostgreSQL DB'sine bağlan
  → İşlem tamamlanınca TenantContext.clear()  ← finally bloğunda!
```

## Kritik Notlar

**@Async kullanımında:**
Async method'larda ThreadLocal kaybolur. Tenant'ı manuel aktar:
```java
String tenantId = TenantContext.getCurrentTenant();
CompletableFuture.runAsync(() -> {
    TenantContext.setCurrentTenant(tenantId);
    try { /* iş */ } finally { TenantContext.clear(); }
});
```

**Cache key'lerinde:**
Multi-tenant cache'ler için tenantId'yi key'e ekle:
```java
@Cacheable(value = "users", key = "#tenantId + ':' + #userId")
```

**Test'lerde:**
```java
@BeforeEach
void setUp() {
    TenantContext.setCurrentTenant("basedb");
}
@AfterEach
void tearDown() {
    TenantContext.clear();
}
```

## User Routing — Kritik Bilgi (2026-04-29'dan itibaren)

### Kullanıcı Nerede Saklanır?
```
basedb   → Panel admin kullanıcıları  (loginSource="admin")
tenant1  → Tenant1 site kullanıcıları (loginSource="tenant", tenantId="tenant1")
tenant2  → Tenant2 site kullanıcıları
```

### JwtAuthenticationFilter — loadUserFromDbAndCache()
Artık **hardcoded basedb kullanmıyor**. JwtTenantFilter'ın set ettiği TenantContext geçerli:
- Admin JWT → JwtTenantFilter null set eder → defaultDataSource (basedb) → user basedb'den yüklenir
- Tenant JWT → JwtTenantFilter "tenant1" set eder → tenant1 DataSource → user tenant1'den yüklenir

### register() — Tenant DB'sine Kayıt
```json
POST /api/v1/auth/register
{
  "username": "ali",
  "email": "ali@site.com",
  "password": "123456",
  "tenantId": "tenant1"   ← bu alan varsa tenant1 DB'sine kaydeder
}
```
`tenantId` verilmezse mevcut TenantContext kullanılır (genellikle basedb).

### Admin Panel → Tenant Kullanıcı Yönetimi
Panel admin JWT ile tenant DB'lerindeki kullanıcılara erişim:
```
GET/POST/PUT/DELETE /api/v1/admin/tenants/{tenantId}/users
PATCH               /api/v1/admin/tenants/{tenantId}/users/{id}/status
```
`TenantUserService.inTenantContext(tenantId, ...)` — her metod geçici switch yapar, finally'de restore eder.

## Hata Senaryoları
- `TenantContext` null ise → `basedb`'ye fallback (DataSourceConfig'de tanımlı)
- Bilinmeyen tenant adı → `IllegalArgumentException` fırlatılır
- Transaction içinde tenant değiştirme → **Tehlikeli**, kaçın

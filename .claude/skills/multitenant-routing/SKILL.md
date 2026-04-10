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

## Hata Senaryoları
- `TenantContext` null ise → `basedb`'ye fallback (DataSourceConfig'de tanımlı)
- Bilinmeyen tenant adı → `IllegalArgumentException` fırlatılır
- Transaction içinde tenant değiştirme → **Tehlikeli**, kaçın

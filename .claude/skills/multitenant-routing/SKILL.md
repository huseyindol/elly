---
name: multitenant-routing
description: Multi-tenant veri yönlendirme, tenant context yönetimi ve tenant izolasyonu. Tenant routing, DataSource değiştirme, tenant-specific query veya tenant context hataları ile ilgili konularda otomatik aktif et.
version: 1.0.0
---

# Elly Multi-Tenant Routing

## Mimari
- **Strateji:** Database-per-tenant (her tenant ayrı PostgreSQL DB'si)
- **Tenant'lar:** `basedb` (varsayılan), `tenant1`, `tenant2`
- **DataSource Routing:** `TenantRoutingDataSource extends AbstractRoutingDataSource`
- **Context Holder:** `TenantContext` (ThreadLocal tabanlı)

## Request Akışı
```
HTTP Request
  → JwtTenantFilter
      → JWT'den tenantId claim'ini çıkar
      → TenantContext.setTenantId(tenantId)
  → TenantRoutingDataSource.determineCurrentLookupKey()
      → TenantContext.getTenantId() döndürür
  → Doğru PostgreSQL DB'sine bağlan
  → İşlem tamamlanınca TenantContext.clear()  ← finally bloğunda!
```

## Kritik Notlar

**@Async kullanımında:**
Async method'larda ThreadLocal kaybolur. Tenant'ı manuel aktar:
```java
String tenantId = TenantContext.getTenantId();
CompletableFuture.runAsync(() -> {
    TenantContext.setTenantId(tenantId);
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
    TenantContext.setTenantId("basedb");
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

## Cross-Tenant Query — Geçici Context Switch Pattern

Bir tenant context'inde çalışırken farklı bir tenant'ın DB'sine sorgu yapmak için (örn. tenant1 kaydında basedb'deki mail hesabını bulmak):

```java
String currentTenant = TenantContext.getTenantId();
try {
  TenantContext.setTenantId("basedb");
  var account = mailAccountRepository.findByTenantIdAndIsPrimaryTrueAndActiveTrue(tenantId);
  // ... iş mantığı
} finally {
  TenantContext.setTenantId(currentTenant);   // ZORUNLU restore — clear() değil
}
```

**Kullanım yerleri:**
- `AuthService.sendVerificationEmail()` — tenant kaydı sırasında basedb'deki primary mail hesabı
- `TenantUserService.inTenantContext(tenantId, ...)` — admin panelden tenant user yönetimi

**Kural:** `finally` bloğu mutlaka restore etmeli. `TenantContext.clear()` kullanma — JwtTenantFilter outer finally'de zaten clear ediyor, içeride clear edersen önceki context'i kaybedersin.

## Migration Kapsamı — Entity vs DB Eşleşmesi (kritik)

Bir entity'ye yeni alan eklendiğinde, **entity sınıfının erişilebildiği tüm tenant DB'lerinde** kolon olmalıdır. Veri sadece basedb'de saklansa bile.

**Neden:** `hbm2ddl.auto=validate` sadece basedb'i doğrular (startup başarılı) ama runtime'da Hibernate her tenant context'inde aynı SQL'i üretir:
```sql
SELECT id, name, ..., yeni_kolon FROM tablo WHERE ...
```
Tenant1'de `yeni_kolon` yoksa runtime'da `column "yeni_kolon" does not exist` → 500.

**Tipik tuzak:** "Bu kolon sadece basedb'de kullanılacak" → sadece basedb'de migration → tenant1 üzerinden gelen ilk istek patlar.

**Çözüm:** `com.cms.entity.*` altındaki entity değişiklikleri için migration **basedb + tenant1 + tenant2** üzerinde çalıştırılır. Veri içeriği basedb'ye özelse, tenant1/tenant2'deki kolonlar boş kalır — sorun değil.

# Elly CMS — Claude Proje Bağlamı

## Proje Özeti
Spring Boot 3.5.7 tabanlı multi-tenant CMS. Java 21, PostgreSQL (database-per-tenant), Redis cache, JWT + OAuth2 auth.

## Mimari
- **Pattern:** Monolith, katmanlı mimari
- **Katmanlar:** IController → Controller → IService → Service → Repository → Entity
- **DTO mapping:** MapStruct (`@Mapper(componentModel = "spring")`)
- **Response wrapper:** `RootEntityResponse<T>` — tüm API yanıtları bu wrapper ile döner
- **Boilerplate:** Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`)

## Multi-Tenancy
- **Strateji:** Database-per-tenant
- **Tenant'lar:** `basedb` (varsayılan), `tenant1`, `tenant2`
- **Routing:** `DataSourceConfig` → `TenantDataSourceRouter`
- **Context:** JWT claim'den `tenantId` alınır, `TenantContext.setCurrentTenant()` ile set edilir
- **Filtreleme:** `JwtTenantFilter` → her request'te tenant context kurulur

## Kimlik Doğrulama
- **Admin login:** `POST /api/auth/admin/login` → `AdminLoginInterceptor`
- **Tenant login:** `POST /api/auth/login` → `TenantLoginInterceptor`
- **OAuth2:** Google, Facebook, GitHub → `OAuth2AuthenticationSuccessHandler`
- **JWT:** Access token + refresh token, `loginSource` ve `tenantId` claim'leri içerir
- **Util:** `JwtUtil.generateToken(user, loginSource, tenantId)`

## Build & Çalıştırma
```bash
./mvnw clean package -DskipTests   # Build
./mvnw spring-boot:run              # Lokal çalıştır
make build && make up               # Docker ile
./mvnw test                         # Testler
```

## K8s Deployment
```bash
kubectl apply -f k8s/               # Tüm kaynakları uygula
kubectl rollout restart deployment/elly-app -n elly  # Yeniden başlat
```

## Veritabanı
- PostgreSQL multi-DB: `elly_basedb`, `elly_tenant1`, `elly_tenant2`
- Migrations: `src/main/resources/` altındaki `.sql` dosyaları
- Backup: `make backup` / `make restore`

## Kod Standartları
- Yeni özellik eklerken IService + Service + IController + Controller + DTO + Mapper pattern'ini takip et
- Her endpoint `RootEntityResponse<T>` döndürmeli
- Cache invalidation: `@CacheEvict(value = "...", allEntries = true)` kullan
- Exception: `GlobalExceptionHandler` (@ControllerAdvice) mevcut, yeni exception'ları buraya ekle
- Multi-tenant sorgularda her zaman tenant context'e göre filtrele
- `@Autowired` field injection kullanma, constructor injection kullan (`@RequiredArgsConstructor`)
- Entity'yi doğrudan controller'dan döndürme, MapStruct mapper kullan

Rol ve Görev:
Sen uzman bir Java Spring Boot Yazılım Mimarı ve Backend Geliştiricisisin. Senden tek bir kod tabanı üzerinden birden fazla PostgreSQL veritabanına hizmet verecek, "Database-per-Tenant" (Multi-Tenancy) mimarisine sahip bir REST API altyapısı kurmanı istiyorum.

Kritik Mimari Kurallar ve Kısıtlamalar:

Dinamik Yönlendirme: Çözüm kesinlikle AbstractRoutingDataSource üzerine kurulmalıdır.

Güvenlik (JWT Entegrasyonu): Tenant bilgisini (hangi DB'ye gidileceği) asla güvensiz olan raw HTTP Header'larından (örn: X-Tenant-ID) almamalısın. Sistem tamamen Stateless olmalı ve tenant bilgisi, gelen Authorization header'ındaki JWT token'ın payload kısmından (claims) çıkarılmalıdır.

Thread Yönetimi: Her HTTP isteği için tenant bilgisini tutmak adına ThreadLocal kullanan bir TenantContext sınıfı yazmalısın. Memory leak (hafıza sızıntısı) oluşmaması için request bittiğinde ThreadLocal'in kesinlikle temizlendiğinden (clear) emin olmalısın. Bunu bir OncePerRequestFilter veya HandlerInterceptor içinde yapmalısın.

Yatay Ölçekleme ve Connection Pool Optimizasyonu: Bu uygulama yatayda ölçeklenen (auto-scaling) bir konteyner ortamında (Cloud Run / K8s) çalışacak. Veritabanının connection pool limitlerini tüketmemek için, her bir tenant'ın veritabanı bağlantısı için HikariCP ayarlarını açıkça konfigüre etmelisin (maximumPoolSize, minimumIdle, connectionTimeout, maxLifetime gibi değerleri best-practice'lere göre belirle).

JPA/Hibernate Uyumluluğu: Kurduğun yapının Spring Data JPA ve Hibernate ile sorunsuz çalıştığından emin ol.

Senden İstediklerim (Kod Çıktıları):
Lütfen aşağıdaki sınıfları ve konfigürasyonları sırasıyla ve açıklamalı olarak yaz:

TenantContext.java (ThreadLocal yönetimi)

JwtTenantFilter.java (JWT'den tenantId okuyup context'e set eden ve finally bloğunda temizleyen filtre)

TenantRoutingDataSource.java (AbstractRoutingDataSource implementasyonu)

DataSourceConfig.java (HikariCP ayarlarıyla birlikte statik veya dinamik olarak tenant veritabanlarını yükleyen konfigürasyon sınıfı)

Eğer gerekiyorsa JPA/Hibernate için ek konfigürasyon sınıfı.

Lütfen kodların temiz, okunabilir ve güncel Spring Boot 3.x standartlarına uygun olduğundan emin ol.
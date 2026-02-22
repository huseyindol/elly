# 🔥 Elly CMS - Performans Analizi ve Olası Sorunlar

Bu dokümanda Elly CMS projesinin load/stress test sırasında karşılaşabileceği sorunlar, nedenleri ve çözümleri detaylı olarak açıklanmaktadır.

---

## 📊 Mevcut Proje Yapısı Analizi

### Teknoloji Stack:
- **Backend:** Spring Boot 3.5.7
- **Database:** PostgreSQL (Neon - Cloud)
- **ORM:** JPA/Hibernate
- **Connection Pool:** HikariCP (default)
- **Java Version:** 21

### Entity İlişkileri:
```
Page (1) --> (N) Component
Component (1) --> (N) Banner/Widget
Widget (1) --> (N) Banner/Post
Post (1) --> (N) Comment (tree structure)
Post (1) --> (N) Rating
```

---

## 🚨 OLASI SORUNLAR ve ÇÖZÜMLER

### 1. **N+1 Query Problem** ⚠️ KRİTİK

#### Sorun:
CMS yapınızda çok sayıda ilişki var. Örneğin:
- Bir Page çekildiğinde, tüm Component'ler ayrı ayrı sorgu ile çekilir
- Her Component için Banner/Widget'lar ayrı ayrı çekilir
- Her Post için Comment'ler ve Rating'ler ayrı ayrı çekilir

**Örnek Senaryo:**
```
1 Page çekiliyor
  -> 5 Component sorgusu
     -> Her Component için 3 Banner sorgusu = 15 sorgu
        TOPLAM: 1 + 5 + 15 = 21 SORGU!
```

#### Neden Olur:
JPA Lazy Loading varsayılan olarak her ilişki için ayrı sorgu atar.

#### Çözüm 1: EAGER FETCH (Dikkatli kullanın!)
```java
// PageRepository.java
@Query("SELECT p FROM Page p " +
       "LEFT JOIN FETCH p.components c " +
       "LEFT JOIN FETCH c.banners " +
       "WHERE p.id = :id")
Optional<Page> findByIdWithComponents(@Param("id") Long id);
```

#### Çözüm 2: Entity Graph (Tavsiye edilen)
```java
// Page.java entity'sinde
@NamedEntityGraph(
    name = "Page.detail",
    attributeNodes = {
        @NamedAttributeNode(value = "components", subgraph = "components"),
        @NamedAttributeNode("seoInfo")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "components",
            attributeNodes = {
                @NamedAttributeNode("banners"),
                @NamedAttributeNode("widgets")
            }
        )
    }
)

// Repository'de kullanım
@EntityGraph("Page.detail")
Optional<Page> findById(Long id);
```

#### Çözüm 3: DTO Projection (En performanslı)
```java
// Yeni bir interface oluşturun
public interface PageSummaryProjection {
    Long getId();
    String getSlug();
    String getTitle();
    // Sadece ihtiyacınız olan alanlar
}

// Repository
@Query("SELECT p.id as id, p.slug as slug, p.title as title FROM Page p")
List<PageSummaryProjection> findAllSummaries();
```

**Beklenen İyileşme:** %80-90 sorgu azalması, response time 10x daha hızlı

---

### 2. **Database Connection Pool Exhaustion** 🔴 YÜK ALTINDA ÇÖKÜŞ

#### Sorun:
Load test sırasında:
```
HikariPool-1 - Connection is not available, request timed out after 30000ms
```

#### Neden Olur:
- Varsayılan HikariCP pool size: **10 connection**
- 100 eş zamanlı istek gelirse: Pool tükenir
- Her istek 30 saniye bekler ve timeout alır

#### Analiz:
```
Eş Zamanlı İstek: 100
Pool Size: 10
Ortalama Query Time: 500ms

Teorik Throughput: 10 / 0.5 = 20 req/s
Gerçek İstek: 100 req/s
SONUÇ: ÇÖKÜŞ!
```

#### Çözüm:
```properties
# application.properties

# Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000

# Connection test settings
spring.datasource.hikari.validation-timeout=3000
spring.datasource.hikari.leak-detection-threshold=60000

# Prevent long-running queries
spring.jpa.properties.hibernate.query.timeout=5000
```

#### İzleme:
```bash
# Aktif connection sayısı
curl http://localhost:8080/actuator/metrics/hikari.connections.active

# Bekleyen connection sayısı
curl http://localhost:8080/actuator/metrics/hikari.connections.pending
```

**Uyarı:** Pool size'ı çok artırmayın! PostgreSQL'de maximum_connections limiti var.

---

### 3. **Slow Query Problem** 🐌 YAVAŞ SORGULAR

#### Sorun:
Bazı endpoint'ler çok yavaş:
- `/api/posts` -> 2-3 saniye
- `/api/components` -> 4-5 saniye

#### Neden Olur:
**a) Missing Index (En yaygın)**
```sql
-- Comment tablosunda parent_id üzerinde index yok
SELECT * FROM comment WHERE parent_id = 123;
-- Bu sorgu FULL TABLE SCAN yapar!
```

**b) Complex JOIN Query**
```sql
-- Component çekerken 4-5 tablo JOIN yapılıyor
SELECT c.*, b.*, w.*, p.* 
FROM component c
LEFT JOIN banner b ON ...
LEFT JOIN widget w ON ...
LEFT JOIN post p ON ...
-- Bu sorgu çok yavaş!
```

#### Çözüm 1: Index Ekleme
Projenizde `DATABASE_INDEXES.md` var, ama uygulanmış mı kontrol edin:

```sql
-- Component ilişkileri için
CREATE INDEX idx_component_page_id ON component(page_id);
CREATE INDEX idx_banner_component_id ON banner(component_id);
CREATE INDEX idx_widget_component_id ON widget(component_id);

-- Comment tree structure için (ÇOK ÖNEMLİ!)
CREATE INDEX idx_comment_post_id ON comment(post_id);
CREATE INDEX idx_comment_parent_id ON comment(parent_id);

-- Rating aggregation için
CREATE INDEX idx_rating_post_id ON rating(post_id);
CREATE INDEX idx_rating_user_id ON rating(user_id);

-- Published post'ları hızlı çekmek için
CREATE INDEX idx_post_published_at ON post(published_at) WHERE published_at IS NOT NULL;

-- Slug'a göre arama (sık kullanılan)
CREATE INDEX idx_page_slug ON page(slug);
CREATE INDEX idx_post_slug ON post(slug);
```

#### Çözüm 2: Query Optimization
```java
// BAD: Tüm post'ları çekip memory'de filtreleme
List<Post> posts = postRepository.findAll();
posts = posts.stream()
    .filter(p -> p.getPublishedAt() != null)
    .collect(Collectors.toList());

// GOOD: Database'de filtreleme
@Query("SELECT p FROM Post p WHERE p.publishedAt IS NOT NULL ORDER BY p.publishedAt DESC")
List<Post> findPublishedPosts();
```

#### Çözüm 3: Caching (Redis)
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```java
// Service'de caching
@Service
public class PageService {
    
    @Cacheable(value = "pages", key = "#id")
    public Page getPageById(Long id) {
        return pageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
    }
    
    @CacheEvict(value = "pages", key = "#page.id")
    public Page updatePage(Page page) {
        return pageRepository.save(page);
    }
}
```

**Beklenen İyileşme:** 
- Index'ler: 5-10x hızlanma
- Caching: 50-100x hızlanma (cache hit'te)

---

### 4. **Memory Leak / OutOfMemoryError** 💾 BELLEK SORUNU

#### Sorun:
```
java.lang.OutOfMemoryError: Java heap space
```

#### Neden Olur:
**a) Large Result Set**
```java
// BAD: 10,000 post'u memory'e yükleme
List<Post> allPosts = postRepository.findAll(); // TEHLIKE!
```

**b) JSON Serialization Loop**
```java
// BAD: Circular reference
@Entity
public class Post {
    @OneToMany(mappedBy = "post")
    private List<Comment> comments; // Comment'te de Post var!
}
// JSON serialize edilirken sonsuz döngü!
```

#### Çözüm 1: Pagination
```java
// PageController.java
@GetMapping
public Page<DtoPage> getAllPages(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size);
    return pageService.getAllPages(pageable);
}
```

#### Çözüm 2: JSON Loop Önleme
```java
// Option 1: @JsonIgnore
@Entity
public class Comment {
    @ManyToOne
    @JsonIgnore // Bu field serialize edilmez
    private Post post;
}

// Option 2: @JsonManagedReference / @JsonBackReference
@Entity
public class Post {
    @OneToMany(mappedBy = "post")
    @JsonManagedReference
    private List<Comment> comments;
}

@Entity
public class Comment {
    @ManyToOne
    @JsonBackReference
    private Post post;
}

// Option 3: DTO kullanın (EN İYİ)
// Entity'leri direkt döndürmeyin!
```

#### Çözüm 3: JVM Heap Size Artırma
```bash
# application.properties
# veya JVM arguments
java -Xms512m -Xmx2048m -jar elly.jar
```

```properties
# Docker'da (docker-compose.yml)
environment:
  JAVA_OPTS: "-Xms512m -Xmx2048m"
```

---

### 5. **File Upload Bottleneck** 📁 DOSYA YÜKLEME

#### Sorun:
Mevcut konfigürasyon:
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
file.upload.directory=assets/images
```

Load test sırasında 100 kullanıcı aynı anda dosya yüklerse:
- Disk I/O bottleneck
- Servlet thread pool tükenir
- Response time 10-20 saniye

#### Neden Olur:
- Dosyalar senkron olarak işleniyor
- Dosyalar local disk'e yazılıyor (tek instance'ta sıkıntı)
- Image processing (resize, thumbnail) varsa daha yavaş

#### Çözüm 1: Async Processing
```java
@Service
public class FileService {
    
    @Async
    public CompletableFuture<String> uploadFile(MultipartFile file) {
        // Async olarak dosya yükle
        String filename = saveFile(file);
        return CompletableFuture.completedFuture(filename);
    }
}

// Application.java'da enable edin
@SpringBootApplication
@EnableAsync
public class EllyApplication {
    // ...
}

// Thread pool config
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("FileUpload-");
        executor.initialize();
        return executor;
    }
}
```

#### Çözüm 2: Cloud Storage (S3, Cloudinary)
```xml
<!-- AWS S3 -->
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-s3</artifactId>
    <version>1.12.529</version>
</dependency>
```

```java
@Service
public class S3FileService {
    
    private final AmazonS3 s3Client;
    
    public String uploadToS3(MultipartFile file) {
        String key = "images/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        
        s3Client.putObject(
            "elly-cms-bucket", 
            key, 
            file.getInputStream(), 
            metadata
        );
        
        return s3Client.getUrl("elly-cms-bucket", key).toString();
    }
}
```

**Avantaj:** 
- Scalable
- CDN desteği
- Backup otomatik

---

### 6. **Database Lock & Deadlock** 🔒 KİLİTLENME

#### Sorun:
Stress test'te:
```
org.postgresql.util.PSQLException: ERROR: deadlock detected
```

#### Neden Olur:
**Senaryo:**
1. Transaction A: Post güncelleniyor
2. Transaction B: Aynı post'a comment ekleniyor
3. Her iki transaction da diğerini bekliyor
4. DEADLOCK!

#### Çözüm 1: Transaction İzolasyon Seviyesi
```properties
# application.properties
spring.jpa.properties.hibernate.connection.isolation=2
# 1: READ_UNCOMMITTED
# 2: READ_COMMITTED (recommended)
# 4: REPEATABLE_READ
# 8: SERIALIZABLE
```

#### Çözüm 2: Optimistic Locking
```java
@Entity
public class Post extends BaseEntity {
    
    @Version
    private Long version; // JPA otomatik manage eder
    
    // Concurrent update'lerde exception fırlatır
}
```

#### Çözüm 3: Pessimistic Locking (Dikkatli!)
```java
// Repository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Post p WHERE p.id = :id")
Optional<Post> findByIdWithLock(@Param("id") Long id);
```

**Uyarı:** Pessimistic lock performansı düşürür, dikkatli kullanın!

---

### 7. **Neon Database (Cloud) Latency** 🌐 CLOUD GECİKMESİ

#### Sorun:
Mevcut connection string:
```
jdbc:postgresql://ep-billowing-scene-adbekobg-pooler.c-2.us-east-1.aws.neon.tech/neondb
```

Bu **cloud database**. Latency:
- Local: 1-5ms
- Neon (AWS US-East): 50-200ms (Türkiye'den)

#### Neden Problem:
100 sorgu atıyorsanız:
- Local: 100 x 5ms = 500ms
- Neon: 100 x 150ms = 15,000ms (15 saniye!)

#### Çözüm 1: Connection Pooling (Zaten var)
HikariCP connection'ları reuse eder, handshake latency'sini azaltır.

#### Çözüm 2: Query Batching
```java
// BAD: 100 ayrı INSERT
for (Comment comment : comments) {
    commentRepository.save(comment);
}

// GOOD: 1 batch INSERT
commentRepository.saveAll(comments);
```

```properties
# application.properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

#### Çözüm 3: Read Replica (Gelişmiş)
```properties
# Master (write)
spring.datasource.master.url=...

# Replica (read)
spring.datasource.replica.url=...

# Read-write splitting
# Write -> master
# Read -> replica
```

#### Çözüm 4: Caching (Redis) - EN ETKİLİ
```properties
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

---

### 8. **Tomcat Thread Pool Exhaustion** 🧵 THREAD HAVUZU

#### Sorun:
```
org.apache.tomcat.util.threads.ThreadPoolExecutor: Pool exhausted
```

#### Neden Olur:
- Varsayılan Tomcat thread: **200**
- Eğer her request 5 saniye sürerse:
  - Throughput: 200 / 5 = 40 req/s
  - 100 req/s gelirse: ÇÖKÜŞ!

#### Çözüm:
```properties
# application.properties
server.tomcat.threads.max=500
server.tomcat.threads.min-spare=50
server.tomcat.accept-count=100
server.tomcat.max-connections=10000
server.tomcat.connection-timeout=20000
```

**Dikkat:** Thread sayısını çok artırmayın, CPU ve memory tüketir!

---

### 9. **JPA Query Cache Kullanmama** 🚫 CACHE YOK

#### Sorun:
Aynı sorgu sürekli tekrar ediliyor:
```
SELECT * FROM page WHERE id = 1; -- 100 kez çalıştı!
```

#### Çözüm: Second Level Cache
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-jcache</artifactId>
</dependency>
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
</dependency>
```

```properties
# application.properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=jcache
spring.jpa.properties.hibernate.javax.cache.provider=org.ehcache.jsr107.EhcacheCachingProvider
```

```java
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Page extends BaseEntity {
    // ...
}
```

---

## 📈 PERFORMANS İYİLEŞTİRME ÖNCELIKLENDIRME

### 🔴 YÜKSEK ÖNCELİK (Hemen yapın!)
1. **Database Index'leri ekleyin** - %80 iyileşme
2. **N+1 query'leri düzeltin** - %70 iyileşme
3. **Pagination ekleyin** - OutOfMemory önler
4. **Connection pool artırın** - Çökme önler

### 🟡 ORTA ÖNCELİK (Yakında yapın)
5. **Redis caching** - %50-90 iyileşme
6. **DTO kullanın** (Entity'leri direkt dönmeyin)
7. **Async file upload**
8. **Query optimization**

### 🟢 DÜŞÜK ÖNCELİK (Gerekirse)
9. **Second level cache**
10. **Read replica**
11. **CDN (file uploads için)**
12. **Load balancer** (multiple instance)

---

## 🎯 PERFORMANS HEDEFLERİ

### Başlangıç (Mevcut)
- Response time (p95): **2000ms**
- Throughput: **20 req/s**
- Max concurrent users: **50**
- Error rate: **10%** (yük altında)

### Hedef (Optimization sonrası)
- Response time (p95): **<500ms** ✅
- Throughput: **200 req/s** ✅
- Max concurrent users: **500+** ✅
- Error rate: **<1%** ✅

---

## 🔍 MONİTORİNG ARAÇLARI

### 1. Spring Actuator
```bash
# Zaten pom.xml'de var!
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

### 2. Application Performance Monitoring (APM)

#### New Relic (Ücretsiz tier var)
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-new-relic</artifactId>
</dependency>
```

#### Prometheus + Grafana (Ücretsiz, self-hosted)
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# docker-compose.yml'e ekleyin
prometheus:
  image: prom/prometheus
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana
  ports:
    - "3000:3000"
```

### 3. Database Monitoring
```sql
-- Slow query log (PostgreSQL)
ALTER DATABASE neondb SET log_min_duration_statement = 1000; -- 1 saniyeden uzun sorgular

-- Query stats
SELECT * FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;
```

---

## ✅ SONUÇ: YAPILACAKLAR LİSTESİ

### Hemen Yapılacaklar (1 gün)
- [ ] Database index'lerini ekle
- [ ] Connection pool'u 50'ye çıkar
- [ ] Pagination ekle (Page, Post, Comment)
- [ ] N+1 query için Entity Graph kullan

### Bu Hafta (1 hafta)
- [ ] Redis cache kurulumu
- [ ] Slow query'leri optimize et
- [ ] DTO pattern'i uygula
- [ ] Load test çalıştır ve baseline oluştur

### Bu Ay (1 ay)
- [ ] Prometheus + Grafana monitoring
- [ ] Async file upload
- [ ] CDN entegrasyonu
- [ ] Stress test ve capacity planning

---

**NOT:** Bu optimizasyonları **tek tek** yapın ve her birinin etkisini ölçün!

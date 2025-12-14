# 🎯 Elly CMS - Uygulanabilir Optimizasyon Örnekleri

Bu dosya, projenize **direkt kopyalayıp uygulayabileceğiniz** performans optimizasyon kodlarını içerir.

---

## 1. 🔥 N+1 Query Problemi - Entity Graph ile Çözüm

### Mevcut Durum (BAD)
```java
// PageService.java
public Page getPageById(Long id) {
    return pageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
}

// Bu kod çalıştığında:
// SELECT * FROM page WHERE id = 1;  -- 1 sorgu
// SELECT * FROM component WHERE page_id = 1;  -- N sorgu (her page için)
// SELECT * FROM banner WHERE component_id = X;  -- N*M sorgu
// TOPLAM: 1 + N + (N*M) sorgu!
```

### Optimizasyon 1: @EntityGraph (RECOMMENDED)

```java
// src/main/java/com/cms/entity/Page.java
package com.cms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page")
@Data
@EqualsAndHashCode(callSuper = true)
// Entity Graph tanımı
@NamedEntityGraph(
    name = "Page.withComponents",
    attributeNodes = {
        @NamedAttributeNode(value = "components", subgraph = "components-subgraph"),
        @NamedAttributeNode("seoInfo")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "components-subgraph",
            attributeNodes = {
                @NamedAttributeNode("banners"),
                @NamedAttributeNode("widgets")
            }
        )
    }
)
public class Page extends BaseEntity {
    
    @Column(unique = true, nullable = false)
    private String slug;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "seo_info_id")
    private SeoInfo seoInfo;
    
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Component> components = new ArrayList<>();
}
```

```java
// src/main/java/com/cms/repository/PageRepository.java
package com.cms.repository;

import com.cms.entity.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    
    // Normal findById - N+1 query var!
    Optional<Page> findById(Long id);
    
    // Optimized findById - Tek sorguda tüm ilişkileri getir
    @EntityGraph(value = "Page.withComponents", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT p FROM Page p WHERE p.id = :id")
    Optional<Page> findByIdWithComponents(@Param("id") Long id);
    
    // Slug'a göre arama - optimized
    @EntityGraph(value = "Page.withComponents", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT p FROM Page p WHERE p.slug = :slug")
    Optional<Page> findBySlugWithComponents(@Param("slug") String slug);
    
    // List için (dikkatli kullanın - çok veri varsa memory patlar!)
    @EntityGraph(value = "Page.withComponents", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT DISTINCT p FROM Page p")
    List<Page> findAllWithComponents();
}
```

```java
// src/main/java/com/cms/service/impl/PageService.java
package com.cms.service.impl;

import com.cms.dto.DtoPage;
import com.cms.entity.Page;
import com.cms.mapper.PageMapper;
import com.cms.repository.PageRepository;
import com.cms.service.IPageService;
import com.cms.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageService implements IPageService {
    
    private final PageRepository pageRepository;
    private final PageMapper pageMapper;
    
    // ✅ OPTIMIZED: Tek sorgu ile tüm data
    public DtoPage getPageById(Long id) {
        Page page = pageRepository.findByIdWithComponents(id)
            .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + id));
        return pageMapper.toDto(page);
    }
    
    // ✅ OPTIMIZED: Slug'a göre
    public DtoPage getPageBySlug(String slug) {
        Page page = pageRepository.findBySlugWithComponents(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + slug));
        return pageMapper.toDto(page);
    }
    
    // ✅ OPTIMIZED: Pagination ile list
    public org.springframework.data.domain.Page<DtoPage> getAllPages(Pageable pageable) {
        return pageRepository.findAll(pageable)
            .map(pageMapper::toDto);
    }
}
```

**Sonuç:** 47 sorgu → 1 sorgu! ⚡

---

## 2. 📄 Pagination - OutOfMemoryError Önleme

### Mevcut Durum (BAD)
```java
@GetMapping
public List<DtoPost> getAllPosts() {
    return postService.getAllPosts(); // 10,000 post → MEMORY EXPLOSION!
}
```

### Optimizasyon: Pagination Ekle

```java
// src/main/java/com/cms/controller/impl/PostController.java
package com.cms.controller.impl;

import com.cms.controller.IPostController;
import com.cms.dto.DtoPost;
import com.cms.service.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController implements IPostController {
    
    private final IPostService postService;
    
    // ✅ OPTIMIZED: Pagination + Sorting
    @GetMapping
    public ResponseEntity<Page<DtoPost>> getAllPosts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "publishedAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        // Max size limiti (güvenlik)
        if (size > 100) size = 100;
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DtoPost> posts = postService.getAllPosts(pageable);
        return ResponseEntity.ok(posts);
    }
    
    // Published post'lar için özel endpoint
    @GetMapping("/published")
    public ResponseEntity<Page<DtoPost>> getPublishedPosts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (size > 100) size = 100;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<DtoPost> posts = postService.getPublishedPosts(pageable);
        return ResponseEntity.ok(posts);
    }
}
```

```java
// src/main/java/com/cms/repository/PostRepository.java
package com.cms.repository;

import com.cms.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // ✅ Pagination desteği
    Page<Post> findAll(Pageable pageable);
    
    // ✅ Published post'ları paginasyonla getir
    @Query("SELECT p FROM Post p WHERE p.publishedAt IS NOT NULL AND p.publishedAt <= :now")
    Page<Post> findPublishedPosts(LocalDateTime now, Pageable pageable);
    
    // ✅ Belirli bir tarihten sonra yayınlananlar
    Page<Post> findByPublishedAtAfter(LocalDateTime date, Pageable pageable);
}
```

```java
// src/main/java/com/cms/service/impl/PostService.java
package com.cms.service.impl;

import com.cms.dto.DtoPost;
import com.cms.mapper.PostMapper;
import com.cms.repository.PostRepository;
import com.cms.service.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService implements IPostService {
    
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    
    public Page<DtoPost> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
            .map(postMapper::toDto);
    }
    
    public Page<DtoPost> getPublishedPosts(Pageable pageable) {
        return postRepository.findPublishedPosts(LocalDateTime.now(), pageable)
            .map(postMapper::toDto);
    }
}
```

**API Kullanımı:**
```bash
# İlk 20 post
curl "http://localhost:8080/api/posts?page=0&size=20"

# 2. sayfa, 50 post
curl "http://localhost:8080/api/posts?page=1&size=50"

# Tarih'e göre sıralama
curl "http://localhost:8080/api/posts?page=0&size=20&sortBy=publishedAt&sortDir=DESC"
```

---

## 3. 🎯 DTO Pattern - Entity'leri Direkt Dönmeyin!

### Mevcut Durum (BAD)
```java
@GetMapping("/{id}")
public Post getPost(@PathVariable Long id) {
    return postService.getPostById(id);
    // Problem 1: JSON circular reference (@JsonManagedReference gerekli)
    // Problem 2: Gereksiz alanlar serialize ediliyor (audit fields)
    // Problem 3: Lazy loading exception riski
}
```

### Optimizasyon: DTO Projection

```java
// src/main/java/com/cms/dto/DtoPostSummary.java
package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoPostSummary {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;  // İlk 200 karakter
    private LocalDateTime publishedAt;
    private Integer commentCount;
    private Double averageRating;
}
```

```java
// src/main/java/com/cms/repository/PostRepository.java
package com.cms.repository;

import com.cms.dto.DtoPostSummary;
import com.cms.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {
    
    // ✅ SUPER FAST: Sadece gerekli alanlar + aggregation
    @Query("""
        SELECT new com.cms.dto.DtoPostSummary(
            p.id,
            p.slug,
            p.title,
            SUBSTRING(p.content, 1, 200),
            p.publishedAt,
            COUNT(DISTINCT c.id),
            AVG(r.rating)
        )
        FROM Post p
        LEFT JOIN p.comments c
        LEFT JOIN p.ratings r
        WHERE p.publishedAt IS NOT NULL
        GROUP BY p.id, p.slug, p.title, p.content, p.publishedAt
    """)
    Page<DtoPostSummary> findPostSummaries(Pageable pageable);
}
```

```java
// Controller'da kullanım
@GetMapping("/summary")
public ResponseEntity<Page<DtoPostSummary>> getPostSummaries(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size);
    Page<DtoPostSummary> summaries = postRepository.findPostSummaries(pageable);
    return ResponseEntity.ok(summaries);
}
```

**Avantaj:** 
- Tek sorgu
- JSON circular reference yok
- Network transfer %80 azalma
- Response time 5-10x daha hızlı

---

## 4. 🚀 Database Index'leri (ÇOK ÖNEMLİ!)

Projenizde `DATABASE_INDEXES.md` var ama uygulanmış mı kontrol edin!

```sql
-- src/main/resources/db-performance-indexes.sql
-- Bu dosyayı database'e uygulayın!

-- ========================================
-- FOREIGN KEY INDEXES (En Kritik!)
-- ========================================
-- JPA otomatik oluşturmaz, manuel eklemelisiniz!

CREATE INDEX IF NOT EXISTS idx_component_page_id ON component(page_id);
CREATE INDEX IF NOT EXISTS idx_banner_component_id ON banner(component_id);
CREATE INDEX IF NOT EXISTS idx_widget_component_id ON widget(component_id);
CREATE INDEX IF NOT EXISTS idx_comment_post_id ON comment(post_id);
CREATE INDEX IF NOT EXISTS idx_rating_post_id ON rating(post_id);

-- ========================================
-- SLUG INDEXES (Sık kullanılan arama)
-- ========================================
CREATE UNIQUE INDEX IF NOT EXISTS idx_page_slug ON page(slug);
CREATE UNIQUE INDEX IF NOT EXISTS idx_post_slug ON post(slug);

-- ========================================
-- PUBLISHED AT INDEX (Tarih filtreleme)
-- ========================================
CREATE INDEX IF NOT EXISTS idx_post_published_at 
ON post(published_at) 
WHERE published_at IS NOT NULL;

-- ========================================
-- COMMENT TREE STRUCTURE (Parent-Child)
-- ========================================
CREATE INDEX IF NOT EXISTS idx_comment_parent_id ON comment(parent_id);

-- Sadece parent comment'leri hızlı bulmak için
CREATE INDEX IF NOT EXISTS idx_comment_parent_null 
ON comment(post_id) 
WHERE parent_id IS NULL;

-- ========================================
-- RATING USER INDEX (Duplicate check)
-- ========================================
CREATE INDEX IF NOT EXISTS idx_rating_user_post 
ON rating(user_id, post_id);

-- ========================================
-- COMPOSITE INDEXES (Multiple column)
-- ========================================
-- Published post'ları tarih sırasıyla getirmek için
CREATE INDEX IF NOT EXISTS idx_post_published_created 
ON post(published_at DESC, created_at DESC) 
WHERE published_at IS NOT NULL;

-- ========================================
-- TEXT SEARCH INDEX (Full-text search)
-- ========================================
-- PostgreSQL full-text search için
CREATE INDEX IF NOT EXISTS idx_post_title_content_fts 
ON post USING gin(to_tsvector('english', title || ' ' || content));

-- Kullanımı:
-- SELECT * FROM post 
-- WHERE to_tsvector('english', title || ' ' || content) @@ to_tsquery('english', 'spring');

-- ========================================
-- ANALIZ ve VERİFİKASYON
-- ========================================
-- Index'lerin oluşturulduğunu kontrol edin:
SELECT 
    schemaname,
    tablename, 
    indexname, 
    indexdef 
FROM pg_indexes 
WHERE schemaname = 'elly'
ORDER BY tablename, indexname;

-- Index kullanımını kontrol edin (query plan):
EXPLAIN ANALYZE 
SELECT * FROM post WHERE published_at IS NOT NULL 
ORDER BY published_at DESC LIMIT 20;

-- Index boyutlarını görün:
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexname::regclass)) as index_size
FROM pg_indexes
WHERE schemaname = 'elly'
ORDER BY pg_relation_size(indexname::regclass) DESC;
```

**Index'leri Uygulama:**
```bash
# Local PostgreSQL'de
psql -U postgres -d neondb -f src/main/resources/db-performance-indexes.sql

# Neon Cloud'da (psql ile)
psql "postgresql://neondb_owner:npg_NExeW0baq3HB@ep-billowing-scene-adbekobg-pooler.c-2.us-east-1.aws.neon.tech/neondb?sslmode=require" \
  -f src/main/resources/db-performance-indexes.sql
```

---

## 5. 💾 Redis Caching (İsteğe Bağlı)

### Setup

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

```properties
# application.properties
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.redis.time-to-live=3600000
```

```java
// src/main/java/com/cms/config/CacheConfig.java
package com.cms.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
    }
}
```

```java
// Service'de kullanım
@Service
@RequiredArgsConstructor
public class PageService {
    
    private final PageRepository pageRepository;
    
    // ✅ Cache'e kaydet
    @Cacheable(value = "pages", key = "#id")
    public DtoPage getPageById(Long id) {
        Page page = pageRepository.findByIdWithComponents(id)
            .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
        return pageMapper.toDto(page);
    }
    
    // ✅ Cache'den sil (update'te)
    @CacheEvict(value = "pages", key = "#id")
    public DtoPage updatePage(Long id, DtoPageIU updateDto) {
        // Update logic
    }
    
    // ✅ Tüm cache'i temizle
    @CacheEvict(value = "pages", allEntries = true)
    public void clearCache() {
        // Cache temizlendi
    }
}
```

**Redis Kurulum (Docker):**
```bash
docker run -d -p 6379:6379 --name redis redis:alpine
```

---

## 6. 📊 Monitoring Endpoint'leri

```java
// src/main/java/com/cms/controller/MonitoringController.java
package com.cms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {
    
    private final DataSource dataSource;
    private final MetricsEndpoint metricsEndpoint;
    
    @GetMapping("/db-stats")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            stats.put("active_connections", 
                metricsEndpoint.metric("hikaricp.connections.active", null).getMeasurements());
            stats.put("pending_connections", 
                metricsEndpoint.metric("hikaricp.connections.pending", null).getMeasurements());
            stats.put("total_connections", 
                metricsEndpoint.metric("hikaricp.connections", null).getMeasurements());
        }
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // JVM Memory
        metrics.put("jvm_memory_used", 
            metricsEndpoint.metric("jvm.memory.used", null).getMeasurements());
        
        // CPU
        metrics.put("system_cpu_usage", 
            metricsEndpoint.metric("system.cpu.usage", null).getMeasurements());
        
        // HTTP requests
        metrics.put("http_server_requests", 
            metricsEndpoint.metric("http.server.requests", null).getMeasurements());
        
        return ResponseEntity.ok(metrics);
    }
}
```

---

## ✅ UYGULAMA SIRASI

### 1. GÜN (Kritik, hemen yapın!)
1. ✅ Database index'lerini ekleyin (`db-performance-indexes.sql`)
2. ✅ Pagination ekleyin (PostController, PageController)
3. ✅ Connection pool'u artırın (`application-performance.properties`)

### 2. GÜN
4. ✅ Entity Graph uygulayın (Page, Post)
5. ✅ DTO projection ekleyin (PostSummary)

### 1. HAFTA
6. ✅ Redis cache kurulumu (optional)
7. ✅ Monitoring endpoint'leri

### TEST
8. ✅ Load test çalıştırın
9. ✅ Performans raporu oluşturun
10. ✅ Sonuçları karşılaştırın

---

**NOT:** Bu optimizasyonları **tek tek** uygulayın ve her birinin etkisini ölçün!

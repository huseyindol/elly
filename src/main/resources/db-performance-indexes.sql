-- ========================================
-- ELLY CMS - Performance Indexes
-- ========================================
-- Bu dosyayı database'e uygulayın!
-- Usage: psql -U postgres -d neondb -f src/main/resources/db-performance-indexes.sql

-- ========================================
-- FOREIGN KEY INDEXES (En Kritik!)
-- ========================================
-- JPA otomatik oluşturmaz, manuel eklemelisiniz!

CREATE INDEX IF NOT EXISTS idx_component_page_id ON elly.component (page_id);

CREATE INDEX IF NOT EXISTS idx_banner_component_id ON elly.banner (component_id);

CREATE INDEX IF NOT EXISTS idx_widget_component_id ON elly.widget (component_id);

CREATE INDEX IF NOT EXISTS idx_comment_post_id ON elly.comment (post_id);

CREATE INDEX IF NOT EXISTS idx_rating_post_id ON elly.rating (post_id);

-- Banner-Widget many-to-many relationship
CREATE INDEX IF NOT EXISTS idx_banner_widget_banner_id ON elly.banner_widget (banner_id);

CREATE INDEX IF NOT EXISTS idx_banner_widget_widget_id ON elly.banner_widget (widget_id);

-- Post-Widget many-to-many relationship
CREATE INDEX IF NOT EXISTS idx_post_widget_post_id ON elly.post_widget (post_id);

CREATE INDEX IF NOT EXISTS idx_post_widget_widget_id ON elly.post_widget (widget_id);

-- ========================================
-- SLUG INDEXES (Sık kullanılan arama)
-- ========================================
CREATE UNIQUE INDEX IF NOT EXISTS idx_page_slug ON elly.page (slug);

CREATE UNIQUE INDEX IF NOT EXISTS idx_post_slug ON elly.post (slug);

-- ========================================
-- PUBLISHED AT INDEX (Tarih filtreleme)
-- ========================================
-- Partial index: Sadece published post'lar için
CREATE INDEX IF NOT EXISTS idx_post_published_at ON elly.post (published_at)
WHERE
    published_at IS NOT NULL;

-- Published post'ları tarih sırasıyla getirmek için composite index
CREATE INDEX IF NOT EXISTS idx_post_published_created ON elly.post (
    published_at DESC,
    created_at DESC
)
WHERE
    published_at IS NOT NULL;

-- ========================================
-- COMMENT TREE STRUCTURE (Parent-Child)
-- ========================================
CREATE INDEX IF NOT EXISTS idx_comment_parent_id ON elly.comment (parent_id);

-- Sadece root comment'leri (parent_id NULL olanlar) hızlı bulmak için
CREATE INDEX IF NOT EXISTS idx_comment_root ON elly.comment (post_id, created_at DESC)
WHERE
    parent_id IS NULL;

-- Bir comment'in tüm children'larını hızlı bulmak için
CREATE INDEX IF NOT EXISTS idx_comment_children ON elly.comment (parent_id, created_at ASC)
WHERE
    parent_id IS NOT NULL;

-- ========================================
-- RATING INDEXES
-- ========================================
-- User'ın aynı post'a duplicate rating vermesini önlemek için
CREATE UNIQUE INDEX IF NOT EXISTS idx_rating_unique_user_post ON elly.rating (user_id, post_id);

-- Rating istatistiklerini hızlı hesaplamak için
CREATE INDEX IF NOT EXISTS idx_rating_post_rating ON elly.rating (post_id, rating);

-- ========================================
-- COMPONENT TYPE FILTERING
-- ========================================
-- Component type'a göre filtreleme (BANNER, WIDGET)
CREATE INDEX IF NOT EXISTS idx_component_type ON elly.component (component_type);

-- Widget type'a göre filtreleme (BANNER, POST)
CREATE INDEX IF NOT EXISTS idx_widget_type ON elly.widget (widget_type);

-- ========================================
-- SEO INFO
-- ========================================
-- Page-SeoInfo relationship
CREATE INDEX IF NOT EXISTS idx_seo_info_page ON elly.seo_info (page_id);

-- ========================================
-- ASSETS (File Upload)
-- ========================================
-- Asset type'a göre filtreleme (image, file, etc.)
CREATE INDEX IF NOT EXISTS idx_assets_type ON elly.assets (asset_type);

-- Upload date'e göre sıralama
CREATE INDEX IF NOT EXISTS idx_assets_created ON elly.assets (created_at DESC);

-- ========================================
-- AUDIT FIELDS (BaseEntity)
-- ========================================
-- Tüm tablolarda created_at ve updated_at var
-- Reporting ve analytics için index'ler

CREATE INDEX IF NOT EXISTS idx_page_created ON elly.page (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_post_created ON elly.post (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_comment_created ON elly.comment (created_at DESC);

-- ========================================
-- TEXT SEARCH INDEX (Full-text search)
-- ========================================
-- PostgreSQL full-text search için GIN index
-- Post title ve content'te arama yapmak için

CREATE INDEX IF NOT EXISTS idx_post_title_content_fts ON elly.post USING gin (
    to_tsvector(
        'english',
        COALESCE(title, '') || ' ' || COALESCE(content, '')
    )
);

-- Page content'te arama
CREATE INDEX IF NOT EXISTS idx_page_content_fts ON elly.page USING gin (
    to_tsvector(
        'english',
        COALESCE(title, '') || ' ' || COALESCE(content, '')
    )
);

-- Comment content'te arama
CREATE INDEX IF NOT EXISTS idx_comment_content_fts ON elly.comment USING gin (
    to_tsvector(
        'english',
        COALESCE(content, '')
    )
);

-- ========================================
-- COMPOSITE INDEXES (Advanced)
-- ========================================
-- Component + Page + Type (hızlı filtreleme)
CREATE INDEX IF NOT EXISTS idx_component_page_type ON elly.component (page_id, component_type);

-- Widget + Component (component'in widget'larını getir)
CREATE INDEX IF NOT EXISTS idx_widget_component_type ON elly.widget (component_id, widget_type);

-- ========================================
-- VERİFİKASYON ve ANALİZ
-- ========================================
-- Index'lerin oluşturulduğunu kontrol edin:
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE
    schemaname = 'elly'
ORDER BY tablename, indexname;

-- Index kullanımını kontrol edin (query plan):
-- EXPLAIN ANALYZE SELECT * FROM elly.post WHERE published_at IS NOT NULL ORDER BY published_at DESC LIMIT 20;

-- Index boyutlarını görün:
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(
        pg_relation_size(
            ('elly.' || indexname)::regclass
        )
    ) as index_size
FROM pg_indexes
WHERE
    schemaname = 'elly'
ORDER BY pg_relation_size(
        ('elly.' || indexname)::regclass
    ) DESC;

-- Index hit rate (cache performance):
SELECT 'index hit rate' AS name, (sum(idx_blks_hit)) / nullif(
        sum(idx_blks_hit + idx_blks_read), 0
    ) AS ratio
FROM pg_statio_user_indexes;

-- Slow query'leri bul (pg_stat_statements extension gerekli):
-- SELECT * FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;

-- ========================================
-- MAINTENANCE
-- ========================================
-- Index'leri yeniden oluştur (fragmentasyon durumunda):
-- REINDEX SCHEMA elly;

-- Vacuum ve analyze (istatistikleri güncelle):
-- VACUUM ANALYZE elly.post;
-- VACUUM ANALYZE elly.comment;
-- VACUUM ANALYZE elly.rating;

-- ========================================
-- NOTLAR
-- ========================================
-- 1. Bu index'ler ~50-100MB yer kaplayabilir
-- 2. Write performance biraz düşer (%5-10) ama read performance 10-100x artar
-- 3. Index'ler otomatik güncelenir, manuel maintenance gerekmez (PostgreSQL)
-- 4. Full-text search için to_tsvector kullanın:
--    SELECT * FROM elly.post
--    WHERE to_tsvector('english', title || ' ' || content) @@ to_tsquery('english', 'spring & boot');
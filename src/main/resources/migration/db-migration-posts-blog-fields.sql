-- Blog alanları — posts tablosuna eklenir (her tenant DB'sinde çalıştırılmalı)
-- Tarih: 2026-05-18

ALTER TABLE posts ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS category VARCHAR(255);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS cover_image VARCHAR(500);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS author VARCHAR(255);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS reading_time VARCHAR(50);

-- İndeksler (opsiyonel, blog listeleme sorgularını hızlandırır)
CREATE INDEX IF NOT EXISTS idx_post_category ON posts (category);
CREATE INDEX IF NOT EXISTS idx_post_published_at ON posts (published_at);

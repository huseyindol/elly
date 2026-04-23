-- Migration: Add token_version column to users table
-- Bu migration, token versioning özelliği için gerekli

-- Schema'yı seç (Neon PostgreSQL için)
SET search_path TO elly;

-- PostgreSQL için
ALTER TABLE users
ADD COLUMN IF NOT EXISTS token_version BIGINT NOT NULL DEFAULT 0;

-- Mevcut kullanıcılar için token_version'ı 0 olarak ayarla (eğer null ise)
UPDATE users SET token_version = 0 WHERE token_version IS NULL;

-- Index ekle (opsiyonel, performans için)
CREATE INDEX IF NOT EXISTS idx_users_token_version ON users (token_version);
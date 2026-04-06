-- =============================================================================
-- Migration: Mail Hesabı Yönetimi (DB Tabanlı SMTP)
-- Versiyon : 2026-04-03
-- Hedef    : Her tenant DB'sine uygulanır (basedb, tenant1, tenant2, ...)
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. mail_accounts — SMTP hesaplarını tutan ana tablo
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mail_accounts (
    id            BIGSERIAL     PRIMARY KEY,
    name          VARCHAR(255)  NOT NULL,
    from_address  VARCHAR(255)  NOT NULL,
    smtp_host     VARCHAR(255)  NOT NULL,
    smtp_port     INTEGER       NOT NULL,
    smtp_username VARCHAR(255)  NOT NULL,
    smtp_password VARCHAR(512)  NOT NULL,   -- AES-256-CBC ile şifreli
    is_default    BOOLEAN       NOT NULL DEFAULT FALSE,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Sık kullanılan sorgular için indeksler
CREATE INDEX IF NOT EXISTS idx_mail_account_default ON mail_accounts (is_default);
CREATE INDEX IF NOT EXISTS idx_mail_account_active  ON mail_accounts (active);

-- Aynı anda yalnızca bir varsayılan hesaba izin ver
-- (kısmi unique index — sadece is_default = TRUE satırları kısıtlanır)
CREATE UNIQUE INDEX IF NOT EXISTS uq_mail_account_one_default
    ON mail_accounts (is_default)
    WHERE is_default = TRUE;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. form_definitions — hangi hesabın kullanılacağını seçer
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE form_definitions
    ADD COLUMN IF NOT EXISTS mail_account_id BIGINT
        REFERENCES mail_accounts (id) ON DELETE SET NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. email_logs — hangi hesaptan gönderildiğini kaydeder
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE email_logs
    ADD COLUMN IF NOT EXISTS mail_account_id BIGINT
        REFERENCES mail_accounts (id) ON DELETE SET NULL;

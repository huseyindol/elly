-- Migration: Add tenant_id and is_primary columns to mail_accounts table
-- Tüm DB'lerde çalıştır: basedb, tenant1, tenant2
-- (tenant_id ve is_primary alanları entity'de tanımlı — şema eşleşmeli)
-- tenant1/tenant2'de kayıtlar boş kalır; mail hesapları sadece basedb'de yönetilir.

ALTER TABLE mail_accounts
  ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50),
  ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_mail_accounts_tenant_id ON mail_accounts (tenant_id);

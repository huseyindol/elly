-- Migration: Add tenant_id and is_primary columns to mail_accounts table
-- Sadece basedb'de çalıştır (mail_accounts basedb'de yönetilir)

ALTER TABLE mail_accounts
  ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50),
  ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_mail_accounts_tenant_id ON mail_accounts (tenant_id);

-- Mevcut hesapların tenant_id'si null kalır (eski kayıtlar atanmamış)
-- is_primary = false varsayılanı: panelden manuel olarak ana hesap seçilmeli

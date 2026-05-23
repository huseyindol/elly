-- Migration: Add tenant_id column to mail_accounts table
-- Sadece basedb'de çalıştır (mail_accounts basedb'de yönetilir)

ALTER TABLE mail_accounts
  ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_mail_accounts_tenant_id ON mail_accounts (tenant_id);

-- Mevcut hesapların tenant_id'si null kalır (eski kayıtlar atanmamış)

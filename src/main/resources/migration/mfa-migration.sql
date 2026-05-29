-- 2FA (TOTP) Migration
-- Tüm tenant DB'lerine (basedb, tenant1, tenant2) uygulanmalı

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(128),         -- AES-256 şifreli Base32 secret
  ADD COLUMN IF NOT EXISTS mfa_setup_verified BOOLEAN NOT NULL DEFAULT FALSE;

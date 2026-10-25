-- =============================================================================
-- Migration: Mail+Form v5 (Grup B) — CC/BCC, Daily SMTP Quota, Form Template Key
-- Versiyon : 2026-10-25
-- Hedef    : HER tenant DB (tenant1, tenant2, ...)
--
-- Degisiklikler:
--   1. form_definitions.cc_emails (VARCHAR(1000)) — B1 CC alicilari
--   2. form_definitions.bcc_emails (VARCHAR(1000)) — B1 BCC alicilari
--   3. form_definitions.notification_template_key (VARCHAR(100)) — B6 Ozel sablon
--   4. email_logs.cc (VARCHAR(1000)) — B1 CC log kaydi
--   5. email_logs.bcc (VARCHAR(1000)) — B1 BCC log kaydi
--   6. mail_accounts.daily_limit (INTEGER DEFAULT 500) — B2 SMTP gunluk kota
-- =============================================================================

BEGIN;

ALTER TABLE IF EXISTS form_definitions
  ADD COLUMN IF NOT EXISTS cc_emails VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS bcc_emails VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS notification_template_key VARCHAR(100);

ALTER TABLE IF EXISTS email_logs
  ADD COLUMN IF NOT EXISTS cc VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS bcc VARCHAR(1000);

ALTER TABLE IF EXISTS mail_accounts
  ADD COLUMN IF NOT EXISTS daily_limit INTEGER DEFAULT 500;

UPDATE mail_accounts SET daily_limit = 500 WHERE daily_limit IS NULL;

COMMIT;

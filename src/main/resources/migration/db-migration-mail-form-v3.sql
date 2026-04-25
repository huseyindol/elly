-- =============================================================================
-- Migration: Mail+Form v3 — Multiple Recipients
-- Versiyon : 2026-04-25
-- Hedef    : Her tenant DB'sine uygulanir (basedb, tenant1, tenant2, ...)
--
-- Degisiklikler:
--   form_definitions.recipient_email uzunlugu 1000'e cikarildi.
--   email_logs.recipient uzunlugu 1000'e cikarildi.
-- =============================================================================

BEGIN;

ALTER TABLE form_definitions ALTER COLUMN recipient_email TYPE VARCHAR(1000);
ALTER TABLE email_logs ALTER COLUMN recipient TYPE VARCHAR(1000);

COMMIT;

-- =============================================================================
-- Migration: Mail+Form v4 — Optional Notification Config
-- Versiyon : 2026-04-27
-- Hedef    : Her tenant DB'sine uygulanir (basedb, tenant1, tenant2, ...)
--
-- Degisiklikler:
--   form_definitions.sender_mail_account_id NOT NULL constraint'i kaldirildi.
--   form_definitions.recipient_email NOT NULL constraint'i kaldirildi.
--
-- Sebep: notificationEnabled=false oldugunda form bildirimi yapilmayacagi icin
-- sender ve recipient bilgisi opsiyonel olmali. Submit akisinda validation
-- service katmaninda yapiliyor (FormDefinitionService.validateNotificationConfig).
--
-- Geriye uyumluluk: Mevcut kayitlardaki dolu alanlar olduğu gibi kalir;
-- sadece NULL değerler artık reddedilmiyor.
-- =============================================================================

BEGIN;

ALTER TABLE form_definitions ALTER COLUMN sender_mail_account_id DROP NOT NULL;
ALTER TABLE form_definitions ALTER COLUMN recipient_email DROP NOT NULL;

COMMIT;

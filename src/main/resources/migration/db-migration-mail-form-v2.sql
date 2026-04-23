-- =============================================================================
-- Migration: Mail+Form v2 — DB-Based SMTP + Form Submission Notifications
-- Versiyon : 2026-04-21
-- Hedef    : Her tenant DB'sine uygulanir (basedb, tenant1, tenant2, ...)
--
-- Onceki durum (db-migration-mail-accounts.sql sonrasi):
--   mail_accounts: id, name, from_address, smtp_host, smtp_port, smtp_username,
--                  smtp_password (AES-256 sifreli), is_default, active, timestamps
--   form_definitions.mail_account_id (nullable FK, varsayilan hesap fallback'i)
--   email_logs.mail_account_id (nullable FK)
--
-- Yeni durum (v2):
--   mail_accounts: is_default KALDIRILDI (coklu hesap + form-level secim)
--   form_definitions:
--     - mail_account_id KALDIRILDI
--     - sender_mail_account_id BIGINT NOT NULL FK (form-level zorunlu secim)
--     - recipient_email VARCHAR(255) NOT NULL
--     - notification_subject VARCHAR(255) NULL (bos ise varsayilan konu)
--     - notification_enabled BOOLEAN NOT NULL DEFAULT TRUE
--   email_logs.mail_account_id: degisiklik yok (nullable FK aynen kalir)
--
-- UYARI: form_definitions icindeki mevcut kayitlar TRUNCATE edilir cunku yeni
--        NOT NULL sender/recipient alanlari icin geriye donuk default yok.
--        Form_submissions CASCADE ile duser. Uretim ortaminda calistirmadan
--        once yedek alin.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. mail_accounts: is_default konsepti kaldirildi
-- -----------------------------------------------------------------------------
-- v2'de "varsayilan hesap" yok. Her form hangi hesabi kullanacagini explicit
-- olarak secer (form_definitions.sender_mail_account_id NOT NULL).

DROP INDEX IF EXISTS uq_mail_account_one_default;
DROP INDEX IF EXISTS idx_mail_account_default;

ALTER TABLE mail_accounts
    DROP COLUMN IF EXISTS is_default;

-- idx_mail_account_active zaten db-migration-mail-accounts.sql ile olusturulmustu
-- ve v2'de hala kullanilir (MailAccountRepository.findAllByActiveTrue icin).

-- -----------------------------------------------------------------------------
-- 2. form_definitions: explicit sender + recipient + notification alanlari
-- -----------------------------------------------------------------------------
-- Eski mail_account_id fallback'li FK'yi kaldir, yerine NOT NULL sender koy.

ALTER TABLE form_definitions
    DROP COLUMN IF EXISTS mail_account_id;

ALTER TABLE form_definitions
    ADD COLUMN IF NOT EXISTS sender_mail_account_id BIGINT,
    ADD COLUMN IF NOT EXISTS recipient_email        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS notification_subject   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS notification_enabled   BOOLEAN NOT NULL DEFAULT TRUE;

-- Mevcut form_definitions kayitlari yeni mimari ile uyumsuz (NOT NULL sender yok).
-- Kullanici karari: v2 temiz baslar. form_submissions CASCADE ile dusurulur.
TRUNCATE TABLE form_definitions CASCADE;

ALTER TABLE form_definitions
    ALTER COLUMN sender_mail_account_id SET NOT NULL,
    ALTER COLUMN recipient_email        SET NOT NULL;

ALTER TABLE form_definitions
    ADD CONSTRAINT fk_form_def_sender_mail_account
    FOREIGN KEY (sender_mail_account_id)
    REFERENCES mail_accounts (id)
    ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_form_def_sender_mail
    ON form_definitions (sender_mail_account_id);

-- -----------------------------------------------------------------------------
-- 3. email_logs: mevcut yapiyi koru
-- -----------------------------------------------------------------------------
-- email_logs.mail_account_id nullable FK olarak kalir. Uygulama katmani
-- (EmailRequest.mailAccountId @NotNull) yeni kayitlarda NOT NULL dogrulamasini
-- yapar. Eski log kayitlari (varsa) null kalabilir.

COMMIT;

-- =============================================================================
-- CALISTIRMA
-- =============================================================================
-- Her tenant DB icin (basedb, tenant1, tenant2) tek tek uygulanir:
--   psql -h <host> -U <user> -d elly_basedb  -f db-migration-mail-form-v2.sql
--   psql -h <host> -U <user> -d elly_tenant1 -f db-migration-mail-form-v2.sql
--   psql -h <host> -U <user> -d elly_tenant2 -f db-migration-mail-form-v2.sql
--
-- Uygulama sonrasi:
--   1. Panel uzerinden mail hesabi olustur:
--      POST /api/v1/mail-accounts
--      { "name":"...", "fromAddress":"...", "smtpHost":"smtp.gmail.com",
--        "smtpPort":587, "smtpUsername":"...", "smtpPassword":"<app-password>" }
--   2. SMTP baglantisini dogrula: POST /api/v1/mail-accounts/{id}/verify
--   3. Form olustururken hesabi sec:
--      POST /api/v1/form-definitions
--      { ..., "senderMailAccountId": <id>, "recipientEmail": "form-to@..." }
--
-- ROLLBACK (pre-v2 schema'ya geri don):
--   ALTER TABLE mail_accounts
--       ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;
--   CREATE INDEX idx_mail_account_default ON mail_accounts (is_default);
--   CREATE UNIQUE INDEX uq_mail_account_one_default
--       ON mail_accounts (is_default) WHERE is_default = TRUE;
--
--   ALTER TABLE form_definitions DROP CONSTRAINT fk_form_def_sender_mail_account;
--   DROP INDEX IF EXISTS idx_form_def_sender_mail;
--   ALTER TABLE form_definitions
--       DROP COLUMN sender_mail_account_id,
--       DROP COLUMN recipient_email,
--       DROP COLUMN notification_subject,
--       DROP COLUMN notification_enabled,
--       ADD COLUMN mail_account_id BIGINT REFERENCES mail_accounts(id) ON DELETE SET NULL;
-- =============================================================================

-- =============================================================================
-- Migration: Chat Bans (TC ban/unban)
-- Versiyon : 2026-06-05
-- Hedef    : basedb + her tenant DB (basedb, tenant1, tenant2, ...)
--
-- Amaç: Tenant chat (TC) gruplarında bir guest (session_id) veya kayıtlı visitor
--       (visitor_id) yazma yetkisinin kaldırılması. Banlı kişi mesajları OKUYABİLİR
--       ama YAZAMAZ (gönderim service katmanında CHAT_BANNED ile reddedilir).
--
-- NOT: prod JPA_DDL_AUTO=validate → entity ChatBan tüm tenant context'lerinde aynı
--      SQL'i ürettiğinden tablo HER DB'de olmalı. Deploy'dan ÖNCE çalıştır.
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS chat_bans (
  id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  group_id            UUID         NOT NULL,
  session_id          UUID,
  visitor_id          BIGINT,
  banned_by_user_id   BIGINT       NOT NULL,
  banned_by_username  VARCHAR(100),
  reason              VARCHAR(300),
  created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
  -- Tam olarak bir hedef: guest (session_id) VEYA visitor (visitor_id)
  CONSTRAINT chk_chat_bans_target CHECK (
    (session_id IS NOT NULL AND visitor_id IS NULL) OR
    (visitor_id IS NOT NULL AND session_id IS NULL)
  )
);

-- Aynı grupta aynı hedef bir kez banlanabilir (idempotent)
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_bans_group_session
  ON chat_bans(group_id, session_id) WHERE session_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_bans_group_visitor
  ON chat_bans(group_id, visitor_id) WHERE visitor_id IS NOT NULL;

COMMIT;

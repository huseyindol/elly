-- =============================================================================
-- Migration: Chat Tenant-Aware (Faz 1 — DB hazırlığı)
-- Versiyon : 2026-04-28
-- Hedef    : basedb + her tenant DB (basedb, tenant1, tenant2, ...)
--
-- Amaç:
--   1) Mevcut admin chat (AC) tablolarına iki yeni kolon eklemek:
--        chat_groups.tenant_id     → NULL=basedb AC, "tenantX"=TC
--        chat_groups.visitor_access → BOOLEAN, ziyaretçi erişimi
--      Mevcut AC kayıtları default değerlerle (NULL, FALSE) korunur.
--   2) chat_messages tablosuna polymorphic sender kolonları:
--        sender_type ENUM('ADMIN','VISITOR') DEFAULT 'ADMIN'
--        visitor_id  → visitor_identities.id (sender_type=VISITOR ise)
--      sender_id kolonu NOT NULL kaldırılır (visitor mesajında null).
--   3) Tenant DB'lerinde tüm chat tablolarını yaratmak (chat_groups,
--      chat_group_members, chat_messages, chat_message_reads,
--      chat_message_edits) — TC kayıtları buraya düşecek.
--   4) visitor_identities tablosu: kayıtlı tenant user'ları veya anonim
--      cookie session'larını temsil eder. Her tenant DB'sinde.
--
-- Mevcut AC akışı bozulmaz: basedb'deki chat_* tabloları zaten var,
-- IF NOT EXISTS olduğu için yeniden yaratılmaz; sadece yeni kolonlar
-- ADD COLUMN IF NOT EXISTS ile eklenir.
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. chat_groups — basedb'de zaten var (alter); tenant DB'lerinde yeni (create)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_groups (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name             VARCHAR(100),
  description      TEXT,
  type             VARCHAR(10) NOT NULL,
  created_by       BIGINT NOT NULL,
  visibility_level INT NOT NULL DEFAULT 1,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE chat_groups ADD COLUMN IF NOT EXISTS tenant_id      VARCHAR(64);
ALTER TABLE chat_groups ADD COLUMN IF NOT EXISTS visitor_access BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_chat_groups_created_by ON chat_groups(created_by);
CREATE INDEX IF NOT EXISTS idx_chat_groups_tenant     ON chat_groups(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_chat_groups_visitor    ON chat_groups(visitor_access) WHERE visitor_access = TRUE;

-- ---------------------------------------------------------------------------
-- 2. chat_group_members
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_group_members (
  group_id  UUID NOT NULL,
  user_id   BIGINT NOT NULL,
  role      VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (group_id, user_id)
);

-- ---------------------------------------------------------------------------
-- 3. chat_messages — polymorphic sender alanları eklenir
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_messages (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  group_id     UUID NOT NULL,
  sender_id    BIGINT,
  content      TEXT NOT NULL,
  content_type VARCHAR(10) NOT NULL DEFAULT 'TEXT',
  file_url     VARCHAR(500),
  parent_id    UUID,
  deleted_at   TIMESTAMP,
  edited_at    TIMESTAMP,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS sender_type VARCHAR(10) NOT NULL DEFAULT 'ADMIN';
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS visitor_id  BIGINT;

-- Mevcut sender_id NOT NULL idi; visitor mesajlarında null olabilmeli
ALTER TABLE chat_messages ALTER COLUMN sender_id DROP NOT NULL;

-- Polymorphic guard: en az birinin dolu olması ve tutarlı olması
ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chk_chat_messages_sender;
ALTER TABLE chat_messages ADD  CONSTRAINT chk_chat_messages_sender CHECK (
  (sender_type = 'ADMIN'   AND sender_id  IS NOT NULL AND visitor_id IS NULL) OR
  (sender_type = 'VISITOR' AND visitor_id IS NOT NULL AND sender_id  IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_group_created   ON chat_messages(group_id, created_at);
CREATE INDEX IF NOT EXISTS idx_chat_messages_group_id_cursor ON chat_messages(group_id, id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_parent_id       ON chat_messages(parent_id);

-- ---------------------------------------------------------------------------
-- 4. chat_message_reads
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_message_reads (
  message_id UUID NOT NULL,
  user_id    BIGINT NOT NULL,
  read_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id, user_id)
);

-- ---------------------------------------------------------------------------
-- 5. chat_message_edits
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_message_edits (
  id               BIGSERIAL PRIMARY KEY,
  message_id       UUID NOT NULL,
  previous_content TEXT NOT NULL,
  edited_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_message_edits_message ON chat_message_edits(message_id);

-- ---------------------------------------------------------------------------
-- 6. visitor_identities — tenant DB'lerinde anlamlı; basedb'de simetri için
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS visitor_identities (
  id              BIGSERIAL PRIMARY KEY,
  tenant_user_id  BIGINT,                          -- kayıtlı tenant user'a bağlı ise
  session_token   UUID UNIQUE,                     -- anonim ziyaretçi cookie'si ise
  display_name    VARCHAR(80) NOT NULL,
  email           VARCHAR(255),
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_seen_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT visitor_identity_has_anchor CHECK (
    tenant_user_id IS NOT NULL OR session_token IS NOT NULL
  )
);

CREATE INDEX IF NOT EXISTS idx_visitor_identities_tenant_user
  ON visitor_identities(tenant_user_id) WHERE tenant_user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_visitor_identities_session
  ON visitor_identities(session_token) WHERE session_token IS NOT NULL;

COMMIT;

-- ============================================================
-- Chat System Migration — basedb only
-- Tables: chat_groups, chat_group_members, chat_messages,
--         chat_message_reads, chat_message_edits
-- ============================================================

CREATE TABLE IF NOT EXISTS chat_groups (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name             VARCHAR(100),
  description      TEXT,
  type             VARCHAR(10) NOT NULL CHECK (type IN ('GROUP', 'DM')),
  created_by       BIGINT NOT NULL REFERENCES users(id),
  visibility_level INT NOT NULL DEFAULT 1,
  created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Add visibility_level to existing installations that ran an earlier migration
ALTER TABLE chat_groups ADD COLUMN IF NOT EXISTS visibility_level INT NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS chat_group_members (
  group_id  UUID    NOT NULL REFERENCES chat_groups(id) ON DELETE CASCADE,
  user_id   BIGINT  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role      VARCHAR(10) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('OWNER', 'MEMBER')),
  joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
  PRIMARY KEY (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
  id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
  group_id     UUID    NOT NULL REFERENCES chat_groups(id) ON DELETE CASCADE,
  sender_id    BIGINT  NOT NULL REFERENCES users(id),
  content      TEXT    NOT NULL,
  content_type VARCHAR(10) NOT NULL DEFAULT 'TEXT' CHECK (content_type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM')),
  file_url     VARCHAR(500),
  parent_id    UUID    REFERENCES chat_messages(id),
  deleted_at   TIMESTAMP,
  edited_at    TIMESTAMP,
  created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_message_reads (
  message_id UUID    NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
  user_id    BIGINT  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  read_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  PRIMARY KEY (message_id, user_id)
);

CREATE TABLE IF NOT EXISTS chat_message_edits (
  id               BIGSERIAL PRIMARY KEY,
  message_id       UUID   NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
  previous_content TEXT   NOT NULL,
  edited_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_chat_messages_group_created
  ON chat_messages(group_id, created_at DESC) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_messages_group_id
  ON chat_messages(group_id, id);

CREATE INDEX IF NOT EXISTS idx_chat_group_members_user
  ON chat_group_members(user_id);

CREATE INDEX IF NOT EXISTS idx_chat_messages_parent
  ON chat_messages(parent_id) WHERE parent_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_chat_message_edits_message
  ON chat_message_edits(message_id);

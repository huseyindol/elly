-- ============================================================
-- Notification System Migration — basedb only
-- Table: notifications
-- ============================================================

CREATE TABLE IF NOT EXISTS notifications (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type        VARCHAR(30)  NOT NULL,
  title       VARCHAR(200) NOT NULL,
  message     VARCHAR(500) NOT NULL,
  link        VARCHAR(500),
  read        BOOLEAN      NOT NULL DEFAULT FALSE,
  tenant_id   VARCHAR(50),
  metadata    JSONB,
  created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read_created
  ON notifications (user_id, read, created_at DESC);

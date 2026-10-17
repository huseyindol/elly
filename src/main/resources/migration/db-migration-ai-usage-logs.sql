-- =============================================================================
-- Migration: AI Usage Logs (Gemini AI istek, prompt, yanıt ve token kullanım kaydı)
-- Versiyon : 2026-10-17
-- Hedef    : HER tenant DB (tenant1, tenant2, ...)
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS ai_usage_logs (
  id              BIGSERIAL PRIMARY KEY,
  created_at      TIMESTAMP(6),
  updated_at      TIMESTAMP(6),
  request_id      VARCHAR(36)  NOT NULL,
  step_order      INTEGER      NOT NULL DEFAULT 1,
  endpoint        VARCHAR(100) NOT NULL,
  action          VARCHAR(60)  NOT NULL,
  model           VARCHAR(60)  NOT NULL,
  username        VARCHAR(100),
  tenant_id       VARCHAR(60),
  user_input      TEXT,
  prompt_text     TEXT,
  response_text   TEXT,
  parsed_output   TEXT,
  prompt_chars    INTEGER      NOT NULL DEFAULT 0,
  response_chars  INTEGER      NOT NULL DEFAULT 0,
  input_tokens    INTEGER      NOT NULL DEFAULT 0,
  output_tokens   INTEGER      NOT NULL DEFAULT 0,
  thought_tokens  INTEGER      NOT NULL DEFAULT 0,
  cached_tokens   INTEGER      NOT NULL DEFAULT 0,
  total_tokens    INTEGER      NOT NULL DEFAULT 0,
  duration_ms     BIGINT       NOT NULL DEFAULT 0,
  status          VARCHAR(20)  NOT NULL,
  http_status     INTEGER,
  error_code      VARCHAR(60),
  error_message   TEXT
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_created_at ON ai_usage_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_action     ON ai_usage_logs (action);
CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_status     ON ai_usage_logs (status);
CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_request_id ON ai_usage_logs (request_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_username   ON ai_usage_logs (username);

COMMIT;

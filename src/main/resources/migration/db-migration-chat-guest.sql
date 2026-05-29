-- =============================================================================
-- Migration: Chat Guest (anonim ziyaretçi) — VISITOR + GUEST birleşimi
-- Versiyon : 2026-05-28
-- Hedef    : basedb + her tenant DB (basedb, tenant1, tenant2, ...)
--
-- Amaç:
--   chat_messages tablosuna anonim guest sender alanları eklemek:
--     session_id           UUID          → guest token oturum kimliği
--     sender_display_name  VARCHAR(100)  → guest'in ekran adı (denormalize)
--   ve sender_type CHECK constraint'ini GUEST'i kapsayacak şekilde güncellemek.
--
-- Birleşik sender modeli (sender_type):
--   ADMIN   → sender_id dolu                  (basedb users)
--   VISITOR → visitor_id dolu                 (kayıtlı tenant user → visitor_identities)
--   GUEST   → session_id + sender_display_name dolu  (anonim ziyaretçi)
--
-- Geriye uyumluluk: Mevcut ADMIN/VISITOR mesajları etkilenmez; yeni kolonlar
-- nullable. CHECK constraint yeniden yazılır (eski hali ADMIN+VISITOR'dı).
-- =============================================================================

BEGIN;

ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS session_id          UUID;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS sender_display_name VARCHAR(100);

-- Polymorphic guard: tam olarak bir sender kaynağı dolu olmalı
ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chk_chat_messages_sender;
ALTER TABLE chat_messages ADD  CONSTRAINT chk_chat_messages_sender CHECK (
  (sender_type = 'ADMIN'   AND sender_id  IS NOT NULL AND visitor_id IS NULL     AND session_id IS NULL) OR
  (sender_type = 'VISITOR' AND visitor_id IS NOT NULL AND sender_id  IS NULL     AND session_id IS NULL) OR
  (sender_type = 'GUEST'   AND session_id IS NOT NULL AND sender_id  IS NULL     AND visitor_id IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_id) WHERE session_id IS NOT NULL;

COMMIT;

-- Guest Chat Migration
-- chat_messages.sender_id nullable yapılıyor (guest kullanıcılar için)
-- session_id ve sender_display_name kolonları ekleniyor

ALTER TABLE chat_messages ALTER COLUMN sender_id DROP NOT NULL;

ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS session_id UUID;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS sender_display_name VARCHAR(100);

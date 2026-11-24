-- Çoklu cihaz oturumları: bir kullanıcı aynı anda birden fazla cihazdan (web, mobil, mac-agent)
-- giriş yapıp bağımsız refresh token taşıyabilsin diye refresh_tokens.user_id üzerindeki
-- tekil (UNIQUE) kısıt kaldırılır; token sütunu UNIQUE kalır, user_id indeks olarak korunur.
-- İdempotent: her tenant DB'sine (elly_tenant1, elly_tenant2) uygulanır.

DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT c.conname
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    JOIN pg_namespace n ON t.relnamespace = n.oid
    WHERE n.nspname = 'public'
      AND t.relname = 'refresh_tokens'
      AND c.contype = 'u'
      AND array_length(c.conkey, 1) = 1
      AND (SELECT a.attname FROM pg_attribute a WHERE a.attrelid = t.oid AND a.attnum = c.conkey[1]) = 'user_id'
  LOOP
    EXECUTE format('ALTER TABLE public.refresh_tokens DROP CONSTRAINT IF EXISTS %I', r.conname);
  END LOOP;
END $$;

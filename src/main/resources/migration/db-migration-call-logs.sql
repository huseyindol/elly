-- =============================================================================
-- Migration: Call Logs (WebRTC görüntülü görüşme metadata kaydı)
-- Versiyon : 2026-07-08
-- Hedef    : HER tenant DB (tenant1, tenant2, ...) — basedb YOK
--
-- Video KAYDI değil; yalnız kim/ne zaman/durum. id = uygulama tarafından üretilen
-- callId (CallSessionService), DB @GeneratedValue DEĞİL.
--
-- LOKAL: ddl-auto=update + JpaConfig.initializeTenantSchemas tabloyu tüm tenant
--        DB'lerinde otomatik kurar → bu SQL'i lokalde çalıştırmaya gerek yok.
-- PROD (JPA_DDL_AUTO=validate): entity CallLog tüm tenant context'lerinde aynı SQL'i
--        doğrular → tablo HER tenant DB'de olmalı. Deploy'dan ÖNCE çalıştır
--        (aksi halde SchemaManagementException → CrashLoopBackOff).
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS call_logs (
  id                  UUID PRIMARY KEY,
  caller_user_id      BIGINT,
  caller_display_name VARCHAR(100),
  callee_user_id      BIGINT,
  status              VARCHAR(20) NOT NULL,
  started_at          TIMESTAMP   NOT NULL,
  answered_at         TIMESTAMP,
  ended_at            TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_call_logs_status     ON call_logs (status);
CREATE INDEX IF NOT EXISTS idx_call_logs_started_at ON call_logs (started_at);

COMMIT;

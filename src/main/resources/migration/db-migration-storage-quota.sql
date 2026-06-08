-- =============================================================================
-- Migration: Storage Quota (tenant başına dosya depolama kotası)
-- Versiyon : 2026-06-09
-- Hedef    : basedb + her tenant DB (basedb, tenant1, tenant2, ...)
--
-- Her DB kendi tenant'ının kotasını tutar (enforcement upload anındaki tenant
-- context'inde çalışır → cross-DB yok). Pratikte her DB'de tek satır.
--
-- NOT: prod JPA_DDL_AUTO=validate → entity StorageQuota tüm context'lerde aynı SQL'i
--      üretir → tablo HER DB'de olmalı. Deploy'dan ÖNCE çalıştır.
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS storage_quota (
  tenant_id    VARCHAR(64) PRIMARY KEY,
  limit_bytes  BIGINT,                       -- null/0 → config varsayılanı (3GB)
  used_bytes   BIGINT      NOT NULL DEFAULT 0,
  updated_at   TIMESTAMP   DEFAULT NOW()
);

COMMIT;

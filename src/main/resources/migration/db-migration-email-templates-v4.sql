-- ============================================================
-- v4 Email Templates Migration
-- Her tenant DB'sinde çalıştır (basedb, tenant1, tenant2, ...)
-- ============================================================

CREATE TABLE IF NOT EXISTS email_templates (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             VARCHAR(64),
    template_key          VARCHAR(100) NOT NULL,
    subject               VARCHAR(255) NOT NULL,
    html_body             TEXT         NOT NULL,
    description           VARCHAR(500),
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    version               INTEGER      NOT NULL DEFAULT 1,
    optimistic_lock_version BIGINT     NOT NULL DEFAULT 0,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_email_templates_tenant_key UNIQUE (tenant_id, template_key)
);

CREATE INDEX IF NOT EXISTS idx_email_templates_key    ON email_templates(template_key);
CREATE INDEX IF NOT EXISTS idx_email_templates_active ON email_templates(active);

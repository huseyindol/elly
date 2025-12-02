-- ============================================
-- Elly CMS Database Initialization
-- ============================================
-- This script runs automatically when PostgreSQL container starts for the first time
-- ============================================

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS elly;

-- Set default schema
SET search_path TO elly;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA elly TO postgres;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA elly TO postgres;

GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA elly TO postgres;

-- Log message
DO $$
BEGIN
    RAISE NOTICE 'Elly CMS schema created successfully!';
END $$;
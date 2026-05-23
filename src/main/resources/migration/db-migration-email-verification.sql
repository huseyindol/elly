-- Migration: Add email verification columns to users table
-- Run on all tenant DBs: basedb, tenant1, tenant2

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255),
  ADD COLUMN IF NOT EXISTS verification_token_expires_at TIMESTAMP;

-- Existing users (admins) are already verified
UPDATE users SET email_verified = TRUE WHERE email_verified = FALSE;

CREATE INDEX IF NOT EXISTS idx_users_email_verification_token ON users (email_verification_token);

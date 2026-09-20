-- 12. ADD SECURE_ACCOUNT TO ACCOUNTS TABLE (auth_ctx)
ALTER TABLE auth_ctx.accounts ADD COLUMN IF NOT EXISTS secure_account BOOLEAN DEFAULT FALSE;

-- Ensure default false for existing records
UPDATE auth_ctx.accounts SET secure_account = FALSE WHERE secure_account IS NULL;

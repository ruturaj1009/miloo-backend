-- 8. ADD COUNTRY CODE TO ACCOUNTS
ALTER TABLE auth_ctx.accounts ADD COLUMN IF NOT EXISTS country_code VARCHAR(10);
UPDATE auth_ctx.accounts SET country_code = '+1' WHERE country_code IS NULL;

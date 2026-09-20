-- 11. ADD IS_ACTIVE TO ACCOUNTS AND CREATE ACCOUNT ACTIVATIONS TABLE (auth_ctx)
ALTER TABLE auth_ctx.accounts ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT FALSE;

-- Ensure existing verified demo accounts are marked as active
UPDATE auth_ctx.accounts SET is_active = TRUE WHERE is_verified = TRUE OR is_active IS NULL;

CREATE TABLE IF NOT EXISTS auth_ctx.account_activations (
    activation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES auth_ctx.accounts(account_id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_account_activations_token ON auth_ctx.account_activations(token);
CREATE INDEX IF NOT EXISTS idx_account_activations_account ON auth_ctx.account_activations(account_id);

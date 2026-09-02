-- 1. AUTH CONTEXT (auth_ctx)
CREATE TABLE IF NOT EXISTS auth_ctx.accounts (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) UNIQUE,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_accounts_phone ON auth_ctx.accounts(phone_number);
CREATE INDEX IF NOT EXISTS idx_accounts_email ON auth_ctx.accounts(email);

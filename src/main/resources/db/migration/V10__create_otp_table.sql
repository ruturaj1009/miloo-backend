-- 10. CREATE OTP TABLE (auth_ctx)
CREATE TABLE IF NOT EXISTS auth_ctx.otps (
    otp_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destination VARCHAR(255) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_otps_destination_status ON auth_ctx.otps(destination, status);
CREATE INDEX IF NOT EXISTS idx_otps_expires_at ON auth_ctx.otps(expires_at);

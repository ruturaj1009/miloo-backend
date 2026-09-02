-- 4. CHAT & CALLING CONTEXT (chat_ctx)
CREATE TABLE IF NOT EXISTS chat_ctx.messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    content TEXT NOT NULL,
    media_url VARCHAR(1024),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_ctx.call_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL,
    caller_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    call_type VARCHAR(20) DEFAULT 'VIDEO' CHECK (call_type IN ('AUDIO', 'VIDEO')),
    status VARCHAR(20) DEFAULT 'INITIATED' CHECK (status IN ('INITIATED', 'CALLING', 'RINGING', 'CONNECTED', 'ENDED', 'REJECTED', 'MISSED')),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_messages_match_created ON chat_ctx.messages(match_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_recipient_unread ON chat_ctx.messages(recipient_id, is_read);
CREATE INDEX IF NOT EXISTS idx_calls_match ON chat_ctx.call_sessions(match_id);

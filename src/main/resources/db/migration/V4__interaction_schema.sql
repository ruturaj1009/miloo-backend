-- 3. INTERACTION & MATCHING CONTEXT (interaction_ctx)
CREATE TABLE IF NOT EXISTS interaction_ctx.swipes (
    swipe_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    swiper_id UUID NOT NULL,
    target_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('LIKE', 'PASS', 'SUPERLIKE')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_swiper_target UNIQUE (swiper_id, target_id)
);

CREATE TABLE IF NOT EXISTS interaction_ctx.matches (
    match_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_one_id UUID NOT NULL,
    user_two_id UUID NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_match_pair UNIQUE (user_one_id, user_two_id),
    CONSTRAINT chk_user_ordering CHECK (user_one_id < user_two_id)
);

CREATE INDEX IF NOT EXISTS idx_swipes_swiper ON interaction_ctx.swipes(swiper_id);
CREATE INDEX IF NOT EXISTS idx_swipes_target ON interaction_ctx.swipes(target_id);
CREATE INDEX IF NOT EXISTS idx_matches_user_one ON interaction_ctx.matches(user_one_id);
CREATE INDEX IF NOT EXISTS idx_matches_user_two ON interaction_ctx.matches(user_two_id);

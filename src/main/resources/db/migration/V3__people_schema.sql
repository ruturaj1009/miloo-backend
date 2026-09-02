-- 2. PEOPLE & PROFILE CONTEXT (people_ctx)
CREATE TABLE IF NOT EXISTS people_ctx.user_profiles (
    user_id UUID PRIMARY KEY, -- Decoupled virtual reference to auth_ctx.accounts(account_id)
    first_name VARCHAR(50) NOT NULL,
    birthdate DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    interested_in VARCHAR(20) NOT NULL,
    bio TEXT,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    max_distance_km INT DEFAULT 50,
    age_min_pref INT DEFAULT 18,
    age_max_pref INT DEFAULT 35,
    interest_tags TEXT[] DEFAULT '{}',
    job_title VARCHAR(100),
    company VARCHAR(100),
    school VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS people_ctx.profile_media (
    media_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    r2_object_key VARCHAR(512) NOT NULL,
    media_url VARCHAR(1024) NOT NULL,
    display_order SMALLINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_geo ON people_ctx.user_profiles USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_profile_media_user ON people_ctx.profile_media(user_id, display_order);

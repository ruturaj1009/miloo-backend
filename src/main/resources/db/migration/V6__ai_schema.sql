-- 5. AI RECOMMENDATION CONTEXT (ai_ctx)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vector') THEN
        EXECUTE 'CREATE TABLE IF NOT EXISTS ai_ctx.user_embeddings (
            user_id UUID PRIMARY KEY,
            interest_tags TEXT[],
            embedding vector(1536),
            last_updated TIMESTAMPTZ DEFAULT NOW()
        )';
        BEGIN
            EXECUTE 'CREATE INDEX IF NOT EXISTS idx_user_embeddings_vec ON ai_ctx.user_embeddings 
                     USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)';
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Skipping ivfflat index creation on initial setup';
        END;
    ELSE
        CREATE TABLE IF NOT EXISTS ai_ctx.user_embeddings (
            user_id UUID PRIMARY KEY,
            interest_tags TEXT[],
            embedding_json TEXT,
            last_updated TIMESTAMPTZ DEFAULT NOW()
        );
    END IF;
END $$;

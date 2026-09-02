-- Extensions Setup
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS "vector";
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'pgvector extension not available on this server; skipping vector extension creation';
END $$;

-- Logical Context Schemas
CREATE SCHEMA IF NOT EXISTS auth_ctx;
CREATE SCHEMA IF NOT EXISTS people_ctx;
CREATE SCHEMA IF NOT EXISTS interaction_ctx;
CREATE SCHEMA IF NOT EXISTS chat_ctx;
CREATE SCHEMA IF NOT EXISTS ai_ctx;

CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    api_key_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT merchants_api_key_hash_format CHECK (api_key_hash ~ '^[0-9a-f]{64}$')
);

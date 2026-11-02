-- Flyway Migration V7: OAuth Tokens persistence table for seamless Cloud (Render) restarts
CREATE TABLE IF NOT EXISTS oauth_tokens (
    id VARCHAR(50) PRIMARY KEY,
    access_token TEXT,
    refresh_token TEXT,
    expiration_time_ms BIGINT,
    email_address VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

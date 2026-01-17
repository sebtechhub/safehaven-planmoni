-- V3: OAuth Token Tables
-- Creates oauth_access_tokens and refresh_tokens tables for OAuth 2.0 token management.
-- These tables store encrypted tokens and support token lifecycle management.

-- OAuth Access Tokens Table
CREATE TABLE IF NOT EXISTS oauth_access_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_value VARCHAR(2048) NOT NULL UNIQUE,
    identity_mapping_id BIGINT NOT NULL,
    token_type VARCHAR(50) NOT NULL DEFAULT 'Bearer',
    scope VARCHAR(500),
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    refresh_token_id BIGINT,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_access_token_identity 
        FOREIGN KEY (identity_mapping_id) 
        REFERENCES safehaven_identity_mappings(id) 
        ON DELETE CASCADE,
    CONSTRAINT chk_access_token_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'))
);

-- Indexes for access tokens
CREATE UNIQUE INDEX idx_token_token_value ON oauth_access_tokens(token_value);
CREATE INDEX idx_token_identity_id ON oauth_access_tokens(identity_mapping_id);
CREATE INDEX idx_token_expires_at ON oauth_access_tokens(expires_at);
CREATE INDEX idx_token_status ON oauth_access_tokens(status);
CREATE INDEX idx_token_refresh_id ON oauth_access_tokens(refresh_token_id);

-- Refresh Tokens Table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_value VARCHAR(2048) NOT NULL UNIQUE,
    identity_mapping_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    replaced_by_token_id BIGINT,
    last_used_at TIMESTAMP,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_identity 
        FOREIGN KEY (identity_mapping_id) 
        REFERENCES safehaven_identity_mappings(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_replaced_by 
        FOREIGN KEY (replaced_by_token_id) 
        REFERENCES refresh_tokens(id) 
        ON DELETE SET NULL,
    CONSTRAINT chk_refresh_token_status CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'REVOKED'))
);

-- Indexes for refresh tokens
CREATE UNIQUE INDEX idx_refresh_token_value ON refresh_tokens(token_value);
CREATE INDEX idx_refresh_identity_id ON refresh_tokens(identity_mapping_id);
CREATE INDEX idx_refresh_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_status ON refresh_tokens(status);
CREATE INDEX idx_refresh_replaced_by ON refresh_tokens(replaced_by_token_id);

-- Comments for documentation
COMMENT ON TABLE oauth_access_tokens IS 'OAuth 2.0 access tokens for SafeHaven API access. Tokens should be encrypted at rest.';
COMMENT ON COLUMN oauth_access_tokens.token_value IS 'Encrypted OAuth access token (should be encrypted using AES-256)';
COMMENT ON COLUMN oauth_access_tokens.expires_at IS 'Token expiry timestamp. Application must check expiry before use.';
COMMENT ON COLUMN oauth_access_tokens.status IS 'Token status: ACTIVE, EXPIRED, REVOKED';

COMMENT ON TABLE refresh_tokens IS 'OAuth 2.0 refresh tokens for obtaining new access tokens. Supports token rotation.';
COMMENT ON COLUMN refresh_tokens.token_value IS 'Encrypted OAuth refresh token (should be encrypted using AES-256)';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Refresh token expiry timestamp. Typically longer than access tokens.';
COMMENT ON COLUMN refresh_tokens.status IS 'Token status: ACTIVE, USED, EXPIRED, REVOKED';
COMMENT ON COLUMN refresh_tokens.replaced_by_token_id IS 'If token rotation enabled, tracks new refresh token issued when this was used';

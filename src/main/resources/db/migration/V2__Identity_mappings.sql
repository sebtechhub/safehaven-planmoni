-- V2: Identity Mappings Table
-- Creates safehaven_identity_mappings table for mapping SafeHaven user identities
-- to internal system identities. Ensures provider-scoped isolation.

CREATE TABLE IF NOT EXISTS safehaven_identity_mappings (
    id BIGSERIAL PRIMARY KEY,
    safehaven_user_id VARCHAR(255) NOT NULL UNIQUE,
    internal_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_verified_at TIMESTAMP,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_identity_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- Indexes for identity mappings
CREATE UNIQUE INDEX idx_identity_safehaven_id ON safehaven_identity_mappings(safehaven_user_id);
CREATE INDEX idx_identity_internal_id ON safehaven_identity_mappings(internal_user_id);
CREATE INDEX idx_identity_status ON safehaven_identity_mappings(status);
CREATE INDEX idx_identity_email ON safehaven_identity_mappings(email);

-- Comments for documentation
COMMENT ON TABLE safehaven_identity_mappings IS 'Maps SafeHaven-provided user identities to internal system identities. Ensures provider-scoped isolation.';
COMMENT ON COLUMN safehaven_identity_mappings.safehaven_user_id IS 'SafeHaven-provided unique user identifier from OAuth/OIDC';
COMMENT ON COLUMN safehaven_identity_mappings.internal_user_id IS 'Internal system user identifier';
COMMENT ON COLUMN safehaven_identity_mappings.status IS 'Mapping status: ACTIVE, SUSPENDED, DELETED';
COMMENT ON COLUMN safehaven_identity_mappings.last_verified_at IS 'Timestamp when identity was last verified with SafeHaven';

-- V1: Initial Schema - Safe Havens Table
-- Creates the base safe_havens table for the SafeHaven Service

CREATE TABLE IF NOT EXISTS safe_havens (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(100) NOT NULL UNIQUE,
    owner_name VARCHAR(255) NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);

-- Indexes for safe_havens table
CREATE INDEX idx_safe_havens_reference ON safe_havens(reference);
CREATE INDEX idx_safe_havens_status ON safe_havens(status);
CREATE INDEX idx_safe_havens_created_at ON safe_havens(created_at);

-- Comment on table and columns for documentation
COMMENT ON TABLE safe_havens IS 'Safe Haven entities managed by the service';
COMMENT ON COLUMN safe_havens.reference IS 'Unique business reference for the Safe Haven';
COMMENT ON COLUMN safe_havens.status IS 'Status: ACTIVE, SUSPENDED';

-- V4: Webhook Event Logs Table
-- Creates webhook_event_logs table for SafeHaven webhook processing.
-- Enforces idempotency via unique event_id constraint and supports replay protection.

CREATE TABLE IF NOT EXISTS webhook_event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    identity_mapping_id BIGINT,
    signature VARCHAR(512),
    signature_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payload TEXT NOT NULL,
    headers TEXT,
    error_message TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_started_at TIMESTAMP,
    processed_at TIMESTAMP,
    CONSTRAINT fk_webhook_identity 
        FOREIGN KEY (identity_mapping_id) 
        REFERENCES safehaven_identity_mappings(id) 
        ON DELETE SET NULL,
    CONSTRAINT chk_webhook_signature_status 
        CHECK (signature_status IN ('PENDING', 'VALID', 'INVALID', 'SKIPPED')),
    CONSTRAINT chk_webhook_processing_status 
        CHECK (processing_status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'DUPLICATE')),
    CONSTRAINT chk_attempt_count_non_negative CHECK (attempt_count >= 0)
);

-- Indexes for webhook event logs
CREATE UNIQUE INDEX idx_webhook_event_id ON webhook_event_logs(event_id);
CREATE INDEX idx_webhook_event_type ON webhook_event_logs(event_type);
CREATE INDEX idx_webhook_identity_id ON webhook_event_logs(identity_mapping_id);
CREATE INDEX idx_webhook_status ON webhook_event_logs(processing_status);
CREATE INDEX idx_webhook_signature_status ON webhook_event_logs(signature_status);
CREATE INDEX idx_webhook_created_at ON webhook_event_logs(created_at);
CREATE INDEX idx_webhook_processed_at ON webhook_event_logs(processed_at);
CREATE INDEX idx_webhook_processing_started_at ON webhook_event_logs(processing_started_at);

-- Composite index for efficient retry queries
CREATE INDEX idx_webhook_retry ON webhook_event_logs(processing_status, attempt_count, processed_at);

-- Comments for documentation
COMMENT ON TABLE webhook_event_logs IS 'Webhook event logs for SafeHaven integration. Enforces idempotency and replay protection via unique event_id.';
COMMENT ON COLUMN webhook_event_logs.event_id IS 'SafeHaven-provided unique event identifier. Used for idempotency: same event_id = same event, processed only once.';
COMMENT ON COLUMN webhook_event_logs.event_type IS 'Event type (e.g., identity.created, token.revoked, payment.completed)';
COMMENT ON COLUMN webhook_event_logs.signature_status IS 'Signature validation result: PENDING, VALID, INVALID, SKIPPED';
COMMENT ON COLUMN webhook_event_logs.processing_status IS 'Processing status for idempotency and retry: PENDING, PROCESSING, SUCCESS, FAILED, DUPLICATE';
COMMENT ON COLUMN webhook_event_logs.payload IS 'Full webhook event payload (JSON) stored for audit, replay, and debugging';
COMMENT ON COLUMN webhook_event_logs.attempt_count IS 'Number of processing attempts (for retry logic)';

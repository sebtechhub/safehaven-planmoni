-- V5: Utility Functions and Triggers
-- Creates helper functions and triggers for timestamp management and data integrity

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at timestamp management
CREATE TRIGGER update_safe_havens_updated_at
    BEFORE UPDATE ON safe_havens
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_identity_mappings_updated_at
    BEFORE UPDATE ON safehaven_identity_mappings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_access_tokens_updated_at
    BEFORE UPDATE ON oauth_access_tokens
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_refresh_tokens_updated_at
    BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to clean up expired tokens (can be called by scheduled jobs)
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS TABLE(
    expired_access_tokens BIGINT,
    expired_refresh_tokens BIGINT
) AS $$
DECLARE
    access_count BIGINT;
    refresh_count BIGINT;
BEGIN
    -- Mark expired access tokens
    UPDATE oauth_access_tokens
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE' AND expires_at <= CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS access_count = ROW_COUNT;
    
    -- Mark expired refresh tokens
    UPDATE refresh_tokens
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE' AND expires_at <= CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS refresh_count = ROW_COUNT;
    
    RETURN QUERY SELECT access_count, refresh_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_tokens() IS 'Marks expired tokens as EXPIRED. Can be called by scheduled jobs.';

package org.planmoni.safehavenservice.entity.webhook;

/**
 * Signature validation status for webhook events.
 * 
 * Used in WebhookEventLog entity to track signature validation results.
 * 
 * Status values:
 * - PENDING: Signature validation not yet performed
 * - VALID: Signature verified successfully
 * - INVALID: Signature verification failed (security issue)
 * - SKIPPED: Signature validation was skipped (not recommended in production)
 */
public enum SignatureStatus {
    PENDING,
    VALID,
    INVALID,
    SKIPPED
}

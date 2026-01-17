package org.planmoni.safehavenservice.entity.webhook;

/**
 * Processing status for webhook events.
 * 
 * Used in WebhookEventLog entity and repository methods for idempotency and retry handling.
 * 
 * Status values:
 * - PENDING: Event received, not yet processed
 * - PROCESSING: Currently being processed
 * - SUCCESS: Successfully processed
 * - FAILED: Processing failed (may retry)
 * - DUPLICATE: Event already processed (idempotency check)
 */
public enum ProcessingStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    DUPLICATE
}

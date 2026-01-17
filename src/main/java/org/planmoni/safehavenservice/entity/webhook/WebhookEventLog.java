package org.planmoni.safehavenservice.entity.webhook;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Webhook Event Log entity for SafeHaven webhook processing.
 * 
 * Enforces idempotency and replay protection via unique event ID constraint.
 * Stores full event payload for audit, replay, and debugging purposes.
 * 
 * Production Considerations:
 * - Event IDs are globally unique per SafeHaven account
 * - Signature validation status is tracked
 * - Processing status enables idempotent retries
 * - Large payloads are stored efficiently (TEXT column)
 * - Indexed for fast duplicate detection
 */
@Entity
@Table(
    name = "webhook_event_logs",
    indexes = {
        @Index(name = "idx_webhook_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_webhook_event_type", columnList = "event_type"),
        @Index(name = "idx_webhook_identity_id", columnList = "identity_mapping_id"),
        @Index(name = "idx_webhook_status", columnList = "processing_status"),
        @Index(name = "idx_webhook_created_at", columnList = "created_at"),
        @Index(name = "idx_webhook_processed_at", columnList = "processed_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_webhook_event_id", columnNames = "event_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class WebhookEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SafeHaven-provided unique event identifier.
     * Used for idempotency: same event ID = same event, processed only once.
     * This is the primary key for deduplication.
     */
    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;

    /**
     * Event type (e.g., "identity.created", "token.revoked", "payment.completed").
     * Used for routing events to appropriate handlers.
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * Reference to the SafeHaven identity mapping if event is user-scoped.
     * Nullable for system-level events.
     */
    @Column(name = "identity_mapping_id")
    private Long identityMappingId;

    /**
     * SafeHaven webhook signature for validation.
     * Stored for audit and troubleshooting.
     */
    @Column(name = "signature", length = 512)
    private String signature;

    /**
     * Signature validation result.
     * VALID: Signature verified successfully
     * INVALID: Signature verification failed (security issue)
     * SKIPPED: Signature validation was skipped (not recommended in production)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "signature_status", nullable = false, length = 20)
    @Builder.Default
    private SignatureStatus signatureStatus = SignatureStatus.PENDING;

    /**
     * Processing status for idempotency and retry handling.
     * PENDING: Event received, not yet processed
     * PROCESSING: Currently being processed
     * SUCCESS: Successfully processed
     * FAILED: Processing failed (may retry)
     * DUPLICATE: Event already processed (idempotency check)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    /**
     * Full webhook event payload (JSON).
     * Stored for audit, replay, and debugging.
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * HTTP headers received with webhook (JSON).
     * Useful for debugging and audit.
     */
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    /**
     * Processing error message if status is FAILED.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Number of processing attempts (for retry logic).
     */
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    /**
     * Timestamp when event was received from SafeHaven.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when event processing started.
     */
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    /**
     * Timestamp when event processing completed (success or failure).
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Marks event as processing started.
     */
    public void markProcessingStarted() {
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
        this.attemptCount++;
    }

    /**
     * Marks event as successfully processed.
     */
    public void markSuccess() {
        this.processingStatus = ProcessingStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Marks event as duplicate (idempotency).
     */
    public void markDuplicate() {
        this.processingStatus = ProcessingStatus.DUPLICATE;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Marks event as failed with error message.
     */
    public void markFailed(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Resets processing status for retry.
     */
    public void resetForRetry() {
        this.processingStatus = ProcessingStatus.PENDING;
        this.processingStartedAt = null;
        this.errorMessage = null;
    }

}

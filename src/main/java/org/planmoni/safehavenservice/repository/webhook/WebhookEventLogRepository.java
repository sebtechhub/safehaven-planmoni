package org.planmoni.safehavenservice.repository.webhook;

import org.planmoni.safehavenservice.entity.webhook.ProcessingStatus;
import org.planmoni.safehavenservice.entity.webhook.WebhookEventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, Long> {

    /**
     * Find webhook event by SafeHaven event ID.
     * Primary method for idempotency checks.
     */
    Optional<WebhookEventLog> findByEventId(String eventId);

    /**
     * Check if event ID exists (fast idempotency check).
     */
    boolean existsByEventId(String eventId);

    /**
     * Find events by processing status.
     * Used for retry mechanisms and monitoring.
     */
    Page<WebhookEventLog> findByProcessingStatus(ProcessingStatus status, Pageable pageable);

    /**
     * Find failed events ready for retry.
     * Used by retry job to process failed webhooks.
     */
    @Query("SELECT e FROM WebhookEventLog e WHERE e.processingStatus = 'FAILED' " +
           "AND e.attemptCount < :maxAttempts " +
           "AND (e.processedAt IS NULL OR e.processedAt < :retryAfter) " +
           "ORDER BY e.createdAt ASC")
    List<WebhookEventLog> findFailedEventsReadyForRetry(
            @Param("maxAttempts") Integer maxAttempts,
            @Param("retryAfter") LocalDateTime retryAfter,
            Pageable pageable);

    /**
     * Find pending events for processing.
     * Used by async processor to pick up new events.
     */
    @Query("SELECT e FROM WebhookEventLog e WHERE e.processingStatus = 'PENDING' " +
           "AND e.signatureStatus = 'VALID' " +
           "ORDER BY e.createdAt ASC")
    List<WebhookEventLog> findPendingEvents(Pageable pageable);

    /**
     * Find events by identity mapping ID.
     * Used to retrieve webhook history for a user.
     */
    Page<WebhookEventLog> findByIdentityMappingIdOrderByCreatedAtDesc(
            Long identityMappingId, Pageable pageable);

    /**
     * Find events by event type.
     * Used for event type-specific queries and analytics.
     */
    Page<WebhookEventLog> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);

    /**
     * Count events by processing status.
     * Used for monitoring and alerting.
     */
    long countByProcessingStatus(ProcessingStatus status);

    /**
     * Find events created within a time range.
     * Used for reporting and analytics.
     */
    @Query("SELECT e FROM WebhookEventLog e WHERE e.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY e.createdAt DESC")
    List<WebhookEventLog> findEventsByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find stale processing events (events stuck in PROCESSING state).
     * Used for detecting and recovering from stuck processes.
     */
    @Query("SELECT e FROM WebhookEventLog e WHERE e.processingStatus = 'PROCESSING' " +
           "AND e.processingStartedAt < :staleThreshold")
    List<WebhookEventLog> findStaleProcessingEvents(@Param("staleThreshold") LocalDateTime staleThreshold);

    /**
     * Reset stale processing events back to PENDING for retry.
     */
    @Modifying
    @Query("UPDATE WebhookEventLog e SET e.processingStatus = 'PENDING', " +
           "e.processingStartedAt = NULL WHERE e.processingStatus = 'PROCESSING' " +
           "AND e.processingStartedAt < :staleThreshold")
    int resetStaleProcessingEvents(@Param("staleThreshold") LocalDateTime staleThreshold);
}

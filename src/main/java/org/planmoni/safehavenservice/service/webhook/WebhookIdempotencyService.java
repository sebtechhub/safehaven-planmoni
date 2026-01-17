package org.planmoni.safehavenservice.service.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planmoni.safehavenservice.entity.webhook.ProcessingStatus;
import org.planmoni.safehavenservice.entity.webhook.SignatureStatus;
import org.planmoni.safehavenservice.entity.webhook.WebhookEventLog;
import org.planmoni.safehavenservice.repository.webhook.WebhookEventLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for enforcing idempotency in webhook processing.
 * 
 * Production Considerations:
 * - Uses database unique constraint on event_id for atomic duplicate detection
 * - Supports idempotent retries (returns previously processed result)
 * - Prevents duplicate processing via transaction isolation
 * - Tracks processing status for monitoring
 * 
 * Idempotency Strategy:
 * 1. Check if event_id exists in database
 * 2. If exists and processed successfully, return existing result (idempotent)
 * 3. If exists but failed, allow retry (with attempt count tracking)
 * 4. If not exists, create new event log and process
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookIdempotencyService {

    private final WebhookEventLogRepository webhookEventLogRepository;

    /**
     * Checks if webhook event has already been processed (idempotency check).
     * 
     * @param eventId SafeHaven-provided unique event identifier
     * @return Optional containing existing event log if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<WebhookEventLog> checkIdempotency(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            log.warn("Event ID is null or empty, cannot perform idempotency check");
            return Optional.empty();
        }

        Optional<WebhookEventLog> existingEvent = webhookEventLogRepository.findByEventId(eventId);
        
        if (existingEvent.isPresent()) {
            WebhookEventLog event = existingEvent.get();
            log.info("Event ID {} already exists with status: {}", eventId, event.getProcessingStatus());
            
            // If already successfully processed, return duplicate status
            if (event.getProcessingStatus() == ProcessingStatus.SUCCESS) {
                log.info("Event ID {} already processed successfully, marking as duplicate", eventId);
                return Optional.of(event);
            }
            
            // If duplicate (from previous idempotency check)
            if (event.getProcessingStatus() == ProcessingStatus.DUPLICATE) {
                log.info("Event ID {} is a duplicate", eventId);
                return Optional.of(event);
            }
        }
        
        return existingEvent;
    }

    /**
     * Creates a new webhook event log for processing.
     * Uses database unique constraint to atomically detect duplicates.
     * 
     * @param eventId SafeHaven-provided unique event identifier
     * @param eventType Event type (e.g., "identity.created")
     * @param payload Full webhook payload (JSON)
     * @param signature Webhook signature for validation
     * @param headers HTTP headers (JSON)
     * @param identityMappingId Optional identity mapping ID if event is user-scoped
     * @return Created webhook event log
     * @throws DuplicateEventException if event_id already exists (should not happen in normal flow)
     */
    @Transactional
    public WebhookEventLog createEventLog(
            String eventId,
            String eventType,
            String payload,
            String signature,
            String headers,
            Long identityMappingId) throws DuplicateEventException {
        
        // Double-check idempotency (defensive programming)
        if (webhookEventLogRepository.existsByEventId(eventId)) {
            log.warn("Event ID {} already exists during creation, returning existing event", eventId);
            WebhookEventLog existing = webhookEventLogRepository.findByEventId(eventId)
                    .orElseThrow(() -> new IllegalStateException("Event exists but cannot be retrieved"));
            existing.markDuplicate();
            webhookEventLogRepository.save(existing);
            throw new DuplicateEventException("Event ID already exists: " + eventId, existing);
        }

        WebhookEventLog eventLog = WebhookEventLog.builder()
                .eventId(eventId)
                .eventType(eventType)
                .payload(payload)
                .signature(signature)
                .headers(headers)
                .identityMappingId(identityMappingId)
                .signatureStatus(SignatureStatus.PENDING)
                .processingStatus(ProcessingStatus.PENDING)
                .attemptCount(0)
                .build();

        WebhookEventLog saved = webhookEventLogRepository.save(eventLog);
        log.info("Created new webhook event log for event ID: {}, type: {}", eventId, eventType);
        
        return saved;
    }

    /**
     * Records signature validation result.
     * 
     * @param eventLog Event log to update
     * @param isValid Whether signature is valid
     * @return Updated event log
     */
    @Transactional
    public WebhookEventLog recordSignatureValidation(WebhookEventLog eventLog, boolean isValid) {
        eventLog.setSignatureStatus(isValid ? SignatureStatus.VALID : SignatureStatus.INVALID);
        WebhookEventLog saved = webhookEventLogRepository.save(eventLog);
        
        if (!isValid) {
            log.warn("Invalid signature for event ID: {}", eventLog.getEventId());
        }
        
        return saved;
    }

    /**
     * Gets event log by ID for processing.
     * 
     * @param eventId Event ID
     * @return Event log if found
     */
    @Transactional(readOnly = true)
    public Optional<WebhookEventLog> getEventLog(String eventId) {
        return webhookEventLogRepository.findByEventId(eventId);
    }

    /**
     * Exception thrown when attempting to create a duplicate event log.
     */
    public static class DuplicateEventException extends Exception {
        private final WebhookEventLog existingEvent;

        public DuplicateEventException(String message, WebhookEventLog existingEvent) {
            super(message);
            this.existingEvent = existingEvent;
        }

        public WebhookEventLog getExistingEvent() {
            return existingEvent;
        }
    }
}

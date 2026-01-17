package org.planmoni.safehavenservice.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planmoni.safehavenservice.entity.webhook.ProcessingStatus;
import org.planmoni.safehavenservice.entity.webhook.WebhookEventLog;
import org.planmoni.safehavenservice.repository.webhook.WebhookEventLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing SafeHaven webhook events asynchronously.
 * 
 * Production Considerations:
 * - Async processing with @Async for non-blocking webhook responses
 * - Idempotency enforcement via event ID checks
 * - Event routing based on event type
 * - Comprehensive error handling and retry support
 * - Status tracking for monitoring
 * 
 * Event Flow:
 * 1. Webhook received → Event logged (idempotency check)
 * 2. Signature validated
 * 3. Event routed to appropriate handler based on event_type
 * 4. Domain event published (if needed)
 * 5. Status updated (SUCCESS/FAILED)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessingService {

    private final WebhookEventLogRepository webhookEventLogRepository;
    private final WebhookEventRouter webhookEventRouter;
    private final ObjectMapper objectMapper;

    @Value("${safehaven.webhook.timeout-seconds:30}")
    private int processingTimeoutSeconds;

    /**
     * Processes webhook event asynchronously.
     * 
     * @param eventLog Event log to process
     * @return CompletableFuture for async processing
     */
    @Async("webhookProcessingExecutor")
    @Transactional
    public CompletableFuture<Void> processWebhookEvent(WebhookEventLog eventLog) {
        log.info("Processing webhook event: id={}, eventId={}, type={}", 
                 eventLog.getId(), eventLog.getEventId(), eventLog.getEventType());

        try {
            // Mark processing started
            eventLog.markProcessingStarted();
            webhookEventLogRepository.save(eventLog);

            // Validate signature status (must be valid)
            if (eventLog.getSignatureStatus() != org.planmoni.safehavenservice.entity.webhook.SignatureStatus.VALID) {
                String errorMsg = String.format("Invalid signature status: %s", eventLog.getSignatureStatus());
                log.warn("Rejecting event {} due to invalid signature", eventLog.getEventId());
                eventLog.markFailed(errorMsg);
                webhookEventLogRepository.save(eventLog);
                return CompletableFuture.completedFuture(null);
            }

            // Parse payload
            JsonNode payloadNode;
            try {
                payloadNode = objectMapper.readTree(eventLog.getPayload());
            } catch (Exception e) {
                String errorMsg = "Failed to parse webhook payload: " + e.getMessage();
                log.error("Error parsing payload for event {}", eventLog.getEventId(), e);
                eventLog.markFailed(errorMsg);
                webhookEventLogRepository.save(eventLog);
                return CompletableFuture.completedFuture(null);
            }

            // Route event to appropriate handler
            try {
                webhookEventRouter.routeEvent(eventLog.getEventType(), payloadNode, eventLog);
                
                // Mark as successfully processed
                eventLog.markSuccess();
                webhookEventLogRepository.save(eventLog);
                
                log.info("Successfully processed webhook event: id={}, eventId={}", 
                         eventLog.getId(), eventLog.getEventId());
                
            } catch (Exception e) {
                String errorMsg = "Error processing event: " + e.getMessage();
                log.error("Error processing webhook event: id={}, eventId={}", 
                          eventLog.getId(), eventLog.getEventId(), e);
                
                eventLog.markFailed(errorMsg);
                webhookEventLogRepository.save(eventLog);
                
                // In production, consider publishing to dead letter queue or retry queue
                // For now, failed events are stored and can be manually retried
            }

        } catch (Exception e) {
            log.error("Unexpected error during webhook processing: id={}", eventLog.getId(), e);
            try {
                eventLog.markFailed("Unexpected error: " + e.getMessage());
                webhookEventLogRepository.save(eventLog);
            } catch (Exception saveError) {
                log.error("Failed to save error status for event {}", eventLog.getId(), saveError);
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Gets processing statistics for monitoring.
     */
    @Transactional(readOnly = true)
    public Map<ProcessingStatus, Long> getProcessingStatistics() {
        return Map.of(
                ProcessingStatus.PENDING, webhookEventLogRepository.countByProcessingStatus(ProcessingStatus.PENDING),
                ProcessingStatus.PROCESSING, webhookEventLogRepository.countByProcessingStatus(ProcessingStatus.PROCESSING),
                ProcessingStatus.SUCCESS, webhookEventLogRepository.countByProcessingStatus(ProcessingStatus.SUCCESS),
                ProcessingStatus.FAILED, webhookEventLogRepository.countByProcessingStatus(ProcessingStatus.FAILED),
                ProcessingStatus.DUPLICATE, webhookEventLogRepository.countByProcessingStatus(ProcessingStatus.DUPLICATE)
        );
    }
}

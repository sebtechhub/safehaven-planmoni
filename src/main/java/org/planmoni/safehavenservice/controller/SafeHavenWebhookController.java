package org.planmoni.safehavenservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planmoni.safehavenservice.entity.webhook.ProcessingStatus;
import org.planmoni.safehavenservice.entity.webhook.SignatureStatus;
import org.planmoni.safehavenservice.entity.webhook.WebhookEventLog;
import org.planmoni.safehavenservice.repository.identity.SafeHavenIdentityMappingRepository;
import org.planmoni.safehavenservice.service.webhook.WebhookIdempotencyService;
import org.planmoni.safehavenservice.service.webhook.WebhookProcessingService;
import org.planmoni.safehavenservice.service.webhook.WebhookSignatureValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for SafeHaven Webhook Events.
 * 
 * Production Considerations:
 * - Signature validation for security
 * - Idempotency enforcement via event ID
 * - Async processing for fast response times
 * - Comprehensive error handling
 * - Structured logging for monitoring
 * 
 * Endpoints:
 * POST /api/v1/safehaven/webhooks - Webhook event receiver
 * GET /api/v1/safehaven/webhooks/health - Health check for webhook processing
 */
@RestController
@RequestMapping("/api/v1/safehaven/webhooks")
@RequiredArgsConstructor
@Slf4j
public class SafeHavenWebhookController {

    private final WebhookSignatureValidator signatureValidator;
    private final WebhookIdempotencyService idempotencyService;
    private final WebhookProcessingService processingService;
    private final SafeHavenIdentityMappingRepository identityMappingRepository;
    private final ObjectMapper objectMapper;

    @Value("${safehaven.webhook.event-id-header:X-SafeHaven-Event-Id}")
    private String eventIdHeader;

    @Value("${safehaven.webhook.signature-header:X-SafeHaven-Signature}")
    private String signatureHeader;

    /**
     * Receives SafeHaven webhook events.
     * 
     * Flow:
     * 1. Extract event ID from header (idempotency check)
     * 2. Extract signature from header (security validation)
     * 3. Read request body (payload)
     * 4. Validate signature
     * 5. Check idempotency (if duplicate, return existing result)
     * 6. Create event log
     * 7. Process asynchronously
     * 8. Return 202 Accepted (async processing)
     * 
     * @param request HTTP request containing webhook event
     * @return 202 Accepted if event accepted, 400 Bad Request if invalid, 409 Conflict if duplicate
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> receiveWebhook(HttpServletRequest request) {
        String eventId = null;
        String signature = null;
        String payload = null;

        try {
            // Extract event ID from header (required for idempotency)
            eventId = request.getHeader(eventIdHeader);
            if (eventId == null || eventId.isEmpty()) {
                log.warn("Webhook received without event ID header: {}", eventIdHeader);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Missing event ID header", 
                                   "header", eventIdHeader));
            }

            // Extract signature from header (required for security)
            signature = request.getHeader(signatureHeader);
            if (signature == null || signature.isEmpty()) {
                log.warn("Webhook received without signature header: {} for event ID: {}", 
                        signatureHeader, eventId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Missing signature header", 
                                   "header", signatureHeader));
            }

            // Read request body (payload)
            try {
                byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
                payload = new String(bodyBytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Error reading webhook payload for event ID: {}", eventId, e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to read request body", 
                                   "eventId", eventId));
            }

            log.info("Received webhook event: eventId={}, signaturePresent={}", 
                    eventId, signature != null);

            // Check idempotency (if event already processed, return existing result)
            Optional<WebhookEventLog> existingEvent = idempotencyService.checkIdempotency(eventId);
            if (existingEvent.isPresent()) {
                WebhookEventLog event = existingEvent.get();
                
                // If already successfully processed, return success
                if (event.getProcessingStatus() == ProcessingStatus.SUCCESS) {
                    log.info("Event ID {} already processed successfully, returning idempotent response", eventId);
                    return ResponseEntity.status(HttpStatus.OK)
                            .body(Map.of("status", "success",
                                       "message", "Event already processed",
                                       "eventId", eventId,
                                       "processedAt", event.getProcessedAt() != null 
                                               ? event.getProcessedAt().toString() 
                                               : "unknown"));
                }
                
                // If duplicate (from previous idempotency check)
                if (event.getProcessingStatus() == ProcessingStatus.DUPLICATE) {
                    log.info("Event ID {} is a duplicate", eventId);
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("status", "duplicate",
                                       "message", "Event ID already exists",
                                       "eventId", eventId));
                }
            }

            // Validate signature
            boolean isValidSignature = signatureValidator.validateSignature(payload, signature);
            if (!isValidSignature) {
                log.warn("Invalid signature for event ID: {}", eventId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid signature", 
                                   "eventId", eventId));
            }

            // Extract event type from payload
            String eventType = extractEventType(payload);

            // Extract headers as JSON for audit
            String headersJson = extractHeadersAsJson(request);

            // Extract identity mapping ID from payload (if present)
            Long identityMappingId = extractIdentityMappingId(payload);

            // Create event log (database unique constraint prevents duplicates)
            WebhookEventLog eventLog;
            try {
                eventLog = idempotencyService.createEventLog(
                        eventId, eventType, payload, signature, headersJson, identityMappingId);
            } catch (WebhookIdempotencyService.DuplicateEventException e) {
                // Duplicate detected during creation (race condition)
                log.warn("Duplicate event detected during creation: eventId={}", eventId);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("status", "duplicate",
                                   "message", "Event ID already exists",
                                   "eventId", eventId));
            }

            // Record signature validation result
            eventLog = idempotencyService.recordSignatureValidation(eventLog, true);

            // Process event asynchronously
            processingService.processWebhookEvent(eventLog);

            log.info("Webhook event accepted and queued for processing: eventId={}, type={}", 
                    eventId, eventType);

            // Return 202 Accepted (async processing)
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("status", "accepted",
                               "message", "Event queued for processing",
                               "eventId", eventId));

        } catch (Exception e) {
            log.error("Unexpected error processing webhook for event ID: {}", eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error",
                               "eventId", eventId != null ? eventId : "unknown"));
        }
    }

    /**
     * Health check endpoint for webhook processing.
     * Returns processing statistics.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> webhookHealth() {
        try {
            Map<ProcessingStatus, Long> statistics = processingService.getProcessingStatistics();
            
            return ResponseEntity.ok(Map.of(
                    "status", "healthy",
                    "statistics", statistics,
                    "signatureValidatorConfigured", signatureValidator.isConfigured()
            ));
        } catch (Exception e) {
            log.error("Error retrieving webhook health", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "unhealthy", "error", e.getMessage()));
        }
    }

    /**
     * Extracts event type from payload JSON.
     */
    private String extractEventType(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            Object eventType = payloadMap.get("type");
            if (eventType != null) {
                return eventType.toString();
            }
            // Fallback: try "event_type" or "eventType"
            eventType = payloadMap.getOrDefault("event_type", payloadMap.get("eventType"));
            return eventType != null ? eventType.toString() : "unknown";
        } catch (Exception e) {
            log.warn("Failed to extract event type from payload, using 'unknown'", e);
            return "unknown";
        }
    }

    /**
     * Extracts headers as JSON for audit purposes.
     */
    private String extractHeadersAsJson(HttpServletRequest request) {
        try {
            Map<String, String> headers = Collections.list(request.getHeaderNames())
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            name -> name,
                            request::getHeader
                    ));
            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            log.warn("Failed to extract headers as JSON", e);
            return "{}";
        }
    }

    /**
     * Extracts identity mapping ID from payload (if present).
     */
    private Long extractIdentityMappingId(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            
            // Try different possible field names
            Object userId = payloadMap.getOrDefault("user_id", 
                    payloadMap.getOrDefault("userId", 
                    payloadMap.get("identity_mapping_id")));
            
            if (userId != null) {
                String userIdStr = userId.toString();
                // Look up identity mapping by SafeHaven user ID
                return identityMappingRepository.findBySafehavenUserId(userIdStr)
                        .map(org.planmoni.safehavenservice.entity.identity.SafeHavenIdentityMapping::getId)
                        .orElse(null);
            }
        } catch (Exception e) {
            log.debug("Failed to extract identity mapping ID from payload", e);
        }
        return null;
    }
}

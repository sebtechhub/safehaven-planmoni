package org.planmoni.safehavenservice.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.planmoni.safehavenservice.entity.webhook.WebhookEventLog;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Registry for webhook event handlers.
 * 
 * Maps SafeHaven webhook event types to handler functions.
 * 
 * Production Considerations:
 * - Extensible pattern for adding new event types
 * - Pattern matching support (e.g., "identity.*" matches "identity.created")
 * - Default handler for unknown event types
 * - Easy to extend without modifying core routing logic
 */
@Component
@Slf4j
public class WebhookEventHandlerRegistry {

    private final Map<String, BiConsumer<JsonNode, WebhookEventLog>> handlers = new HashMap<>();
    
    private BiConsumer<JsonNode, WebhookEventLog> defaultHandler;

    @PostConstruct
    public void init() {
        // Register default handler for unknown event types
        defaultHandler = (payload, eventLog) -> {
            log.info("Processing unknown event type: {} with default handler", eventLog.getEventType());
            // Default handler logs the event but doesn't process it
            // In production, consider storing in a dead letter queue for review
        };

        // Register event handlers
        // Identity events
        registerHandler("identity.created", this::handleIdentityCreated);
        registerHandler("identity.updated", this::handleIdentityUpdated);
        registerHandler("identity.deleted", this::handleIdentityDeleted);

        // Token events
        registerHandler("token.revoked", this::handleTokenRevoked);
        registerHandler("token.expired", this::handleTokenExpired);

        // Payment events (examples - customize based on SafeHaven API)
        registerHandler("payment.completed", this::handlePaymentCompleted);
        registerHandler("payment.failed", this::handlePaymentFailed);

        // Account events
        registerHandler("account.suspended", this::handleAccountSuspended);
        registerHandler("account.activated", this::handleAccountActivated);

        log.info("Initialized webhook event handler registry with {} handlers", handlers.size());
    }

    /**
     * Registers a handler for an event type.
     */
    public void registerHandler(String eventType, BiConsumer<JsonNode, WebhookEventLog> handler) {
        handlers.put(eventType, handler);
        log.debug("Registered handler for event type: {}", eventType);
    }

    /**
     * Gets handler for event type.
     * Supports exact match and wildcard patterns.
     */
    public BiConsumer<JsonNode, WebhookEventLog> getHandler(String eventType) {
        // Exact match
        BiConsumer<JsonNode, WebhookEventLog> handler = handlers.get(eventType);
        if (handler != null) {
            return handler;
        }

        // Pattern matching (e.g., "identity.*")
        for (Map.Entry<String, BiConsumer<JsonNode, WebhookEventLog>> entry : handlers.entrySet()) {
            if (matchesPattern(eventType, entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Gets default handler for unknown event types.
     */
    public BiConsumer<JsonNode, WebhookEventLog> getDefaultHandler() {
        return defaultHandler;
    }

    /**
     * Simple pattern matching (e.g., "identity.*" matches "identity.created").
     */
    private boolean matchesPattern(String eventType, String pattern) {
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return eventType.startsWith(prefix + ".");
        }
        return false;
    }

    // Event Handlers - Implementation Examples

    private void handleIdentityCreated(JsonNode payload, WebhookEventLog eventLog) {
        log.info("Handling identity.created event: eventId={}", eventLog.getEventId());
        
        // Extract SafeHaven user ID from payload
        String safehavenUserId = payload.path("user_id").asText(null);
        if (safehavenUserId == null) {
            throw new IllegalArgumentException("Missing user_id in identity.created event");
        }

        // TODO: Create identity mapping
        // SafeHavenIdentityMapping mapping = identityMappingService.createIdentityMapping(...);
        
        log.debug("Processed identity.created event: safehavenUserId={}", safehavenUserId);
    }

    private void handleIdentityUpdated(JsonNode payload, WebhookEventLog eventLog) {
        log.info("Handling identity.updated event: eventId={}", eventLog.getEventId());
        // TODO: Update identity mapping
    }

    private void handleIdentityDeleted(JsonNode payload, WebhookEventLog eventLog) {
        log.info("Handling identity.deleted event: eventId={}", eventLog.getEventId());
        // TODO: Mark identity mapping as deleted
    }

    private void handleTokenRevoked(JsonNode payload, WebhookEventLog eventLog) {
        log.info("Handling token.revoked event: eventId={}", eventLog.getEventId());
        // TODO: Revoke tokens in database
    }

    private void handleTokenExpired(JsonNode payload, WebhookEventLog eventLog) {
        log.info("Handling token.expired event: eventId={}", eventLog.getEventId());
        // TODO: Mark tokens as expired
    }

    private void handlePaymentCompleted(JsonNode payload, WebhookEventLog eventLog) {
        log.info("Handling payment.completed event: eventId={}", eventLog.getEventId());
        // TODO: Process payment completion
    }

    private void handlePaymentFailed(JsonNode payload, WebhookEventLog eventLog) {
        log.info("Handling payment.failed event: eventId={}", eventLog.getEventId());
        // TODO: Process payment failure
    }

    private void handleAccountSuspended(JsonNode payload, WebhookEventLog eventLog) {
        log.info("Handling account.suspended event: eventId={}", eventLog.getEventId());
        // TODO: Suspend identity mapping and revoke tokens
    }

    private void handleAccountActivated(JsonNode payload, WebhookEventLog eventLog) {
        log.info("Handling account.activated event: eventId={}", eventLog.getEventId());
        // TODO: Activate identity mapping
    }
}

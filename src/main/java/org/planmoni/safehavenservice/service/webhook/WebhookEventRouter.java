package org.planmoni.safehavenservice.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planmoni.safehavenservice.entity.webhook.WebhookEventLog;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Routes SafeHaven webhook events to appropriate handlers based on event type.
 * 
 * Production Considerations:
 * - Event type → handler mapping for extensibility
 * - Clear separation of concerns (routing vs processing)
 * - Support for internal domain event publishing
 * - Easy to add new event types without modifying core logic
 * 
 * Event Types:
 * - identity.* → Identity events (created, updated, deleted)
 * - token.* → Token events (revoked, expired)
 * - payment.* → Payment events (completed, failed)
 * - account.* → Account events (suspended, activated)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookEventRouter {

    private final WebhookEventHandlerRegistry eventHandlerRegistry;

    /**
     * Routes webhook event to appropriate handler.
     * 
     * @param eventType Event type (e.g., "identity.created")
     * @param payload Parsed JSON payload
     * @param eventLog Event log for context
     */
    public void routeEvent(String eventType, JsonNode payload, WebhookEventLog eventLog) {
        log.debug("Routing event type: {}", eventType);

        // Find handler for event type
        BiConsumer<JsonNode, WebhookEventLog> handler = eventHandlerRegistry.getHandler(eventType);
        
        if (handler == null) {
            log.warn("No handler found for event type: {}. Using default handler.", eventType);
            handler = eventHandlerRegistry.getDefaultHandler();
        }

        // Execute handler
        try {
            handler.accept(payload, eventLog);
            log.debug("Successfully routed event type: {}", eventType);
        } catch (Exception e) {
            log.error("Error in handler for event type: {}", eventType, e);
            throw new WebhookProcessingException(
                    "Handler execution failed for event type: " + eventType, e);
        }
    }

    /**
     * Exception thrown during webhook event routing/processing.
     */
    public static class WebhookProcessingException extends RuntimeException {
        public WebhookProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

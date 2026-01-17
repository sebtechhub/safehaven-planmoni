package org.planmoni.safehavenservice.service.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Service for validating SafeHaven webhook signatures.
 * 
 * Production Considerations:
 * - Uses HMAC-SHA256 for signature validation (industry standard)
 * - Constant-time comparison to prevent timing attacks
 * - Secure secret storage (via environment variables)
 * - Comprehensive logging for security monitoring
 * 
 * Signature Format:
 * SafeHaven signs webhooks using HMAC-SHA256 with the webhook secret.
 * Signature is typically provided in the X-SafeHaven-Signature header.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureValidator {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    @Value("${safehaven.webhook.secret:}")
    private String webhookSecret;

    @Value("${safehaven.webhook.signature-header:X-SafeHaven-Signature}")
    private String signatureHeader;

    /**
     * Validates webhook signature using HMAC-SHA256.
     * 
     * @param payload Raw webhook payload (JSON string)
     * @param signature Signature from header
     * @return true if signature is valid, false otherwise
     */
    public boolean validateSignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Webhook secret is not configured. Signature validation skipped.");
            return false; // Fail secure: reject if secret not configured
        }

        if (payload == null || signature == null || signature.isEmpty()) {
            log.warn("Invalid payload or signature provided for validation");
            return false;
        }

        try {
            String computedSignature = computeHmacSha256(payload, webhookSecret);
            boolean isValid = constantTimeEquals(computedSignature, signature);
            
            if (!isValid) {
                log.warn("Webhook signature validation failed. Expected: {}, Received: {}", 
                         computedSignature, signature);
            } else {
                log.debug("Webhook signature validation succeeded");
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error during webhook signature validation", e);
            return false; // Fail secure: reject on error
        }
    }

    /**
     * Computes HMAC-SHA256 signature for given payload and secret.
     * 
     * @param payload Payload to sign
     * @param secret Secret key
     * @return Hex-encoded HMAC-SHA256 signature
     */
    private String computeHmacSha256(String payload, String secret) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 
                HMAC_SHA256_ALGORITHM
        );
        mac.init(secretKeySpec);
        
        byte[] hashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hashBytes);
    }

    /**
     * Converts byte array to hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     * 
     * Production Security:
     * - Always compares all characters regardless of early mismatch
     * - Prevents timing-based side-channel attacks
     * 
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }

    /**
     * Gets the signature header name for extraction from HTTP headers.
     */
    public String getSignatureHeaderName() {
        return signatureHeader;
    }

    /**
     * Checks if webhook secret is configured.
     * Used for startup validation.
     */
    public boolean isConfigured() {
        return webhookSecret != null && !webhookSecret.isEmpty();
    }
}

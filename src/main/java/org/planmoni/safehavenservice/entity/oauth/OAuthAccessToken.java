package org.planmoni.safehavenservice.entity.oauth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * OAuth 2.0 Access Token entity for SafeHaven integration.
 * Stores access tokens with metadata for token lifecycle management.
 * 
 * Production Considerations:
 * - Tokens are encrypted at rest via application-level encryption (use Jasypt or similar)
 * - Expiry is enforced at both database and application level
 * - Token revocation is supported via status field
 */
@Entity
@Table(
    name = "oauth_access_tokens",
    indexes = {
        @Index(name = "idx_token_token_value", columnList = "token_value"),
        @Index(name = "idx_token_identity_id", columnList = "identity_mapping_id"),
        @Index(name = "idx_token_expires_at", columnList = "expires_at"),
        @Index(name = "idx_token_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_token_value", columnNames = "token_value")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OAuthAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Encrypted OAuth access token value.
     * In production, this should be encrypted using AES-256 or similar.
     */
    @Column(name = "token_value", nullable = false, unique = true, length = 2048)
    private String tokenValue;

    /**
     * Reference to the SafeHaven identity mapping.
     * Foreign key to safehaven_identity_mappings table.
     */
    @Column(name = "identity_mapping_id", nullable = false)
    private Long identityMappingId;

    /**
     * Token type (typically "Bearer").
     */
    @Column(name = "token_type", nullable = false, length = 50)
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Space-separated list of scopes granted to this token.
     */
    @Column(name = "scope", length = 500)
    private String scope;

    /**
     * When this token expires.
     * Application must check expiry before using token.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Token status for lifecycle management.
     * ACTIVE: Token is valid and can be used
     * EXPIRED: Token has expired (for audit purposes)
     * REVOKED: Token was explicitly revoked
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TokenStatus status = TokenStatus.ACTIVE;

    /**
     * Optional refresh token ID if this access token was issued with a refresh token.
     */
    @Column(name = "refresh_token_id")
    private Long refreshTokenId;

    /**
     * SafeHaven API response metadata stored as JSON.
     * Useful for debugging and compliance.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Checks if token is currently valid (not expired and not revoked).
     */
    public boolean isValid() {
        return status == TokenStatus.ACTIVE && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Marks token as expired.
     */
    public void markExpired() {
        this.status = TokenStatus.EXPIRED;
    }

    /**
     * Revokes the token.
     */
    public void revoke() {
        this.status = TokenStatus.REVOKED;
    }

    public enum TokenStatus {
        ACTIVE,
        EXPIRED,
        REVOKED
    }
}

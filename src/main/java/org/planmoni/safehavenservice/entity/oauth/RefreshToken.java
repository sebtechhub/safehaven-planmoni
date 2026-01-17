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
 * OAuth 2.0 Refresh Token entity for SafeHaven integration.
 * Stores long-lived refresh tokens used to obtain new access tokens.
 * 
 * Production Considerations:
 * - Refresh tokens should be rotated on use (one-time use)
 * - Tokens are encrypted at rest
 * - Revocation is supported for security incidents
 * - Expiry tracking prevents indefinite token reuse
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token_value", columnList = "token_value"),
        @Index(name = "idx_refresh_identity_id", columnList = "identity_mapping_id"),
        @Index(name = "idx_refresh_expires_at", columnList = "expires_at"),
        @Index(name = "idx_refresh_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_refresh_token_value", columnNames = "token_value")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Encrypted OAuth refresh token value.
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
     * When this refresh token expires.
     * Typically much longer than access tokens (e.g., 90 days).
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Token status for lifecycle management.
     * ACTIVE: Token is valid and can be used
     * USED: Token has been used (if rotation is enabled)
     * EXPIRED: Token has expired
     * REVOKED: Token was explicitly revoked (e.g., security incident)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RefreshTokenStatus status = RefreshTokenStatus.ACTIVE;

    /**
     * If token rotation is enabled, tracks the new refresh token ID
     * issued when this token was used.
     */
    @Column(name = "replaced_by_token_id")
    private Long replacedByTokenId;

    /**
     * Timestamp when token was last used to obtain a new access token.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * SafeHaven API response metadata stored as JSON.
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
     * Checks if refresh token is currently valid.
     */
    public boolean isValid() {
        return (status == RefreshTokenStatus.ACTIVE) && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Marks token as used (for rotation scenarios).
     */
    public void markUsed(Long replacedByTokenId) {
        this.status = RefreshTokenStatus.USED;
        this.replacedByTokenId = replacedByTokenId;
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Marks token as expired.
     */
    public void markExpired() {
        this.status = RefreshTokenStatus.EXPIRED;
    }

    /**
     * Revokes the token.
     */
    public void revoke() {
        this.status = RefreshTokenStatus.REVOKED;
    }

    public enum RefreshTokenStatus {
        ACTIVE,
        USED,
        EXPIRED,
        REVOKED
    }
}

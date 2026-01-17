package org.planmoni.safehavenservice.entity.identity;

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
 * SafeHaven Identity Mapping entity.
 * Maps internal system identities to SafeHaven-provided user identities.
 * 
 * This table ensures isolation between SafeHaven and other providers
 * while maintaining referential integrity for OAuth tokens.
 * 
 * Production Considerations:
 * - Provider-scoped isolation (SafeHaven only)
 * - Audit trail for identity linking/unlinking
 * - Support for identity updates from SafeHaven
 */
@Entity
@Table(
    name = "safehaven_identity_mappings",
    indexes = {
        @Index(name = "idx_identity_safehaven_id", columnList = "safehaven_user_id", unique = true),
        @Index(name = "idx_identity_internal_id", columnList = "internal_user_id"),
        @Index(name = "idx_identity_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_safehaven_user_id", columnNames = "safehaven_user_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SafeHavenIdentityMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SafeHaven-provided user identifier.
     * This is the user ID returned by SafeHaven OAuth/OIDC.
     */
    @Column(name = "safehaven_user_id", nullable = false, unique = true, length = 255)
    private String safehavenUserId;

    /**
     * Internal system user identifier.
     * Links to your internal user/account management system.
     */
    @Column(name = "internal_user_id", nullable = false, length = 255)
    private String internalUserId;

    /**
     * SafeHaven user email (from OAuth profile).
     * Stored for quick lookup and validation.
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Status of the identity mapping.
     * ACTIVE: Mapping is active and can be used
     * SUSPENDED: Temporarily disabled (e.g., user request)
     * DELETED: Mapping removed (soft delete for audit)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private IdentityMappingStatus status = IdentityMappingStatus.ACTIVE;

    /**
     * Timestamp when identity was last verified with SafeHaven.
     * Used for stale identity detection.
     */
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    /**
     * OAuth provider-specific metadata stored as JSON.
     * Contains profile information, custom claims, etc.
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
     * Checks if mapping is currently active.
     */
    public boolean isActive() {
        return status == IdentityMappingStatus.ACTIVE;
    }

    /**
     * Suspends the identity mapping.
     */
    public void suspend() {
        this.status = IdentityMappingStatus.SUSPENDED;
    }

    /**
     * Activates the identity mapping.
     */
    public void activate() {
        this.status = IdentityMappingStatus.ACTIVE;
        this.lastVerifiedAt = LocalDateTime.now();
    }

    /**
     * Marks mapping as deleted (soft delete).
     */
    public void markDeleted() {
        this.status = IdentityMappingStatus.DELETED;
    }

    public enum IdentityMappingStatus {
        ACTIVE,
        SUSPENDED,
        DELETED
    }
}

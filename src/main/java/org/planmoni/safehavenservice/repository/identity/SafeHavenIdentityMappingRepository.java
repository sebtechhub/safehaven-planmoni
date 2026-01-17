package org.planmoni.safehavenservice.repository.identity;

import org.planmoni.safehavenservice.entity.identity.SafeHavenIdentityMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafeHavenIdentityMappingRepository extends JpaRepository<SafeHavenIdentityMapping, Long> {

    /**
     * Find identity mapping by SafeHaven user ID.
     * Primary lookup method for OAuth callback processing.
     */
    Optional<SafeHavenIdentityMapping> findBySafehavenUserId(String safehavenUserId);

    /**
     * Find active identity mappings for an internal user ID.
     * Used to retrieve all active SafeHaven identities for a user.
     */
    @Query("SELECT m FROM SafeHavenIdentityMapping m WHERE m.internalUserId = :internalUserId " +
           "AND m.status = 'ACTIVE'")
    List<SafeHavenIdentityMapping> findActiveByInternalUserId(@Param("internalUserId") String internalUserId);

    /**
     * Find all identity mappings for an internal user ID (including inactive).
     * Used for audit and management.
     */
    List<SafeHavenIdentityMapping> findByInternalUserIdOrderByCreatedAtDesc(String internalUserId);

    /**
     * Check if SafeHaven user ID exists.
     * Used for duplicate detection during identity linking.
     */
    boolean existsBySafehavenUserId(String safehavenUserId);

    /**
     * Check if active mapping exists for SafeHaven user ID.
     * Used to verify if identity is already linked.
     */
    @Query("SELECT COUNT(m) > 0 FROM SafeHavenIdentityMapping m " +
           "WHERE m.safehavenUserId = :safehavenUserId AND m.status = 'ACTIVE'")
    boolean existsActiveBySafehavenUserId(@Param("safehavenUserId") String safehavenUserId);

    /**
     * Find identity mapping by email (for lookup).
     * Used during OAuth profile matching.
     */
    Optional<SafeHavenIdentityMapping> findByEmail(String email);

    /**
     * Suspend all active identity mappings for an internal user.
     * Used during account suspension.
     */
    @Modifying
    @Query("UPDATE SafeHavenIdentityMapping m SET m.status = 'SUSPENDED' " +
           "WHERE m.internalUserId = :internalUserId AND m.status = 'ACTIVE'")
    int suspendAllByInternalUserId(@Param("internalUserId") String internalUserId);

    /**
     * Count active identity mappings for an internal user.
     * Used for monitoring and limits.
     */
    @Query("SELECT COUNT(m) FROM SafeHavenIdentityMapping m " +
           "WHERE m.internalUserId = :internalUserId AND m.status = 'ACTIVE'")
    long countActiveByInternalUserId(@Param("internalUserId") String internalUserId);
}

package org.planmoni.safehavenservice.repository.oauth;

import org.planmoni.safehavenservice.entity.oauth.OAuthAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthAccessTokenRepository extends JpaRepository<OAuthAccessToken, Long> {

    /**
     * Find access token by token value.
     * Used for token lookup during API calls.
     */
    Optional<OAuthAccessToken> findByTokenValue(String tokenValue);

    /**
     * Find active access tokens for an identity mapping.
     * Used to retrieve valid tokens for a user.
     */
    @Query("SELECT t FROM OAuthAccessToken t WHERE t.identityMappingId = :identityMappingId " +
           "AND t.status = 'ACTIVE' AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    List<OAuthAccessToken> findActiveTokensByIdentityMappingId(
            @Param("identityMappingId") Long identityMappingId,
            @Param("now") LocalDateTime now);

    /**
     * Find all tokens (active and expired) for an identity mapping.
     * Used for token management and revocation.
     */
    List<OAuthAccessToken> findByIdentityMappingIdOrderByCreatedAtDesc(Long identityMappingId);

    /**
     * Check if a token value exists (for duplicate detection).
     */
    boolean existsByTokenValue(String tokenValue);

    /**
     * Mark expired tokens as EXPIRED.
     * Scheduled job should call this periodically.
     */
    @Modifying
    @Query("UPDATE OAuthAccessToken t SET t.status = 'EXPIRED' " +
           "WHERE t.status = 'ACTIVE' AND t.expiresAt <= :now")
    int markExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Revoke all active tokens for an identity mapping.
     * Used during security incidents or user logout.
     */
    @Modifying
    @Query("UPDATE OAuthAccessToken t SET t.status = 'REVOKED' " +
           "WHERE t.identityMappingId = :identityMappingId AND t.status = 'ACTIVE'")
    int revokeAllActiveTokensByIdentityMappingId(@Param("identityMappingId") Long identityMappingId);

    /**
     * Count active tokens for an identity mapping.
     * Used for monitoring and limits.
     */
    @Query("SELECT COUNT(t) FROM OAuthAccessToken t WHERE t.identityMappingId = :identityMappingId " +
           "AND t.status = 'ACTIVE' AND t.expiresAt > :now")
    long countActiveTokensByIdentityMappingId(
            @Param("identityMappingId") Long identityMappingId,
            @Param("now") LocalDateTime now);
}

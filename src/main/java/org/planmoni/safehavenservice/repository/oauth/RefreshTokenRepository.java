package org.planmoni.safehavenservice.repository.oauth;

import org.planmoni.safehavenservice.entity.oauth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find refresh token by token value.
     * Used during token refresh flow.
     */
    Optional<RefreshToken> findByTokenValue(String tokenValue);

    /**
     * Find active refresh tokens for an identity mapping.
     * Used to retrieve valid refresh tokens for a user.
     */
    @Query("SELECT t FROM RefreshToken t WHERE t.identityMappingId = :identityMappingId " +
           "AND t.status = 'ACTIVE' AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    List<RefreshToken> findActiveTokensByIdentityMappingId(
            @Param("identityMappingId") Long identityMappingId,
            @Param("now") LocalDateTime now);

    /**
     * Find all refresh tokens for an identity mapping.
     * Used for token management and revocation.
     */
    List<RefreshToken> findByIdentityMappingIdOrderByCreatedAtDesc(Long identityMappingId);

    /**
     * Check if a token value exists (for duplicate detection).
     */
    boolean existsByTokenValue(String tokenValue);

    /**
     * Mark expired refresh tokens as EXPIRED.
     * Scheduled job should call this periodically.
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.status = 'EXPIRED' " +
           "WHERE t.status = 'ACTIVE' AND t.expiresAt <= :now")
    int markExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Revoke all active refresh tokens for an identity mapping.
     * Used during security incidents or user logout.
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.status = 'REVOKED' " +
           "WHERE t.identityMappingId = :identityMappingId AND t.status = 'ACTIVE'")
    int revokeAllActiveTokensByIdentityMappingId(@Param("identityMappingId") Long identityMappingId);

    /**
     * Find refresh token by replacement token ID (for token rotation tracking).
     */
    Optional<RefreshToken> findByReplacedByTokenId(Long replacedByTokenId);
}

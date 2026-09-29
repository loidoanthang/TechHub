package com.techhub.repository;

import com.techhub.model.entity.User;
import com.techhub.model.entity.VerificationTokens;
import com.techhub.model.enums.VerificationTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokensRepository extends JpaRepository<VerificationTokens, UUID> {

    Optional<VerificationTokens> findByUserAndType(User user, VerificationTokenType type);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM VerificationTokens v WHERE v.expiresAt < :now")
    int deleteAllByExpiresAtBefore(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM verification_tokens WHERE user_id IN (SELECT id FROM users WHERE email_verified = false AND created_at < :cutoff)", nativeQuery = true)
    int deleteTokensForUnverifiedUsersBefore(@Param("cutoff") LocalDateTime cutoff);
}

package com.techhub.repository;

import com.techhub.model.entity.EmailVerificationToken;
import com.techhub.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByUser(User user);

    void deleteAllByExpiresAtBefore(java.time.LocalDateTime now);
}

package com.techhub.repository;

import com.techhub.model.entity.PasswordResetToken;
import com.techhub.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByUser(User user);

    void deleteAllByExpiresAtBefore(java.time.LocalDateTime now);
}

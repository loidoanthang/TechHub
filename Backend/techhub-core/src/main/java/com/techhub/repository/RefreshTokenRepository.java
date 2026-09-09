package com.techhub.repository;

import com.techhub.model.entity.RefreshToken;
import com.techhub.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUser(User user);

    void deleteAllByExpiresAtBefore(java.time.LocalDateTime now);
}

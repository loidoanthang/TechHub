package com.techhub.modules.auth.repository;

import com.techhub.modules.auth.entity.RefreshToken;
import com.techhub.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.techhub.modules.auth.entity.RefreshToken;
import com.techhub.modules.user.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUser(User user);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteAllByExpiresAtBefore(@Param("now") LocalDateTime now);
}

package com.techhub.service.impl;

import com.techhub.config.JwtProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.entity.RefreshToken;
import com.techhub.model.entity.User;
import com.techhub.repository.RefreshTokenRepository;
import com.techhub.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl
        implements RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;

    @Override
    public RefreshToken create(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpiration() / 1000));
        refreshToken.setRevoked(false);
        return repository.save(refreshToken);
    }

    @Override
    public RefreshToken getByToken(String token) {

        return repository.findByToken(token).orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    @Override
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        repository.save(token);
    }

    @Override
    @Transactional
    public RefreshToken validate(String tokenValue) {
        RefreshToken refreshToken =
                getByToken(tokenValue);
        if (refreshToken.isRevoked()) {
            revokeAll(refreshToken.getUser());
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            repository.delete(refreshToken);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeAll(User user) {
        List<RefreshToken> tokens = repository.findAllByUser(user);
        tokens.forEach(token -> token.setRevoked(true));
        repository.saveAll(tokens);
    }

    @Override
    @Transactional
    public void revokeByToken(String token) {
        repository.findByToken(token)
                .ifPresent(this::revoke);
    }

}

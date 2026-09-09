package com.techhub.service.impl;

import com.techhub.config.TokenProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.entity.PasswordResetToken;
import com.techhub.model.entity.User;
import com.techhub.repository.PasswordResetTokenRepository;
import com.techhub.service.PasswordResetTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl
        implements PasswordResetTokenService {

    private final PasswordResetTokenRepository repository;
    private final TokenProperties tokenProperties;
    private static final SecureRandom RANDOM = new SecureRandom();


    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    public PasswordResetToken create(User user) {
        PasswordResetToken token = repository.findByUser(user)
                .orElseGet(() -> {
                    PasswordResetToken t = new PasswordResetToken();
                    t.setUser(user);
                    return t;
                });
        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        token.setToken(otp);
        token.setExpiresAt(LocalDateTime.now().plusSeconds(tokenProperties.getPasswordResetExpiration() / 1000));
        token.setFailedAttempts(0);
        return repository.save(token);
    }

    @Override
    public PasswordResetToken validate(User user, String tokenValue) {
        PasswordResetToken token = repository.findByUser(user).orElseThrow(() ->
                new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            repository.delete(token);
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        if (token.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new BusinessException(ErrorCode.TOO_MANY_FAILED_ATTEMPTS);
        }

        if (!token.getToken().equals(tokenValue)) {
            token.setFailedAttempts(token.getFailedAttempts() + 1);
            repository.save(token);
            if (token.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                throw new BusinessException(ErrorCode.TOO_MANY_FAILED_ATTEMPTS);
            }
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        return token;
    }

    @Override
    public void delete(PasswordResetToken token) {
        repository.delete(token);
    }

}

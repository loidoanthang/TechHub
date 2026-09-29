package com.techhub.service.impl;

import com.techhub.config.properties.TokenProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.entity.User;
import com.techhub.model.entity.VerificationTokens;
import com.techhub.model.enums.VerificationTokenType;
import com.techhub.repository.VerificationTokensRepository;
import com.techhub.service.VerificationTokensService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationTokensServiceImpl implements VerificationTokensService {

    private final VerificationTokensRepository repository;
    private final TokenProperties tokenProperties;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    public VerificationTokens create(User user, VerificationTokenType type) {
        VerificationTokens token = repository.findByUserAndType(user, type)
                .orElseGet(() -> {
                    VerificationTokens t = new VerificationTokens();
                    t.setUser(user);
                    t.setType(type);
                    return t;
                });

        LocalDateTime lastSentAt = token.getUpdatedAt() != null ? token.getUpdatedAt() : token.getCreatedAt();
        if (lastSentAt != null && lastSentAt.isAfter(LocalDateTime.now().minusSeconds(60))) {
            throw new BusinessException(ErrorCode.OTP_RESEND_COOLDOWN);
        }

        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        token.setToken(otp);

        long expirationMillis = (type == VerificationTokenType.EMAIL_VERIFICATION)
                ? tokenProperties.getVerificationExpiration()
                : tokenProperties.getPasswordResetExpiration();

        token.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(expirationMillis)));
        token.setFailedAttempts(0);
        return repository.save(token);
    }

    @Override
    public VerificationTokens getByUserAndType(User user, VerificationTokenType type) {
        return repository.findByUserAndType(user, type).orElseThrow(() -> {
            if (type == VerificationTokenType.EMAIL_VERIFICATION) {
                return new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
            } else {
                return new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND);
            }
        });
    }

    @Override
    public VerificationTokens validate(User user, String tokenValue, VerificationTokenType type) {
        VerificationTokens token = getByUserAndType(user, type);

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        if (token.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new BusinessException(ErrorCode.TOO_MANY_FAILED_ATTEMPTS);
        }

        String cleanToken = tokenValue != null ? tokenValue.trim() : "";
        if (!token.getToken().equals(cleanToken)) {
            token.setFailedAttempts(token.getFailedAttempts() + 1);
            repository.save(token);
            if (token.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                throw new BusinessException(ErrorCode.TOO_MANY_FAILED_ATTEMPTS);
            }
            if (type == VerificationTokenType.EMAIL_VERIFICATION) {
                throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
            } else {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
            }
        }

        return token;
    }

    @Override
    public void delete(VerificationTokens token) {
        repository.delete(token);
    }
}

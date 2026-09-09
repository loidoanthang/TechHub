package com.techhub.service.impl;

import com.techhub.config.TokenProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.entity.EmailVerificationToken;
import com.techhub.model.entity.User;
import com.techhub.repository.EmailVerificationTokenRepository;
import com.techhub.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private final EmailVerificationTokenRepository repository;
    private final TokenProperties tokenProperties;
    private static final SecureRandom RANDOM = new SecureRandom();


    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    public EmailVerificationToken create(User user) {
        EmailVerificationToken token = repository.findByUser(user)
                .orElseGet(() -> {
                    EmailVerificationToken t = new EmailVerificationToken();
                    t.setUser(user);
                    return t;
                });
        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        token.setToken(otp);
        token.setExpiresAt(LocalDateTime.now()
                .plus(Duration.ofMillis(tokenProperties.getVerificationExpiration())));
        token.setFailedAttempts(0);
        return repository.save(token);
    }

    @Override
    public EmailVerificationToken getByUser(User user) {
        return repository.findByUser(user).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN));
    }

    @Override
    public EmailVerificationToken validate(User user, String tokenValue) {
        EmailVerificationToken token = getByUser(user);

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            repository.delete(token);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
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
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }

        return token;
    }

    @Override
    public void delete(EmailVerificationToken token) {
        repository.delete(token);
    }

}

package com.techhub.task;

import com.techhub.repository.EmailVerificationTokenRepository;
import com.techhub.repository.PasswordResetTokenRepository;
import com.techhub.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Dọn dẹp token hết hạn hoặc token đã bị thu hồi (revoked)
     * Chạy tự động vào lúc 00:00 sáng mỗi ngày
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting expired token cleanup job at {}", now);

        emailVerificationTokenRepository.deleteAllByExpiresAtBefore(now);
        passwordResetTokenRepository.deleteAllByExpiresAtBefore(now);
        refreshTokenRepository.deleteAllByExpiresAtBefore(now);

        log.info("Expired token cleanup job completed successfully");
    }
}

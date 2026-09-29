package com.techhub.task;

import com.techhub.repository.RefreshTokenRepository;
import com.techhub.repository.UserRepository;
import com.techhub.repository.VerificationTokensRepository;
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

    private final VerificationTokensRepository verificationTokensRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * Dọn dẹp token hết hạn hoặc token đã bị thu hồi (revoked)
     * Chạy tự động vào lúc 00:00 sáng mỗi ngày bằng Bulk Delete
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting expired token cleanup job at {}", now);

        int deletedVerifications = verificationTokensRepository.deleteAllByExpiresAtBefore(now);
        int deletedRefreshes = refreshTokenRepository.deleteAllByExpiresAtBefore(now);

        log.info("Expired token cleanup job completed successfully: removed {} verification tokens, {} refresh tokens",
                deletedVerifications, deletedRefreshes);
    }

    /**
     * Dọn dẹp tài khoản chưa kích hoạt quá 72 giờ (3 ngày)
     * Chạy tự động vào lúc 02:00 sáng mỗi ngày bằng Bulk Delete tập hợp (Set-based)
     * Thứ tự xóa an toàn: verification_tokens -> user_roles -> users (tránh vi phạm Foreign Key Constraint)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanUnverifiedUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        log.info("Starting unverified users cleanup job for accounts created before {}", cutoff);

        int deletedTokens = verificationTokensRepository.deleteTokensForUnverifiedUsersBefore(cutoff);
        int deletedRoles = userRepository.deleteUserRolesForUnverifiedUsersBefore(cutoff);
        int deletedUsers = userRepository.deleteUnverifiedUsersOlderThan(cutoff);

        log.info("Cleaned up {} unverified user accounts (removed {} tokens, {} roles)",
                deletedUsers, deletedTokens, deletedRoles);
    }
}

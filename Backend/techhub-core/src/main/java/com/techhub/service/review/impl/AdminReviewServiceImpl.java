package com.techhub.service.review.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.entity.Review;
import com.techhub.repository.ReviewRepository;
import com.techhub.security.SecurityUtils;
import com.techhub.service.review.AdminReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(UUID reviewId) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        log.info("Admin {} attempting to soft-delete review: {}", adminId, reviewId);

        // 1. Kiểm tra tồn tại trong DB
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.warn("Review not found for deletion: reviewId={}", reviewId);
                    return new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
                });

        // 2. Chặn nếu bài đánh giá đã bị xóa mềm trước đó
        if (review.getDeletedAt() != null) {
            log.warn("Review {} has already been soft-deleted at {}", reviewId, review.getDeletedAt());
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 3. Thực thi Atomic Conditional Soft-Delete
        LocalDateTime now = LocalDateTime.now();
        int rowsUpdated = reviewRepository.softDeleteReview(reviewId, now);
        if (rowsUpdated == 0) {
            log.warn("Concurrent deletion conflict for review: {}", reviewId);
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 4. Ghi Audit Log mức WARN phục vụ tra soát
        log.warn("AUDIT: Admin {} successfully soft-deleted review {} at {}", adminId, reviewId, now);
    }
}

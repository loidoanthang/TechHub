package com.techhub.service.review;

import com.techhub.common.PageResponse;
import com.techhub.modules.review.dto.response.SellerReviewResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface SellerReviewService {

    PageResponse<SellerReviewResponse> getShopReviews(
            UUID userId,
            Integer rating,
            Boolean hasResponded,
            Boolean hasMedia,
            UUID productId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    SellerReviewResponse respondToReview(UUID userId, UUID reviewId, com.techhub.modules.review.dto.request.SellerResponseRequest request);
}

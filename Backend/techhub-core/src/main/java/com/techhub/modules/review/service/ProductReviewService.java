package com.techhub.modules.review.service;

import com.techhub.common.PageResponse;
import com.techhub.modules.review.dto.response.ProductRatingSummaryResponse;
import com.techhub.modules.review.dto.response.ProductReviewResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public interface ProductReviewService {

    PageResponse<ProductReviewResponse> getProductReviews(
            UUID productId,
            UUID currentUserId,
            Integer rating,
            Boolean hasMedia,
            String sortBy,
            Sort.Direction sortDirection,
            Pageable pageable
    );

    ProductRatingSummaryResponse getProductRatingSummary(UUID productId);
}

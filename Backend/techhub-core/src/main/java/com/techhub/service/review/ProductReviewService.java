package com.techhub.service.review;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.review.ProductRatingSummaryResponse;
import com.techhub.model.dto.response.review.ProductReviewResponse;
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

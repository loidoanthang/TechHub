package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.review.ProductRatingSummaryResponse;
import com.techhub.model.dto.response.review.ProductReviewResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.review.ProductReviewService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping("/{id}/reviews")
    public ApiResponse<PageResponse<ProductReviewResponse>> getProductReviews(
            @PathVariable("id") UUID productId,
            @RequestParam(required = false) @Min(value = 1, message = "Rating must be at least 1") @Max(value = 5, message = "Rating must not exceed 5") Integer rating,
            @RequestParam(required = false) Boolean hasMedia,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be non-negative") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 50, message = "Page size must not exceed 50") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserIdOptional().orElse(null);
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = "rating".equalsIgnoreCase(sortBy)
                ? Sort.by(direction, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"))
                : Sort.by(direction, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, currentUserId, rating, hasMedia, sortBy, direction, pageable
        );

        return ApiResponse.success(response, "Product reviews retrieved successfully", null);
    }

    @GetMapping("/{id}/rating-summary")
    public ApiResponse<ProductRatingSummaryResponse> getProductRatingSummary(
            @PathVariable("id") UUID productId
    ) {
        ProductRatingSummaryResponse response = productReviewService.getProductRatingSummary(productId);
        return ApiResponse.success(response, "Product rating summary retrieved successfully", null);
    }
}

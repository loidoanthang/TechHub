package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.review.SellerResponseRequest;
import com.techhub.model.dto.response.review.SellerReviewResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.review.SellerReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/reviews")
@RequiredArgsConstructor
@Validated
public class SellerReviewController {

    private final SellerReviewService sellerReviewService;

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<PageResponse<SellerReviewResponse>> getShopReviews(
            @RequestParam(required = false) @Min(value = 1, message = "Rating must be at least 1") @Max(value = 5, message = "Rating must not exceed 5") Integer rating,
            @RequestParam(required = false) Boolean hasResponded,
            @RequestParam(required = false) Boolean hasMedia,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be non-negative") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 50, message = "Page size must not exceed 50") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = ("rating".equalsIgnoreCase(sortBy)) ? "rating" : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                currentUserId, rating, hasResponded, hasMedia, productId, keyword, fromDate, toDate, pageable
        );

        return ApiResponse.success(response, "Shop reviews retrieved successfully", null);
    }

    @PostMapping("/{id}/response")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerReviewResponse> respondToReview(
            @PathVariable("id") UUID reviewId,
            @Valid @RequestBody SellerResponseRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        SellerReviewResponse response = sellerReviewService.respondToReview(currentUserId, reviewId, request);
        return ApiResponse.success(response, "Review responded successfully", null);
    }
}

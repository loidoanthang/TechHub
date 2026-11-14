package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.modules.review.dto.request.CreateReviewRequest;
import com.techhub.modules.review.dto.request.UpdateReviewRequest;
import com.techhub.modules.review.dto.response.ReviewResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/buyer/reviews")
@RequiredArgsConstructor
public class BuyerReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.createReview(currentUserId, request);
        return ApiResponse.success(response, "Review submitted successfully", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.updateReview(currentUserId, id, request);
        return ApiResponse.success(response, "Review updated successfully", null);
    }
}

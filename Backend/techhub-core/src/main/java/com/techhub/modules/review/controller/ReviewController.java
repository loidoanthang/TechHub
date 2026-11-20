package com.techhub.modules.review.controller;

import com.techhub.common.ApiResponse;
import com.techhub.modules.review.dto.request.CreateReviewRequest;
import com.techhub.modules.review.dto.request.UpdateReviewRequest;
import com.techhub.modules.review.dto.response.HelpfulVoteResponse;
import com.techhub.modules.review.dto.response.ReviewResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.security.annotation.RequireActiveBuyer;
import com.techhub.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @RequireActiveBuyer
    public ApiResponse<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.createReview(currentUserId, request);
        return ApiResponse.success(response, "Review submitted successfully", null);
    }

    @PutMapping("/{id}")
    @RequireActiveBuyer
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.updateReview(currentUserId, id, request);
        return ApiResponse.success(response, "Review updated successfully", null);
    }

    @PostMapping("/{id}/helpful")
    @RequireActiveBuyer
    public ApiResponse<HelpfulVoteResponse> toggleHelpfulVote(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        HelpfulVoteResponse response = reviewService.toggleHelpfulVote(currentUserId, id);
        String message = response.isHelpful() ? "Review marked as helpful" : "Review unmarked as helpful";
        return ApiResponse.success(response, message, null);
    }
}

package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.review.HelpfulVoteResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{id}/helpful")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<HelpfulVoteResponse> toggleHelpfulVote(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        HelpfulVoteResponse response = reviewService.toggleHelpfulVote(currentUserId, id);
        String message = response.isHelpful() ? "Review marked as helpful" : "Review unmarked as helpful";
        return ApiResponse.success(response, message, null);
    }
}

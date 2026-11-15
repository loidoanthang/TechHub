package com.techhub.modules.review.service;

import com.techhub.modules.review.dto.request.CreateReviewRequest;
import com.techhub.modules.review.dto.request.UpdateReviewRequest;
import com.techhub.modules.review.dto.response.HelpfulVoteResponse;
import com.techhub.modules.review.dto.response.ReviewResponse;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID userId, CreateReviewRequest request);

    ReviewResponse updateReview(UUID userId, UUID reviewId, UpdateReviewRequest request);

    HelpfulVoteResponse toggleHelpfulVote(UUID userId, UUID reviewId);
}

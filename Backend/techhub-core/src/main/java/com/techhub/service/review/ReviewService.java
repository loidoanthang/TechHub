package com.techhub.service.review;

import com.techhub.model.dto.request.review.CreateReviewRequest;
import com.techhub.model.dto.request.review.UpdateReviewRequest;
import com.techhub.model.dto.response.review.HelpfulVoteResponse;
import com.techhub.model.dto.response.review.ReviewResponse;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID userId, CreateReviewRequest request);

    ReviewResponse updateReview(UUID userId, UUID reviewId, UpdateReviewRequest request);

    HelpfulVoteResponse toggleHelpfulVote(UUID userId, UUID reviewId);
}

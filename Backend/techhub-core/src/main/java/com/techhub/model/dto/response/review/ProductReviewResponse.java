package com.techhub.model.dto.response.review;

import com.techhub.model.entity.OrderItem;
import com.techhub.model.entity.Review;
import com.techhub.model.entity.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record ProductReviewResponse(
        UUID id,
        UUID orderItemId,
        int rating,
        String comment,
        String variantName,
        String reviewerName,
        String reviewerAvatar,
        List<ReviewMediaResponse> mediaList,
        String sellerResponse,
        LocalDateTime sellerResponseAt,
        long helpfulCount,
        boolean hasVotedHelpful,
        LocalDateTime createdAt
) {
    public static ProductReviewResponse from(Review review, long helpfulCount, boolean hasVotedHelpful) {
        if (review == null) return null;

        OrderItem orderItem = review.getOrderItem();
        User user = review.getUser();

        String varName = orderItem != null ? orderItem.getVariantNameSnapshot() : null;
        String maskedName = ReviewResponse.maskName(
                user != null ? user.getFirstName() : null,
                user != null ? user.getLastName() : null
        );
        String avatar = user != null ? user.getAvatarUrl() : null;

        List<ReviewMediaResponse> mediaResponses = review.getMediaList() != null
                ? review.getMediaList().stream().map(ReviewMediaResponse::from).toList()
                : Collections.emptyList();

        return new ProductReviewResponse(
                review.getId(),
                orderItem != null ? orderItem.getId() : null,
                review.getRating(),
                review.getComment(),
                varName,
                maskedName,
                avatar,
                mediaResponses,
                review.getSellerResponse(),
                review.getSellerResponseAt(),
                helpfulCount,
                hasVotedHelpful,
                review.getCreatedAt()
        );
    }
}

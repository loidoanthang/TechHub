package com.techhub.model.dto.response.review;

import com.techhub.model.entity.OrderItem;
import com.techhub.model.entity.Review;
import com.techhub.model.entity.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID orderItemId,
        UUID productId,
        String productName,
        String variantName,
        String imageUrl,
        int rating,
        String comment,
        List<ReviewMediaResponse> mediaList,
        String reviewerName,
        String reviewerAvatar,
        String sellerResponse,
        LocalDateTime sellerResponseAt,
        long helpfulCount,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review, long helpfulCount) {
        if (review == null) return null;

        OrderItem orderItem = review.getOrderItem();
        User user = review.getUser();

        UUID prodId = null;
        String prodName = null;
        String varName = null;
        String imgUrl = null;

        if (orderItem != null) {
            prodName = orderItem.getProductNameSnapshot();
            varName = orderItem.getVariantNameSnapshot();
            imgUrl = orderItem.getImageUrlSnapshot();
            if (orderItem.getProductVariant() != null && orderItem.getProductVariant().getProduct() != null) {
                prodId = orderItem.getProductVariant().getProduct().getId();
            }
        }

        String maskedReviewerName = maskName(user != null ? user.getFirstName() : null, user != null ? user.getLastName() : null);
        String avatar = user != null ? user.getAvatarUrl() : null;

        List<ReviewMediaResponse> mediaResponses = review.getMediaList() != null
                ? review.getMediaList().stream().map(ReviewMediaResponse::from).toList()
                : Collections.emptyList();

        return new ReviewResponse(
                review.getId(),
                orderItem != null ? orderItem.getId() : null,
                prodId,
                prodName,
                varName,
                imgUrl,
                review.getRating(),
                review.getComment(),
                mediaResponses,
                maskedReviewerName,
                avatar,
                review.getSellerResponse(),
                review.getSellerResponseAt(),
                helpfulCount,
                review.getCreatedAt()
        );
    }

    public static String maskName(String firstName, String lastName) {
        String f = (firstName != null && !firstName.isBlank()) ? firstName.trim() : "";
        String l = (lastName != null && !lastName.isBlank()) ? lastName.trim() : "";
        if (f.isEmpty() && l.isEmpty()) return "N*** A";
        if (f.isEmpty()) return l.charAt(0) + "***";
        if (l.isEmpty()) return f.charAt(0) + "***";
        return f.charAt(0) + "*** " + l.charAt(0);
    }
}

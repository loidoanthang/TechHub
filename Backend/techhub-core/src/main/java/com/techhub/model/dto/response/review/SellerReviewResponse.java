package com.techhub.model.dto.response.review;

import com.techhub.model.entity.Order;
import com.techhub.model.entity.OrderItem;
import com.techhub.model.entity.ProductVariant;
import com.techhub.model.entity.Review;
import com.techhub.model.entity.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record SellerReviewResponse(
        UUID id,
        UUID orderItemId,
        UUID orderId,
        String orderNumber,
        UUID productId,
        String productName,
        String variantName,
        String sku,
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
    public static SellerReviewResponse from(Review review, long helpfulCount) {
        if (review == null) return null;

        OrderItem orderItem = review.getOrderItem();
        User user = review.getUser();

        UUID orderId = null;
        String orderNumber = null;
        UUID prodId = null;
        String prodName = null;
        String varName = null;
        String sku = null;
        String imgUrl = null;

        if (orderItem != null) {
            prodName = orderItem.getProductNameSnapshot();
            varName = orderItem.getVariantNameSnapshot();
            imgUrl = orderItem.getImageUrlSnapshot();

            Order order = orderItem.getOrder();
            if (order != null) {
                orderId = order.getId();
                orderNumber = order.getOrderNumber();
            }

            ProductVariant variant = orderItem.getProductVariant();
            if (variant != null) {
                sku = variant.getSku() != null ? variant.getSku() : orderItem.getSkuSnapshot();
                if (variant.getProduct() != null) {
                    prodId = variant.getProduct().getId();
                }
            } else {
                sku = orderItem.getSkuSnapshot();
            }
        }

        String maskedReviewerName = ReviewResponse.maskName(
                user != null ? user.getFirstName() : null,
                user != null ? user.getLastName() : null
        );
        String avatar = user != null ? user.getAvatarUrl() : null;

        List<ReviewMediaResponse> mediaResponses = review.getMediaList() != null
                ? review.getMediaList().stream().map(ReviewMediaResponse::from).toList()
                : Collections.emptyList();

        return new SellerReviewResponse(
                review.getId(),
                orderItem != null ? orderItem.getId() : null,
                orderId,
                orderNumber,
                prodId,
                prodName,
                varName,
                sku,
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
}

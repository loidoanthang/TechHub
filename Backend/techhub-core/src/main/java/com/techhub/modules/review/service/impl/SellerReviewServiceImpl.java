package com.techhub.modules.review.service.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.review.dto.request.SellerResponseRequest;
import com.techhub.modules.review.dto.response.SellerReviewResponse;
import com.techhub.modules.order.entity.OrderItem;
import com.techhub.modules.review.entity.Review;
import com.techhub.modules.seller.entity.Seller;
import com.techhub.modules.shop.entity.Shop;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.shop.enums.ShopStatus;
import com.techhub.modules.user.enums.UserStatus;
import com.techhub.modules.review.repository.ReviewHelpfulRepository;
import com.techhub.modules.review.repository.ReviewRepository;
import com.techhub.modules.seller.repository.SellerRepository;
import com.techhub.modules.shop.repository.ShopRepository;
import com.techhub.modules.review.service.SellerReviewService;
import com.techhub.modules.review.specification.ReviewSpecification;
import com.techhub.modules.notification.enums.NotificationType;
import com.techhub.modules.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.techhub.modules.review.service.SellerReviewService;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerReviewServiceImpl implements SellerReviewService {

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SellerReviewResponse> getShopReviews(
            UUID userId,
            Integer rating,
            Boolean hasResponded,
            Boolean hasMedia,
            UUID productId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        log.info("Fetching shop reviews for seller userId={}, rating={}, hasResponded={}, page={}",
                userId, rating, hasResponded, pageable.getPageNumber());

        // 1. Validate Date Range
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            log.warn("Invalid date range: fromDate {} is after toDate {}", fromDate, toDate);
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        // 2. Validate Seller Profile (BR-20.21)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Access denied.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        User user = seller.getUser();
        if (user != null) {
            if (user.getStatus() == UserStatus.BANNED) {
                log.warn("Seller user {} is BANNED", user.getId());
                throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
            }
        }

        // 3. Validate Shop (BR-20.21)
        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} is BANNED. Rejecting access.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 4. Build Dynamic Specification
        Specification<Review> spec = Specification.where(ReviewSpecification.belongsToShop(shop.getId()))
                .and(ReviewSpecification.notDeleted())
                .and(ReviewSpecification.hasRating(rating))
                .and(ReviewSpecification.hasResponded(hasResponded))
                .and(ReviewSpecification.hasMedia(hasMedia))
                .and(ReviewSpecification.hasProductId(productId))
                .and(ReviewSpecification.hasKeyword(keyword))
                .and(ReviewSpecification.createdBetween(fromDate, toDate));

        // 5. Query Reviews with Pagination
        Page<Review> reviewPage = reviewRepository.findAll(spec, pageable);

        // 6. Batch fetch Helpful Counts to eliminate N+1 queries
        List<UUID> reviewIds = reviewPage.getContent().stream().map(Review::getId).toList();
        Map<UUID, Long> helpfulCountMap = new HashMap<>();
        if (!reviewIds.isEmpty()) {
            List<Object[]> counts = reviewHelpfulRepository.countByReviewIds(reviewIds);
            for (Object[] row : counts) {
                UUID rId = (UUID) row[0];
                Long count = (Long) row[1];
                helpfulCountMap.put(rId, count);
            }
        }

        // 7. Map to DTOs
        List<SellerReviewResponse> content = reviewPage.getContent().stream()
                .map(r -> SellerReviewResponse.from(r, helpfulCountMap.getOrDefault(r.getId(), 0L)))
                .toList();

        return PageResponse.from(reviewPage, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SellerReviewResponse respondToReview(UUID userId, UUID reviewId, SellerResponseRequest request) {
        log.info("Responding to review: userId={}, reviewId={}", userId, reviewId);

        // 1. Validate Seller Profile (BR-20.28)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Access denied.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        User user = seller.getUser();
        if (user != null) {
            if (user.getStatus() == UserStatus.BANNED) {
                log.warn("Seller user {} is BANNED", user.getId());
                throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
            }
        }

        // 2. Validate Shop (BR-20.28)
        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} is BANNED. Rejecting access.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 3. Fetch Review with Details (Zero N+1 Query on response mapping)
        Review review = reviewRepository.findWithDetailsById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 4. Validate Review soft-deleted status (BR-20.27)
        if (review.getDeletedAt() != null) {
            log.warn("Attempt to respond to soft-deleted review: reviewId={}", reviewId);
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 5. Multi-tenancy Isolation & IDOR Check (BR-20.26)
        OrderItem orderItem = review.getOrderItem();
        if (orderItem == null || orderItem.getOrder() == null || orderItem.getOrder().getShop() == null
                || !shop.getId().equals(orderItem.getOrder().getShop().getId())) {
            log.warn("IDOR attempt: Seller shop {} tried to respond to review {} belonging to another shop",
                    shop.getId(), reviewId);
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 6. Validate Seller Response Invariant: Only 1 response allowed (BR-20.25)
        if (review.getSellerResponse() != null) {
            log.warn("Review has already been responded to: reviewId={}", reviewId);
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_RESPONDED);
        }

        // 7. Atomic DB conditional update to prevent race conditions (BR-20.25, BR-20.29, BR-20.30)
        LocalDateTime now = LocalDateTime.now();
        String trimmedResponse = request.response().trim();
        int rowsUpdated = reviewRepository.updateSellerResponse(reviewId, trimmedResponse, now);
        if (rowsUpdated == 0) {
            log.warn("Concurrent response attempt or review already responded: reviewId={}", reviewId);
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_RESPONDED);
        }

        review.setSellerResponse(trimmedResponse);
        review.setSellerResponseAt(now);

        // Notify buyer that seller responded to their review
        if (review.getUser() != null) {
            String productName = (orderItem != null && orderItem.getProductNameSnapshot() != null)
                    ? " cho sản phẩm " + orderItem.getProductNameSnapshot()
                    : "";
            notificationDispatcher.dispatch(
                    review.getUser().getId(),
                    NotificationType.REVIEW,
                    "Shop đã phản hồi đánh giá của bạn",
                    "Shop " + shop.getName() + " vừa phản hồi đánh giá của bạn" + productName + ".",
                    "REVIEW",
                    review.getId()
            );
        }

        // 8. Fetch helpful count
        long helpfulCount = reviewHelpfulRepository.countByReviewId(reviewId);

        log.info("Review responded successfully: reviewId={}, sellerId={}", reviewId, seller.getId());
        return SellerReviewResponse.from(review, helpfulCount);
    }
}

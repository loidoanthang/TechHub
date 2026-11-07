package com.techhub.service.review.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.review.ProductRatingSummaryResponse;
import com.techhub.model.dto.response.review.ProductReviewResponse;
import com.techhub.model.entity.Review;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ReviewHelpfulRepository;
import com.techhub.repository.ReviewRepository;
import com.techhub.service.review.ProductReviewService;
import com.techhub.specification.ReviewSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getProductReviews(
            UUID productId,
            UUID currentUserId,
            Integer rating,
            Boolean hasMedia,
            String sortBy,
            Sort.Direction sortDirection,
            Pageable pageable
    ) {
        log.info("Fetching reviews for productId={}, currentUserId={}, rating={}, hasMedia={}, sortBy={}, page={}",
                productId, currentUserId, rating, hasMedia, sortBy, pageable.getPageNumber());

        // 1. Validate Product exists and not deleted (BR-20.31, EC-08)
        if (!productRepository.existsByIdAndIsDeletedFalse(productId)) {
            log.warn("Product not found or deleted: productId={}", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. Build Dynamic Specification
        Specification<Review> spec = Specification.where(ReviewSpecification.hasProductId(productId))
                .and(ReviewSpecification.notDeleted())
                .and(ReviewSpecification.hasRating(rating))
                .and(ReviewSpecification.hasMedia(hasMedia));

        Pageable effectivePageable = pageable;
        if ("helpful".equalsIgnoreCase(sortBy)) {
            spec = spec.and(ReviewSpecification.orderByHelpfulCount(sortDirection));
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }

        // 3. Query Reviews with Pagination & EntityGraph
        Page<Review> reviewPage = reviewRepository.findAll(spec, effectivePageable);

        // 4. Batch fetch Helpful Counts to eliminate N+1 queries
        List<UUID> reviewIds = reviewPage.getContent().stream().map(Review::getId).toList();
        Map<UUID, Long> helpfulCountMap = new HashMap<>();
        Set<UUID> votedReviewIds = Collections.emptySet();

        if (!reviewIds.isEmpty()) {
            List<Object[]> counts = reviewHelpfulRepository.countByReviewIds(reviewIds);
            for (Object[] row : counts) {
                UUID rId = (UUID) row[0];
                Long count = (Long) row[1];
                helpfulCountMap.put(rId, count);
            }

            // 5. Batch check Voted State for authenticated user (BR-20.36)
            if (currentUserId != null) {
                List<UUID> votedList = reviewHelpfulRepository.findVotedReviewIdsByUserIdAndReviewIds(currentUserId, reviewIds);
                votedReviewIds = new HashSet<>(votedList);
            }
        }

        // 6. Map to DTOs
        final Set<UUID> finalVotedReviewIds = votedReviewIds;
        List<ProductReviewResponse> content = reviewPage.getContent().stream()
                .map(r -> ProductReviewResponse.from(
                        r,
                        helpfulCountMap.getOrDefault(r.getId(), 0L),
                        finalVotedReviewIds.contains(r.getId())
                ))
                .toList();

        return PageResponse.from(reviewPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(UUID productId) {
        log.info("Fetching rating summary for productId={}", productId);

        // 1. Validate Product exists and not deleted (BR-20.32)
        if (!productRepository.existsByIdAndIsDeletedFalse(productId)) {
            log.warn("Product not found or deleted: productId={}", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. Query Aggregate Rating Summary
        List<Object[]> rows = reviewRepository.aggregateRatingSummaryByProductId(productId);
        if (rows.isEmpty()) {
            return ProductRatingSummaryResponse.empty(productId);
        }

        Object[] row = rows.get(0);
        long totalReviews = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        if (totalReviews == 0L) {
            return ProductRatingSummaryResponse.empty(productId);
        }

        double rawAvg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        double roundedAvg = BigDecimal.valueOf(rawAvg).setScale(1, RoundingMode.HALF_UP).doubleValue();

        long count5 = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        long count4 = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        long count3 = row[4] != null ? ((Number) row[4]).longValue() : 0L;
        long count2 = row[5] != null ? ((Number) row[5]).longValue() : 0L;
        long count1 = row[6] != null ? ((Number) row[6]).longValue() : 0L;

        // 3. Query Media Reviews Count
        long mediaReviewCount = reviewRepository.countMediaReviewsByProductId(productId);

        // 4. Build Map ratingCounts (guaranteed 1..5 keys)
        Map<Integer, Long> ratingCounts = new LinkedHashMap<>();
        ratingCounts.put(1, count1);
        ratingCounts.put(2, count2);
        ratingCounts.put(3, count3);
        ratingCounts.put(4, count4);
        ratingCounts.put(5, count5);

        return new ProductRatingSummaryResponse(
                productId,
                roundedAvg,
                totalReviews,
                mediaReviewCount,
                ratingCounts
        );
    }
}

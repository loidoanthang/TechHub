package com.techhub.model.dto.response.review;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ProductRatingSummaryResponse(
        UUID productId,
        double averageRating,
        long totalReviews,
        long mediaReviewCount,
        Map<Integer, Long> ratingCounts
) {
    public static ProductRatingSummaryResponse empty(UUID productId) {
        Map<Integer, Long> emptyCounts = new LinkedHashMap<>();
        emptyCounts.put(1, 0L);
        emptyCounts.put(2, 0L);
        emptyCounts.put(3, 0L);
        emptyCounts.put(4, 0L);
        emptyCounts.put(5, 0L);
        return new ProductRatingSummaryResponse(
                productId,
                0.0,
                0L,
                0L,
                emptyCounts
        );
    }
}

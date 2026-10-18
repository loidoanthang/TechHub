package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSlug,
        UUID variantId,
        String variantSku,
        String variantName,
        Map<String, Object> variantAttributes,
        BigDecimal price,
        int quantity,
        BigDecimal itemSubtotal,
        String imageUrl,
        int stockQuantity,
        boolean isOutOfStock,
        boolean isAvailable,
        String unavailableReason,
        LocalDateTime createdAt
) {}

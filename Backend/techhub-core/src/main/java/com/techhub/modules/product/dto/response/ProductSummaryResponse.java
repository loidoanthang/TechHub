package com.techhub.modules.product.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        String brandName,
        UUID categoryId,
        String categoryName,
        UUID shopId,
        String shopName,
        String sellerName,
        boolean isShopPaused,
        String coverImageUrl,
        BigDecimal price,
        boolean inStock,
        LocalDateTime createdAt
) {}

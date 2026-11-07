package com.techhub.model.dto.response.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record LowStockVariantDto(
        UUID variantId,
        UUID productId,
        String productName,
        String variantName,
        String sku,
        String imageUrl,
        int stockQuantity,
        BigDecimal price
) {}

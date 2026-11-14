package com.techhub.modules.dashboard.dto.response;

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

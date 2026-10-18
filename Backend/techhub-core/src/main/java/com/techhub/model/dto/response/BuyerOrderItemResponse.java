package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BuyerOrderItemResponse(
        UUID orderItemId,
        UUID productVariantId,
        String productName,
        String variantName,
        String sku,
        String imageUrl,
        BigDecimal originalPrice,
        int quantity,
        BigDecimal itemDiscountAmount,
        BigDecimal discountedUnitPrice,
        BigDecimal finalSubtotal
) {}

package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemPreviewResponse(
        UUID cartItemId,
        UUID productVariantId,
        String productName,
        String variantName,
        String imageUrl,
        BigDecimal originalPrice,
        int quantity,
        BigDecimal originalSubtotal,
        BigDecimal itemDiscountAmount,
        BigDecimal discountedUnitPrice,
        BigDecimal finalItemSubtotal
) {}

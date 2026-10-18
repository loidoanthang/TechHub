package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SellerOrderItemResponse(
        UUID orderItemId,
        UUID productVariantId,
        String productName,
        String variantName,
        String sku,
        String imageUrl,
        BigDecimal originalPrice,
        BigDecimal discountedUnitPrice,
        int quantity,
        BigDecimal finalSubtotal
) {}

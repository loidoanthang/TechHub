package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ReorderItemDto(
        UUID productVariantId,
        UUID productId,
        String productName,
        String variantName,
        String sku,
        String imageUrl,
        int quantityAdded,
        BigDecimal price
) {}

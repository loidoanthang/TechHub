package com.techhub.model.dto.response;

import com.techhub.model.enums.ProductVariantStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        String sku,
        String variantName,
        BigDecimal price,
        int stockQuantity,
        String imageUrl,
        Map<String, Object> attributes,
        ProductVariantStatus status
) {}

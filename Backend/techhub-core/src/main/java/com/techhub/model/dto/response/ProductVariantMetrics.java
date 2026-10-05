package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantMetrics(
        UUID productId,
        long variantsCount,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        long totalStock
) {}

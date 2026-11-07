package com.techhub.model.dto.response.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record TopSellingProductDto(
        UUID productId,
        String productName,
        String imageUrl,
        long unitsSold,
        BigDecimal totalRevenue
) {}

package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SellerShippingOptionResponse(
        UUID id,
        UUID shopId,
        String name,
        BigDecimal shippingFee,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

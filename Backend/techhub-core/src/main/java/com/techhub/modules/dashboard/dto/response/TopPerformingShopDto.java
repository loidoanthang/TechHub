package com.techhub.modules.dashboard.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TopPerformingShopDto(
        UUID shopId,
        String shopName,
        String logoUrl,
        long totalOrders,
        BigDecimal totalGmv,
        BigDecimal commissionContributed
) {}

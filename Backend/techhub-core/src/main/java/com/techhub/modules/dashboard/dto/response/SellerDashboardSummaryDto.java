package com.techhub.modules.dashboard.dto.response;

import java.math.BigDecimal;

public record SellerDashboardSummaryDto(
        BigDecimal netRevenue,
        long totalOrders,
        long completedOrders,
        long totalUnitsSold,
        BigDecimal walletBalance,
        BigDecimal pendingPayoutAmount,
        BigDecimal averageOrderValue
) {}

package com.techhub.model.dto.response.dashboard;

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

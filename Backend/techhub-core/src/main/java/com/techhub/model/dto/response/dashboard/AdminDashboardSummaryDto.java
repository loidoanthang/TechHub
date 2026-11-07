package com.techhub.model.dto.response.dashboard;

import java.math.BigDecimal;

public record AdminDashboardSummaryDto(
        BigDecimal totalGmv,
        BigDecimal completedGmv,
        BigDecimal platformCommissionRevenue,
        long totalOrders,
        long completedOrders,
        long totalUnitsSold,
        BigDecimal averageOrderValue,
        double cancellationRate,
        double disputeRate,
        BigDecimal totalSellersWalletBalance,
        BigDecimal pendingPayoutAmount,
        BigDecimal completedPayoutAmount
) {}

package com.techhub.modules.dashboard.dto.response;

import com.techhub.model.enums.DashboardPeriod;

import java.time.LocalDate;
import java.util.List;

public record SellerDashboardMetricsResponse(
        DashboardPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        SellerDashboardSummaryDto summary,
        SellerOrderStatusBreakdownDto orderStatusBreakdown,
        List<TopSellingProductDto> topSellingProducts,
        List<LowStockVariantDto> lowStockAlerts
) {}

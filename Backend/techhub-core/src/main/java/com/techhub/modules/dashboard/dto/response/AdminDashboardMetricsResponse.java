package com.techhub.modules.dashboard.dto.response;

import com.techhub.modules.dashboard.enums.DashboardPeriod;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardMetricsResponse(
        DashboardPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        AdminDashboardSummaryDto summary,
        AdminOrderStatusBreakdownDto orderStatusBreakdown,
        PlatformGrowthDto growthMetrics,
        List<TopPerformingShopDto> topPerformingShops
) {}

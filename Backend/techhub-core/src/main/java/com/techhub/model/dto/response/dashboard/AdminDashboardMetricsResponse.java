package com.techhub.model.dto.response.dashboard;

import com.techhub.model.enums.DashboardPeriod;

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

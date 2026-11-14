package com.techhub.service.dashboard;

import com.techhub.modules.dashboard.dto.response.SellerDashboardMetricsResponse;
import com.techhub.modules.dashboard.enums.DashboardPeriod;

import java.time.LocalDate;
import java.util.UUID;

public interface SellerDashboardService {

    SellerDashboardMetricsResponse getDashboardMetrics(
            UUID userId,
            DashboardPeriod period,
            LocalDate startDate,
            LocalDate endDate
    );
}

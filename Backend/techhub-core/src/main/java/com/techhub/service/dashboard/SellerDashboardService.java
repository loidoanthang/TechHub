package com.techhub.service.dashboard;

import com.techhub.model.dto.response.dashboard.SellerDashboardMetricsResponse;
import com.techhub.model.enums.DashboardPeriod;

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

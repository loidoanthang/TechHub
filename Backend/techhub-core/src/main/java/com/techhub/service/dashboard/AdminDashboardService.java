package com.techhub.service.dashboard;

import com.techhub.modules.dashboard.dto.response.AdminDashboardMetricsResponse;
import com.techhub.modules.dashboard.enums.DashboardPeriod;

import java.time.LocalDate;

public interface AdminDashboardService {

    AdminDashboardMetricsResponse getPlatformDashboardMetrics(
            DashboardPeriod period,
            LocalDate startDate,
            LocalDate endDate
    );
}

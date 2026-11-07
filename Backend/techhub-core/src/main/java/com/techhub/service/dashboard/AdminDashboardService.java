package com.techhub.service.dashboard;

import com.techhub.model.dto.response.dashboard.AdminDashboardMetricsResponse;
import com.techhub.model.enums.DashboardPeriod;

import java.time.LocalDate;

public interface AdminDashboardService {

    AdminDashboardMetricsResponse getPlatformDashboardMetrics(
            DashboardPeriod period,
            LocalDate startDate,
            LocalDate endDate
    );
}

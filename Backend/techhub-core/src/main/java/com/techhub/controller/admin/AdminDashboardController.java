package com.techhub.controller.admin;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.dashboard.AdminDashboardMetricsResponse;
import com.techhub.model.enums.DashboardPeriod;
import com.techhub.service.dashboard.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDashboardMetricsResponse> getPlatformDashboardMetrics(
            @RequestParam(required = false) DashboardPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(
                period, startDate, endDate);
        return ApiResponse.success(response, "Lấy chỉ số vận hành toàn sàn thành công", null);
    }
}

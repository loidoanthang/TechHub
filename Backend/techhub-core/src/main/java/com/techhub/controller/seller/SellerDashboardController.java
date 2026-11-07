package com.techhub.controller.seller;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.dashboard.SellerDashboardMetricsResponse;
import com.techhub.model.enums.DashboardPeriod;
import com.techhub.security.SecurityUtils;
import com.techhub.service.dashboard.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/dashboard")
@RequiredArgsConstructor
@Slf4j
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerDashboardMetricsResponse> getDashboardMetrics(
            @RequestParam(required = false) DashboardPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(
                currentUserId, period, startDate, endDate);
        return ApiResponse.success(response, "Lấy chỉ số vận hành gian hàng thành công", null);
    }
}

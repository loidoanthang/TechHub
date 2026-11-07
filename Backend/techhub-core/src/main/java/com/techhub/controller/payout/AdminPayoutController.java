package com.techhub.controller.payout;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.payout.ProcessPayoutRequest;
import com.techhub.model.dto.response.payout.AdminPayoutResponse;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.security.SecurityUtils;
import com.techhub.service.payout.AdminPayoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payouts")
@RequiredArgsConstructor
@Validated
public class AdminPayoutController {

    private final AdminPayoutService adminPayoutService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminPayoutResponse>> getPayoutRequests(
            @RequestParam(required = false) PayoutStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must not be negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size must not exceed 100") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PageResponse<AdminPayoutResponse> data = adminPayoutService.getPayoutRequests(
                status, keyword, startDate, endDate, page, size, sortBy, direction
        );
        return ApiResponse.success(data, "Payout requests retrieved successfully", null);
    }

    @PatchMapping("/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminPayoutResponse> processPayoutRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ProcessPayoutRequest request
    ) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        AdminPayoutResponse data = adminPayoutService.processPayoutRequest(id, adminId, request);
        return ApiResponse.success(data, "Payout request processed successfully", null);
    }
}

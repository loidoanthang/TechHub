package com.techhub.controller.order;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.AdminOrderDetailResponse;
import com.techhub.model.dto.response.AdminOrderResponse;
import com.techhub.model.enums.OrderStatus;
import com.techhub.security.SecurityUtils;
import com.techhub.service.order.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminOrderResponse>> getAdminOrders(
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                currentUserId, shopId, userId, status, keyword, startDate, endDate, page, size, sortBy, sortDir
        );
        return ApiResponse.success(response, "Fetched platform orders successfully", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminOrderDetailResponse> getAdminOrderDetail(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(currentUserId, id);
        return ApiResponse.success(response, "Fetched platform order details successfully", null);
    }
}

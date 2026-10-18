package com.techhub.controller.order;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.UpdateSellerOrderStatusRequest;
import com.techhub.model.dto.response.SellerOrderDetailResponse;
import com.techhub.model.dto.response.SellerOrderResponse;
import com.techhub.model.enums.OrderStatus;
import com.techhub.security.SecurityUtils;
import com.techhub.service.order.SellerOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<PageResponse<SellerOrderResponse>> getSellerOrders(
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
        PageResponse<SellerOrderResponse> response = sellerOrderService.getSellerOrders(
                currentUserId, status, keyword, startDate, endDate, page, size, sortBy, sortDir
        );
        return ApiResponse.success(response, "Fetched seller orders successfully", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerOrderDetailResponse> getSellerOrderDetail(
            @PathVariable UUID id
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(currentUserId, id);
        return ApiResponse.success(response, "Fetched seller order details successfully", null);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerOrderDetailResponse> updateSellerOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSellerOrderStatusRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        SellerOrderDetailResponse response = sellerOrderService.updateSellerOrderStatus(currentUserId, id, request);
        return ApiResponse.success(response, "Updated order fulfillment status successfully", null);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerOrderDetailResponse> cancelSellerOrder(
            @PathVariable UUID id,
            @Valid @RequestBody com.techhub.model.dto.request.CancelOrderRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        SellerOrderDetailResponse response = sellerOrderService.cancelSellerOrder(currentUserId, id, request);
        return ApiResponse.success(response, "Hủy đơn hàng thành công", null);
    }

    @GetMapping("/cancellation-requests")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<PageResponse<com.techhub.model.dto.response.CancellationRequestResponse>> getSellerCancellationRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PageResponse<com.techhub.model.dto.response.CancellationRequestResponse> response =
                sellerOrderService.getSellerCancellationRequests(currentUserId, page, size);
        return ApiResponse.success(response, "Fetched seller cancellation requests successfully", null);
    }

    @PatchMapping("/cancellation-requests/{id}/process")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<com.techhub.model.dto.response.CancellationRequestResponse> processCancellationRequest(
            @PathVariable UUID id,
            @Valid @RequestBody com.techhub.model.dto.request.ProcessCancellationRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        com.techhub.model.dto.response.CancellationRequestResponse response =
                sellerOrderService.processCancellationRequest(currentUserId, id, request);
        return ApiResponse.success(response, "Xử lý yêu cầu hủy đơn thành công", null);
    }
}

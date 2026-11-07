package com.techhub.controller.order;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.BuyerOrderDetailResponse;
import com.techhub.model.dto.response.BuyerOrderResponse;
import com.techhub.model.enums.OrderStatus;
import com.techhub.security.SecurityUtils;
import com.techhub.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import com.techhub.model.dto.request.CreateCancellationRequest;
import com.techhub.model.dto.response.CancellationRequestResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/buyer/orders")
@RequiredArgsConstructor
public class BuyerOrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PageResponse<BuyerOrderResponse>> getBuyerOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                currentUserId, status, keyword, page, size, sortBy, sortDir
        );
        return ApiResponse.success(response, "Fetched buyer orders successfully", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuyerOrderDetailResponse> getBuyerOrderDetail(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        BuyerOrderDetailResponse response = orderService.getBuyerOrderDetail(currentUserId, id);
        return ApiResponse.success(response, "Fetched buyer order details successfully", null);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuyerOrderDetailResponse> completeOrder(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        BuyerOrderDetailResponse response = orderService.completeOrder(currentUserId, id);
        return ApiResponse.success(response, "Order completed successfully", null);
    }

    @PostMapping("/{id}/cancellation-request")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<CancellationRequestResponse> requestOrderCancellation(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCancellationRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        CancellationRequestResponse response = orderService.requestOrderCancellation(currentUserId, id, request);
        return ApiResponse.success(response, "Gửi yêu cầu hủy đơn hàng thành công. Vui lòng chờ Người bán phản hồi.", null);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuyerOrderDetailResponse> cancelBuyerOrder(
            @PathVariable UUID id,
            @Valid @RequestBody com.techhub.model.dto.request.CancelOrderRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        BuyerOrderDetailResponse response = orderService.cancelBuyerOrder(currentUserId, id, request);
        return ApiResponse.success(response, "Hủy đơn hàng thành công", null);
    }

    @PostMapping("/{id}/reorder")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<com.techhub.model.dto.response.ReorderResponse> reorder(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        com.techhub.model.dto.response.ReorderResponse response = orderService.reorder(currentUserId, id);
        return ApiResponse.success(response, "Xử lý mua lại đơn hàng thành công", null);
    }
}

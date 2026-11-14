package com.techhub.service.order;

import com.techhub.common.PageResponse;
import com.techhub.modules.seller.dto.request.UpdateSellerOrderStatusRequest;
import com.techhub.modules.seller.dto.response.SellerOrderDetailResponse;
import com.techhub.modules.seller.dto.response.SellerOrderResponse;
import com.techhub.modules.order.enums.OrderStatus;

import java.time.LocalDate;
import java.util.UUID;

public interface SellerOrderService {

    PageResponse<SellerOrderResponse> getSellerOrders(
            UUID userId,
            OrderStatus status,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    SellerOrderDetailResponse getSellerOrderDetail(UUID userId, UUID orderId);

    SellerOrderDetailResponse updateSellerOrderStatus(UUID userId, UUID orderId, UpdateSellerOrderStatusRequest request);

    SellerOrderDetailResponse cancelSellerOrder(UUID userId, UUID orderId, com.techhub.modules.order.dto.request.CancelOrderRequest request);

    com.techhub.modules.order.dto.response.CancellationRequestResponse processCancellationRequest(UUID userId, UUID requestId, com.techhub.modules.order.dto.request.ProcessCancellationRequest request);

    PageResponse<com.techhub.modules.order.dto.response.CancellationRequestResponse> getSellerCancellationRequests(UUID userId, int page, int size);
}

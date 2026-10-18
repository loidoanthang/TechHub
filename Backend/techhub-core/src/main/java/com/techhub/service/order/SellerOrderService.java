package com.techhub.service.order;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.UpdateSellerOrderStatusRequest;
import com.techhub.model.dto.response.SellerOrderDetailResponse;
import com.techhub.model.dto.response.SellerOrderResponse;
import com.techhub.model.enums.OrderStatus;

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

    SellerOrderDetailResponse cancelSellerOrder(UUID userId, UUID orderId, com.techhub.model.dto.request.CancelOrderRequest request);

    com.techhub.model.dto.response.CancellationRequestResponse processCancellationRequest(UUID userId, UUID requestId, com.techhub.model.dto.request.ProcessCancellationRequest request);

    PageResponse<com.techhub.model.dto.response.CancellationRequestResponse> getSellerCancellationRequests(UUID userId, int page, int size);
}

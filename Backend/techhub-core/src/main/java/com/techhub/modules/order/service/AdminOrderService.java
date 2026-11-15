package com.techhub.modules.order.service;

import com.techhub.common.PageResponse;
import com.techhub.modules.order.dto.request.CancelOrderRequest;
import com.techhub.modules.order.dto.response.AdminOrderDetailResponse;
import com.techhub.modules.order.dto.response.AdminOrderResponse;
import com.techhub.modules.order.enums.OrderStatus;

import java.time.LocalDate;
import java.util.UUID;

public interface AdminOrderService {

    PageResponse<AdminOrderResponse> getAdminOrders(
            UUID currentUserId,
            UUID shopId,
            UUID buyerId,
            OrderStatus status,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    AdminOrderDetailResponse getAdminOrderDetail(UUID currentUserId, UUID orderId);

    AdminOrderDetailResponse cancelAdminOrder(UUID currentUserId, UUID orderId, CancelOrderRequest request);
}

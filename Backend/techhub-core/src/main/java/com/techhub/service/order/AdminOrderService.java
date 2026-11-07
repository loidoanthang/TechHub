package com.techhub.service.order;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CancelOrderRequest;
import com.techhub.model.dto.response.AdminOrderDetailResponse;
import com.techhub.model.dto.response.AdminOrderResponse;
import com.techhub.model.enums.OrderStatus;

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

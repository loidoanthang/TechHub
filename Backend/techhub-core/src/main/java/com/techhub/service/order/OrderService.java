package com.techhub.service.order;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.BuyerOrderDetailResponse;
import com.techhub.model.dto.response.BuyerOrderResponse;
import com.techhub.model.enums.OrderStatus;

import java.util.UUID;

public interface OrderService {

    PageResponse<BuyerOrderResponse> getBuyerOrders(
            UUID userId,
            OrderStatus status,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    BuyerOrderDetailResponse getBuyerOrderDetail(UUID userId, UUID orderId);

    BuyerOrderDetailResponse completeOrder(UUID userId, UUID orderId);

    com.techhub.model.dto.response.CancellationRequestResponse requestOrderCancellation(
            UUID userId,
            UUID orderId,
            com.techhub.model.dto.request.CreateCancellationRequest request
    );

    BuyerOrderDetailResponse cancelBuyerOrder(
            UUID userId,
            UUID orderId,
            com.techhub.model.dto.request.CancelOrderRequest request
    );

    com.techhub.model.dto.response.ReorderResponse reorder(UUID userId, UUID orderId);
}

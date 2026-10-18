package com.techhub.model.dto.response;

import com.techhub.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminOrderDetailResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        String statusDescription,
        AdminOrderShopResponse shop,
        AdminOrderBuyerResponse buyer,
        AddressSnapshotResponse deliveryAddress,
        List<AdminOrderItemResponse> items,
        int totalItemsCount,
        String note,
        BigDecimal totalProductAmount,
        BigDecimal shippingFee,
        BigDecimal shopDiscountAmount,
        BigDecimal platformDiscountAmount,
        BigDecimal totalCommission,
        BigDecimal totalSellerEarnings,
        BigDecimal buyerTotalAmount,
        AdminOrderPaymentResponse paymentInfo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deliveredAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        List<OrderTimelineStepResponse> timeline,
        List<String> availableActions
) {}

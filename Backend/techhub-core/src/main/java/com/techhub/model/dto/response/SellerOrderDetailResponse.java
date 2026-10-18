package com.techhub.model.dto.response;

import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SellerOrderDetailResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        String statusDescription,
        UUID shopId,
        String shopName,
        SellerOrderBuyerResponse buyerInfo,
        AddressSnapshotResponse deliveryAddress,
        List<SellerOrderItemResponse> items,
        int totalItemsCount,
        String note,
        BigDecimal totalProductAmount,
        BigDecimal shippingFee,
        BigDecimal shopDiscountAmount,
        BigDecimal platformDiscountAmount,
        BigDecimal totalCommission,
        BigDecimal totalSellerEarnings,
        BigDecimal buyerTotalAmount,
        PaymentMethod paymentMethod,
        String paymentMethodName,
        PaymentStatus paymentStatus,
        String paymentStatusDescription,
        LocalDateTime createdAt,
        LocalDateTime deliveredAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        List<OrderTimelineStepResponse> timeline,
        List<String> availableActions
) {}

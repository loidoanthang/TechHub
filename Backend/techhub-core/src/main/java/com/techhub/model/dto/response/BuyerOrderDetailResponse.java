package com.techhub.model.dto.response;

import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BuyerOrderDetailResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        String statusDescription,
        UUID shopId,
        String shopName,
        String shopSlug,
        String shopAvatarUrl,
        AddressSnapshotResponse deliveryAddress,
        List<BuyerOrderItemResponse> items,
        int totalItemsCount,
        BigDecimal totalProductAmount,
        BigDecimal shippingFee,
        BigDecimal shopDiscountAmount,
        BigDecimal platformDiscountAmount,
        BigDecimal totalAmount,
        String note,
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

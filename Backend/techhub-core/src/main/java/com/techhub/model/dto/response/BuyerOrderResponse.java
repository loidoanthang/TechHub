package com.techhub.model.dto.response;

import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BuyerOrderResponse(
        UUID orderId,
        String orderNumber,
        UUID shopId,
        String shopName,
        String shopSlug,
        OrderStatus status,
        String statusDescription,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal totalProductAmount,
        BigDecimal shippingFee,
        BigDecimal shopDiscountAmount,
        BigDecimal platformDiscountAmount,
        BigDecimal totalAmount,
        int totalItemsCount,
        List<BuyerOrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime deliveredAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        List<String> availableActions
) {}

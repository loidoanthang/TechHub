package com.techhub.modules.order.dto.response;

import com.techhub.modules.order.enums.OrderStatus;
import com.techhub.modules.payment.enums.PaymentMethod;
import com.techhub.modules.payment.enums.PaymentStatus;

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
        UUID paymentId,
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

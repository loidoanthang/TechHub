package com.techhub.model.dto.response;

import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminOrderResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        String statusDescription,
        AdminOrderShopResponse shop,
        AdminOrderBuyerResponse buyer,
        int totalItemsCount,
        List<AdminOrderItemResponse> items,
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
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> availableActions
) {}

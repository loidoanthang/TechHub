package com.techhub.model.dto.response;

import com.techhub.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID orderId,
        String orderNumber,
        UUID shopId,
        String shopName,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        int itemsCount
) {}

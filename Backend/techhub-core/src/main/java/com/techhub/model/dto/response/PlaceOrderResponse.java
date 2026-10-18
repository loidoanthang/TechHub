package com.techhub.model.dto.response;

import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PlaceOrderResponse(
        UUID paymentId,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal finalTotalAmount,
        int totalOrdersCount,
        List<OrderSummaryResponse> orders,
        String paymentUrl,
        String bankCode,
        LocalDateTime createdAt
) {
    public PlaceOrderResponse(
            UUID paymentId,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            BigDecimal finalTotalAmount,
            int totalOrdersCount,
            List<OrderSummaryResponse> orders,
            String paymentUrl,
            LocalDateTime createdAt
    ) {
        this(paymentId, paymentMethod, paymentStatus, finalTotalAmount, totalOrdersCount, orders, paymentUrl, null, createdAt);
    }
}

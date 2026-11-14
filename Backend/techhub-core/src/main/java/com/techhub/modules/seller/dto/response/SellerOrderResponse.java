package com.techhub.modules.seller.dto.response;

import com.techhub.modules.order.enums.OrderStatus;
import com.techhub.modules.payment.enums.PaymentMethod;
import com.techhub.modules.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SellerOrderResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        String statusDescription,
        SellerOrderBuyerResponse buyerInfo,
        int totalItemsCount,
        List<SellerOrderItemResponse> items,
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

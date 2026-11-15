package com.techhub.modules.order.dto.response;

import com.techhub.modules.payment.enums.PaymentMethod;
import com.techhub.modules.payment.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminOrderPaymentResponse(
        UUID paymentId,
        PaymentMethod paymentMethod,
        String paymentMethodName,
        PaymentStatus paymentStatus,
        String paymentStatusDescription,
        String transactionId,
        LocalDateTime paidAt
) {}

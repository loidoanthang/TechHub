package com.techhub.model.dto.response;

import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;

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

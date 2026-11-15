package com.techhub.modules.payment.dto.response;

import com.techhub.modules.payment.enums.PaymentMethod;
import com.techhub.modules.payment.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record BuyerPaymentStatusResponse(
        UUID paymentId,
        PaymentMethod paymentMethod,
        String bankCode,
        String transactionId,
        PaymentStatus status,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean isExpired,
        List<UUID> orderIds,
        List<String> orderNumbers
) {}

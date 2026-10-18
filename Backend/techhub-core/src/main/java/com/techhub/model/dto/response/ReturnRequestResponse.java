package com.techhub.model.dto.response;

import com.techhub.model.enums.ReturnRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReturnRequestResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        UUID shopId,
        String shopName,
        ReturnRequestStatus status,
        String statusDescription,
        String reason,
        String proofImages,
        BigDecimal refundAmount,
        String sellerNote,
        String bankName,
        String bankAccountNumber,
        String bankAccountHolder,
        LocalDateTime createdAt,
        LocalDateTime respondedAt
) {}

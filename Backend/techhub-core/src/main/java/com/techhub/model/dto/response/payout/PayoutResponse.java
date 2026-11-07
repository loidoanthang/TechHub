package com.techhub.model.dto.response.payout;

import com.techhub.model.entity.SellerPayoutRequest;
import com.techhub.model.enums.PayoutStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PayoutResponse(
        UUID id,
        UUID sellerId,
        String shopName,
        BigDecimal amount,
        String bankName,
        String bankAccountNumber,
        String bankAccountHolder,
        PayoutStatus status,
        String rejectionReason,
        BigDecimal remainingBalance,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
    public static PayoutResponse from(SellerPayoutRequest request, BigDecimal remainingBalance, String shopName) {
        String maskedAccount = maskAccountNumber(request.getBankAccountNumber());
        return PayoutResponse.builder()
                .id(request.getId())
                .sellerId(request.getSeller() != null ? request.getSeller().getId() : null)
                .shopName(shopName)
                .amount(request.getAmount())
                .bankName(request.getBankName())
                .bankAccountNumber(maskedAccount)
                .bankAccountHolder(request.getBankAccountHolder())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .remainingBalance(remainingBalance)
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .build();
    }

    public static String maskAccountNumber(String accNumber) {
        if (accNumber == null || accNumber.isBlank()) {
            return "******";
        }
        String trimmed = accNumber.trim();
        if (trimmed.length() <= 4) {
            return "****" + trimmed;
        }
        return "*".repeat(trimmed.length() - 4) + trimmed.substring(trimmed.length() - 4);
    }
}

package com.techhub.model.dto.response;

import com.techhub.model.entity.UserPaymentInfo;
import com.techhub.model.enums.PaymentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentInfoResponse(
        UUID id,
        PaymentType paymentType,
        String provider,
        String accountNumber,
        String accountHolderName,
        String cardExpiry,
        boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentInfoResponse from(UserPaymentInfo info) {
        return new PaymentInfoResponse(
                info.getId(),
                info.getPaymentType(),
                info.getProvider(),
                maskAccountNumber(info.getAccountNumber()),
                info.getAccountHolderName(),
                info.getCardExpiry(),
                info.isDefault(),
                info.getCreatedAt(),
                info.getUpdatedAt()
        );
    }

    public static String maskAccountNumber(String rawNumber) {
        if (rawNumber == null || rawNumber.isBlank()) {
            return "";
        }
        int len = rawNumber.length();
        if (len <= 4) {
            return "*".repeat(len);
        }
        String lastFour = rawNumber.substring(len - 4);
        int maskLen = Math.max(6, len - 4);
        return "*".repeat(maskLen) + lastFour;
    }
}

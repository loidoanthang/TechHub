package com.techhub.modules.payment.dto.response;

import com.techhub.modules.payment.enums.PaymentInfoStatus;
import com.techhub.modules.payment.enums.PaymentType;
import com.techhub.modules.user.entity.UserPaymentInfo;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentInfoResponse(
        UUID id,
        PaymentType paymentType,
        String bankCode,
        String cardMask,
        String cardType,
        String accountHolderName,
        PaymentInfoStatus status,
        boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentInfoResponse from(UserPaymentInfo info) {
        return new PaymentInfoResponse(
                info.getId(),
                info.getPaymentType(),
                info.getBankCode(),
                info.getCardMask(),
                info.getCardType(),
                info.getAccountHolderName(),
                info.getStatus(),
                info.isDefault(),
                info.getCreatedAt(),
                info.getUpdatedAt()
        );
    }
}

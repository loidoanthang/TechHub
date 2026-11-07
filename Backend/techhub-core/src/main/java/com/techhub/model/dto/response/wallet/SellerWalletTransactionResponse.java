package com.techhub.model.dto.response.wallet;

import com.techhub.model.entity.SellerWalletTransaction;
import com.techhub.model.enums.SellerWalletTransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record SellerWalletTransactionResponse(
        UUID id,
        SellerWalletTransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        UUID referenceId,
        String description,
        LocalDateTime createdAt
) {
    public static SellerWalletTransactionResponse from(SellerWalletTransaction tx) {
        return SellerWalletTransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())
                .referenceId(tx.getReferenceId())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}

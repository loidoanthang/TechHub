package com.techhub.model.dto.response.payout;

import com.techhub.model.entity.SellerPayoutRequest;
import com.techhub.model.enums.PayoutStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AdminPayoutResponse(
        UUID id,
        UUID sellerId,
        String sellerName,
        String shopName,
        BigDecimal amount,
        String bankName,
        String bankAccountNumber,
        String bankAccountHolder,
        PayoutStatus status,
        String rejectionReason,
        UUID processedById,
        String processedByName,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminPayoutResponse from(SellerPayoutRequest request, String shopName) {
        String adminName = null;
        UUID adminId = null;
        if (request.getProcessedBy() != null) {
            adminId = request.getProcessedBy().getId();
            String firstName = request.getProcessedBy().getFirstName();
            String lastName = request.getProcessedBy().getLastName();
            if (firstName != null && !firstName.isBlank()) {
                adminName = (lastName != null && !lastName.isBlank()) ? lastName + " " + firstName : firstName;
            } else {
                adminName = request.getProcessedBy().getEmail();
            }
        }

        return AdminPayoutResponse.builder()
                .id(request.getId())
                .sellerId(request.getSeller() != null ? request.getSeller().getId() : null)
                .sellerName(request.getSeller() != null ? request.getSeller().getSellerName() : null)
                .shopName(shopName)
                .amount(request.getAmount())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankAccountHolder(request.getBankAccountHolder())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .processedById(adminId)
                .processedByName(adminName)
                .processedAt(request.getProcessedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}

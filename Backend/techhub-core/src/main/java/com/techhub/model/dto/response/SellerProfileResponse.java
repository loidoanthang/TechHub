package com.techhub.model.dto.response;

import com.techhub.model.enums.SellerVerificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerProfileResponse(
        UUID sellerId,
        String sellerName,
        SellerVerificationStatus verificationStatus,
        String bankName,
        String bankAccountNumber,
        String bankAccountHolder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

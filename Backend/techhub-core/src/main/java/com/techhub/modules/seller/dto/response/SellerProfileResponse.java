package com.techhub.modules.seller.dto.response;

import com.techhub.modules.seller.enums.SellerVerificationStatus;

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

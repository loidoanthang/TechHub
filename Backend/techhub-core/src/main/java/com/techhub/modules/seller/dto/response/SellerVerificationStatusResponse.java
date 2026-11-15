package com.techhub.modules.seller.dto.response;

import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.shop.enums.ShopStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerVerificationStatusResponse(
        boolean hasApplied,
        UUID sellerId,
        UUID shopId,
        String sellerName,
        String shopName,
        SellerVerificationStatus verificationStatus,
        ShopStatus shopStatus,
        String rejectionReason,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt
) {
    public static SellerVerificationStatusResponse notApplied() {
        return new SellerVerificationStatusResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}

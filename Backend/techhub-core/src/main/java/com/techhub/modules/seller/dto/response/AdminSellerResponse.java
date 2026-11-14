package com.techhub.modules.seller.dto.response;

import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.shop.enums.ShopStatus;
import com.techhub.modules.user.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminSellerResponse(
        UUID id,
        UUID userId,
        String ownerName,
        String ownerEmail,
        String ownerPhone,
        UserStatus ownerStatus,
        UUID shopId,
        String shopName,
        ShopStatus shopStatus,
        String sellerName,
        SellerVerificationStatus verificationStatus,
        String rejectionReason,
        String bankName,
        String bankAccountNumber,
        String bankAccountHolder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

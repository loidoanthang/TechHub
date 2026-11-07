package com.techhub.model.dto.response;

import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminShopResponse(
        UUID id,
        String name,
        String description,
        String avatarUrl,
        String bannerUrl,
        ShopStatus status,
        UUID sellerId,
        String sellerName,
        SellerVerificationStatus sellerVerificationStatus,
        UUID ownerId,
        String ownerName,
        String ownerEmail,
        String ownerPhone,
        UserStatus ownerStatus,
        BigDecimal balance,
        Long productCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

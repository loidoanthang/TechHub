package com.techhub.modules.shop.dto.response;

import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.shop.enums.ShopStatus;
import com.techhub.modules.user.enums.UserStatus;

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

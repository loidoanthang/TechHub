package com.techhub.modules.seller.dto.response;

import com.techhub.modules.shop.enums.ShopStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerShopResponse(
        UUID shopId,
        UUID sellerId,
        String sellerName,
        String name,
        String description,
        String avatarUrl,
        String bannerUrl,
        ShopStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

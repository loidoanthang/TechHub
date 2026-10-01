package com.techhub.model.dto.response;

import com.techhub.model.enums.ShopStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PublicShopResponse(
        UUID id,
        String sellerName,
        String name,
        String description,
        String avatarUrl,
        String bannerUrl,
        ShopStatus status,
        String warehouseProvince,
        String warehouseDistrict,
        LocalDateTime createdAt
) {
}

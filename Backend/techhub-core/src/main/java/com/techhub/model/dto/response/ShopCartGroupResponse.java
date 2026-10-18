package com.techhub.model.dto.response;

import com.techhub.model.enums.ShopStatus;

import java.util.List;
import java.util.UUID;

public record ShopCartGroupResponse(
        UUID shopId,
        String shopName,
        String sellerName,
        ShopStatus shopStatus,
        boolean isShopPaused,
        List<CartItemResponse> items
) {}

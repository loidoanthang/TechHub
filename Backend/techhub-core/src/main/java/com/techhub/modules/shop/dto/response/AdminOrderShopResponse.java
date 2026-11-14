package com.techhub.modules.shop.dto.response;

import java.util.UUID;

public record AdminOrderShopResponse(
        UUID shopId,
        String shopName,
        String shopSlug,
        String sellerEmail,
        String sellerPhone
) {}

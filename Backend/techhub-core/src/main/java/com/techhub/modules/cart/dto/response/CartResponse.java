package com.techhub.modules.cart.dto.response;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        int totalItemTypes,
        int totalQuantity,
        List<ShopCartGroupResponse> shops
) {}

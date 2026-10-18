package com.techhub.model.dto.response;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        int totalItemTypes,
        int totalQuantity,
        List<ShopCartGroupResponse> shops
) {}

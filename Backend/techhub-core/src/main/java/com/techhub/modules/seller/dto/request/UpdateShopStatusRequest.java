package com.techhub.modules.seller.dto.request;

import com.techhub.model.enums.ShopStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateShopStatusRequest(
        @NotNull(message = "Shop status is required")
        ShopStatus status
) {
}

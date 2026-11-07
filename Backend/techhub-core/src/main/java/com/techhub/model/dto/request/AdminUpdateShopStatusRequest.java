package com.techhub.model.dto.request;

import com.techhub.model.enums.ShopStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUpdateShopStatusRequest(
        @NotNull(message = "Shop status is required")
        ShopStatus status,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}

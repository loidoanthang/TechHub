package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ShopOrderRequest(
        @NotNull(message = "Shop ID is required")
        UUID shopId,

        @Size(max = 2, message = "Maximum 2 vouchers per shop allowed")
        List<@NotNull(message = "Shop voucher ID cannot be null") UUID> shopVoucherIds,

        @Size(max = 500, message = "Note cannot exceed 500 characters")
        String note
) {
    public ShopOrderRequest(UUID shopId, List<UUID> shopVoucherIds) {
        this(shopId, shopVoucherIds, null);
    }
}

package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CheckoutPreviewRequest(
        @NotEmpty(message = "Selected cart item IDs cannot be empty")
        @Size(max = 50, message = "Selected cart items cannot exceed 50")
        List<@NotNull(message = "Selected cart item ID cannot be null") UUID> selectedCartItemIds,

        UUID addressId,

        @Size(max = 50, message = "Shop voucher IDs cannot exceed 50")
        List<@NotNull(message = "Shop voucher ID cannot be null") UUID> shopVoucherIds,

        UUID platformVoucherId,

        UUID platformShippingVoucherId
) {
    public CheckoutPreviewRequest(List<UUID> selectedCartItemIds, UUID addressId, List<UUID> shopVoucherIds, UUID platformVoucherId) {
        this(selectedCartItemIds, addressId, shopVoucherIds, platformVoucherId, null);
    }
}

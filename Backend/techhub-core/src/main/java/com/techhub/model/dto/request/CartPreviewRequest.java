package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CartPreviewRequest(
        @NotEmpty(message = "Selected cart item IDs cannot be empty")
        @Size(max = 50, message = "Selected cart items cannot exceed 50")
        List<@NotNull(message = "Selected cart item ID cannot be null") UUID> selectedCartItemIds,

        @Size(max = 50, message = "Shop voucher IDs cannot exceed 50")
        List<@NotNull(message = "Shop voucher ID cannot be null") UUID> shopVoucherIds,

        UUID platformVoucherId,

        UUID platformShippingVoucherId
) {
    public CartPreviewRequest(List<UUID> selectedCartItemIds, List<UUID> shopVoucherIds, UUID platformVoucherId) {
        this(selectedCartItemIds, shopVoucherIds, platformVoucherId, null);
    }
}

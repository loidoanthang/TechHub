package com.techhub.model.dto.request;

import com.techhub.model.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull(message = "Address ID is required")
        UUID addressId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String bankCode,

        @NotEmpty(message = "Selected cart item IDs cannot be empty")
        @Size(max = 50, message = "Selected cart items cannot exceed 50")
        List<@NotNull(message = "Selected cart item ID cannot be null") UUID> selectedCartItemIds,

        @NotEmpty(message = "Shop orders cannot be empty")
        List<@NotNull(message = "Shop order cannot be null") @Valid ShopOrderRequest> shopOrders,

        UUID platformVoucherId,

        UUID platformShippingVoucherId,

        @Size(max = 64, message = "Idempotency key cannot exceed 64 characters")
        String idempotencyKey
) {
    public PlaceOrderRequest(
            UUID addressId,
            PaymentMethod paymentMethod,
            List<UUID> selectedCartItemIds,
            List<ShopOrderRequest> shopOrders
    ) {
        this(addressId, paymentMethod, null, selectedCartItemIds, shopOrders, null, null, null);
    }

    public PlaceOrderRequest(
            UUID addressId,
            PaymentMethod paymentMethod,
            List<UUID> selectedCartItemIds,
            List<ShopOrderRequest> shopOrders,
            UUID platformVoucherId,
            UUID platformShippingVoucherId,
            String idempotencyKey
    ) {
        this(addressId, paymentMethod, null, selectedCartItemIds, shopOrders, platformVoucherId, platformShippingVoucherId, idempotencyKey);
    }
}

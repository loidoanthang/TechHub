package com.techhub.modules.seller.dto.request;

import com.techhub.modules.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSellerOrderStatusRequest(
        @NotNull(message = "Order status is required")
        OrderStatus status,

        @Size(max = 500, message = "Note cannot exceed 500 characters")
        String note
) {}

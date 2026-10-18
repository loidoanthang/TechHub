package com.techhub.model.dto.request;

import com.techhub.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSellerOrderStatusRequest(
        @NotNull(message = "Order status is required")
        OrderStatus status,

        @Size(max = 500, message = "Note cannot exceed 500 characters")
        String note
) {}

package com.techhub.modules.product.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateProductSuspendRequest(
        @NotNull(message = "Suspension status is required")
        Boolean isSuspended
) {}

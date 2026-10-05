package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateProductSuspendRequest(
        @NotNull(message = "Suspension status is required")
        Boolean isSuspended
) {}

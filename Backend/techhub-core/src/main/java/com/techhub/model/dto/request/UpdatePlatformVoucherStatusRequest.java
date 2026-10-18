package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdatePlatformVoucherStatusRequest(
        @NotNull(message = "Active status is required")
        Boolean isActive
) {}

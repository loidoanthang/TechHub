package com.techhub.model.dto.request;

import com.techhub.model.enums.ProductVisibilityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProductVisibilityRequest(
        @NotNull(message = "Visibility status is required")
        ProductVisibilityStatus visibilityStatus
) {
}

package com.techhub.modules.product.dto.request;

import com.techhub.modules.product.enums.ProductVisibilityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProductVisibilityRequest(
        @NotNull(message = "Visibility status is required")
        ProductVisibilityStatus visibilityStatus
) {
}

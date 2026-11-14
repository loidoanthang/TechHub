package com.techhub.modules.category.dto.request;

import com.techhub.modules.category.enums.CategoryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCategoryStatusRequest(
        @NotNull(message = "Category status is required")
        CategoryStatus status
) {
}

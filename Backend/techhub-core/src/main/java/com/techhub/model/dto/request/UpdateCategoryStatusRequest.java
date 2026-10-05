package com.techhub.model.dto.request;

import com.techhub.model.enums.CategoryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCategoryStatusRequest(
        @NotNull(message = "Category status is required")
        CategoryStatus status
) {
}

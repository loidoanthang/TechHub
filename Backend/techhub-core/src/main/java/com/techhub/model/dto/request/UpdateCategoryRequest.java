package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) {
    public UpdateCategoryRequest {
        name = name != null ? name.trim() : null;
        description = (description != null && !description.trim().isEmpty()) ? description.trim() : null;
    }
}

package com.techhub.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotEmpty(message = "Category must have at least one attribute")
        @Size(min = 1, max = 50, message = "Category attributes must be between 1 and 50")
        @Valid
        List<CreateCategoryAttributeRequest> attributes
) {
    public CreateCategoryRequest {
        name = name != null ? name.trim() : null;
        description = (description != null && !description.trim().isEmpty()) ? description.trim() : null;
    }
}

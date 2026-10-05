package com.techhub.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductImageRequest(
        @NotBlank(message = "Image URL is required")
        @Size(max = 500, message = "Image URL cannot exceed 500 characters")
        String imageUrl,

        @NotNull(message = "Display order is required")
        @Min(value = 0, message = "Display order must be greater than or equal to 0")
        @Max(value = 5, message = "Display order cannot exceed 5")
        Integer displayOrder
) {}

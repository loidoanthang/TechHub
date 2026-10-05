package com.techhub.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateProductRequest(
        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotBlank(message = "Product name is required")
        @Size(min = 5, max = 300, message = "Product name must be between 5 and 300 characters")
        String name,

        @NotBlank(message = "Brand name is required")
        @Size(max = 100, message = "Brand name cannot exceed 100 characters")
        String brandName,

        String description,

        @NotNull(message = "Product specifications attributes are required")
        Map<String, Object> attributes,

        @NotNull(message = "Images are required")
        @Size(min = 1, max = 6, message = "Product must have between 1 and 6 images")
        @Valid
        List<CreateProductImageRequest> images
) {}

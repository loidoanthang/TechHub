package com.techhub.model.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record CreateProductVariantRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU cannot exceed 100 characters")
        String sku,

        @NotBlank(message = "Variant name is required")
        @Size(max = 200, message = "Variant name cannot exceed 200 characters")
        String variantName,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        @DecimalMax(value = "1000000000.00", message = "Price cannot exceed 1,000,000,000")
        BigDecimal price,

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        @Max(value = 1000000, message = "Stock quantity cannot exceed 1,000,000")
        Integer stockQuantity,

        @Size(max = 500, message = "Variant image URL cannot exceed 500 characters")
        String imageUrl,

        Map<String, Object> attributes
) {}

package com.techhub.model.dto.request;

import com.techhub.model.enums.CategoryAttributeDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCategoryAttributeRequest(
        @NotBlank(message = "Attribute name is required")
        @Size(min = 1, max = 100, message = "Attribute name must be between 1 and 100 characters")
        String name,

        @NotNull(message = "Data type is required")
        CategoryAttributeDataType dataType,

        List<String> options,

        @Size(max = 255, message = "Sample value cannot exceed 255 characters")
        String sampleValue
) {
    public UpdateCategoryAttributeRequest {
        name = name != null ? name.trim() : null;
        sampleValue = (sampleValue != null && !sampleValue.trim().isEmpty()) ? sampleValue.trim() : null;
    }
}

package com.techhub.model.dto.request.review;

import com.techhub.model.enums.ReviewMediaType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewMediaRequest(
        @NotBlank(message = "Media URL cannot be blank")
        @Size(max = 500, message = "Media URL cannot exceed 500 characters")
        String mediaUrl,

        @NotNull(message = "Media type is required")
        ReviewMediaType mediaType,

        @Min(value = 0, message = "Display order must be between 0 and 4")
        @Max(value = 4, message = "Display order must be between 0 and 4")
        Integer displayOrder
) {}

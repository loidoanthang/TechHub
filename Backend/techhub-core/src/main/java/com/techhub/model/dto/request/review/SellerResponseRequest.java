package com.techhub.model.dto.request.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerResponseRequest(
        @NotBlank(message = "Response content cannot be blank")
        @Size(min = 1, max = 1000, message = "Response content must be between 1 and 1000 characters")
        String response
) {}

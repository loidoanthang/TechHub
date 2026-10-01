package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSellerShopRequest(
        @NotBlank(message = "Shop name is required")
        @Size(min = 2, max = 200, message = "Shop name must be between 2 and 200 characters")
        String name,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
        String avatarUrl,

        @Size(max = 500, message = "Banner URL cannot exceed 500 characters")
        String bannerUrl
) {
}

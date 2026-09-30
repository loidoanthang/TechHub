package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^(0|\\+84)[35789][0-9]{8}$",
                message = "Invalid Vietnamese phone number format"
        )
        String phone,

        @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
        String avatarUrl
) {}

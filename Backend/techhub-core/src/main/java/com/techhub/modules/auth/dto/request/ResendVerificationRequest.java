package com.techhub.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(

        @Email(message = "Invalid email")
        @NotBlank(message = "Email is required")
        String email

) {
}

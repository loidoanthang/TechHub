package com.techhub.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(

        @Email(message = "Invalid email")
        @NotBlank(message = "Email is required")
        String email

) {
}

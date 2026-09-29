package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(

        @NotBlank(message = "Password is required")
        String password,

        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason

) {
}

package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSellerProfileRequest(
        @NotBlank(message = "Seller name is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Seller name must contain only lowercase letters, numbers, and hyphens")
        @Size(min = 3, max = 50, message = "Seller name must be between 3 and 50 characters")
        String sellerName,

        @NotBlank(message = "Bank name is required")
        @Size(min = 2, max = 100, message = "Bank name must be between 2 and 100 characters")
        String bankName,

        @NotBlank(message = "Bank account number is required")
        @Pattern(regexp = "^[0-9A-Za-z]+$", message = "Bank account number must contain only alphanumeric characters")
        @Size(min = 5, max = 50, message = "Bank account number must be between 5 and 50 characters")
        String bankAccountNumber,

        @NotBlank(message = "Bank account holder name is required")
        @Size(min = 2, max = 150, message = "Bank account holder name must be between 2 and 150 characters")
        String bankAccountHolder
) {
}

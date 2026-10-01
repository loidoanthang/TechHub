package com.techhub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWarehouseAddressRequest(
        @NotBlank(message = "Contact name is required")
        @Size(max = 150, message = "Contact name cannot exceed 150 characters")
        String contactName,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Phone must be a valid Vietnamese phone number (10 digits starting with 0 or +84)")
        String phone,

        @NotBlank(message = "Province is required")
        @Size(max = 100, message = "Province cannot exceed 100 characters")
        String province,

        @NotBlank(message = "District is required")
        @Size(max = 100, message = "District cannot exceed 100 characters")
        String district,

        @NotBlank(message = "Ward is required")
        @Size(max = 100, message = "Ward cannot exceed 100 characters")
        String ward,

        @NotBlank(message = "Street address is required")
        @Size(max = 500, message = "Street address cannot exceed 500 characters")
        String streetAddress
) {
}

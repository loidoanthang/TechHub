package com.techhub.model.dto.request;

import com.techhub.model.enums.AddressLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        AddressLabel label,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 150, message = "Recipient name cannot exceed 150 characters")
        String recipientName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^(0|\\+84)[35789][0-9]{8}$", message = "Invalid Vietnamese phone number")
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
        String streetAddress,

        Boolean isDefault
) {}

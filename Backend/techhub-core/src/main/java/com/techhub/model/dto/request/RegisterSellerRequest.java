package com.techhub.model.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record RegisterSellerRequest(
        @NotBlank(message = "Seller name is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Seller name must contain only lowercase letters, numbers, and hyphens")
        @Size(min = 3, max = 50, message = "Seller name must be between 3 and 50 characters")
        String sellerName,

        @NotBlank(message = "Shop name is required")
        @Size(min = 2, max = 200, message = "Shop name must be between 2 and 200 characters")
        String shopName,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String shopDescription,

        @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
        String avatarUrl,

        @Size(max = 500, message = "Banner URL cannot exceed 500 characters")
        String bannerUrl,

        @NotBlank(message = "Bank name is required")
        @Size(max = 100, message = "Bank name cannot exceed 100 characters")
        String bankName,

        @NotBlank(message = "Bank account number is required")
        @Pattern(regexp = "^[0-9]{6,30}$", message = "Bank account number must be between 6 and 30 digits")
        String bankAccountNumber,

        @NotBlank(message = "Bank account holder is required")
        @Size(max = 150, message = "Bank account holder cannot exceed 150 characters")
        String bankAccountHolder,

        @NotBlank(message = "Warehouse contact name is required")
        @Size(max = 150, message = "Warehouse contact name cannot exceed 150 characters")
        String warehouseContactName,

        @NotBlank(message = "Warehouse phone is required")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Warehouse phone must be a valid 10-digit Vietnamese phone number")
        String warehousePhone,

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

        @NotNull(message = "Shipping fee is required")
        @DecimalMin(value = "0.0", message = "Shipping fee must be greater than or equal to 0")
        BigDecimal shippingFee
) {}

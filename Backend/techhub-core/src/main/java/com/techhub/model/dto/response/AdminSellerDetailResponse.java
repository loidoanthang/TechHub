package com.techhub.model.dto.response;

import com.techhub.model.enums.Role;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AdminSellerDetailResponse(
        // 1. Seller Profile & Verification
        UUID id,
        String sellerName,
        SellerVerificationStatus verificationStatus,
        String rejectionReason,
        String bankName,
        String bankAccountNumber,
        String bankAccountHolder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        // 2. User / Owner Account Information
        UserDetailResponse user,

        // 3. Shop Commercial Profile
        ShopDetailResponse shop,

        // 4. Warehouse / Pickup Address
        WarehouseAddressResponse warehouseAddress,

        // 5. Shipping Option Configuration
        ShippingOptionResponse shippingOption
) {
    public record UserDetailResponse(
            UUID id,
            String firstName,
            String lastName,
            String fullName,
            String email,
            boolean emailVerified,
            String phone,
            String avatarUrl,
            UserStatus status,
            Set<Role> roles,
            LocalDateTime createdAt
    ) {}

    public record ShopDetailResponse(
            UUID id,
            String name,
            String description,
            String avatarUrl,
            String bannerUrl,
            ShopStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record WarehouseAddressResponse(
            UUID id,
            String contactName,
            String phone,
            String province,
            String district,
            String ward,
            String streetAddress,
            String fullAddress,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ShippingOptionResponse(
            UUID id,
            String name,
            BigDecimal shippingFee,
            boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}

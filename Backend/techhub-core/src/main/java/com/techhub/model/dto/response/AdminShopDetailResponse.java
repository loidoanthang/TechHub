package com.techhub.model.dto.response;

import com.techhub.model.enums.Role;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AdminShopDetailResponse(
        ShopCommercialResponse shop,
        SellerProfileResponse seller,
        OwnerAccountResponse owner,
        WarehouseAddressResponse warehouseAddress,
        ShippingOptionResponse shippingOption,
        ShopMetricsResponse metrics
) {
    public record ShopCommercialResponse(
            UUID id,
            String name,
            String description,
            String avatarUrl,
            String bannerUrl,
            ShopStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record SellerProfileResponse(
            UUID id,
            String sellerName,
            SellerVerificationStatus verificationStatus,
            String rejectionReason,
            String bankName,
            String bankAccountNumber,
            String bankAccountHolder,
            BigDecimal balance,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record OwnerAccountResponse(
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

    public record ShopMetricsResponse(
            Long totalProducts,
            Long activeProducts,
            Long totalCompletedOrders,
            BigDecimal cumulativeRevenue
    ) {}
}

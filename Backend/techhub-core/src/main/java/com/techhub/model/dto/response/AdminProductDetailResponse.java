package com.techhub.model.dto.response;

import com.techhub.model.enums.CategoryStatus;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.model.enums.ShopStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminProductDetailResponse(
        UUID id,
        UUID shopId,
        String shopName,
        String sellerName,
        ShopStatus shopStatus,
        UUID categoryId,
        String categoryName,
        CategoryStatus categoryStatus,
        String brandName,
        String name,
        String slug,
        String description,
        Map<String, Object> attributes,
        ProductApprovalStatus approvalStatus,
        ProductVisibilityStatus visibilityStatus,
        String rejectionReason,
        boolean isSuspended,
        boolean isDeleted,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public AdminProductDetailResponse(
            UUID id, UUID shopId, String shopName, String sellerName, ShopStatus shopStatus,
            UUID categoryId, String categoryName, CategoryStatus categoryStatus,
            String brandName, String name, String slug, String description,
            Map<String, Object> attributes, ProductApprovalStatus approvalStatus,
            ProductVisibilityStatus visibilityStatus, boolean isSuspended, boolean isDeleted,
            List<ProductImageResponse> images, List<ProductVariantResponse> variants,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        this(id, shopId, shopName, sellerName, shopStatus, categoryId, categoryName, categoryStatus,
                brandName, name, slug, description, attributes, approvalStatus, visibilityStatus,
                null, isSuspended, isDeleted, images, variants, createdAt, updatedAt);
    }
}

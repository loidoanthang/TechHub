package com.techhub.model.dto.response;

import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        UUID shopId,
        String sellerName,
        UUID categoryId,
        String categoryName,
        String brandName,
        String name,
        String slug,
        String description,
        Map<String, Object> attributes,
        ProductApprovalStatus approvalStatus,
        ProductVisibilityStatus visibilityStatus,
        boolean isSuspended,
        boolean isDeleted,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public ProductDetailResponse(
            UUID id,
            UUID shopId,
            UUID categoryId,
            String categoryName,
            String brandName,
            String name,
            String slug,
            String description,
            Map<String, Object> attributes,
            ProductApprovalStatus approvalStatus,
            ProductVisibilityStatus visibilityStatus,
            boolean isSuspended,
            boolean isDeleted,
            List<ProductImageResponse> images,
            List<ProductVariantResponse> variants,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, shopId, null, categoryId, categoryName, brandName, name, slug, description,
                attributes, approvalStatus, visibilityStatus, isSuspended, isDeleted,
                images, variants, createdAt, updatedAt);
    }
}

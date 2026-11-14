package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PublicProductDetailResponse(
        UUID id,
        String name,
        String slug,
        String brandName,
        String description,
        UUID categoryId,
        String categoryName,
        UUID shopId,
        String shopName,
        String sellerName,
        String shopAvatarUrl,
        boolean isShopPaused,
        Map<String, Object> attributes,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants,
        UUID defaultVariantId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        long totalStock,
        boolean inStock,
        LocalDateTime createdAt,
        UUID sellerUserId
) {
    public PublicProductDetailResponse(
            UUID id,
            String name,
            String slug,
            String brandName,
            String description,
            UUID categoryId,
            String categoryName,
            UUID shopId,
            String shopName,
            String shopAvatarUrl,
            boolean isShopPaused,
            Map<String, Object> attributes,
            List<ProductImageResponse> images,
            List<ProductVariantResponse> variants,
            UUID defaultVariantId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            long totalStock,
            boolean inStock,
            LocalDateTime createdAt
    ) {
        this(id, name, slug, brandName, description, categoryId, categoryName, shopId,
                shopName, null, shopAvatarUrl, isShopPaused, attributes, images, variants,
                defaultVariantId, minPrice, maxPrice, totalStock, inStock, createdAt, null);
    }
}

package com.techhub.model.dto.response;

import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SellerProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        String brandName,
        UUID categoryId,
        String categoryName,
        String coverImageUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        long totalStock,
        long variantsCount,
        ProductApprovalStatus approvalStatus,
        ProductVisibilityStatus visibilityStatus,
        boolean isSuspended,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

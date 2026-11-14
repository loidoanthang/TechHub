package com.techhub.service.product;

import com.techhub.common.PageResponse;
import com.techhub.modules.product.dto.request.UpdateProductApprovalRequest;
import com.techhub.modules.product.dto.request.UpdateProductSuspendRequest;
import com.techhub.modules.product.dto.response.AdminProductDetailResponse;
import com.techhub.modules.product.dto.response.AdminProductSummaryResponse;
import com.techhub.modules.product.enums.ProductApprovalStatus;
import com.techhub.modules.product.enums.ProductVisibilityStatus;

import java.util.UUID;

public interface AdminProductService {
    PageResponse<AdminProductSummaryResponse> getProducts(
            UUID shopId,
            UUID categoryId,
            String keyword,
            ProductApprovalStatus approvalStatus,
            ProductVisibilityStatus visibilityStatus,
            Boolean isSuspended,
            Boolean isDeleted,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    AdminProductDetailResponse getProductDetail(UUID productId);

    AdminProductDetailResponse updateProductApproval(UUID productId, UpdateProductApprovalRequest request);

    AdminProductDetailResponse updateProductSuspend(UUID productId, UpdateProductSuspendRequest request);
}

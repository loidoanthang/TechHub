package com.techhub.service.product;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.UpdateProductApprovalRequest;
import com.techhub.model.dto.request.UpdateProductSuspendRequest;
import com.techhub.model.dto.response.AdminProductDetailResponse;
import com.techhub.model.dto.response.AdminProductSummaryResponse;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;

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

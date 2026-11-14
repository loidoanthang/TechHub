package com.techhub.modules.product.service;

import com.techhub.common.PageResponse;
import com.techhub.modules.product.dto.request.CreateProductRequest;
import com.techhub.modules.product.dto.response.ProductDetailResponse;
import com.techhub.modules.seller.dto.response.SellerProductSummaryResponse;
import com.techhub.modules.product.enums.ProductApprovalStatus;
import com.techhub.modules.product.enums.ProductVisibilityStatus;

import com.techhub.modules.product.dto.request.CreateProductVariantRequest;
import com.techhub.modules.product.dto.request.UpdateProductRequest;
import com.techhub.modules.product.dto.request.UpdateProductVariantRequest;
import com.techhub.modules.product.dto.response.ProductVariantResponse;

import com.techhub.modules.product.dto.request.UpdateProductVisibilityRequest;

import java.util.UUID;

public interface SellerProductService {
    ProductDetailResponse createProduct(UUID userId, CreateProductRequest request);

    PageResponse<SellerProductSummaryResponse> getProducts(
            UUID userId,
            String keyword,
            UUID categoryId,
            ProductApprovalStatus approvalStatus,
            ProductVisibilityStatus visibilityStatus,
            Boolean isSuspended,
            Boolean isDeleted,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    ProductDetailResponse getProductDetail(UUID userId, UUID productId);

    ProductDetailResponse updateProduct(UUID userId, UUID productId, UpdateProductRequest request);

    void deleteProduct(UUID userId, UUID productId);

    ProductVariantResponse addProductVariant(UUID userId, UUID productId, CreateProductVariantRequest request);

    ProductVariantResponse updateProductVariant(UUID userId, UUID productId, UUID variantId, UpdateProductVariantRequest request);
 
    void deleteProductVariant(UUID userId, UUID productId, UUID variantId);

    ProductDetailResponse submitProductForApproval(UUID userId, UUID productId);

    ProductDetailResponse updateProductVisibility(UUID userId, UUID productId, UpdateProductVisibilityRequest request);
}

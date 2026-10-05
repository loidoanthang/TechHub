package com.techhub.controller.seller;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreateProductRequest;
import com.techhub.model.dto.request.CreateProductVariantRequest;
import com.techhub.model.dto.request.UpdateProductRequest;
import com.techhub.model.dto.request.UpdateProductVariantRequest;
import com.techhub.model.dto.request.UpdateProductVisibilityRequest;
import com.techhub.model.dto.response.ProductDetailResponse;
import com.techhub.model.dto.response.ProductVariantResponse;
import com.techhub.model.dto.response.SellerProductSummaryResponse;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.security.SecurityUtils;
import com.techhub.service.seller.SellerProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
@Validated
public class SellerProductController {

    private final SellerProductService sellerProductService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDetailResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ProductDetailResponse response = sellerProductService.createProduct(userId, request);
        return ApiResponse.success(response, "Product draft created successfully", null);
    }

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<PageResponse<SellerProductSummaryResponse>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ProductApprovalStatus approvalStatus,
            @RequestParam(required = false) ProductVisibilityStatus visibilityStatus,
            @RequestParam(required = false) Boolean isSuspended,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, keyword, categoryId, approvalStatus, visibilityStatus, isSuspended, isDeleted, page, size, sortBy, sortDir
        );
        return ApiResponse.success(response, "Product list retrieved successfully", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductDetailResponse> getProductDetail(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ProductDetailResponse response = sellerProductService.getProductDetail(userId, id);
        return ApiResponse.success(response, "Product detail retrieved successfully", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductDetailResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ProductDetailResponse response = sellerProductService.updateProduct(userId, id, request);
        return ApiResponse.success(response, "Product updated successfully", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<Void> deleteProduct(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        sellerProductService.deleteProduct(userId, id);
        return ApiResponse.success(null, "Product deleted successfully", null);
    }

    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductVariantResponse> addProductVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ProductVariantResponse response = sellerProductService.addProductVariant(userId, productId, request);
        return ApiResponse.success(response, "Product variant created successfully", null);
    }

    @PutMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductVariantResponse> updateProductVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateProductVariantRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);
        return ApiResponse.success(response, "Product variant updated successfully", null);
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<Void> deleteProductVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        sellerProductService.deleteProductVariant(userId, productId, variantId);
        return ApiResponse.success(null, "Product variant deleted successfully", null);
    }

    @PatchMapping("/{id}/submit-approval")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductDetailResponse> submitProductForApproval(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ProductDetailResponse response = sellerProductService.submitProductForApproval(userId, id);
        return ApiResponse.success(response, "Product submitted for approval successfully", null);
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductDetailResponse> updateProductVisibility(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductVisibilityRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ProductDetailResponse response = sellerProductService.updateProductVisibility(userId, id, request);
        return ApiResponse.success(response, "Product visibility updated successfully", null);
    }
}

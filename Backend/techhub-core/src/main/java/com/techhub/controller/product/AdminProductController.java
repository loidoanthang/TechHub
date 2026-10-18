package com.techhub.controller.product;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.UpdateProductApprovalRequest;
import com.techhub.model.dto.request.UpdateProductSuspendRequest;
import com.techhub.model.dto.response.AdminProductDetailResponse;
import com.techhub.model.dto.response.AdminProductSummaryResponse;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.service.product.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Validated
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminProductSummaryResponse>> getProducts(
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductApprovalStatus approvalStatus,
            @RequestParam(required = false) ProductVisibilityStatus visibilityStatus,
            @RequestParam(required = false) Boolean isSuspended,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                shopId,
                categoryId,
                keyword,
                approvalStatus,
                visibilityStatus,
                isSuspended,
                isDeleted,
                page,
                size,
                sortBy,
                sortDir
        );
        return ApiResponse.success(response, "Fetched admin products successfully", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminProductDetailResponse> getProductDetail(@PathVariable UUID id) {
        AdminProductDetailResponse response = adminProductService.getProductDetail(id);
        return ApiResponse.success(response, "Fetched admin product detail successfully", null);
    }

    @PatchMapping("/{id}/approval")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminProductDetailResponse> updateProductApproval(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductApprovalRequest request
    ) {
        AdminProductDetailResponse response = adminProductService.updateProductApproval(id, request);
        String message = request.status() == ProductApprovalStatus.APPROVED
                ? "Product approved successfully"
                : "Product rejected successfully";
        return ApiResponse.success(response, message, null);
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminProductDetailResponse> updateProductSuspend(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductSuspendRequest request
    ) {
        AdminProductDetailResponse response = adminProductService.updateProductSuspend(id, request);
        String message = Boolean.TRUE.equals(request.isSuspended())
                ? "Product suspended successfully"
                : "Product unsuspended successfully";
        return ApiResponse.success(response, message, null);
    }
}

package com.techhub.controller.admin;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreateCategoryAttributeRequest;
import com.techhub.model.dto.request.CreateCategoryRequest;
import com.techhub.model.dto.request.UpdateCategoryAttributeRequest;
import com.techhub.model.dto.request.UpdateCategoryRequest;
import com.techhub.model.dto.request.UpdateCategoryStatusRequest;
import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategoryDetailResponse;
import com.techhub.model.dto.response.CategorySummaryResponse;
import com.techhub.model.enums.CategoryStatus;
import com.techhub.service.admin.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public ApiResponse<PageResponse<CategorySummaryResponse>> getCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CategoryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<CategorySummaryResponse> data = adminCategoryService.getCategories(
                keyword, status, page, size, sortBy, sortDir
        );
        return ApiResponse.success(data, "Category list retrieved successfully", null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryDetailResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryDetailResponse data = adminCategoryService.createCategory(request);
        return ApiResponse.success(data, "Category created successfully", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryDetailResponse> getCategoryDetail(
            @PathVariable UUID id
    ) {
        CategoryDetailResponse data = adminCategoryService.getCategoryDetail(id);
        return ApiResponse.success(data, "Category details retrieved successfully", null);
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryDetailResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryDetailResponse data = adminCategoryService.updateCategory(id, request);
        return ApiResponse.success(data, "Category updated successfully", null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CategoryDetailResponse> updateCategoryStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryStatusRequest request
    ) {
        CategoryDetailResponse data = adminCategoryService.updateCategoryStatus(id, request);
        return ApiResponse.success(data, "Category status updated successfully", null);
    }

    @PostMapping("/{id}/attributes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryAttributeResponse> createCategoryAttribute(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCategoryAttributeRequest request
    ) {
        CategoryAttributeResponse data = adminCategoryService.createCategoryAttribute(id, request);
        return ApiResponse.success(data, "Category attribute created successfully", null);
    }

    @PutMapping("/{categoryId}/attributes/{attributeId}")
    public ApiResponse<CategoryAttributeResponse> updateCategoryAttribute(
            @PathVariable UUID categoryId,
            @PathVariable UUID attributeId,
            @Valid @RequestBody UpdateCategoryAttributeRequest request
    ) {
        CategoryAttributeResponse data = adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request);
        return ApiResponse.success(data, "Category attribute updated successfully", null);
    }

    @DeleteMapping("/{categoryId}/attributes/{attributeId}")
    public ApiResponse<Void> deleteCategoryAttribute(
            @PathVariable UUID categoryId,
            @PathVariable UUID attributeId
    ) {
        adminCategoryService.deleteCategoryAttribute(categoryId, attributeId);
        return ApiResponse.success(null, "Category attribute deleted successfully", null);
    }
}

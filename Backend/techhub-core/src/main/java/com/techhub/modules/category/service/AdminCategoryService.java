package com.techhub.modules.category.service;

import com.techhub.common.PageResponse;
import com.techhub.modules.category.dto.request.CreateCategoryAttributeRequest;
import com.techhub.modules.category.dto.request.CreateCategoryRequest;
import com.techhub.modules.category.dto.request.UpdateCategoryRequest;
import com.techhub.modules.category.dto.response.CategoryAttributeResponse;
import com.techhub.modules.category.dto.response.CategoryDetailResponse;
import com.techhub.modules.category.dto.response.CategorySummaryResponse;

import com.techhub.modules.category.enums.CategoryStatus;

import java.util.UUID;

import com.techhub.modules.category.dto.request.UpdateCategoryAttributeRequest;
import com.techhub.modules.category.dto.request.UpdateCategoryStatusRequest;

public interface AdminCategoryService {
    CategoryDetailResponse createCategory(CreateCategoryRequest request);

    PageResponse<CategorySummaryResponse> getCategories(
            String keyword,
            CategoryStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    CategoryDetailResponse getCategoryDetail(UUID categoryId);

    CategoryDetailResponse updateCategory(UUID categoryId, UpdateCategoryRequest request);

    CategoryDetailResponse updateCategoryStatus(UUID categoryId, UpdateCategoryStatusRequest request);

    CategoryAttributeResponse createCategoryAttribute(UUID categoryId, CreateCategoryAttributeRequest request);

    CategoryAttributeResponse updateCategoryAttribute(UUID categoryId, UUID attributeId, UpdateCategoryAttributeRequest request);

    void deleteCategoryAttribute(UUID categoryId, UUID attributeId);

    void deleteCategory(UUID categoryId);
}

package com.techhub.service.category;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreateCategoryAttributeRequest;
import com.techhub.model.dto.request.CreateCategoryRequest;
import com.techhub.model.dto.request.UpdateCategoryRequest;
import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategoryDetailResponse;
import com.techhub.model.dto.response.CategorySummaryResponse;

import com.techhub.model.enums.CategoryStatus;

import java.util.UUID;

import com.techhub.model.dto.request.UpdateCategoryAttributeRequest;
import com.techhub.model.dto.request.UpdateCategoryStatusRequest;

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
}

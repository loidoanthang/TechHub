package com.techhub.service.category;

import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategorySummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    List<CategorySummaryResponse> getCategories(String keyword, String sortBy, String sortDir);
    List<CategoryAttributeResponse> getCategoryAttributes(UUID categoryId);
}

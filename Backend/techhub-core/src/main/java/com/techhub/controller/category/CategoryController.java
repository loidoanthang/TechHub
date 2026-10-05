package com.techhub.controller.category;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategorySummaryResponse;
import com.techhub.service.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategorySummaryResponse>> getCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        List<CategorySummaryResponse> response = categoryService.getCategories(keyword, sortBy, sortDir);
        return ApiResponse.success(response, "Fetched categories successfully", null);
    }

    @GetMapping("/{id}/attributes")
    public ApiResponse<List<CategoryAttributeResponse>> getCategoryAttributes(@PathVariable UUID id) {
        List<CategoryAttributeResponse> response = categoryService.getCategoryAttributes(id);
        return ApiResponse.success(response, "Fetched category attributes successfully", null);
    }
}

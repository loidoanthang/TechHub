package com.techhub.service.product;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.ProductSummaryResponse;

import com.techhub.model.dto.response.PublicProductDetailResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    PageResponse<ProductSummaryResponse> getPublicProducts(
            String keyword,
            UUID categoryId,
            String brandName,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            String sortBy,
            int page,
            int size
    );

    PublicProductDetailResponse getPublicProductBySlug(String slug);

    List<ProductSummaryResponse> getSimilarProducts(UUID productId);
}

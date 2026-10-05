package com.techhub.controller.product;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.ProductSummaryResponse;
import com.techhub.model.dto.response.PublicProductDetailResponse;
import com.techhub.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brandName,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") Boolean inStockOnly,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                keyword, categoryId, brandName, minPrice, maxPrice, inStockOnly, sortBy, page, size
        );
        return ApiResponse.success(response, "Fetched marketplace products successfully", null);
    }

    @GetMapping("/{slug}")
    public ApiResponse<PublicProductDetailResponse> getProductBySlug(@PathVariable String slug) {
        PublicProductDetailResponse response = productService.getPublicProductBySlug(slug);
        return ApiResponse.success(response, "Fetched product details successfully", null);
    }
}

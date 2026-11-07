package com.techhub.controller.product;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.ProductSummaryResponse;
import com.techhub.model.dto.response.PublicProductDetailResponse;
import com.techhub.model.dto.response.RecentlyViewedResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.product.ProductService;
import com.techhub.service.recentlyviewed.RecentlyViewedService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final RecentlyViewedService recentlyViewedService;

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

    @GetMapping("/recently-viewed")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<List<RecentlyViewedResponse>> getRecentlyViewedProducts() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<RecentlyViewedResponse> response = recentlyViewedService.getRecentlyViewedProducts(currentUserId);
        return ApiResponse.success(response, "Lấy danh sách sản phẩm đã xem gần đây thành công", null);
    }

    @GetMapping("/{slug}")
    public ApiResponse<PublicProductDetailResponse> getProductBySlug(@PathVariable String slug) {
        PublicProductDetailResponse response = productService.getPublicProductBySlug(slug);
        return ApiResponse.success(response, "Fetched product details successfully", null);
    }

    @GetMapping("/{id}/similar")
    public ApiResponse<List<ProductSummaryResponse>> getSimilarProducts(@PathVariable UUID id) {
        List<ProductSummaryResponse> response = productService.getSimilarProducts(id);
        return ApiResponse.success(response, "Fetched similar products successfully", null);
    }

    @PostMapping("/{id}/view")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<Void> recordProductView(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        recentlyViewedService.recordProductView(currentUserId, id);
        return ApiResponse.success(null, "Ghi nhận lượt xem sản phẩm thành công", null);
    }
}

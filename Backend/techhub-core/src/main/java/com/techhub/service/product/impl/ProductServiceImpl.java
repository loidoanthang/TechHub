package com.techhub.service.product.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.ProductImageResponse;
import com.techhub.model.dto.response.ProductSummaryResponse;
import com.techhub.model.dto.response.ProductVariantMetrics;
import com.techhub.model.dto.response.ProductVariantResponse;
import com.techhub.model.dto.response.PublicProductDetailResponse;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.ProductImage;
import com.techhub.model.entity.ProductVariant;
import com.techhub.model.enums.*;
import com.techhub.repository.ProductImageRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ProductVariantRepository;
import com.techhub.service.product.ProductService;
import com.techhub.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getPublicProducts(
            String keyword,
            UUID categoryId,
            String brandName,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            String sortBy,
            int page,
            int size
    ) {
        log.info("Public search products: keyword='{}', categoryId='{}', brandName='{}', minPrice='{}', maxPrice='{}', inStockOnly='{}', sortBy='{}', page={}, size={}",
                keyword, categoryId, brandName, minPrice, maxPrice, inStockOnly, sortBy, page, size);

        // 1. Sanitize & Validate Pagination
        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(Math.max(1, size), 100);

        // 2. Validate Price Range
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_PRICE_RANGE);
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_PRICE_RANGE);
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PRICE_RANGE);
        }

        // 3. Whitelist & Sanitize Sort By
        String sanitizedSortBy = "newest";
        if (sortBy != null) {
            String lowerSort = sortBy.trim().toLowerCase();
            if (lowerSort.equals("price_asc") || lowerSort.equals("price_desc") || lowerSort.equals("popular")) {
                sanitizedSortBy = lowerSort;
            }
        }

        // 4. Construct Pageable
        Pageable pageable;
        if ("price_asc".equals(sanitizedSortBy) || "price_desc".equals(sanitizedSortBy)) {
            pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.unsorted());
        } else {
            pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        // 5. Build Specification (Gatekeeper 5-tier + Filters + Subquery Ordering)
        Specification<Product> spec = ProductSpecification.filterPublicProducts(
                keyword,
                categoryId,
                brandName,
                minPrice,
                maxPrice,
                inStockOnly,
                sanitizedSortBy
        );

        // 6. Query Products
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        if (productPage.isEmpty()) {
            return PageResponse.from(productPage, Collections.emptyList());
        }

        List<UUID> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();

        // 7. Batch Fetch Cover Images (1 Query O(1))
        Map<UUID, String> coverImageMap = new HashMap<>();
        List<Object[]> coverImageRows = productImageRepository.findCoverImagesByProductIds(productIds);
        for (Object[] row : coverImageRows) {
            coverImageMap.put((UUID) row[0], (String) row[1]);
        }

        // 8. Batch Fetch Active Variant Metrics (1 Query O(1))
        Map<UUID, ProductVariantMetrics> metricsMap = new HashMap<>();
        List<Object[]> metricRows = productVariantRepository.aggregateActiveVariantMetricsByProductIds(productIds);
        for (Object[] row : metricRows) {
            UUID pId = (UUID) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal variantMinPrice = row[2] instanceof BigDecimal bd ? bd : (row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO);
            BigDecimal variantMaxPrice = row[3] instanceof BigDecimal bd ? bd : (row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO);
            long totalStock = ((Number) row[4]).longValue();
            metricsMap.put(pId, new ProductVariantMetrics(pId, count, variantMinPrice, variantMaxPrice, totalStock));
        }

        // 9. In-Memory Assembly to ProductSummaryResponse
        List<ProductSummaryResponse> dtoList = productPage.getContent().stream()
                .map(product -> {
                    ProductVariantMetrics metrics = metricsMap.get(product.getId());
                    BigDecimal displayPrice = metrics != null ? metrics.minPrice() : BigDecimal.ZERO;
                    boolean inStock = metrics != null && metrics.totalStock() > 0;
                    boolean isShopPaused = product.getShop() != null && product.getShop().getStatus() == ShopStatus.PAUSED;

                    return new ProductSummaryResponse(
                            product.getId(),
                            product.getName(),
                            product.getSlug(),
                            product.getBrandName(),
                            product.getCategory() != null ? product.getCategory().getId() : null,
                            product.getCategory() != null ? product.getCategory().getName() : null,
                            product.getShop() != null ? product.getShop().getId() : null,
                            product.getShop() != null ? product.getShop().getName() : null,
                            isShopPaused,
                            coverImageMap.get(product.getId()),
                            displayPrice,
                            inStock,
                            product.getCreatedAt()
                    );
                })
                .toList();

        return PageResponse.from(productPage, dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProductDetailResponse getPublicProductBySlug(String slug) {
        log.info("Public fetching product details by slug: '{}'", slug);

        // 1. Sanitize slug
        if (slug == null || slug.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        String sanitizedSlug = slug.trim().toLowerCase();

        // 2. Fetch Product & Joined Entities with EntityGraph (1 Query O(1))
        Product product = productRepository.findBySlug(sanitizedSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. Verify Gatekeeper 5-Tier (BR-11C, BR-07, BR-11A)
        // 3.1 Approval Status = APPROVED
        if (product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
            log.warn("Product slug '{}' is not APPROVED (status={}). Blocking public access.", sanitizedSlug, product.getApprovalStatus());
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 3.2 Visibility Status = PUBLISHED
        if (product.getVisibilityStatus() != ProductVisibilityStatus.PUBLISHED) {
            log.warn("Product slug '{}' is not PUBLISHED (status={}). Blocking public access.", sanitizedSlug, product.getVisibilityStatus());
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 3.3 isSuspended = false AND isDeleted = false
        if (product.isSuspended() || product.isDeleted()) {
            log.warn("Product slug '{}' is suspended={} or deleted={}. Blocking public access.", sanitizedSlug, product.isSuspended(), product.isDeleted());
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 3.4 Category Status = ACTIVE (BR-11A)
        if (product.getCategory() == null || product.getCategory().getStatus() != CategoryStatus.ACTIVE) {
            log.warn("Category of product slug '{}' is null or INACTIVE. Blocking public access.", sanitizedSlug);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 3.5 Shop Status IN (ACTIVE, PAUSED) (BR-07)
        if (product.getShop() == null || (product.getShop().getStatus() != ShopStatus.ACTIVE && product.getShop().getStatus() != ShopStatus.PAUSED)) {
            log.warn("Shop of product slug '{}' is null or not ACTIVE/PAUSED. Blocking public access.", sanitizedSlug);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 3.6 Shop Owner User Status NOT IN (BANNED, DELETED)
        if (product.getShop().getSeller() == null || product.getShop().getSeller().getUser() == null
                || product.getShop().getSeller().getUser().getStatus() == UserStatus.BANNED
                || product.getShop().getSeller().getUser().getStatus() == UserStatus.DELETED) {
            log.warn("Shop owner user of product slug '{}' is BANNED/DELETED. Blocking public access.", sanitizedSlug);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 4. Query Gallery Images ordered by displayOrder ASC (1 Query O(1))
        List<ProductImage> imageEntities = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<ProductImageResponse> images = imageEntities.stream()
                .map(img -> new ProductImageResponse(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
                .toList();

        // 5. Query Active Variants ordered by price ASC, createdAt ASC (1 Query O(1))
        List<ProductVariant> activeVariants = productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(
                product.getId(), ProductVariantStatus.ACTIVE
        );
        // Gatekeeper check 7: Must have at least 1 ACTIVE variant (BR-11B)
        if (activeVariants == null || activeVariants.isEmpty()) {
            log.warn("Product slug '{}' has no ACTIVE variants. Blocking public access.", sanitizedSlug);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductVariantResponse> variants = activeVariants.stream()
                .map(v -> new ProductVariantResponse(
                        v.getId(),
                        v.getSku(),
                        v.getVariantName(),
                        v.getPrice(),
                        v.getStockQuantity(),
                        v.getImageUrl(),
                        v.getAttributes(),
                        v.getStatus()
                ))
                .toList();

        // 6. Resolve Default Variant & Metrics (BR-11B)
        UUID defaultVariantId = activeVariants.stream()
                .filter(v -> v.getStockQuantity() > 0)
                .findFirst()
                .orElse(activeVariants.get(0))
                .getId();

        BigDecimal minPrice = activeVariants.get(0).getPrice();
        BigDecimal maxPrice = activeVariants.stream()
                .map(ProductVariant::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(minPrice);
        long totalStock = activeVariants.stream()
                .mapToLong(ProductVariant::getStockQuantity)
                .sum();
        boolean inStock = totalStock > 0;
        boolean isShopPaused = product.getShop().getStatus() == ShopStatus.PAUSED;

        // 7. In-Memory Assembly to PublicProductDetailResponse DTO
        return new PublicProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getBrandName(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getShop().getId(),
                product.getShop().getName(),
                product.getShop().getAvatarUrl(),
                isShopPaused,
                product.getAttributes() != null ? product.getAttributes() : Collections.emptyMap(),
                images,
                variants,
                defaultVariantId,
                minPrice,
                maxPrice,
                totalStock,
                inStock,
                product.getCreatedAt()
        );
    }
}

package com.techhub.service.admin.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateProductApprovalRequest;
import com.techhub.model.dto.request.UpdateProductSuspendRequest;
import com.techhub.model.dto.response.AdminProductDetailResponse;
import com.techhub.model.dto.response.AdminProductSummaryResponse;
import com.techhub.model.dto.response.ProductImageResponse;
import com.techhub.model.dto.response.ProductVariantMetrics;
import com.techhub.model.dto.response.ProductVariantResponse;
import com.techhub.model.entity.Category;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.ProductImage;
import com.techhub.model.entity.ProductVariant;
import com.techhub.model.entity.Shop;
import com.techhub.model.enums.CategoryStatus;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.repository.ProductImageRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ProductVariantRepository;
import com.techhub.service.admin.AdminProductService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
            "createdat", "createdAt",
            "created_at", "createdAt",
            "updatedat", "updatedAt",
            "updated_at", "updatedAt",
            "name", "name",
            "brandname", "brandName",
            "brand_name", "brandName"
    );

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminProductSummaryResponse> getProducts(
            UUID shopId,
            UUID categoryId,
            String keyword,
            ProductApprovalStatus approvalStatus,
            ProductVisibilityStatus visibilityStatus,
            Boolean isSuspended,
            Boolean isDeleted,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.info("Admin fetching products: shopId={}, categoryId={}, keyword='{}', approvalStatus={}, visibilityStatus={}, isSuspended={}, isDeleted={}, page={}, size={}",
                shopId, categoryId, keyword, approvalStatus, visibilityStatus, isSuspended, isDeleted, page, size);

        // 1. Sanitize Pagination & Sort
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        String normalizedSortKey = sortBy != null ? sortBy.trim().toLowerCase() : "";
        String sanitizedSortBy = SORT_FIELD_MAPPING.getOrDefault(normalizedSortKey, "createdAt");
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sanitizedSortBy).and(Sort.by(direction, "id"));
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);

        // 2. Build Specification for Admin cross-shop view
        Specification<Product> spec = ProductSpecification.filterAdminProducts(
                shopId,
                keyword,
                categoryId,
                approvalStatus,
                visibilityStatus,
                isSuspended,
                isDeleted
        );

        // 3. Query Database
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        if (productPage.isEmpty()) {
            return PageResponse.from(productPage, Collections.emptyList());
        }

        List<UUID> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();

        // 4. Batch Fetch Cover Images (displayOrder = 0)
        Map<UUID, String> coverImageMap = new HashMap<>();
        List<Object[]> coverImageRows = productImageRepository.findCoverImagesByProductIds(productIds);
        for (Object[] row : coverImageRows) {
            coverImageMap.put((UUID) row[0], (String) row[1]);
        }

        // 5. Batch Fetch Variant Metrics (COUNT, MIN, MAX, SUM)
        Map<UUID, ProductVariantMetrics> metricsMap = new HashMap<>();
        List<Object[]> metricRows = productVariantRepository.aggregateVariantMetricsByProductIds(productIds);
        for (Object[] row : metricRows) {
            UUID pId = (UUID) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal minPrice = row[2] instanceof BigDecimal bd ? bd : (row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO);
            BigDecimal maxPrice = row[3] instanceof BigDecimal bd ? bd : (row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO);
            long totalStock = ((Number) row[4]).longValue();
            metricsMap.put(pId, new ProductVariantMetrics(pId, count, minPrice, maxPrice, totalStock));
        }

        // 6. In-Memory Assembly to DTO
        List<AdminProductSummaryResponse> dtoList = productPage.getContent().stream()
                .map(product -> {
                    ProductVariantMetrics metrics = metricsMap.get(product.getId());
                    return new AdminProductSummaryResponse(
                            product.getId(),
                            product.getShop() != null ? product.getShop().getId() : null,
                            product.getShop() != null ? product.getShop().getName() : null,
                            (product.getShop() != null && product.getShop().getSeller() != null) ? product.getShop().getSeller().getSellerName() : null,
                            product.getName(),
                            product.getSlug(),
                            product.getBrandName(),
                            product.getCategory() != null ? product.getCategory().getId() : null,
                            product.getCategory() != null ? product.getCategory().getName() : null,
                            coverImageMap.get(product.getId()),
                            metrics != null ? metrics.minPrice() : BigDecimal.ZERO,
                            metrics != null ? metrics.maxPrice() : BigDecimal.ZERO,
                            metrics != null ? metrics.totalStock() : 0L,
                            metrics != null ? metrics.variantsCount() : 0L,
                            product.getApprovalStatus(),
                            product.getVisibilityStatus(),
                            product.isSuspended(),
                            product.isDeleted(),
                            product.getCreatedAt(),
                            product.getUpdatedAt()
                    );
                })
                .toList();

        return PageResponse.from(productPage, dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductDetailResponse getProductDetail(UUID productId) {
        log.info("Admin fetching product detail: productId={}", productId);

        // 1. Query Product Entity (Eagerly fetch Category, Shop, Seller via EntityGraph)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. Query Images (displayOrder ASC)
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());

        // 3. Query Variants (createdAt ASC)
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(product.getId());

        return buildAdminProductDetailResponse(product, images, variants);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductDetailResponse updateProductApproval(UUID productId, UpdateProductApprovalRequest request) {
        if (request == null || request.status() == null ||
                (request.status() != ProductApprovalStatus.APPROVED && request.status() != ProductApprovalStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Shop shop = product.getShop();
        Category category = product.getCategory();

        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(product.getId());

        if (request.status() == ProductApprovalStatus.APPROVED) {
            // Cannot approve a DRAFT product directly (Seller must submit for approval first)
            if (product.getApprovalStatus() == ProductApprovalStatus.DRAFT) {
                throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
            }

            // Shop Lifecycle Guard (BR-07): Cannot approve if Shop is BANNED or SUSPENDED
            if (shop != null && (shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED)) {
                throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
            }

            // Category Status Guard (BR-11A): Cannot approve if Category is INACTIVE
            if (category != null && category.getStatus() == CategoryStatus.INACTIVE) {
                throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
            }

            // Variant completeness (BR-11): Must have at least 1 variant
            if (variants.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_VARIANT_COUNT);
            }

            // Image completeness (BR-11): Must have at least 1 cover image (displayOrder = 0)
            boolean hasCoverImage = images.stream().anyMatch(img -> img.getDisplayOrder() == 0);
            if (!hasCoverImage) {
                throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
            }

            product.setApprovalStatus(ProductApprovalStatus.APPROVED);
            product.setRejectionReason(null);
            // Note: visibilityStatus remains unchanged (e.g. UNPUBLISHED) - BR-08

        } else if (request.status() == ProductApprovalStatus.REJECTED) {
            if (request.rejectionReason() == null || request.rejectionReason().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.REJECTION_REASON_REQUIRED);
            }

            product.setApprovalStatus(ProductApprovalStatus.REJECTED);
            product.setRejectionReason(request.rejectionReason().trim());

            // BR-08: If currently PUBLISHED, automatically reset to UNPUBLISHED
            if (product.getVisibilityStatus() == ProductVisibilityStatus.PUBLISHED) {
                product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
            }
        }

        Product savedProduct = productRepository.save(product);

        log.info("Admin updated product approval: productId={}, status={}, visibilityStatus={}, rejectionReason='{}'",
                productId, savedProduct.getApprovalStatus(), savedProduct.getVisibilityStatus(), savedProduct.getRejectionReason());

        return buildAdminProductDetailResponse(savedProduct, images, variants);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductDetailResponse updateProductSuspend(UUID productId, UpdateProductSuspendRequest request) {
        if (request == null || request.isSuspended() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(product.getId());

        // Idempotency check: if status is different, update & save
        if (product.isSuspended() != request.isSuspended()) {
            product.setSuspended(request.isSuspended());
            product = productRepository.save(product);
        }

        log.info("Admin updated product suspension: productId={}, isSuspended={}", productId, product.isSuspended());

        return buildAdminProductDetailResponse(product, images, variants);
    }

    private AdminProductDetailResponse buildAdminProductDetailResponse(
            Product product,
            List<ProductImage> images,
            List<ProductVariant> variants
    ) {
        List<ProductImageResponse> imgResponses = images.stream()
                .map(img -> new ProductImageResponse(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
                .toList();

        List<ProductVariantResponse> varResponses = variants.stream()
                .map(v -> new ProductVariantResponse(
                        v.getId(),
                        v.getSku(),
                        v.getVariantName(),
                        v.getPrice(),
                        v.getStockQuantity(),
                        v.getImageUrl(),
                        v.getAttributes() != null ? v.getAttributes() : Collections.emptyMap(),
                        v.getStatus()
                ))
                .toList();

        Shop shop = product.getShop();
        Category category = product.getCategory();

        return new AdminProductDetailResponse(
                product.getId(),
                shop != null ? shop.getId() : null,
                shop != null ? shop.getName() : null,
                (shop != null && shop.getSeller() != null) ? shop.getSeller().getSellerName() : null,
                shop != null ? shop.getStatus() : null,
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                category != null ? category.getStatus() : null,
                product.getBrandName(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getAttributes() != null ? product.getAttributes() : Collections.emptyMap(),
                product.getApprovalStatus(),
                product.getVisibilityStatus(),
                product.getRejectionReason(),
                product.isSuspended(),
                product.isDeleted(),
                imgResponses,
                varResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

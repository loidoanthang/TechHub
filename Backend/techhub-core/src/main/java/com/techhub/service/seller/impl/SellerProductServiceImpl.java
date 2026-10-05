package com.techhub.service.seller.impl;

import com.techhub.common.PageResponse;
import com.techhub.common.SlugUtils;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateProductImageRequest;
import com.techhub.model.dto.request.CreateProductRequest;
import com.techhub.model.dto.request.CreateProductVariantRequest;
import com.techhub.model.dto.request.UpdateProductRequest;
import com.techhub.model.dto.request.UpdateProductVariantRequest;
import com.techhub.model.dto.request.UpdateProductVisibilityRequest;
import com.techhub.model.dto.response.ProductDetailResponse;
import com.techhub.model.dto.response.ProductImageResponse;
import com.techhub.model.dto.response.ProductVariantMetrics;
import com.techhub.model.dto.response.ProductVariantResponse;
import com.techhub.model.dto.response.SellerProductSummaryResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.seller.SellerProductService;
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
public class SellerProductServiceImpl implements SellerProductService {

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDetailResponse createProduct(UUID userId, CreateProductRequest request) {
        log.info("Processing createProduct for userId={}, productName='{}'", userId, request.name());

        // 1. Validate Seller and Shop
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.SUSPENDED || shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} status {} is not active. Rejecting product creation.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 2. Validate Category
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getStatus() == CategoryStatus.INACTIVE) {
            log.warn("Category {} is inactive. Rejecting product creation.", category.getId());
            throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
        }

        // 3. Validate Strict Schema for Category Attributes (BR-09)
        List<CategoryAttribute> catAttrs = categoryAttributeRepository.findByCategoryId(category.getId()); // lấy attribute dựa trên cateId
        Map<String, Object> reqAttrs = request.attributes() != null ? request.attributes() : Collections.emptyMap(); // lấy attribute trong request
        Set<String> expectedAttrNames = new HashSet<>();

        for (CategoryAttribute catAttr : catAttrs) {
            expectedAttrNames.add(catAttr.getName());
            Object val = reqAttrs.get(catAttr.getName());
            if (val == null || (val instanceof String s && s.trim().isEmpty())) {
                log.warn("Missing category attribute '{}' in product creation request", catAttr.getName());
                throw new BusinessException(ErrorCode.MISSING_CATEGORY_ATTRIBUTE);
            }
            validateAttributeValue(catAttr, val);
        }

        for (String key : reqAttrs.keySet()) {
            if (!expectedAttrNames.contains(key)) {
                log.warn("Unknown attribute '{}' not defined in category schema", key);
                throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
            }
        }

        // 4. Validate Images (BR-11)
        List<CreateProductImageRequest> images = request.images();
        if (images == null || images.isEmpty() || images.size() > 6) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
        }

        int coverCount = 0;
        Set<Integer> displayOrders = new HashSet<>();
        for (CreateProductImageRequest img : images) {
            if (img.displayOrder() == null) {
                throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
            }
            if (img.displayOrder() == 0) {
                coverCount++;
            }
            if (!displayOrders.add(img.displayOrder())) {
                log.warn("Duplicate displayOrder {} found in product images", img.displayOrder());
                throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
            }
        }
        if (coverCount != 1) {
            log.warn("Invalid product images: expected exactly one cover image with displayOrder=0, found {}", coverCount);
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
        }

        // 5. Validate Variants (BR-09, BR-11)
        List<CreateProductVariantRequest> variants = request.variants();
        if (variants == null || variants.isEmpty() || variants.size() > 50) {
            throw new BusinessException(ErrorCode.INVALID_VARIANT_COUNT);
        }

        Set<String> skus = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (CreateProductVariantRequest v : variants) {
            String sku = v.sku() != null ? v.sku().trim() : "";
            if (!skus.add(sku)) {
                log.warn("Duplicate variant SKU '{}' in product creation request", sku);
                throw new BusinessException(ErrorCode.DUPLICATE_VARIANT_SKU);
            }
        }

        // 6. Generate Unique Slug (BR-10)
        String slug = SlugUtils.generateSlug(request.name().trim());
        int attempts = 0;
        while (productRepository.existsBySlug(slug) && attempts < 5) {
            slug = SlugUtils.generateSlug(request.name().trim());
            attempts++;
        }

        // 7. Save Product Entity
        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .brandName(request.brandName().trim())
                .name(request.name().trim())
                .slug(slug)
                .description(request.description())
                .attributes(reqAttrs)
                .approvalStatus(ProductApprovalStatus.DRAFT)
                .visibilityStatus(ProductVisibilityStatus.UNPUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
        Product savedProduct = productRepository.save(product);

        // 8. Save Images
        List<ProductImage> imageEntities = images.stream()
                .map(img -> ProductImage.builder()
                        .product(savedProduct)
                        .imageUrl(img.imageUrl().trim())
                        .displayOrder(img.displayOrder())
                        .build())
                .toList();
        List<ProductImage> savedImages = productImageRepository.saveAll(imageEntities);

        // 9. Save Variants
        List<ProductVariant> variantEntities = variants.stream()
                .map(v -> ProductVariant.builder()
                        .product(savedProduct)
                        .sku(v.sku().trim())
                        .variantName(v.variantName().trim())
                        .description(v.description())
                        .price(v.price())
                        .stockQuantity(v.stockQuantity())
                        .imageUrl(v.imageUrl() != null ? v.imageUrl().trim() : null)
                        .attributes(v.attributes())
                        .status(ProductVariantStatus.ACTIVE)
                        .build())
                .toList();
        List<ProductVariant> savedVariants = productVariantRepository.saveAll(variantEntities);

        log.info("Successfully created product draft id={}, slug='{}', variantsCount={}",
                savedProduct.getId(), savedProduct.getSlug(), savedVariants.size());

        // 10. Map to Response
        List<ProductImageResponse> imgResponses = savedImages.stream()
                .map(img -> new ProductImageResponse(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
                .toList();

        List<ProductVariantResponse> varResponses = savedVariants.stream()
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

        return new ProductDetailResponse(
                savedProduct.getId(),
                shop.getId(),
                category.getId(),
                category.getName(),
                savedProduct.getBrandName(),
                savedProduct.getName(),
                savedProduct.getSlug(),
                savedProduct.getDescription(),
                savedProduct.getAttributes(),
                savedProduct.getApprovalStatus(),
                savedProduct.getVisibilityStatus(),
                savedProduct.isSuspended(),
                savedProduct.isDeleted(),
                imgResponses,
                varResponses,
                savedProduct.getCreatedAt(),
                savedProduct.getUpdatedAt()
        );
    }

    private void validateAttributeValue(CategoryAttribute catAttr, Object val) {
        switch (catAttr.getDataType()) {
            case TEXT -> {
                if (val instanceof Map || val instanceof Collection) {
                    log.warn("Invalid TEXT attribute value (complex object/array) for '{}'", catAttr.getName());
                    throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                }
                String strVal = val.toString().trim();
                if (strVal.isEmpty() || strVal.length() > 500) {
                    log.warn("Invalid TEXT attribute value for '{}'", catAttr.getName());
                    throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                }
            }
            case NUMBER -> {
                try {
                    double num = Double.parseDouble(val.toString().trim());
                    if (Double.isNaN(num) || Double.isInfinite(num)) {
                        log.warn("Invalid NUMBER attribute value (NaN or Infinite) for '{}': {}", catAttr.getName(), val);
                        throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid NUMBER attribute value for '{}': {}", catAttr.getName(), val);
                    throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                }
            }
            case BOOLEAN -> {
                if (val instanceof Boolean) {
                    return;
                }
                String strVal = val.toString().trim();
                if (!"true".equalsIgnoreCase(strVal) && !"false".equalsIgnoreCase(strVal)) {
                    log.warn("Invalid BOOLEAN attribute value for '{}': {}", catAttr.getName(), val);
                    throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                }
            }
            case SELECT -> {
                String strVal = val.toString().trim();
                List<String> options = catAttr.getOptions();
                if (options == null || !options.contains(strVal)) {
                    log.warn("Invalid SELECT attribute value for '{}': {} not in options {}",
                            catAttr.getName(), strVal, options);
                    throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                }
            }
        }
    }

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
    public PageResponse<SellerProductSummaryResponse> getProducts(
            UUID userId,
            String keyword,
            UUID categoryId,
            ProductApprovalStatus approvalStatus,
            ProductVisibilityStatus visibilityStatus,
            Boolean isSuspended,
            Boolean isDeleted,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.info("Fetching products for userId={}, keyword='{}', categoryId={}, approvalStatus={}, visibilityStatus={}, isSuspended={}, isDeleted={}, page={}, size={}",
                userId, keyword, categoryId, approvalStatus, visibilityStatus, isSuspended, isDeleted, page, size);

        // 1. Resolve Seller and Shop (Isolation & Tiered Penalty BR-07)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // Tiered Penalty Model (BR-07): Shop BANNED is locked out from Seller Center
        if (shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} is BANNED. Rejecting product list access.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: Shop SUSPENDED or PAUSED is allowed to view products in Read-only Mode for Dispute/Appeal

        // 2. Sanitize Pagination & Sort (Hỗ trợ cả camelCase và snake_case chuẩn REST)
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        String normalizedSortKey = sortBy != null ? sortBy.trim().toLowerCase() : "";
        String sanitizedSortBy = SORT_FIELD_MAPPING.getOrDefault(normalizedSortKey, "createdAt");
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(direction, sanitizedSortBy));

        // 3. Build Specification (Gắn chặt shopId để cô lập multi-tenant, bảo tồn cờ isDeleted linh hoạt)
        Specification<Product> spec = ProductSpecification.filterSellerProducts(
                shop.getId(),
                keyword,
                categoryId,
                approvalStatus,
                visibilityStatus,
                isSuspended,
                isDeleted
        );

        // 4. Query Products Page
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        if (productPage.isEmpty()) {
            return PageResponse.from(productPage, Collections.emptyList());
        }

        List<UUID> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();

        // 5. Batch Fetch Cover Images (displayOrder = 0)
        Map<UUID, String> coverImageMap = new HashMap<>();
        List<Object[]> coverImageRows = productImageRepository.findCoverImagesByProductIds(productIds);
        for (Object[] row : coverImageRows) {
            coverImageMap.put((UUID) row[0], (String) row[1]);
        }

        // 6. Batch Fetch Variant Metrics (COUNT, MIN, MAX, SUM với chuyển đổi kiểu dữ liệu an toàn)
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

        // 7. Assemble DTOs
        List<SellerProductSummaryResponse> dtoList = productPage.getContent().stream()
                .map(product -> {
                    ProductVariantMetrics metrics = metricsMap.get(product.getId());
                    return new SellerProductSummaryResponse(
                            product.getId(),
                            product.getName(),
                            product.getSlug(),
                            product.getBrandName(),
                            product.getCategory().getId(),
                            product.getCategory().getName(),
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
    public ProductDetailResponse getProductDetail(UUID userId, UUID productId) {
        log.info("Fetching product detail for userId={}, productId={}", userId, productId);

        // 1. Resolve Seller and Shop (Isolation & Tiered Penalty BR-07)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Rejecting product detail access.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // Tiered Penalty Model (BR-07): Shop BANNED is locked out from Seller Center
        if (shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} is BANNED. Rejecting product detail access.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: Shop SUSPENDED or PAUSED is allowed to view products in Read-only Mode for Dispute/Appeal

        // 2. Query Product with Multi-tenant Isolation (Anti-ID Enumeration: 404 if not found or belongs to another shop)
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. Query Images (Ordered by displayOrder ASC, cover image order=0 first)
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());

        // 4. Query Variants (Ordered by createdAt ASC for deterministic UI listing)
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(product.getId());

        // 5. Map to Response DTO
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

        return new ProductDetailResponse(
                product.getId(),
                shop.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrandName(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getAttributes() != null ? product.getAttributes() : Collections.emptyMap(),
                product.getApprovalStatus(),
                product.getVisibilityStatus(),
                product.isSuspended(),
                product.isDeleted(),
                imgResponses,
                varResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDetailResponse updateProduct(UUID userId, UUID productId, UpdateProductRequest request) {
        log.info("Updating product id={} for userId={}", productId, userId);

        // 1. Resolve Seller and Shop (Isolation & Tiered Penalty BR-06, BR-07)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Rejecting product update.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // Tiered Penalty Model (BR-07): Shop BANNED or SUSPENDED is blocked from Write operations
        if (shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
            log.warn("Shop {} status {} is not active. Rejecting product update.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: Shop PAUSED or ACTIVE is allowed to edit product content

        // 2. Query Product with Multi-tenant Isolation (Anti-ID Enumeration: 404 if not found or belongs to another shop)
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            log.warn("Product {} is soft-deleted. Rejecting product update.", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 3. Resolve Category (BR-11A)
        Category targetCategory;
        if (product.getCategory().getId().equals(request.categoryId())) {
            targetCategory = product.getCategory();
            if (targetCategory.getStatus() == CategoryStatus.INACTIVE) {
                log.warn("Category {} is inactive. Rejecting product update.", targetCategory.getId());
                throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
            }
        } else {
            targetCategory = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            if (targetCategory.getStatus() == CategoryStatus.INACTIVE) {
                log.warn("Target category {} is inactive. Rejecting product update.", targetCategory.getId());
                throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
            }
        }

        // 4. Validate Strict Schema for Category Attributes (BR-09)
        List<CategoryAttribute> catAttrs = categoryAttributeRepository.findByCategoryId(targetCategory.getId());
        Map<String, Object> reqAttrs = request.attributes() != null ? request.attributes() : Collections.emptyMap();
        Set<String> expectedAttrNames = new HashSet<>();

        for (CategoryAttribute catAttr : catAttrs) {
            expectedAttrNames.add(catAttr.getName());
            Object val = reqAttrs.get(catAttr.getName());
            if (val == null || (val instanceof String s && s.trim().isEmpty())) {
                log.warn("Missing category attribute '{}' in product update request", catAttr.getName());
                throw new BusinessException(ErrorCode.MISSING_CATEGORY_ATTRIBUTE);
            }
            validateAttributeValue(catAttr, val);
        }

        for (String key : reqAttrs.keySet()) {
            if (!expectedAttrNames.contains(key)) {
                log.warn("Unknown attribute '{}' not defined in category schema", key);
                throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
            }
        }

        // 5. Load Existing Variants for response mapping
        List<ProductVariant> existingVariants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(product.getId());

        // 6. Validate Images (BR-11)
        List<CreateProductImageRequest> images = request.images();
        if (images == null || images.isEmpty() || images.size() > 6) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
        }

        int coverCount = 0;
        Set<Integer> displayOrders = new HashSet<>();
        for (CreateProductImageRequest img : images) {
            if (img.displayOrder() == null) {
                throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
            }
            if (img.displayOrder() == 0) {
                coverCount++;
            }
            if (!displayOrders.add(img.displayOrder())) {
                log.warn("Duplicate displayOrder {} found in product images update", img.displayOrder());
                throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
            }
        }
        if (coverCount != 1) {
            log.warn("Invalid product images update: expected exactly one cover image with displayOrder=0, found {}", coverCount);
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
        }

        // 7. Check Name Change & Slug Regeneration (BR-10)
        String trimmedNewName = request.name().trim();
        if (!product.getName().equalsIgnoreCase(trimmedNewName)) {
            String slug = SlugUtils.generateSlug(trimmedNewName);
            int attempts = 0;
            while (productRepository.existsBySlug(slug) && attempts < 5) {
                slug = SlugUtils.generateSlug(trimmedNewName);
                attempts++;
            }
            product.setSlug(slug);
        }

        // 8. Apply Content Change State Machine (BR-08)
        if (product.getApprovalStatus() == ProductApprovalStatus.APPROVED) {
            log.info("Product {} was APPROVED. Content update resets approvalStatus to PENDING_APPROVAL and visibilityStatus to UNPUBLISHED.", product.getId());
            product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
            product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
        }

        // 9. Update Product Entity Fields
        product.setCategory(targetCategory);
        product.setBrandName(request.brandName().trim());
        product.setName(trimmedNewName);
        product.setDescription(request.description());
        product.setAttributes(reqAttrs);
        Product updatedProduct = productRepository.save(product);

        // 10. Update Images (Delete and re-insert)
        productImageRepository.deleteByProductId(product.getId());
        List<ProductImage> imageEntities = images.stream()
                .map(img -> ProductImage.builder()
                        .product(updatedProduct)
                        .imageUrl(img.imageUrl().trim())
                        .displayOrder(img.displayOrder())
                        .build())
                .toList();
        List<ProductImage> savedImages = productImageRepository.saveAll(imageEntities);

        // 11. Map to Response DTO
        List<ProductImageResponse> imgResponses = savedImages.stream()
                .map(img -> new ProductImageResponse(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
                .toList();

        List<ProductVariantResponse> varResponses = existingVariants.stream()
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

        return new ProductDetailResponse(
                updatedProduct.getId(),
                shop.getId(),
                targetCategory.getId(),
                targetCategory.getName(),
                updatedProduct.getBrandName(),
                updatedProduct.getName(),
                updatedProduct.getSlug(),
                updatedProduct.getDescription(),
                updatedProduct.getAttributes() != null ? updatedProduct.getAttributes() : Collections.emptyMap(),
                updatedProduct.getApprovalStatus(),
                updatedProduct.getVisibilityStatus(),
                updatedProduct.isSuspended(),
                updatedProduct.isDeleted(),
                imgResponses,
                varResponses,
                updatedProduct.getCreatedAt(),
                updatedProduct.getUpdatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(UUID userId, UUID productId) {
        log.info("Processing deleteProduct for userId={}, productId={}", userId, productId);

        // 1. Validate Seller & Shop Status (BR-06, BR-07)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Rejecting product deletion.", seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.SUSPENDED || shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} status {} is not active. Rejecting product deletion.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: Shop PAUSED or ACTIVE is allowed to soft-delete product

        // 2. Query Product with Multi-tenant Isolation (BR-11B)
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. Check if already soft-deleted (BR-11B)
        if (product.isDeleted()) {
            log.warn("Product {} is already soft-deleted. Rejecting product deletion.", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 4. Soft-delete & unpublish (BR-11B)
        product.setDeleted(true);
        product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
        productRepository.save(product);

        log.info("Product {} soft-deleted successfully by userId={}", productId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVariantResponse addProductVariant(UUID userId, UUID productId, CreateProductVariantRequest request) {
        log.info("Processing addProductVariant for userId={}, productId={}", userId, productId);

        // 1. Resolve Seller and Shop (Isolation & Tiered Penalty BR-06, BR-07)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Rejecting variant addition.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
            log.warn("Shop {} status {} is not active. Rejecting variant addition.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: Shop PAUSED or ACTIVE is allowed to add variant (internal stock preparation)

        // 2. Query Product with Multi-tenant Isolation (Anti-ID Enumeration BR-11B)
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. Check if soft-deleted (BR-11B)
        if (product.isDeleted()) {
            log.warn("Product {} is soft-deleted. Rejecting variant addition.", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 4. Check Variant Count Limit (BR-11: maximum 50 variants)
        long variantCount = productVariantRepository.countByProductId(productId);
        if (variantCount >= 50) {
            log.warn("Product {} has reached maximum 50 variants (current count={}).", productId, variantCount);
            throw new BusinessException(ErrorCode.INVALID_VARIANT_COUNT);
        }

        // 5. Check Duplicate SKU within Product (BR-11: case-insensitive)
        String trimmedSku = request.sku().trim();
        if (productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, trimmedSku)) {
            log.warn("Duplicate SKU '{}' for product {}", trimmedSku, productId);
            throw new BusinessException(ErrorCode.DUPLICATE_VARIANT_SKU);
        }

        // 6. Sanitize Variant Attributes
        Map<String, Object> varAttrs = new HashMap<>();
        if (request.attributes() != null) {
            for (Map.Entry<String, Object> entry : request.attributes().entrySet()) {
                String cleanKey = entry.getKey() != null ? entry.getKey().trim() : "";
                if (!cleanKey.isEmpty()) {
                    varAttrs.put(cleanKey, entry.getValue());
                }
            }
        }

        // 7. Apply Content Change State Machine (BR-08)
        if (product.getApprovalStatus() == ProductApprovalStatus.APPROVED) {
            log.info("Product {} was APPROVED. Adding variant resets approvalStatus to PENDING_APPROVAL and visibilityStatus to UNPUBLISHED.", productId);
            product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
            product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
            productRepository.save(product);
        }

        // 8. Build and save ProductVariant entity
        String trimmedImageUrl = (request.imageUrl() != null && !request.imageUrl().isBlank())
                ? request.imageUrl().trim()
                : null;

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(trimmedSku)
                .variantName(request.variantName().trim())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .imageUrl(trimmedImageUrl)
                .attributes(varAttrs)
                .status(ProductVariantStatus.ACTIVE)
                .build();

        ProductVariant savedVariant = productVariantRepository.save(variant);
        log.info("Variant {} added successfully for product {}", savedVariant.getId(), productId);

        // 9. Map to Response DTO
        return new ProductVariantResponse(
                savedVariant.getId(),
                savedVariant.getSku(),
                savedVariant.getVariantName(),
                savedVariant.getPrice(),
                savedVariant.getStockQuantity(),
                savedVariant.getImageUrl(),
                savedVariant.getAttributes() != null ? savedVariant.getAttributes() : Collections.emptyMap(),
                savedVariant.getStatus()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVariantResponse updateProductVariant(UUID userId, UUID productId, UUID variantId, UpdateProductVariantRequest request) {
        log.info("Processing updateProductVariant for userId={}, productId={}, variantId={}", userId, productId, variantId);

        // 1. Resolve Seller and Shop (Isolation & Tiered Penalty BR-06, BR-07)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Rejecting variant update.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
            log.warn("Shop {} status {} is not active. Rejecting variant update.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: Shop PAUSED or ACTIVE is allowed to update variant (internal price/inventory management)

        // 2. Query Product with Multi-tenant Isolation (Anti-ID Enumeration BR-11B)
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. Check if soft-deleted (BR-11B)
        if (product.isDeleted()) {
            log.warn("Product {} is soft-deleted. Rejecting variant update.", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 4. Query Variant by variantId & productId (BR-11B)
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));

        // 5. Check Duplicate SKU within Product (BR-11: case-insensitive, excluding current variant)
        String trimmedSku = request.sku().trim();
        if (productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, trimmedSku, variantId)) {
            log.warn("Duplicate SKU '{}' for product {} (excluding variant {})", trimmedSku, productId, variantId);
            throw new BusinessException(ErrorCode.DUPLICATE_VARIANT_SKU);
        }

        // 6. Check Overlap with Product Specifications (BR-09) & Sanitize Keys
        Map<String, Object> varAttrs;
        if (request.attributes() != null) {
            varAttrs = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : request.attributes().entrySet()) {
                String cleanKey = entry.getKey() != null ? entry.getKey().trim() : "";
                if (!cleanKey.isEmpty()) {
                    varAttrs.put(cleanKey, entry.getValue());
                }
            }

        } else {
            // Defensive Preservation: If client omits attributes (null), preserve existing variant attributes
            varAttrs = variant.getAttributes() != null ? variant.getAttributes() : Collections.emptyMap();
        }

        // 7. Check Content Change vs Operational Change (BR-08)
        boolean contentChanged = isVariantContentChanged(variant, request, varAttrs);
        if (contentChanged && product.getApprovalStatus() == ProductApprovalStatus.APPROVED) {
            log.info("Product {} was APPROVED. Content change in variant {} resets approvalStatus to PENDING_APPROVAL and visibilityStatus to UNPUBLISHED.",
                    productId, variantId);
            product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
            product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
            productRepository.save(product);
        }

        // 8. Update Variant fields
        String trimmedImageUrl = (request.imageUrl() != null && !request.imageUrl().isBlank())
                ? request.imageUrl().trim()
                : null;
        String trimmedDescription = (request.description() != null && !request.description().isBlank())
                ? request.description().trim()
                : null;

        variant.setSku(trimmedSku);
        variant.setVariantName(request.variantName().trim());
        variant.setDescription(trimmedDescription);
        variant.setPrice(request.price());
        variant.setStockQuantity(request.stockQuantity());
        variant.setImageUrl(trimmedImageUrl);
        variant.setAttributes(varAttrs);
        if (request.status() != null) {
            variant.setStatus(request.status());
        }

        ProductVariant savedVariant = productVariantRepository.saveAndFlush(variant);
        log.info("Variant {} updated successfully for product {}", savedVariant.getId(), productId);

        // 9. Auto-unpublish if 100% variants become INACTIVE (BR-11B)
        if (savedVariant.getStatus() == ProductVariantStatus.INACTIVE && product.getVisibilityStatus() == ProductVisibilityStatus.PUBLISHED) {
            long activeCount = productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE);
            if (activeCount == 0) {
                log.info("All variants of product {} are now INACTIVE. Auto-unpublishing product.", productId);
                product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
                productRepository.save(product);
            }
        }

        // 10. Map to Response DTO
        return new ProductVariantResponse(
                savedVariant.getId(),
                savedVariant.getSku(),
                savedVariant.getVariantName(),
                savedVariant.getPrice(),
                savedVariant.getStockQuantity(),
                savedVariant.getImageUrl(),
                savedVariant.getAttributes() != null ? savedVariant.getAttributes() : Collections.emptyMap(),
                savedVariant.getStatus()
        );
    }

    private boolean isVariantContentChanged(ProductVariant variant, UpdateProductVariantRequest request, Map<String, Object> sanitizedAttrs) {
        // 1. variantName
        String newName = request.variantName() != null ? request.variantName().trim() : "";
        String oldName = variant.getVariantName() != null ? variant.getVariantName().trim() : "";
        if (!oldName.equals(newName)) {
            return true;
        }

        // 2. sku (case-insensitive)
        String newSku = request.sku() != null ? request.sku().trim() : "";
        String oldSku = variant.getSku() != null ? variant.getSku().trim() : "";
        if (!oldSku.equalsIgnoreCase(newSku)) {
            return true;
        }

        // 3. imageUrl (normalize blank to null)
        String newImg = (request.imageUrl() != null && !request.imageUrl().isBlank()) ? request.imageUrl().trim() : null;
        String oldImg = (variant.getImageUrl() != null && !variant.getImageUrl().isBlank()) ? variant.getImageUrl().trim() : null;
        if (!Objects.equals(oldImg, newImg)) {
            return true;
        }

        // 4. description (normalize blank to null)
        String newDesc = (request.description() != null && !request.description().isBlank()) ? request.description().trim() : null;
        String oldDesc = (variant.getDescription() != null && !variant.getDescription().isBlank()) ? variant.getDescription().trim() : null;
        if (!Objects.equals(oldDesc, newDesc)) {
            return true;
        }

        // 5. attributes (only evaluate change if request.attributes() is explicitly provided)
        if (request.attributes() != null) {
            Map<String, Object> oldAttrs = variant.getAttributes() != null ? variant.getAttributes() : Collections.emptyMap();
            if (!oldAttrs.equals(sanitizedAttrs)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductVariant(UUID userId, UUID productId, UUID variantId) {
        log.info("Processing deleteProductVariant for userId={}, productId={}, variantId={}", userId, productId, variantId);

        // 1. Resolve Seller and Shop (Isolation & Tiered Penalty BR-06, BR-07)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Rejecting variant deletion.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
            log.warn("Shop {} status {} is not active. Rejecting variant deletion.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: Shop PAUSED or ACTIVE is allowed to delete variant (inventory/catalog decommissioning)

        // 2. Query Product with Multi-tenant Isolation (Anti-ID Enumeration BR-11B)
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. Check if soft-deleted (BR-11B)
        if (product.isDeleted()) {
            log.warn("Product {} is soft-deleted. Rejecting variant deletion.", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 4. Query Variant by variantId & productId (BR-11B)
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));

        // 5. Invariant: Minimum 1 variant (BR-11)
        long totalVariants = productVariantRepository.countByProductId(productId);
        if (totalVariants <= 1) {
            log.warn("Product {} has only {} variant(s). Cannot delete the last remaining variant.", productId, totalVariants);
            throw new BusinessException(ErrorCode.CANNOT_DELETE_LAST_VARIANT);
        }

        // 6. Hard deletion of the variant (BR-08: Decommission does NOT reset approvalStatus)
        productVariantRepository.delete(variant);
        productVariantRepository.flush();
        log.info("Variant {} deleted successfully from product {}", variantId, productId);

        // 7. Auto-unpublish if 100% remaining variants are INACTIVE (BR-11B)
        if (product.getVisibilityStatus() == ProductVisibilityStatus.PUBLISHED) {
            long activeCount = productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE);
            if (activeCount == 0) {
                log.info("All remaining variants of product {} are INACTIVE. Auto-unpublishing product.", productId);
                product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
                productRepository.save(product);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDetailResponse submitProductForApproval(UUID userId, UUID productId) {
        log.info("Processing submitProductForApproval for userId={}, productId={}", userId, productId);

        // 1. Resolve Seller and Shop (Isolation & Tiered Penalty BR-06, BR-07)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Rejecting submit approval.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
            log.warn("Shop {} status {} is not active. Rejecting submit approval.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: Shop PAUSED or ACTIVE is allowed to submit product for approval (BR-07)

        // 2. Query Product with Multi-tenant Isolation (Anti-ID Enumeration BR-11B)
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. Check if soft-deleted (BR-11B)
        if (product.isDeleted()) {
            log.warn("Product {} is soft-deleted. Rejecting submit approval.", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 4. Validate Approval Status Transition (BR-08 State Machine)
        if (product.getApprovalStatus() != ProductApprovalStatus.DRAFT && product.getApprovalStatus() != ProductApprovalStatus.REJECTED) {
            log.warn("Product {} has invalid approvalStatus {} for submit approval. Must be DRAFT or REJECTED.",
                    productId, product.getApprovalStatus());
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 5. Validate Category Active (BR-11A)
        if (product.getCategory() == null || product.getCategory().getStatus() == CategoryStatus.INACTIVE) {
            log.warn("Product {} belongs to an inactive category. Rejecting submit approval.", productId);
            throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
        }

        // 6. Validate Product Readiness (BR-11 Invariants)
        // a. Variants check: must have at least 1 variant
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId);
        if (variants.isEmpty()) {
            log.warn("Product {} has no variants. Rejecting submit approval.", productId);
            throw new BusinessException(ErrorCode.INVALID_VARIANT_COUNT);
        }

        // b. Images check: must have 1-6 images with exactly one cover image (displayOrder 0)
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        if (images.isEmpty() || images.get(0).getDisplayOrder() != 0) {
            log.warn("Product {} has invalid images (total: {}, first order: {}). Rejecting submit approval.",
                    productId, images.size(), images.isEmpty() ? "N/A" : images.get(0).getDisplayOrder());
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_IMAGES);
        }

        // 7. Update status to PENDING_APPROVAL and UNPUBLISHED
        product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
        product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
        Product savedProduct = productRepository.save(product);
        log.info("Product {} successfully submitted for approval (status: PENDING_APPROVAL)", productId);

        return buildProductDetailResponse(savedProduct, shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDetailResponse updateProductVisibility(UUID userId, UUID productId, UpdateProductVisibilityRequest request) {
        log.info("Processing updateProductVisibility for userId={}, productId={}, targetVisibility={}",
                userId, productId, request.visibilityStatus());

        // 1. Resolve Seller and Shop (Isolation & Tiered Penalty BR-06, BR-07)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (status: {}). Rejecting visibility update.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
            log.warn("Shop {} status {} is not active. Rejecting visibility update.", shop.getId(), shop.getStatus());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // BR-07: Shop PAUSED is blocked from publishing new products, but allowed to unpublish
        if (request.visibilityStatus() == ProductVisibilityStatus.PUBLISHED && shop.getStatus() == ShopStatus.PAUSED) {
            log.warn("Shop {} is PAUSED. Rejecting publish product {}.", shop.getId(), productId);
            throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
        }

        // 2. Query Product with Multi-tenant Isolation (Anti-ID Enumeration BR-11B)
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. Check if soft-deleted (BR-11B)
        if (product.isDeleted()) {
            log.warn("Product {} is soft-deleted. Rejecting visibility update.", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 4. Idempotency Check (If already at requested visibility status, return directly)
        if (product.getVisibilityStatus() == request.visibilityStatus()) {
            log.info("Product {} is already {}. Returning directly (idempotent).", productId, request.visibilityStatus());
            return buildProductDetailResponse(product, shop);
        }

        // 5. Validate Publish Preconditions when transitioning to PUBLISHED
        if (request.visibilityStatus() == ProductVisibilityStatus.PUBLISHED) {
            // a. Approval Status Precondition (BR-08)
            if (product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
                log.warn("Product {} approvalStatus is {}. Only APPROVED products can be published.",
                        productId, product.getApprovalStatus());
                throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
            }

            // b. Suspended Check (BR-08 / BR-13)
            if (product.isSuspended()) {
                log.warn("Product {} is suspended by admin. Cannot be published.", productId);
                throw new BusinessException(ErrorCode.PRODUCT_SUSPENDED);
            }

            // c. Category Active Check (BR-11A)
            if (product.getCategory() == null || product.getCategory().getStatus() == CategoryStatus.INACTIVE) {
                log.warn("Product {} belongs to an inactive category. Rejecting publish.", productId);
                throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
            }

            // d. Active Variants Check (BR-11B Invariant)
            long activeVariantCount = productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE);
            if (activeVariantCount == 0) {
                log.warn("Product {} has 0 active variants. Rejecting publish.", productId);
                throw new BusinessException(ErrorCode.NO_ACTIVE_VARIANT);
            }
        }

        // 6. Update visibility status
        product.setVisibilityStatus(request.visibilityStatus());
        Product savedProduct = productRepository.save(product);
        log.info("Product {} visibility successfully updated to {}", productId, request.visibilityStatus());

        return buildProductDetailResponse(savedProduct, shop);
    }

    private ProductDetailResponse buildProductDetailResponse(Product product, Shop shop) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(product.getId());

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

        return new ProductDetailResponse(
                product.getId(),
                shop.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrandName(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getAttributes(),
                product.getApprovalStatus(),
                product.getVisibilityStatus(),
                product.isSuspended(),
                product.isDeleted(),
                imgResponses,
                varResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

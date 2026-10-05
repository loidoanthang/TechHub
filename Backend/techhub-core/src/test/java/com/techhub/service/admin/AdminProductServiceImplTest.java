package com.techhub.service.admin;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateProductApprovalRequest;
import com.techhub.model.dto.request.UpdateProductSuspendRequest;
import com.techhub.model.dto.response.AdminProductDetailResponse;
import com.techhub.model.dto.response.AdminProductSummaryResponse;
import com.techhub.model.entity.Category;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.ProductImage;
import com.techhub.model.entity.ProductVariant;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.enums.*;
import com.techhub.repository.ProductImageRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ProductVariantRepository;
import com.techhub.service.admin.impl.AdminProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminProductServiceImpl (GET /api/admin/products)")
class AdminProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private AdminProductServiceImpl adminProductService;

    private UUID shopId;
    private UUID categoryId;
    private Shop shop;
    private Category category;
    private Seller seller;

    @BeforeEach
    void setUp() {
        UUID sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        seller = Seller.builder()
                .id(sellerId)
                .sellerName("apple-vietnam")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        shop = Shop.builder()
                .id(shopId)
                .seller(seller)
                .name("Apple Flagship Store")
                .status(ShopStatus.ACTIVE)
                .build();

        category = Category.builder()
                .id(categoryId)
                .name("Điện thoại & Tablet")
                .status(CategoryStatus.ACTIVE)
                .build();
    }

    private Product createSampleProduct(String name, String brand, ProductApprovalStatus approval, ProductVisibilityStatus visibility, boolean isSuspended, boolean isDeleted) {
        return Product.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .category(category)
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID().toString().substring(0, 6))
                .brandName(brand)
                .description("Sample description")
                .approvalStatus(approval)
                .visibilityStatus(visibility)
                .isSuspended(isSuspended)
                .isDeleted(isDeleted)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("TC-01: getProducts_DefaultFilter_Success - Admin xem danh sách sản phẩm toàn sàn mặc định")
    void getProducts_DefaultFilter_Success() {
        Product p1 = createSampleProduct("iPhone 15 Pro", "Apple", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        Product p2 = createSampleProduct("Galaxy S24", "Samsung", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        Page<Product> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 20), 2);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        List<Object[]> coverImageRows = List.of(
                new Object[]{p1.getId(), "https://cdn.techhub.vn/p1-cover.jpg"},
                new Object[]{p2.getId(), "https://cdn.techhub.vn/p2-cover.jpg"}
        );
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(coverImageRows);

        List<Object[]> metricRows = List.of(
                new Object[]{p1.getId(), 2L, new BigDecimal("25000000"), new BigDecimal("30000000"), 100L},
                new Object[]{p2.getId(), 1L, new BigDecimal("20000000"), new BigDecimal("20000000"), 50L}
        );
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(metricRows);

        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                null, null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertEquals(2, response.totalElements());
        assertEquals(0, response.pageNumber());
        assertEquals(20, response.pageSize());

        AdminProductSummaryResponse dto1 = response.content().get(0);
        assertEquals(p1.getId(), dto1.id());
        assertEquals(shopId, dto1.shopId());
        assertEquals("Apple Flagship Store", dto1.shopName());
        assertEquals("apple-vietnam", dto1.sellerName());
        assertEquals(categoryId, dto1.categoryId());
        assertEquals("Điện thoại & Tablet", dto1.categoryName());
        assertEquals("https://cdn.techhub.vn/p1-cover.jpg", dto1.coverImageUrl());
        assertEquals(new BigDecimal("25000000"), dto1.minPrice());
        assertEquals(new BigDecimal("30000000"), dto1.maxPrice());
        assertEquals(100L, dto1.totalStock());
        assertEquals(2L, dto1.variantsCount());
        assertEquals(ProductApprovalStatus.APPROVED, dto1.approvalStatus());
        assertEquals(ProductVisibilityStatus.PUBLISHED, dto1.visibilityStatus());
    }

    @Test
    @DisplayName("TC-02: getProducts_FilterByApprovalStatus_PendingApproval - Lọc hàng đợi kiểm duyệt PENDING_APPROVAL")
    void getProducts_FilterByApprovalStatus_PendingApproval() {
        Product p = createSampleProduct("MacBook Pro M3", "Apple", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                null, null, null, ProductApprovalStatus.PENDING_APPROVAL, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, response.content().get(0).approvalStatus());
    }

    @Test
    @DisplayName("TC-03: getProducts_FilterByShopId_Success - Lọc sản phẩm của một Shop cụ thể")
    void getProducts_FilterByShopId_Success() {
        Product p = createSampleProduct("iPad Air 5", "Apple", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                shopId, null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(shopId, response.content().get(0).shopId());
    }

    @Test
    @DisplayName("TC-04: getProducts_FilterByCategoryId_Success - Lọc sản phẩm theo danh mục ngành hàng")
    void getProducts_FilterByCategoryId_Success() {
        Product p = createSampleProduct("Sony WH-1000XM5", "Sony", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                null, categoryId, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(categoryId, response.content().get(0).categoryId());
    }

    @Test
    @DisplayName("TC-05: getProducts_SearchByKeyword_Success - Tìm kiếm theo từ khóa keyword")
    void getProducts_SearchByKeyword_Success() {
        Product p = createSampleProduct("Logitech MX Master 3S", "Logitech", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                null, null, "Logitech", null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Logitech", response.content().get(0).brandName());
    }

    @Test
    @DisplayName("TC-06: getProducts_FilterByIsSuspended_Success - Lọc sản phẩm bị tạm đình chỉ vi phạm")
    void getProducts_FilterByIsSuspended_Success() {
        Product p = createSampleProduct("Fake AirPods Pro", "Apple", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, true, false);
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                null, null, null, null, null, true, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertTrue(response.content().get(0).isSuspended());
    }

    @Test
    @DisplayName("TC-07: getProducts_FilterByIsDeleted_Success - Xem sản phẩm trong thùng rác / đã xóa mềm")
    void getProducts_FilterByIsDeleted_Success() {
        Product p = createSampleProduct("Old Deleted Keyboard", "Keychron", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, false, true);
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                null, null, null, null, null, null, true, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertTrue(response.content().get(0).isDeleted());
    }

    @Test
    @DisplayName("TC-08: getProducts_SanitizePagination_NegativePageAndExcessiveSize - Sanitize page âm và size vượt ngưỡng")
    void getProducts_SanitizePagination_NegativePageAndExcessiveSize() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(emptyPage);

        adminProductService.getProducts(
                null, null, null, null, null, null, false, -5, 500, "createdAt", "desc"
        );

        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
        assertEquals(100, captured.getPageSize());
    }

    @Test
    @DisplayName("TC-09: getProducts_SanitizeSort_InvalidFieldFallbackToCreatedAt - Sort field không hợp lệ tự động fallback về createdAt")
    void getProducts_SanitizeSort_InvalidFieldFallbackToCreatedAt() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(emptyPage);

        adminProductService.getProducts(
                null, null, null, null, null, null, false, 0, 20, "invalid_column_inject", "asc"
        );

        Pageable captured = pageableCaptor.getValue();
        Sort.Order order = captured.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    @DisplayName("TC-10: getProducts_SupportSnakeCaseSort - Hỗ trợ sắp xếp theo snake_case updated_at")
    void getProducts_SupportSnakeCaseSort() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(emptyPage);

        adminProductService.getProducts(
                null, null, null, null, null, null, false, 0, 20, "updated_at", "desc"
        );

        Pageable captured = pageableCaptor.getValue();
        Sort.Order order = captured.getSort().getOrderFor("updatedAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    @DisplayName("TC-11: getProducts_EmptyPage_ReturnsEmptyWithoutQueryingImagesOrVariants - Trang rỗng không query ảnh hoặc biến thể")
    void getProducts_EmptyPage_ReturnsEmptyWithoutQueryingImagesOrVariants() {
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                null, null, "non_existent_keyword_xyz", null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());

        verify(productImageRepository, never()).findCoverImagesByProductIds(anyList());
        verify(productVariantRepository, never()).aggregateVariantMetricsByProductIds(anyList());
    }

    @Test
    @DisplayName("TC-12: getProducts_ProductWithoutVariantsOrImages_SafeFallback - Xử lý an toàn sản phẩm DRAFT chưa có ảnh hoặc biến thể")
    void getProducts_ProductWithoutVariantsOrImages_SafeFallback() {
        Product p = createSampleProduct("Empty Draft Product", "Generic", ProductApprovalStatus.DRAFT, ProductVisibilityStatus.UNPUBLISHED, false, false);
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        // Trả về rỗng cho ảnh bìa và metrics
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<AdminProductSummaryResponse> response = adminProductService.getProducts(
                null, null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());

        AdminProductSummaryResponse dto = response.content().get(0);
        assertEquals(p.getId(), dto.id());
        assertNull(dto.coverImageUrl());
        assertEquals(BigDecimal.ZERO, dto.minPrice());
        assertEquals(BigDecimal.ZERO, dto.maxPrice());
        assertEquals(0L, dto.totalStock());
        assertEquals(0L, dto.variantsCount());
        assertEquals(ProductApprovalStatus.DRAFT, dto.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, dto.visibilityStatus());
    }

    // ==========================================
    // TESTS FOR getProductDetail (API 11)
    // ==========================================

    @Test
    @DisplayName("TC-01-Detail: getProductDetail_Success_ReturnsFullDetailWithImagesAndVariants - Lấy chi tiết sản phẩm thành công với đầy đủ ảnh và biến thể")
    void getProductDetail_Success_ReturnsFullDetailWithImagesAndVariants() {
        Product product = createSampleProduct("iPhone 15 Pro Max", "Apple", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        product.setAttributes(Map.of("RAM", "8GB", "Chipset", "A17 Pro"));
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductImage img1 = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.vn/cover.jpg").displayOrder(0).build();
        ProductImage img2 = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.vn/detail.jpg").displayOrder(1).build();
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(img1, img2));

        ProductVariant var1 = ProductVariant.builder().id(UUID.randomUUID()).product(product).sku("IP15-256").variantName("256GB").price(new BigDecimal("29990000")).stockQuantity(50).status(ProductVariantStatus.ACTIVE).attributes(Map.of("ROM", "256GB")).build();
        ProductVariant var2 = ProductVariant.builder().id(UUID.randomUUID()).product(product).sku("IP15-512").variantName("512GB").price(new BigDecimal("34990000")).stockQuantity(30).status(ProductVariantStatus.ACTIVE).attributes(Map.of("ROM", "512GB")).build();
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(var1, var2));

        AdminProductDetailResponse response = adminProductService.getProductDetail(productId);

        assertNotNull(response);
        assertEquals(productId, response.id());
        assertEquals(shopId, response.shopId());
        assertEquals("Apple Flagship Store", response.shopName());
        assertEquals("apple-vietnam", response.sellerName());
        assertEquals(ShopStatus.ACTIVE, response.shopStatus());
        assertEquals(categoryId, response.categoryId());
        assertEquals("Điện thoại & Tablet", response.categoryName());
        assertEquals(CategoryStatus.ACTIVE, response.categoryStatus());
        assertEquals("Apple", response.brandName());
        assertEquals("iPhone 15 Pro Max", response.name());
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
        assertFalse(response.isSuspended());
        assertFalse(response.isDeleted());
        assertEquals("8GB", response.attributes().get("RAM"));
        assertEquals(2, response.images().size());
        assertEquals(0, response.images().get(0).displayOrder());
        assertEquals(2, response.variants().size());
        assertEquals("IP15-256", response.variants().get(0).sku());
    }

    @Test
    @DisplayName("TC-02-Detail: getProductDetail_ShopBanned_AuditMode_Success - Admin xem chi tiết sản phẩm thuộc Shop bị BANNED")
    void getProductDetail_ShopBanned_AuditMode_Success() {
        Shop bannedShop = Shop.builder().id(shopId).seller(seller).name("Banned Shop").status(ShopStatus.BANNED).build();
        Product product = createSampleProduct("Violating Product", "Unknown", ProductApprovalStatus.REJECTED, ProductVisibilityStatus.UNPUBLISHED, true, false);
        product.setShop(bannedShop);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());

        AdminProductDetailResponse response = adminProductService.getProductDetail(productId);

        assertNotNull(response);
        assertEquals(ShopStatus.BANNED, response.shopStatus());
        assertTrue(response.isSuspended());
    }

    @Test
    @DisplayName("TC-03-Detail: getProductDetail_ProductDeleted_AuditMode_Success - Admin xem chi tiết sản phẩm đã xóa mềm")
    void getProductDetail_ProductDeleted_AuditMode_Success() {
        Product product = createSampleProduct("Deleted Product", "BrandX", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, false, true);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());

        AdminProductDetailResponse response = adminProductService.getProductDetail(productId);

        assertNotNull(response);
        assertTrue(response.isDeleted());
    }

    @Test
    @DisplayName("TC-04-Detail: getProductDetail_CategoryInactive_Success - Admin xem chi tiết sản phẩm trong danh mục INACTIVE")
    void getProductDetail_CategoryInactive_Success() {
        Category inactiveCategory = Category.builder().id(categoryId).name("Old Category").status(CategoryStatus.INACTIVE).build();
        Product product = createSampleProduct("Hibernated Product", "BrandY", ProductApprovalStatus.DRAFT, ProductVisibilityStatus.UNPUBLISHED, false, false);
        product.setCategory(inactiveCategory);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());

        AdminProductDetailResponse response = adminProductService.getProductDetail(productId);

        assertNotNull(response);
        assertEquals(CategoryStatus.INACTIVE, response.categoryStatus());
    }

    @Test
    @DisplayName("TC-05-Detail: getProductDetail_NotFound_ThrowsProductNotFoundException - Ném PRODUCT_NOT_FOUND khi ID không tồn tại")
    void getProductDetail_NotFound_ThrowsProductNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.getProductDetail(nonExistentId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productImageRepository, never()).findByProductIdOrderByDisplayOrderAsc(any());
        verify(productVariantRepository, never()).findByProductIdOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("TC-06-Detail: getProductDetail_DraftWithoutImagesOrVariants_SafeFallback - Xử lý an toàn sản phẩm DRAFT chưa có ảnh hoặc biến thể")
    void getProductDetail_DraftWithoutImagesOrVariants_SafeFallback() {
        Product product = createSampleProduct("Draft Empty", "BrandZ", ProductApprovalStatus.DRAFT, ProductVisibilityStatus.UNPUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());

        AdminProductDetailResponse response = adminProductService.getProductDetail(productId);

        assertNotNull(response);
        assertNotNull(response.images());
        assertTrue(response.images().isEmpty());
        assertNotNull(response.variants());
        assertTrue(response.variants().isEmpty());
    }

    @Test
    @DisplayName("TC-07-Detail: getProductDetail_NullAttributes_SafeFallbackToEmptyMap - Xử lý an toàn khi attributes JSON là null")
    void getProductDetail_NullAttributes_SafeFallbackToEmptyMap() {
        Product product = createSampleProduct("No Specs Product", "BrandZ", ProductApprovalStatus.DRAFT, ProductVisibilityStatus.UNPUBLISHED, false, false);
        product.setAttributes(null);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());

        AdminProductDetailResponse response = adminProductService.getProductDetail(productId);

        assertNotNull(response);
        assertNotNull(response.attributes());
        assertTrue(response.attributes().isEmpty());
    }

    @Test
    @DisplayName("TC-08-Detail: getProductDetail_ImagesOrderAscending - Danh sách ảnh trả về đúng thứ tự displayOrder ASC")
    void getProductDetail_ImagesOrderAscending() {
        Product product = createSampleProduct("Image Ordered Product", "BrandW", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductImage img0 = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.vn/cover.jpg").displayOrder(0).build();
        ProductImage img1 = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.vn/back.jpg").displayOrder(1).build();
        ProductImage img2 = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.vn/side.jpg").displayOrder(2).build();

        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(img0, img1, img2));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());

        AdminProductDetailResponse response = adminProductService.getProductDetail(productId);

        assertNotNull(response);
        assertEquals(3, response.images().size());
        assertEquals(0, response.images().get(0).displayOrder());
        assertEquals(1, response.images().get(1).displayOrder());
        assertEquals(2, response.images().get(2).displayOrder());
    }

    @Test
    @DisplayName("TC-09-Detail: getProductDetail_VariantsOrderAscending - Danh sách biến thể trả về đúng thứ tự createdAt ASC")
    void getProductDetail_VariantsOrderAscending() {
        Product product = createSampleProduct("Variant Ordered Product", "BrandV", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());

        ProductVariant v1 = ProductVariant.builder().id(UUID.randomUUID()).product(product).sku("SKU-1").variantName("Variant 1").price(new BigDecimal("100000")).stockQuantity(10).status(ProductVariantStatus.ACTIVE).createdAt(LocalDateTime.now().minusHours(2)).build();
        ProductVariant v2 = ProductVariant.builder().id(UUID.randomUUID()).product(product).sku("SKU-2").variantName("Variant 2").price(new BigDecimal("200000")).stockQuantity(20).status(ProductVariantStatus.ACTIVE).createdAt(LocalDateTime.now().minusHours(1)).build();

        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(v1, v2));

        AdminProductDetailResponse response = adminProductService.getProductDetail(productId);

        assertNotNull(response);
        assertEquals(2, response.variants().size());
        assertEquals("SKU-1", response.variants().get(0).sku());
        assertEquals("SKU-2", response.variants().get(1).sku());
    }

    // ==========================================
    // TESTS FOR updateProductApproval (API 12)
    // ==========================================

    @Test
    @DisplayName("TC-01-Approval: updateProductApproval_Success_Approve_FromPendingApproval - Phê duyệt sản phẩm thành công từ PENDING_APPROVAL")
    void updateProductApproval_Success_Approve_FromPendingApproval() {
        Product product = createSampleProduct("iPhone 15 Pro", "Apple", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        product.setRejectionReason("Old rejection reason");
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductImage coverImg = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.vn/cover.jpg").displayOrder(0).build();
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(coverImg));

        ProductVariant variant = ProductVariant.builder().id(UUID.randomUUID()).product(product).sku("IP15-128").variantName("128GB").price(new BigDecimal("25000000")).stockQuantity(10).status(ProductVariantStatus.ACTIVE).build();
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(variant));

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        AdminProductDetailResponse response = adminProductService.updateProductApproval(productId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.APPROVED, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
        assertNull(response.rejectionReason());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("TC-02-Approval: updateProductApproval_Success_Reject_FromPendingApproval - Từ chối duyệt sản phẩm thành công từ PENDING_APPROVAL")
    void updateProductApproval_Success_Reject_FromPendingApproval() {
        Product product = createSampleProduct("Violating Product", "Unknown", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.REJECTED, "Ảnh mờ, thông số sai lệch");

        AdminProductDetailResponse response = adminProductService.updateProductApproval(productId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.REJECTED, response.approvalStatus());
        assertEquals("Ảnh mờ, thông số sai lệch", response.rejectionReason());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("TC-03-Approval: updateProductApproval_Success_Reject_WhenPublished_ResetsToUnpublished - Từ chối sản phẩm đang PUBLISHED tự động reset UNPUBLISHED (BR-08)")
    void updateProductApproval_Success_Reject_WhenPublished_ResetsToUnpublished() {
        Product product = createSampleProduct("Published Violating Product", "BrandA", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.REJECTED, "Phát hiện hàng giả mạo");

        AdminProductDetailResponse response = adminProductService.updateProductApproval(productId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.REJECTED, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
        assertEquals("Phát hiện hàng giả mạo", response.rejectionReason());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("TC-04-Approval: updateProductApproval_Success_Approve_ShopPaused - Phê duyệt sản phẩm thuộc Shop đang PAUSED thành công (BR-07)")
    void updateProductApproval_Success_Approve_ShopPaused() {
        Shop pausedShop = Shop.builder().id(shopId).seller(seller).name("Paused Shop").status(ShopStatus.PAUSED).build();
        Product product = createSampleProduct("Paused Shop Product", "BrandB", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        product.setShop(pausedShop);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductImage coverImg = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.vn/cover.jpg").displayOrder(0).build();
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(coverImg));

        ProductVariant variant = ProductVariant.builder().id(UUID.randomUUID()).product(product).sku("P-SKU").variantName("Standard").price(new BigDecimal("100000")).stockQuantity(5).status(ProductVariantStatus.ACTIVE).build();
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(variant));

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        AdminProductDetailResponse response = adminProductService.updateProductApproval(productId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.APPROVED, response.approvalStatus());
    }

    @Test
    @DisplayName("TC-05-Approval: updateProductApproval_Fail_Approve_ShopBanned_ThrowsShopNotActive - Chặn duyệt khi Shop bị cấm vĩnh viễn (BANNED)")
    void updateProductApproval_Fail_Approve_ShopBanned_ThrowsShopNotActive() {
        Shop bannedShop = Shop.builder().id(shopId).seller(seller).name("Banned Shop").status(ShopStatus.BANNED).build();
        Product product = createSampleProduct("Banned Shop Product", "BrandC", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        product.setShop(bannedShop);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-06-Approval: updateProductApproval_Fail_Approve_ShopSuspended_ThrowsShopNotActive - Chặn duyệt khi Shop bị đình chỉ (SUSPENDED)")
    void updateProductApproval_Fail_Approve_ShopSuspended_ThrowsShopNotActive() {
        Shop suspendedShop = Shop.builder().id(shopId).seller(seller).name("Suspended Shop").status(ShopStatus.SUSPENDED).build();
        Product product = createSampleProduct("Suspended Shop Product", "BrandD", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        product.setShop(suspendedShop);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-07-Approval: updateProductApproval_Fail_Approve_CategoryInactive_ThrowsCategoryInactive - Chặn duyệt khi Danh mục cha đang INACTIVE (BR-11A)")
    void updateProductApproval_Fail_Approve_CategoryInactive_ThrowsCategoryInactive() {
        Category inactiveCategory = Category.builder().id(categoryId).name("Inactive Category").status(CategoryStatus.INACTIVE).build();
        Product product = createSampleProduct("Inactive Cat Product", "BrandE", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        product.setCategory(inactiveCategory);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, request)
        );

        assertEquals(ErrorCode.CATEGORY_INACTIVE, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-08-Approval: updateProductApproval_Fail_Approve_ProductInDraft_ThrowsInvalidStatusTransition - Chặn duyệt trực tiếp sản phẩm DRAFT")
    void updateProductApproval_Fail_Approve_ProductInDraft_ThrowsInvalidStatusTransition() {
        Product product = createSampleProduct("Draft Product", "BrandF", ProductApprovalStatus.DRAFT, ProductVisibilityStatus.UNPUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, request)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-09-Approval: updateProductApproval_Fail_Approve_NoVariants_ThrowsInvalidVariantCount - Chặn duyệt sản phẩm chưa có biến thể (BR-11)")
    void updateProductApproval_Fail_Approve_NoVariants_ThrowsInvalidVariantCount() {
        Product product = createSampleProduct("No Variant Product", "BrandG", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, request)
        );

        assertEquals(ErrorCode.INVALID_VARIANT_COUNT, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-10-Approval: updateProductApproval_Fail_Approve_NoCoverImage_ThrowsInvalidProductImages - Chặn duyệt sản phẩm không có ảnh bìa displayOrder = 0 (BR-11)")
    void updateProductApproval_Fail_Approve_NoCoverImage_ThrowsInvalidProductImages() {
        Product product = createSampleProduct("No Cover Product", "BrandH", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductVariant variant = ProductVariant.builder().id(UUID.randomUUID()).product(product).sku("SKU-H").variantName("Var").price(new BigDecimal("100000")).stockQuantity(10).status(ProductVariantStatus.ACTIVE).build();
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(variant));

        // Chỉ có ảnh order = 1, không có ảnh order = 0
        ProductImage nonCoverImg = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.vn/detail.jpg").displayOrder(1).build();
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(nonCoverImg));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, request)
        );

        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-11-Approval: updateProductApproval_Fail_Reject_NullRejectionReason_ThrowsRejectionReasonRequired - Chặn từ chối khi rejectionReason là null")
    void updateProductApproval_Fail_Reject_NullRejectionReason_ThrowsRejectionReasonRequired() {
        Product product = createSampleProduct("Pending Product", "BrandI", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.REJECTED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, request)
        );

        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-12-Approval: updateProductApproval_Fail_Reject_BlankRejectionReason_ThrowsRejectionReasonRequired - Chặn từ chối khi rejectionReason chỉ chứa khoảng trắng")
    void updateProductApproval_Fail_Reject_BlankRejectionReason_ThrowsRejectionReasonRequired() {
        Product product = createSampleProduct("Pending Product", "BrandJ", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.REJECTED, "    ");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, request)
        );

        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-13-Approval: updateProductApproval_Fail_InvalidStatus_DraftOrPending_ThrowsInvalidStatusTransition - Chặn request gửi status là DRAFT hoặc PENDING_APPROVAL")
    void updateProductApproval_Fail_InvalidStatus_DraftOrPending_ThrowsInvalidStatusTransition() {
        UUID productId = UUID.randomUUID();

        UpdateProductApprovalRequest req1 = new UpdateProductApprovalRequest(ProductApprovalStatus.DRAFT, null);
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, req1)
        );
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex1.getErrorCode());

        UpdateProductApprovalRequest req2 = new UpdateProductApprovalRequest(ProductApprovalStatus.PENDING_APPROVAL, null);
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, req2)
        );
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex2.getErrorCode());

        verify(productRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC-14-Approval: updateProductApproval_Fail_ProductNotFound - Ném lỗi PRODUCT_NOT_FOUND khi ID không tồn tại")
    void updateProductApproval_Fail_ProductNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(nonExistentId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-15-Approval: updateProductApproval_Fail_ProductSoftDeleted - Ném lỗi PRODUCT_NOT_FOUND khi sản phẩm đã bị xóa mềm")
    void updateProductApproval_Fail_ProductSoftDeleted() {
        Product deletedProduct = createSampleProduct("Deleted Product", "BrandK", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, true);
        UUID productId = deletedProduct.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(deletedProduct));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductApproval(productId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-16-Approval: updateProductApproval_Success_Idempotent_AlreadyApproved - Idempotent Approve khi sản phẩm đã đang APPROVED")
    void updateProductApproval_Success_Idempotent_AlreadyApproved() {
        Product product = createSampleProduct("Already Approved Product", "BrandL", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductImage coverImg = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.vn/cover.jpg").displayOrder(0).build();
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(coverImg));

        ProductVariant variant = ProductVariant.builder().id(UUID.randomUUID()).product(product).sku("SKU-L").variantName("Var").price(new BigDecimal("100000")).stockQuantity(10).status(ProductVariantStatus.ACTIVE).build();
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(variant));

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductApprovalRequest request = new UpdateProductApprovalRequest(ProductApprovalStatus.APPROVED, null);

        AdminProductDetailResponse response = adminProductService.updateProductApproval(productId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.APPROVED, response.approvalStatus());
        verify(productRepository).save(product);
    }

    // ==========================================
    // TESTS FOR updateProductSuspend (API 13)
    // ==========================================

    @Test
    @DisplayName("TC-01-Suspend: updateProductSuspend_Success_SuspendActiveProduct - Đình chỉ sản phẩm đang hoạt động thành công")
    void updateProductSuspend_Success_SuspendActiveProduct() {
        Product product = createSampleProduct("Active Product", "BrandA", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(true);

        AdminProductDetailResponse response = adminProductService.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuspended());
        assertEquals(ProductApprovalStatus.APPROVED, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.PUBLISHED, response.visibilityStatus());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("TC-02-Suspend: updateProductSuspend_Success_RestoreSuspendedProduct - Mở khóa phục hồi sản phẩm đang bị đình chỉ thành công")
    void updateProductSuspend_Success_RestoreSuspendedProduct() {
        Product product = createSampleProduct("Suspended Product", "BrandB", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, true, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(false);

        AdminProductDetailResponse response = adminProductService.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertFalse(response.isSuspended());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("TC-03-Suspend: updateProductSuspend_Success_RestorePublishedProduct_AutoReady - Mở khóa sản phẩm đã APPROVED + PUBLISHED tự động sẵn sàng hiển thị (BR-11C)")
    void updateProductSuspend_Success_RestorePublishedProduct_AutoReady() {
        Product product = createSampleProduct("Published Suspended Product", "BrandC", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, true, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(false);

        AdminProductDetailResponse response = adminProductService.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertFalse(response.isSuspended());
        assertEquals(ProductApprovalStatus.APPROVED, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.PUBLISHED, response.visibilityStatus());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("TC-04-Suspend: updateProductSuspend_Success_RestoreUnpublishedProduct_RemainsUnpublished - Mở khóa sản phẩm UNPUBLISHED vẫn giữ nguyên UNPUBLISHED")
    void updateProductSuspend_Success_RestoreUnpublishedProduct_RemainsUnpublished() {
        Product product = createSampleProduct("Unpublished Suspended Product", "BrandD", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, true, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(false);

        AdminProductDetailResponse response = adminProductService.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertFalse(response.isSuspended());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("TC-05-Suspend: updateProductSuspend_Success_ShopPausedOrBanned_AdminPrivilege - Đình chỉ sản phẩm thuộc Shop đang PAUSED/BANNED thành công (Admin Privilege)")
    void updateProductSuspend_Success_ShopPausedOrBanned_AdminPrivilege() {
        Shop bannedShop = Shop.builder().id(shopId).seller(seller).name("Banned Shop").status(ShopStatus.BANNED).build();
        Product product = createSampleProduct("Product of Banned Shop", "BrandE", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, false, false);
        product.setShop(bannedShop);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(true);

        AdminProductDetailResponse response = adminProductService.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuspended());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("TC-06-Suspend: updateProductSuspend_Success_DraftProduct_PreemptiveSuspend - Đình chỉ sản phẩm đang DRAFT thành công")
    void updateProductSuspend_Success_DraftProduct_PreemptiveSuspend() {
        Product product = createSampleProduct("Draft Product", "BrandF", ProductApprovalStatus.DRAFT, ProductVisibilityStatus.UNPUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(true);

        AdminProductDetailResponse response = adminProductService.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuspended());
        assertEquals(ProductApprovalStatus.DRAFT, response.approvalStatus());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("TC-07-Suspend: updateProductSuspend_Success_Idempotent_AlreadySuspended - Idempotent Suspend khi sản phẩm đã đang is_suspended = true")
    void updateProductSuspend_Success_Idempotent_AlreadySuspended() {
        Product product = createSampleProduct("Already Suspended", "BrandG", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, true, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(true);

        AdminProductDetailResponse response = adminProductService.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertTrue(response.isSuspended());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-08-Suspend: updateProductSuspend_Success_Idempotent_AlreadyActive - Idempotent Restore khi sản phẩm đã đang is_suspended = false")
    void updateProductSuspend_Success_Idempotent_AlreadyActive() {
        Product product = createSampleProduct("Already Active", "BrandH", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(Collections.emptyList());

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(false);

        AdminProductDetailResponse response = adminProductService.updateProductSuspend(productId, request);

        assertNotNull(response);
        assertFalse(response.isSuspended());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-09-Suspend: updateProductSuspend_NotFound_ThrowsException - Ném lỗi PRODUCT_NOT_FOUND khi ID không tồn tại")
    void updateProductSuspend_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductSuspend(nonExistentId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-10-Suspend: updateProductSuspend_SoftDeleted_ThrowsException - Ném lỗi PRODUCT_NOT_FOUND khi sản phẩm đã bị xóa mềm")
    void updateProductSuspend_SoftDeleted_ThrowsException() {
        Product product = createSampleProduct("Deleted Product", "BrandI", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, false, true);
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        UpdateProductSuspendRequest request = new UpdateProductSuspendRequest(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminProductService.updateProductSuspend(productId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }
}

package com.techhub.service.seller;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.SellerProductSummaryResponse;
import com.techhub.model.entity.Category;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.seller.impl.SellerProductServiceImpl;
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
@DisplayName("Unit Tests cho SellerProductServiceImpl (GET /api/seller/products)")
class SellerProductGetProductsTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryAttributeRepository categoryAttributeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private SellerProductServiceImpl sellerProductService;

    private UUID userId;
    private UUID sellerId;
    private UUID shopId;
    private UUID categoryId;
    private Seller seller;
    private Shop shop;
    private Category category;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        seller = Seller.builder()
                .id(sellerId)
                .sellerName("techzone-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        shop = Shop.builder()
                .id(shopId)
                .seller(seller)
                .name("TechZone Official")
                .status(ShopStatus.ACTIVE)
                .build();

        category = Category.builder()
                .id(categoryId)
                .name("Bàn phím & Chuột")
                .status(CategoryStatus.ACTIVE)
                .build();
    }

    private Product createSampleProduct(String name, String brand, ProductApprovalStatus approval, ProductVisibilityStatus visibility, boolean isSuspended, boolean isDeleted) {
        return Product.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .category(category)
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID().toString().substring(0, 8))
                .brandName(brand)
                .approvalStatus(approval)
                .visibilityStatus(visibility)
                .isSuspended(isSuspended)
                .isDeleted(isDeleted)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==========================================
    // TC-01: getProducts_Success_DefaultPagination
    // ==========================================
    @Test
    @DisplayName("TC-01: Lấy danh sách sản phẩm thành công với tham số phân trang mặc định")
    void getProducts_Success_DefaultPagination() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product p1 = createSampleProduct("Bàn phím cơ", "Akko", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        Product p2 = createSampleProduct("Chuột gaming", "Razer", ProductApprovalStatus.PENDING_APPROVAL, ProductVisibilityStatus.UNPUBLISHED, false, false);

        Page<Product> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 20), 2);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        List<Object[]> imageRows = new ArrayList<>();
        imageRows.add(new Object[]{p1.getId(), "https://cdn.techhub.com/cover1.jpg"});
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(imageRows);

        List<Object[]> metricRows = new ArrayList<>();
        metricRows.add(new Object[]{p1.getId(), 2L, new BigDecimal("100000"), new BigDecimal("200000"), 50L});
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(metricRows);

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertEquals(2, response.totalElements());
        assertEquals("Bàn phím cơ", response.content().get(0).name());
        assertEquals("https://cdn.techhub.com/cover1.jpg", response.content().get(0).coverImageUrl());
        assertEquals(new BigDecimal("100000"), response.content().get(0).minPrice());
        assertEquals(50L, response.content().get(0).totalStock());
        assertEquals(2L, response.content().get(0).variantsCount());

        // Sản phẩm thứ 2 không có ảnh cover và metrics trong DB -> fallback safe values
        assertNull(response.content().get(1).coverImageUrl());
        assertEquals(BigDecimal.ZERO, response.content().get(1).minPrice());
        assertEquals(0L, response.content().get(1).totalStock());
    }

    // ==========================================
    // TC-02: getProducts_Success_FilterByApprovalStatus_Approved
    // ==========================================
    @Test
    @DisplayName("TC-02: Lọc sản phẩm theo approvalStatus = APPROVED")
    void getProducts_Success_FilterByApprovalStatus_Approved() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product p1 = createSampleProduct("Bàn phím cơ", "Akko", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p1), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, ProductApprovalStatus.APPROVED, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertEquals(ProductApprovalStatus.APPROVED, response.content().get(0).approvalStatus());
    }

    // ==========================================
    // TC-03: getProducts_Success_FilterByApprovalStatus_Draft
    // ==========================================
    @Test
    @DisplayName("TC-03: Lọc sản phẩm theo approvalStatus = DRAFT")
    void getProducts_Success_FilterByApprovalStatus_Draft() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product pDraft = createSampleProduct("Bàn phím nháp", "Logitech", ProductApprovalStatus.DRAFT, ProductVisibilityStatus.UNPUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pDraft), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, ProductApprovalStatus.DRAFT, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertEquals(ProductApprovalStatus.DRAFT, response.content().get(0).approvalStatus());
    }

    // ==========================================
    // TC-04: getProducts_Success_FilterByVisibilityStatus_Published
    // ==========================================
    @Test
    @DisplayName("TC-04: Lọc sản phẩm theo visibilityStatus = PUBLISHED")
    void getProducts_Success_FilterByVisibilityStatus_Published() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product pPub = createSampleProduct("Chuột không dây", "Logitech", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pPub), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, ProductVisibilityStatus.PUBLISHED, null, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertEquals(ProductVisibilityStatus.PUBLISHED, response.content().get(0).visibilityStatus());
    }

    // ==========================================
    // TC-05: getProducts_Success_FilterByCategoryId
    // ==========================================
    @Test
    @DisplayName("TC-05: Lọc sản phẩm theo categoryId")
    void getProducts_Success_FilterByCategoryId() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product p = createSampleProduct("Bàn phím Akko", "Akko", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, categoryId, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertEquals(categoryId, response.content().get(0).categoryId());
    }

    // ==========================================
    // TC-06: getProducts_Success_FilterByIsSuspended_True
    // ==========================================
    @Test
    @DisplayName("TC-06: Lọc sản phẩm bị Admin đình chỉ (isSuspended = true)")
    void getProducts_Success_FilterByIsSuspended_True() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product pSuspended = createSampleProduct("Sản phẩm vi phạm", "FakeBrand", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, true, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pSuspended), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, null, true, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertTrue(response.content().get(0).isSuspended());
    }

    // ==========================================
    // TC-07: getProducts_Success_FilterByIsDeleted_DefaultActiveOnly
    // ==========================================
    @Test
    @DisplayName("TC-07: Mặc định chỉ lấy sản phẩm chưa xóa mềm (isDeleted = false)")
    void getProducts_Success_FilterByIsDeleted_DefaultActiveOnly() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product pActive = createSampleProduct("Bàn phím cơ", "Akko", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pActive), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, null, null, null, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertFalse(response.content().get(0).isDeleted());
    }

    // ==========================================
    // TC-08: getProducts_Success_FilterByIsDeleted_TrueTrashTab
    // ==========================================
    @Test
    @DisplayName("TC-08: Lấy sản phẩm trong Thùng rác (isDeleted = true)")
    void getProducts_Success_FilterByIsDeleted_TrueTrashTab() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product pDeleted = createSampleProduct("Bàn phím cũ", "Akko", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, false, true);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pDeleted), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, null, null, true, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertTrue(response.content().get(0).isDeleted());
    }

    // ==========================================
    // TC-09: getProducts_Success_SearchByKeyword_MatchingName
    // ==========================================
    @Test
    @DisplayName("TC-09: Tìm kiếm theo từ khóa khớp tên sản phẩm")
    void getProducts_Success_SearchByKeyword_MatchingName() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product p = createSampleProduct("Bàn phím cơ Akko 3087", "Akko", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, "Akko 3087", null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertEquals("Bàn phím cơ Akko 3087", response.content().get(0).name());
    }

    // ==========================================
    // TC-10: getProducts_Success_SearchByKeyword_MatchingBrandName
    // ==========================================
    @Test
    @DisplayName("TC-10: Tìm kiếm theo từ khóa khớp thương hiệu")
    void getProducts_Success_SearchByKeyword_MatchingBrandName() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product p = createSampleProduct("Chuột gaming DeathAdder", "Razer", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, "razer", null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertEquals("Razer", response.content().get(0).brandName());
    }

    // ==========================================
    // TC-11: getProducts_Success_SearchByKeyword_CaseInsensitiveAndTrim
    // ==========================================
    @Test
    @DisplayName("TC-11: Từ khóa tìm kiếm tự động trim và không phân biệt hoa thường")
    void getProducts_Success_SearchByKeyword_CaseInsensitiveAndTrim() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product p = createSampleProduct("Tai nghe BlackShark", "Razer", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, "   RAZER   ", null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertEquals("Razer", response.content().get(0).brandName());
    }

    // ==========================================
    // TC-12: getProducts_Success_SortByNameAsc
    // ==========================================
    @Test
    @DisplayName("TC-12: Sắp xếp theo tên sản phẩm tăng dần (name asc)")
    void getProducts_Success_SortByNameAsc() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        sellerProductService.getProducts(
                userId, null, null, null, null, null, false, 0, 20, "name", "asc"
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        Sort.Order order = capturedPageable.getSort().getOrderFor("name");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    // ==========================================
    // TC-13: getProducts_Success_InvalidSortByField_FallbackToCreatedAt
    // ==========================================
    @Test
    @DisplayName("TC-13: Trường sortBy không hợp lệ tự động fallback về 'createdAt'")
    void getProducts_Success_InvalidSortByField_FallbackToCreatedAt() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        sellerProductService.getProducts(
                userId, null, null, null, null, null, false, 0, 20, "maliciousColumn", "desc"
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        Sort.Order order = capturedPageable.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    // ==========================================
    // TC-14: getProducts_Success_PageOutOfBounds_ReturnsEmptyList
    // ==========================================
    @Test
    @DisplayName("TC-14: Trang vượt quá dữ liệu trả về mảng rỗng và không gọi thêm truy vấn batch")
    void getProducts_Success_PageOutOfBounds_ReturnsEmptyList() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Pageable pageable = PageRequest.of(99, 20);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 5));

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, null, null, false, 99, 20, "createdAt", "desc"
        );

        assertTrue(response.content().isEmpty());
        assertEquals(5, response.totalElements());
        assertEquals(99, response.pageNumber());

        // Zero additional queries
        verify(productImageRepository, never()).findCoverImagesByProductIds(anyList());
        verify(productVariantRepository, never()).aggregateVariantMetricsByProductIds(anyList());
    }

    // ==========================================
    // TC-15: getProducts_Success_SizeCapAt100
    // ==========================================
    @Test
    @DisplayName("TC-15: Kích thước trang size=500 tự động giới hạn về 100")
    void getProducts_Success_SizeCapAt100() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 100), 0));

        sellerProductService.getProducts(
                userId, null, null, null, null, null, false, 0, 500, "createdAt", "desc"
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(100, capturedPageable.getPageSize());
    }

    // ==========================================
    // TC-16: getProducts_Success_NegativePageAndSize_HandledSafely
    // ==========================================
    @Test
    @DisplayName("TC-16: Số trang hoặc kích thước trang âm được chuẩn hóa an toàn")
    void getProducts_Success_NegativePageAndSize_HandledSafely() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1), 0));

        sellerProductService.getProducts(
                userId, null, null, null, null, null, false, -5, -10, "createdAt", "desc"
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(1, capturedPageable.getPageSize());
    }

    // ==========================================
    // TC-17: getProducts_Success_MultiTenantIsolation_NeverReturnOtherShopProducts
    // ==========================================
    @Test
    @DisplayName("TC-17: Cô lập dữ liệu đa người bán, luôn gán shopId của Seller đang đăng nhập")
    void getProducts_Success_MultiTenantIsolation_NeverReturnOtherShopProducts() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product myProduct = createSampleProduct("Sản phẩm của Shop A", "Brand A", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(myProduct), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        assertEquals(myProduct.getId(), response.content().get(0).id());
        verify(shopRepository).findBySellerId(sellerId);
    }

    // ==========================================
    // TC-18: getProducts_Success_ShopSuspended_ReadonlyAllowedForDispute
    // ==========================================
    @Test
    @DisplayName("TC-18: Shop bị SUSPENDED vẫn được phép xem sản phẩm ở chế độ Read-only để đối soát/kháng cáo (BR-07)")
    void getProducts_Success_ShopSuspended_ReadonlyAllowedForDispute() {
        shop.setStatus(ShopStatus.SUSPENDED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product p = createSampleProduct("SP đang tranh chấp", "Brand", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.UNPUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    // ==========================================
    // TC-19: getProducts_Success_ShopPaused_ReadonlyAllowed
    // ==========================================
    @Test
    @DisplayName("TC-19: Shop bị PAUSED vẫn được phép xem sản phẩm bình thường (BR-07)")
    void getProducts_Success_ShopPaused_ReadonlyAllowed() {
        shop.setStatus(ShopStatus.PAUSED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product p = createSampleProduct("SP tạm nghỉ", "Brand", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    // ==========================================
    // TC-20: getProducts_Fail_ShopBanned_ThrowsShopNotActive
    // ==========================================
    @Test
    @DisplayName("TC-20: Shop bị BANNED bị từ chối truy cập với lỗi SHOP_NOT_ACTIVE (BR-07 Hard Lockout)")
    void getProducts_Fail_ShopBanned_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProducts(userId, null, null, null, null, null, false, 0, 20, "createdAt", "desc")
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ==========================================
    // TC-21: getProducts_Success_BatchAggregationMetrics_CorrectPriceRangeAndStock
    // ==========================================
    @Test
    @DisplayName("TC-21: Tính toán chính xác dải giá (minPrice, maxPrice) và tổng kho từ batch aggregation")
    void getProducts_Success_BatchAggregationMetrics_CorrectPriceRangeAndStock() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        Product p = createSampleProduct("Bàn phím cơ nhiều switch", "Akko", ProductApprovalStatus.APPROVED, ProductVisibilityStatus.PUBLISHED, false, false);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        List<Object[]> metricRows = new ArrayList<>();
        // row: [productId, count, minPrice, maxPrice, totalStock]
        metricRows.add(new Object[]{p.getId(), 3L, new BigDecimal("1250000.00"), new BigDecimal("1850000.00"), 120L});
        when(productVariantRepository.aggregateVariantMetricsByProductIds(anyList())).thenReturn(metricRows);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerProductSummaryResponse> response = sellerProductService.getProducts(
                userId, null, null, null, null, null, false, 0, 20, "createdAt", "desc"
        );

        assertEquals(1, response.content().size());
        SellerProductSummaryResponse dto = response.content().get(0);
        assertEquals(3L, dto.variantsCount());
        assertEquals(new BigDecimal("1250000.00"), dto.minPrice());
        assertEquals(new BigDecimal("1850000.00"), dto.maxPrice());
        assertEquals(120L, dto.totalStock());
    }

    // ==========================================
    // TC-22: getProducts_Fail_SellerProfileNotFound
    // ==========================================
    @Test
    @DisplayName("TC-22: Ném SELLER_NOT_FOUND khi tài khoản chưa có hồ sơ Seller")
    void getProducts_Fail_SellerProfileNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProducts(userId, null, null, null, null, null, false, 0, 20, "createdAt", "desc")
        );

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // TC-23: getProducts_Fail_ShopNotFound
    // ==========================================
    @Test
    @DisplayName("TC-23: Ném SHOP_NOT_FOUND khi Seller chưa có Shop")
    void getProducts_Fail_ShopNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProducts(userId, null, null, null, null, null, false, 0, 20, "createdAt", "desc")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }
}

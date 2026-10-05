package com.techhub.service.product;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.ProductSummaryResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.ProductImageRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ProductVariantRepository;
import com.techhub.service.product.impl.ProductServiceImpl;
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
@DisplayName("Unit Tests cho ProductServiceImpl (GET /api/products)")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID categoryId;
    private UUID shopId;
    private Category category;
    private Shop shop;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        shopId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        shop = Shop.builder()
                .id(shopId)
                .name("Apple Official Store")
                .status(ShopStatus.ACTIVE)
                .build();

        product1 = Product.builder()
                .id(UUID.randomUUID())
                .name("MacBook Air M3")
                .slug("macbook-air-m3")
                .brandName("Apple")
                .category(category)
                .shop(shop)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        product2 = Product.builder()
                .id(UUID.randomUUID())
                .name("Dell XPS 13")
                .slug("dell-xps-13")
                .brandName("Dell")
                .category(category)
                .shop(shop)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        product3 = Product.builder()
                .id(UUID.randomUUID())
                .name("iPad Pro M4")
                .slug("ipad-pro-m4")
                .brandName("Apple")
                .category(category)
                .shop(shop)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("TC-01: getPublicProducts_Success_DefaultParams")
    void getPublicProducts_Success_DefaultParams() {
        List<Product> products = List.of(product1, product2, product3);
        Page<Product> page = new PageImpl<>(products, PageRequest.of(0, 20), 3);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(List.of(
                new Object[]{product1.getId(), "https://cdn.techhub.com/p1-cover.jpg"},
                new Object[]{product2.getId(), "https://cdn.techhub.com/p2-cover.jpg"}
        ));
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(List.of(
                new Object[]{product1.getId(), 2L, new BigDecimal("27490000"), new BigDecimal("32990000"), 10L},
                new Object[]{product2.getId(), 1L, new BigDecimal("34990000"), new BigDecimal("34990000"), 5L},
                new Object[]{product3.getId(), 1L, new BigDecimal("22990000"), new BigDecimal("22990000"), 0L}
        ));

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertNotNull(response);
        assertEquals(3, response.content().size());
        assertEquals(3, response.totalElements());
        assertEquals(0, response.pageNumber());
        assertEquals(20, response.pageSize());

        ProductSummaryResponse p1 = response.content().get(0);
        assertEquals(product1.getId(), p1.id());
        assertEquals("MacBook Air M3", p1.name());
        assertEquals("Apple", p1.brandName());
        assertEquals("Laptop", p1.categoryName());
        assertEquals("Apple Official Store", p1.shopName());
        assertFalse(p1.isShopPaused());
        assertEquals(new BigDecimal("27490000"), p1.price());
        assertTrue(p1.inStock());
        assertEquals("https://cdn.techhub.com/p1-cover.jpg", p1.coverImageUrl());

        ProductSummaryResponse p3 = response.content().get(2);
        assertFalse(p3.inStock()); // totalStock is 0
    }

    @Test
    @DisplayName("TC-02: getPublicProducts_FilterByKeyword_MatchesNameOrBrand")
    void getPublicProducts_FilterByKeyword_MatchesNameOrBrand() {
        Page<Product> page = new PageImpl<>(List.of(product1, product3), PageRequest.of(0, 20), 2);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                "apple", null, null, null, null, false, "newest", 0, 20
        );

        assertEquals(2, response.content().size());
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-03: getPublicProducts_FilterByCategoryId_MatchesCategory")
    void getPublicProducts_FilterByCategoryId_MatchesCategory() {
        Page<Product> page = new PageImpl<>(List.of(product1), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, categoryId, null, null, null, false, "newest", 0, 20
        );

        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-04: getPublicProducts_FilterByBrandName_CaseInsensitive")
    void getPublicProducts_FilterByBrandName_CaseInsensitive() {
        Page<Product> page = new PageImpl<>(List.of(product2), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, "DELL", null, null, false, "newest", 0, 20
        );

        assertEquals(1, response.content().size());
        assertEquals("Dell XPS 13", response.content().get(0).name());
    }

    @Test
    @DisplayName("TC-05: getPublicProducts_FilterByPriceRange_Success")
    void getPublicProducts_FilterByPriceRange_Success() {
        Page<Product> page = new PageImpl<>(List.of(product1), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, new BigDecimal("25000000"), new BigDecimal("30000000"), false, "newest", 0, 20
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-06: getPublicProducts_FilterInStockOnly_ExcludesOutOfStock")
    void getPublicProducts_FilterInStockOnly_ExcludesOutOfStock() {
        Page<Product> page = new PageImpl<>(List.of(product1, product2), PageRequest.of(0, 20), 2);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, true, "newest", 0, 20
        );

        assertEquals(2, response.content().size());
    }

    @Test
    @DisplayName("TC-07: getPublicProducts_SortByNewest_OrderedByCreatedAtDesc")
    void getPublicProducts_SortByNewest_OrderedByCreatedAtDesc() {
        Page<Product> page = new PageImpl<>(List.of(product3, product2, product1), PageRequest.of(0, 20), 3);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        productService.getPublicProducts(null, null, null, null, null, false, "newest", 0, 20);

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), capturedPageable.getSort());
    }

    @Test
    @DisplayName("TC-08: getPublicProducts_SortByPriceAsc_Success")
    void getPublicProducts_SortByPriceAsc_Success() {
        Page<Product> page = new PageImpl<>(List.of(product3, product1, product2), PageRequest.of(0, 20), 3);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        productService.getPublicProducts(null, null, null, null, null, false, "price_asc", 0, 20);

        Pageable capturedPageable = pageableCaptor.getValue();
        assertTrue(capturedPageable.getSort().isUnsorted()); // Sorted via CriteriaBuilder subquery
    }

    @Test
    @DisplayName("TC-09: getPublicProducts_SortByPriceDesc_Success")
    void getPublicProducts_SortByPriceDesc_Success() {
        Page<Product> page = new PageImpl<>(List.of(product2, product1, product3), PageRequest.of(0, 20), 3);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        productService.getPublicProducts(null, null, null, null, null, false, "price_desc", 0, 20);

        Pageable capturedPageable = pageableCaptor.getValue();
        assertTrue(capturedPageable.getSort().isUnsorted()); // Sorted via CriteriaBuilder subquery
    }

    @Test
    @DisplayName("TC-10: getPublicProducts_Gatekeeper_ExcludesUnapproved")
    void getPublicProducts_Gatekeeper_ExcludesUnapproved() {
        // Only APPROVED products returned by repo
        Page<Product> page = new PageImpl<>(List.of(product1), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertEquals(1, response.content().size());
        assertEquals(ProductApprovalStatus.APPROVED, product1.getApprovalStatus());
    }

    @Test
    @DisplayName("TC-11: getPublicProducts_Gatekeeper_ExcludesUnpublished")
    void getPublicProducts_Gatekeeper_ExcludesUnpublished() {
        Page<Product> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertTrue(response.content().isEmpty());
    }

    @Test
    @DisplayName("TC-12: getPublicProducts_Gatekeeper_ExcludesSuspendedOrDeleted")
    void getPublicProducts_Gatekeeper_ExcludesSuspendedOrDeleted() {
        Page<Product> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertEquals(0, response.totalElements());
    }

    @Test
    @DisplayName("TC-13: getPublicProducts_Gatekeeper_ExcludesInactiveCategory")
    void getPublicProducts_Gatekeeper_ExcludesInactiveCategory() {
        Page<Product> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertEquals(0, response.totalElements());
    }

    @Test
    @DisplayName("TC-14: getPublicProducts_Gatekeeper_ExcludesSuspendedOrBannedShop")
    void getPublicProducts_Gatekeeper_ExcludesSuspendedOrBannedShop() {
        Page<Product> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertTrue(response.content().isEmpty());
    }

    @Test
    @DisplayName("TC-15: getPublicProducts_Gatekeeper_ShopPaused_IncludedWithFlag")
    void getPublicProducts_Gatekeeper_ShopPaused_IncludedWithFlag() {
        Shop pausedShop = Shop.builder()
                .id(UUID.randomUUID())
                .name("Paused Shop")
                .status(ShopStatus.PAUSED)
                .build();

        Product productPaused = Product.builder()
                .id(UUID.randomUUID())
                .name("Keyboard Custom")
                .slug("keyboard-custom")
                .brandName("Keychron")
                .category(category)
                .shop(pausedShop)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Product> page = new PageImpl<>(List.of(productPaused), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.singletonList(
                new Object[]{productPaused.getId(), 1L, new BigDecimal("3500000"), new BigDecimal("3500000"), 5L}
        ));

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertEquals(1, response.content().size());
        ProductSummaryResponse result = response.content().get(0);
        assertTrue(result.isShopPaused());
        assertEquals("Paused Shop", result.shopName());
    }

    @Test
    @DisplayName("TC-16: getPublicProducts_Gatekeeper_ExcludesBannedOrDeletedUser")
    void getPublicProducts_Gatekeeper_ExcludesBannedOrDeletedUser() {
        Page<Product> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertEquals(0, response.totalElements());
    }

    @Test
    @DisplayName("TC-17: getPublicProducts_Gatekeeper_ExcludesProductWithNoActiveVariants")
    void getPublicProducts_Gatekeeper_ExcludesProductWithNoActiveVariants() {
        Page<Product> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                null, null, null, null, null, false, "newest", 0, 20
        );

        assertTrue(response.content().isEmpty());
    }

    @Test
    @DisplayName("TC-18: getPublicProducts_InvalidPriceRange_MinGreaterThanMax_Throws400")
    void getPublicProducts_InvalidPriceRange_MinGreaterThanMax_Throws400() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProducts(
                        null, null, null, new BigDecimal("30000000"), new BigDecimal("10000000"), false, "newest", 0, 20
                )
        );

        assertEquals(ErrorCode.INVALID_PRICE_RANGE, ex.getErrorCode());
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("TC-19: getPublicProducts_NegativePrice_Throws400")
    void getPublicProducts_NegativePrice_Throws400() {
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                productService.getPublicProducts(
                        null, null, null, new BigDecimal("-1000"), new BigDecimal("10000000"), false, "newest", 0, 20
                )
        );
        assertEquals(ErrorCode.INVALID_PRICE_RANGE, ex1.getErrorCode());

        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                productService.getPublicProducts(
                        null, null, null, new BigDecimal("1000"), new BigDecimal("-500"), false, "newest", 0, 20
                )
        );
        assertEquals(ErrorCode.INVALID_PRICE_RANGE, ex2.getErrorCode());

        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("TC-20: getPublicProducts_EmptyPage_ReturnsEmptyContent")
    void getPublicProducts_EmptyPage_ReturnsEmptyContent() {
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<ProductSummaryResponse> response = productService.getPublicProducts(
                "non_existing_keyword", null, null, null, null, false, "newest", 0, 20
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        verifyNoInteractions(productImageRepository);
        verifyNoInteractions(productVariantRepository);
    }

    @Test
    @DisplayName("TC-21: getPublicProducts_ZeroNPlusOne_QueryCountVerified")
    void getPublicProducts_ZeroNPlusOne_QueryCountVerified() {
        List<Product> products = List.of(product1, product2);
        Page<Product> page = new PageImpl<>(products, PageRequest.of(0, 20), 2);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productImageRepository.findCoverImagesByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productVariantRepository.aggregateActiveVariantMetricsByProductIds(anyList())).thenReturn(Collections.emptyList());

        productService.getPublicProducts(null, null, null, null, null, false, "newest", 0, 20);

        // Verify exactly 1 call to findAll (with eager fetch), 1 call to cover images, 1 call to active metrics
        verify(productRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(productImageRepository, times(1)).findCoverImagesByProductIds(anyList());
        verify(productVariantRepository, times(1)).aggregateActiveVariantMetricsByProductIds(anyList());
    }
}

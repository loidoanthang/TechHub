package com.techhub.service.product;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.PublicProductDetailResponse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ProductServiceImpl - Chi tiết sản phẩm công khai (GET /api/products/{slug})")
class ProductServiceGetProductDetailTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private User user;
    private Seller seller;
    private Shop shop;
    private Category category;
    private Product product;
    private ProductVariant variant1;
    private ProductVariant variant2;
    private ProductVariant variant3;
    private ProductImage image0;
    private ProductImage image1;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);

        seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(user)
                .sellerName("apple-store")
                .build();

        shop = Shop.builder()
                .id(UUID.randomUUID())
                .name("Apple Official Store")
                .avatarUrl("https://cdn.techhub.com/shops/apple.png")
                .status(ShopStatus.ACTIVE)
                .seller(seller)
                .build();

        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        product = Product.builder()
                .id(UUID.randomUUID())
                .name("MacBook Air M3 13 inch 2024")
                .slug("macbook-air-m3-13-inch-2024")
                .brandName("Apple")
                .description("# Thiết kế mỏng nhẹ đẳng cấp\nTrang bị chip M3...")
                .attributes(Map.of("cpu", "M3 8-core", "ram", "16GB"))
                .category(category)
                .shop(shop)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        variant1 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .sku("MAC-M3-16-256-GRAY")
                .variantName("Space Gray / 256GB")
                .price(new BigDecimal("27490000"))
                .stockQuantity(0)
                .imageUrl("https://cdn.techhub.com/v1.jpg")
                .attributes(Map.of("color", "Space Gray", "storage", "256GB"))
                .status(ProductVariantStatus.ACTIVE)
                .build();

        variant2 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .sku("MAC-M3-16-512-GRAY")
                .variantName("Space Gray / 512GB")
                .price(new BigDecimal("29490000"))
                .stockQuantity(10)
                .imageUrl("https://cdn.techhub.com/v2.jpg")
                .attributes(Map.of("color", "Space Gray", "storage", "512GB"))
                .status(ProductVariantStatus.ACTIVE)
                .build();

        variant3 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .sku("MAC-M3-16-1TB-GRAY")
                .variantName("Space Gray / 1TB")
                .price(new BigDecimal("35490000"))
                .stockQuantity(5)
                .imageUrl("https://cdn.techhub.com/v3.jpg")
                .attributes(Map.of("color", "Space Gray", "storage", "1TB"))
                .status(ProductVariantStatus.ACTIVE)
                .build();

        image0 = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .imageUrl("https://cdn.techhub.com/cover.jpg")
                .displayOrder(0)
                .build();

        image1 = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .imageUrl("https://cdn.techhub.com/side.jpg")
                .displayOrder(1)
                .build();
    }

    @Test
    @DisplayName("TC-01: getPublicProductBySlug_Success_DefaultVariantSelected")
    void getPublicProductBySlug_Success_DefaultVariantSelected() {
        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(List.of(image0, image1));
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant1, variant2, variant3));

        PublicProductDetailResponse res = productService.getPublicProductBySlug("macbook-air-m3-13-inch-2024");

        assertNotNull(res);
        assertEquals(product.getId(), res.id());
        assertEquals("MacBook Air M3 13 inch 2024", res.name());
        assertEquals("macbook-air-m3-13-inch-2024", res.slug());
        assertEquals("Apple", res.brandName());
        assertEquals("Laptop", res.categoryName());
        assertEquals("Apple Official Store", res.shopName());
        assertEquals("apple-store", res.sellerName());
        assertEquals("https://cdn.techhub.com/shops/apple.png", res.shopAvatarUrl());
        assertFalse(res.isShopPaused());
        assertEquals(2, res.images().size());
        assertEquals(3, res.variants().size());
        // Default variant must be variant2 because variant1 has stock = 0 and variant2 is lowest priced with stock > 0
        assertEquals(variant2.getId(), res.defaultVariantId());
        assertEquals(new BigDecimal("27490000"), res.minPrice());
        assertEquals(new BigDecimal("35490000"), res.maxPrice());
        assertEquals(15, res.totalStock());
        assertTrue(res.inStock());
    }

    @Test
    @DisplayName("TC-02: getPublicProductBySlug_Success_AllVariantsOutOfStock")
    void getPublicProductBySlug_Success_AllVariantsOutOfStock() {
        variant1.setStockQuantity(0);
        variant2.setStockQuantity(0);
        variant3.setStockQuantity(0);

        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(List.of(image0));
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant1, variant2, variant3));

        PublicProductDetailResponse res = productService.getPublicProductBySlug("macbook-air-m3-13-inch-2024");

        assertNotNull(res);
        // Default variant fallback to first variant (lowest priced)
        assertEquals(variant1.getId(), res.defaultVariantId());
        assertEquals(0, res.totalStock());
        assertFalse(res.inStock());
    }

    @Test
    @DisplayName("TC-03: getPublicProductBySlug_Success_ShopPaused_ReturnsBadge")
    void getPublicProductBySlug_Success_ShopPaused_ReturnsBadge() {
        shop.setStatus(ShopStatus.PAUSED);

        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(List.of(image0));
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant2));

        PublicProductDetailResponse res = productService.getPublicProductBySlug("macbook-air-m3-13-inch-2024");

        assertNotNull(res);
        assertTrue(res.isShopPaused());
    }

    @Test
    @DisplayName("TC-04: getPublicProductBySlug_Success_SingleVariant")
    void getPublicProductBySlug_Success_SingleVariant() {
        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(List.of(image0));
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant2));

        PublicProductDetailResponse res = productService.getPublicProductBySlug("macbook-air-m3-13-inch-2024");

        assertNotNull(res);
        assertEquals(variant2.getId(), res.defaultVariantId());
        assertEquals(variant2.getPrice(), res.minPrice());
        assertEquals(variant2.getPrice(), res.maxPrice());
        assertEquals(10, res.totalStock());
        assertTrue(res.inStock());
    }

    @Test
    @DisplayName("TC-05: getPublicProductBySlug_Success_FiltersOutInactiveVariants")
    void getPublicProductBySlug_Success_FiltersOutInactiveVariants() {
        // Repository returns only ACTIVE variants due to status parameter
        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant2, variant3));

        PublicProductDetailResponse res = productService.getPublicProductBySlug("macbook-air-m3-13-inch-2024");

        assertNotNull(res);
        assertEquals(2, res.variants().size());
        assertTrue(res.variants().stream().allMatch(v -> v.status() == ProductVariantStatus.ACTIVE));
    }

    @Test
    @DisplayName("TC-06: getPublicProductBySlug_Success_ImagesOrderedByDisplayOrder")
    void getPublicProductBySlug_Success_ImagesOrderedByDisplayOrder() {
        ProductImage img2 = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.com/2.jpg").displayOrder(2).build();
        ProductImage img3 = ProductImage.builder().id(UUID.randomUUID()).product(product).imageUrl("https://cdn.techhub.com/3.jpg").displayOrder(3).build();

        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(List.of(image0, image1, img2, img3));
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant2));

        PublicProductDetailResponse res = productService.getPublicProductBySlug("macbook-air-m3-13-inch-2024");

        assertNotNull(res);
        assertEquals(4, res.images().size());
        assertEquals(0, res.images().get(0).displayOrder());
        assertEquals(1, res.images().get(1).displayOrder());
        assertEquals(2, res.images().get(2).displayOrder());
        assertEquals(3, res.images().get(3).displayOrder());
    }

    @Test
    @DisplayName("TC-07: getPublicProductBySlug_Success_SlugSanitized")
    void getPublicProductBySlug_Success_SlugSanitized() {
        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant2));

        PublicProductDetailResponse res = productService.getPublicProductBySlug("  MacBook-Air-M3-13-Inch-2024  ");

        assertNotNull(res);
        verify(productRepository).findBySlug("macbook-air-m3-13-inch-2024");
    }

    @Test
    @DisplayName("TC-08: getPublicProductBySlug_NotFound_SlugDoesNotExist")
    void getPublicProductBySlug_NotFound_SlugDoesNotExist() {
        when(productRepository.findBySlug("non-existent-slug")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("non-existent-slug"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-09: getPublicProductBySlug_NotFound_ApprovalStatusNotApproved")
    void getPublicProductBySlug_NotFound_ApprovalStatusNotApproved() {
        product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
        when(productRepository.findBySlug("pending-product")).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("pending-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: getPublicProductBySlug_NotFound_VisibilityStatusUnpublished")
    void getPublicProductBySlug_NotFound_VisibilityStatusUnpublished() {
        product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
        when(productRepository.findBySlug("hidden-product")).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("hidden-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: getPublicProductBySlug_NotFound_IsSuspendedTrue")
    void getPublicProductBySlug_NotFound_IsSuspendedTrue() {
        product.setSuspended(true);
        when(productRepository.findBySlug("suspended-product")).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("suspended-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: getPublicProductBySlug_NotFound_IsDeletedTrue")
    void getPublicProductBySlug_NotFound_IsDeletedTrue() {
        product.setDeleted(true);
        when(productRepository.findBySlug("deleted-product")).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("deleted-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: getPublicProductBySlug_NotFound_CategoryInactive")
    void getPublicProductBySlug_NotFound_CategoryInactive() {
        category.setStatus(CategoryStatus.INACTIVE);
        when(productRepository.findBySlug("inactive-cat-product")).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("inactive-cat-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: getPublicProductBySlug_NotFound_ShopSuspended")
    void getPublicProductBySlug_NotFound_ShopSuspended() {
        shop.setStatus(ShopStatus.SUSPENDED);
        when(productRepository.findBySlug("suspended-shop-product")).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("suspended-shop-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: getPublicProductBySlug_NotFound_ShopBanned")
    void getPublicProductBySlug_NotFound_ShopBanned() {
        shop.setStatus(ShopStatus.BANNED);
        when(productRepository.findBySlug("banned-shop-product")).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("banned-shop-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-16: getPublicProductBySlug_NotFound_ShopOwnerUserBanned")
    void getPublicProductBySlug_NotFound_ShopOwnerUserBanned() {
        user.setStatus(UserStatus.BANNED);
        when(productRepository.findBySlug("banned-owner-product")).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("banned-owner-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-17: getPublicProductBySlug_NotFound_ShopOwnerUserDeleted")
    void getPublicProductBySlug_NotFound_ShopOwnerUserDeleted() {
        user.setStatus(UserStatus.DELETED);
        when(productRepository.findBySlug("deleted-owner-product")).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("deleted-owner-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-18: getPublicProductBySlug_NotFound_NoActiveVariants")
    void getPublicProductBySlug_NotFound_NoActiveVariants() {
        when(productRepository.findBySlug("no-active-variants-product")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("no-active-variants-product"));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-19: getPublicProductBySlug_NotFound_SlugNullOrWhitespaceOnly")
    void getPublicProductBySlug_NotFound_SlugNullOrWhitespaceOnly() {
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug(null));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex1.getErrorCode());

        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                productService.getPublicProductBySlug("   "));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex2.getErrorCode());
    }

    @Test
    @DisplayName("TC-20: getPublicProductBySlug_Success_NoGalleryImages")
    void getPublicProductBySlug_Success_NoGalleryImages() {
        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant2));

        PublicProductDetailResponse res = productService.getPublicProductBySlug("macbook-air-m3-13-inch-2024");

        assertNotNull(res);
        assertNotNull(res.images());
        assertTrue(res.images().isEmpty());
    }

    @Test
    @DisplayName("TC-21: getPublicProductBySlug_Success_EmptyAttributes")
    void getPublicProductBySlug_Success_EmptyAttributes() {
        product.setAttributes(null);
        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant2));

        PublicProductDetailResponse res = productService.getPublicProductBySlug("macbook-air-m3-13-inch-2024");

        assertNotNull(res);
        assertNotNull(res.attributes());
        assertTrue(res.attributes().isEmpty());
    }

    @Test
    @DisplayName("TC-22: getPublicProductBySlug_Performance_StrictThreeQueries")
    void getPublicProductBySlug_Performance_StrictThreeQueries() {
        when(productRepository.findBySlug("macbook-air-m3-13-inch-2024")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())).thenReturn(List.of(image0));
        when(productVariantRepository.findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE))
                .thenReturn(List.of(variant2));

        productService.getPublicProductBySlug("macbook-air-m3-13-inch-2024");

        verify(productRepository, times(1)).findBySlug("macbook-air-m3-13-inch-2024");
        verify(productImageRepository, times(1)).findByProductIdOrderByDisplayOrderAsc(product.getId());
        verify(productVariantRepository, times(1)).findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(product.getId(), ProductVariantStatus.ACTIVE);
    }
}

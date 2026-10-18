package com.techhub.service.product;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateProductVariantRequest;
import com.techhub.model.dto.response.ProductVariantResponse;
import com.techhub.model.entity.Category;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.ProductVariant;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.product.impl.SellerProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerProductServiceImpl (POST /api/seller/products/{productId}/variants)")
class SellerProductAddVariantTest {

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
    private UUID productId;

    private Seller seller;
    private Shop shop;
    private Category category;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        productId = UUID.randomUUID();

        seller = Seller.builder()
                .id(sellerId)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        shop = Shop.builder()
                .id(shopId)
                .seller(seller)
                .name("Apple Store Official")
                .status(ShopStatus.ACTIVE)
                .build();

        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Điện thoại thông minh")
                .status(CategoryStatus.ACTIVE)
                .build();

        sampleProduct = Product.builder()
                .id(productId)
                .shop(shop)
                .category(category)
                .brandName("Apple")
                .name("iPhone 15 Pro")
                .slug("iphone-15-pro-abc12345")
                .description("Mô tả sản phẩm iPhone")
                .attributes(Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"))
                .approvalStatus(ProductApprovalStatus.DRAFT)
                .visibilityStatus(ProductVisibilityStatus.UNPUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
    }

    private CreateProductVariantRequest buildValidVariantRequest(String sku) {
        return new CreateProductVariantRequest(
                sku,
                "Titan Tự Nhiên 512GB",
                "Phiên bản 512GB",
                new BigDecimal("34990000.00"),
                30,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "512GB")
        );
    }

    @Test
    @DisplayName("TC-01: addVariant_Success_WhenDraft_KeepsDraftAndUnpublished")
    void addVariant_Success_WhenDraft_KeepsDraftAndUnpublished() {
        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(1L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "IP15PM-512-NAT")).thenReturn(false);

        UUID generatedVariantId = UUID.randomUUID();
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant pv = invocation.getArgument(0);
            pv.setId(generatedVariantId);
            return pv;
        });

        ProductVariantResponse response = sellerProductService.addProductVariant(userId, productId, request);

        assertNotNull(response);
        assertEquals(generatedVariantId, response.id());
        assertEquals("IP15PM-512-NAT", response.sku());
        assertEquals("Titan Tự Nhiên 512GB", response.variantName());
        assertEquals(new BigDecimal("34990000.00"), response.price());
        assertEquals(30, response.stockQuantity());
        assertEquals(ProductVariantStatus.ACTIVE, response.status());
        assertEquals(ProductApprovalStatus.DRAFT, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, never()).save(any(Product.class));
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("TC-02: addVariant_Success_WhenApproved_TriggersContentChangePendingApprovalAndUnpublished")
    void addVariant_Success_WhenApproved_TriggersContentChangePendingApprovalAndUnpublished() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.APPROVED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(2L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "IP15PM-512-NAT")).thenReturn(false);

        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant pv = invocation.getArgument(0);
            pv.setId(UUID.randomUUID());
            return pv;
        });

        ProductVariantResponse response = sellerProductService.addProductVariant(userId, productId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, times(1)).save(sampleProduct);
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("TC-03: addVariant_Success_WhenRejected_KeepsRejected")
    void addVariant_Success_WhenRejected_KeepsRejected() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.REJECTED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);

        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(1L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "IP15PM-512-NAT")).thenReturn(false);

        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant pv = invocation.getArgument(0);
            pv.setId(UUID.randomUUID());
            return pv;
        });

        ProductVariantResponse response = sellerProductService.addProductVariant(userId, productId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.REJECTED, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, never()).save(any(Product.class));
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("TC-04: addVariant_Success_WhenShopPaused")
    void addVariant_Success_WhenShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);

        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(1L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "IP15PM-512-NAT")).thenReturn(false);

        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant pv = invocation.getArgument(0);
            pv.setId(UUID.randomUUID());
            return pv;
        });

        ProductVariantResponse response = sellerProductService.addProductVariant(userId, productId, request);

        assertNotNull(response);
        assertEquals("IP15PM-512-NAT", response.sku());
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("TC-05: addVariant_Success_WhenAttributesNull_FallbackToEmptyMap")
    void addVariant_Success_WhenAttributesNull_FallbackToEmptyMap() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
                "IP15PM-DEFAULT",
                "Bản Mặc Định",
                null,
                new BigDecimal("29990000.00"),
                10,
                null,
                null
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(0L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "IP15PM-DEFAULT")).thenReturn(false);

        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant pv = invocation.getArgument(0);
            pv.setId(UUID.randomUUID());
            return pv;
        });

        ProductVariantResponse response = sellerProductService.addProductVariant(userId, productId, request);

        assertNotNull(response);
        assertNotNull(response.attributes());
        assertTrue(response.attributes().isEmpty());
    }

    @Test
    @DisplayName("TC-06: addVariant_Fail_ShopBanned_ThrowsShopNotActive")
    void addVariant_Fail_ShopBanned_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.BANNED);

        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-07: addVariant_Fail_ShopSuspended_ThrowsShopNotActive")
    void addVariant_Fail_ShopSuspended_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.SUSPENDED);

        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-08: addVariant_Fail_ProductNotFound")
    void addVariant_Fail_ProductNotFound() {
        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-09: addVariant_Fail_CrossTenantAccess_ProductBelongsToAnotherShop")
    void addVariant_Fail_CrossTenantAccess_ProductBelongsToAnotherShop() {
        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-10: addVariant_Fail_ProductSoftDeleted")
    void addVariant_Fail_ProductSoftDeleted() {
        sampleProduct.setDeleted(true);

        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-11: addVariant_Fail_VariantLimitReached_Already50Variants")
    void addVariant_Fail_VariantLimitReached_Already50Variants() {
        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(50L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.INVALID_VARIANT_COUNT, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-12: addVariant_Fail_DuplicateSkuInSameProduct")
    void addVariant_Fail_DuplicateSkuInSameProduct() {
        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(3L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "IP15PM-512-NAT")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.DUPLICATE_VARIANT_SKU, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-13: addVariant_Fail_DuplicateSkuCaseInsensitive")
    void addVariant_Fail_DuplicateSkuCaseInsensitive() {
        CreateProductVariantRequest request = buildValidVariantRequest("ip15pm-512-nat");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(3L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "ip15pm-512-nat")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.DUPLICATE_VARIANT_SKU, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-14: addVariant_Success_VariantAttributeCanShareKeyWithProductSpecs")
    void addVariant_Success_VariantAttributeCanShareKeyWithProductSpecs() {
        // Product specs has RAM and Chipset. Variant attributes also contains "ram" (allowed)
        CreateProductVariantRequest request = new CreateProductVariantRequest(
                "IP15PM-OVERLAP",
                "Bản Cùng Key",
                null,
                new BigDecimal("29990000.00"),
                10,
                null,
                Map.of("ram", "16GB", "Màu sắc", "Đen")
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(2L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "IP15PM-OVERLAP")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant pv = invocation.getArgument(0);
            pv.setId(UUID.randomUUID());
            return pv;
        });

        ProductVariantResponse response = sellerProductService.addProductVariant(userId, productId, request);

        assertNotNull(response);
        assertEquals("16GB", response.attributes().get("ram"));
        assertEquals("Đen", response.attributes().get("Màu sắc"));
        verify(productVariantRepository).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("TC-15: addVariant_Fail_SellerNotApproved")
    void addVariant_Fail_SellerNotApproved() {
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-16: addVariant_Fail_SellerNotFound")
    void addVariant_Fail_SellerNotFound() {
        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-17: addVariant_Fail_ShopNotFound")
    void addVariant_Fail_ShopNotFound() {
        CreateProductVariantRequest request = buildValidVariantRequest("IP15PM-512-NAT");

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.addProductVariant(userId, productId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-18: addVariant_Success_WhenImageUrlBlank_SanitizedToNull")
    void addVariant_Success_WhenImageUrlBlank_SanitizedToNull() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
                "IP15PM-BLANK-IMG",
                "Bản Ảnh Trống",
                null,
                new BigDecimal("29990000.00"),
                10,
                "   ", // Whitespace blank image URL
                null
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(1L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "IP15PM-BLANK-IMG")).thenReturn(false);

        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant pv = invocation.getArgument(0);
            pv.setId(UUID.randomUUID());
            return pv;
        });

        ProductVariantResponse response = sellerProductService.addProductVariant(userId, productId, request);

        assertNotNull(response);
        assertNull(response.imageUrl(), "Blank image URL must be normalized to null");
    }

    @Test
    @DisplayName("TC-19: addVariant_Success_AttributeKeyWithLeadingTrailingSpaces_Sanitized")
    void addVariant_Success_AttributeKeyWithLeadingTrailingSpaces_Sanitized() {
        // Product specs has RAM and Chipset. Variant attributes has "  ram  " with spaces
        CreateProductVariantRequest request = new CreateProductVariantRequest(
                "IP15PM-SPACES-KEY",
                "Bản Key Có Space",
                null,
                new BigDecimal("29990000.00"),
                10,
                null,
                Map.of("  ram  ", "16GB")
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductId(productId)).thenReturn(2L);
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCase(productId, "IP15PM-SPACES-KEY")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant pv = invocation.getArgument(0);
            pv.setId(UUID.randomUUID());
            return pv;
        });

        ProductVariantResponse response = sellerProductService.addProductVariant(userId, productId, request);

        assertNotNull(response);
        assertEquals("16GB", response.attributes().get("ram"));
        verify(productVariantRepository).save(any(ProductVariant.class));
    }
}

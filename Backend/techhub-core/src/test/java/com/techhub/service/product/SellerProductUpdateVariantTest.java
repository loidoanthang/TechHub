package com.techhub.service.product;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateProductVariantRequest;
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
@DisplayName("Unit Tests cho SellerProductServiceImpl (PUT /api/seller/products/{productId}/variants/{variantId})")
class SellerProductUpdateVariantTest {

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
    private UUID variantId;

    private Seller seller;
    private Shop shop;
    private Category category;
    private Product sampleProduct;
    private ProductVariant existingVariant;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

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
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();

        existingVariant = ProductVariant.builder()
                .id(variantId)
                .product(sampleProduct)
                .sku("IP15PM-256-NAT")
                .variantName("Titan Tự Nhiên 256GB")
                .description("Bản 256GB tiêu chuẩn")
                .price(new BigDecimal("29990000.00"))
                .stockQuantity(20)
                .imageUrl("https://cdn.techhub.vn/variant-natural.jpg")
                .attributes(Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"))
                .status(ProductVariantStatus.ACTIVE)
                .build();
    }

    private UpdateProductVariantRequest buildRequest(
            String sku,
            String variantName,
            String description,
            BigDecimal price,
            Integer stockQuantity,
            String imageUrl,
            Map<String, Object> attributes,
            ProductVariantStatus status
    ) {
        return new UpdateProductVariantRequest(
                sku,
                variantName,
                description,
                price,
                stockQuantity,
                imageUrl,
                attributes,
                status
        );
    }

    @Test
    @DisplayName("TC-01: updateVariant_Success_OperationalChange_KeepsApprovedAndPublished")
    void updateVariant_Success_OperationalChange_KeepsApprovedAndPublished() {
        // Chỉ đổi price và stockQuantity, giữ nguyên các trường khác
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("28990000.00"),
                50,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("28990000.00"), response.price());
        assertEquals(50, response.stockQuantity());
        assertEquals(ProductApprovalStatus.APPROVED, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.PUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, never()).save(any(Product.class));
        verify(productVariantRepository, times(1)).saveAndFlush(any(ProductVariant.class));
    }

    @Test
    @DisplayName("TC-02: updateVariant_Success_ContentChange_ResetsPendingApprovalAndUnpublished")
    void updateVariant_Success_ContentChange_ResetsPendingApprovalAndUnpublished() {
        // Đổi variantName -> Content Change BR-08
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB (VN/A)",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                20,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals("Titan Tự Nhiên 256GB (VN/A)", response.variantName());
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-03: updateVariant_Success_ContentChange_WhenImageUrlChanged")
    void updateVariant_Success_ContentChange_WhenImageUrlChanged() {
        // Đổi imageUrl -> Content Change BR-08
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                20,
                "https://cdn.techhub.vn/new-natural-photo.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals("https://cdn.techhub.vn/new-natural-photo.jpg", response.imageUrl());
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-04: updateVariant_Success_ContentChange_WhenAttributesChanged")
    void updateVariant_Success_ContentChange_WhenAttributesChanged() {
        // Đổi attributes -> Content Change BR-08
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                20,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Xanh", "Dung lượng", "256GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-05: updateVariant_Success_ContentChange_WhenSkuChanged")
    void updateVariant_Success_ContentChange_WhenSkuChanged() {
        // Đổi SKU -> Content Change BR-08
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT-V2",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                20,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT-V2", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals("IP15PM-256-NAT-V2", response.sku());
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-06: updateVariant_Success_ContentChange_WhenDescriptionChanged")
    void updateVariant_Success_ContentChange_WhenDescriptionChanged() {
        // Đổi description -> Content Change BR-08
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn phân phối chính hãng",
                new BigDecimal("29990000.00"),
                20,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-07: updateVariant_Success_WhenDraft_KeepsDraft")
    void updateVariant_Success_WhenDraft_KeepsDraft() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.DRAFT);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);

        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB Bản Mới",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                20,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals(ProductApprovalStatus.DRAFT, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("TC-08: updateVariant_Success_KeepSameSku_DoesNotConflictWithSelf")
    void updateVariant_Success_KeepSameSku_DoesNotConflictWithSelf() {
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("28990000.00"),
                20,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals("IP15PM-256-NAT", response.sku());
    }

    @Test
    @DisplayName("TC-09: updateVariant_Success_WhenShopPaused")
    void updateVariant_Success_WhenShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);

        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("28990000.00"),
                25,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals(25, response.stockQuantity());
    }

    @Test
    @DisplayName("TC-10: updateVariant_Success_AllVariantsInactive_TriggersAutoUnpublish")
    void updateVariant_Success_AllVariantsInactive_TriggersAutoUnpublish() {
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                0,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.INACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> {
            ProductVariant pv = i.getArgument(0);
            pv.setStatus(ProductVariantStatus.INACTIVE);
            return pv;
        });
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(0L);

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals(ProductVariantStatus.INACTIVE, response.status());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-11: updateVariant_Success_SomeVariantsStillActive_DoesNotAutoUnpublish")
    void updateVariant_Success_SomeVariantsStillActive_DoesNotAutoUnpublish() {
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                0,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"),
                ProductVariantStatus.INACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> {
            ProductVariant pv = i.getArgument(0);
            pv.setStatus(ProductVariantStatus.INACTIVE);
            return pv;
        });
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(2L);

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals(ProductVariantStatus.INACTIVE, response.status());
        assertEquals(ProductVisibilityStatus.PUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("TC-12: updateVariant_Fail_DuplicateSkuWithAnotherVariant")
    void updateVariant_Fail_DuplicateSkuWithAnotherVariant() {
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-512-NAT",
                "Titan Tự Nhiên 512GB",
                "Bản 512GB tiêu chuẩn",
                new BigDecimal("34990000.00"),
                20,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "512GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-512-NAT", variantId)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProductVariant(userId, productId, variantId, request)
        );

        assertEquals(ErrorCode.DUPLICATE_VARIANT_SKU, ex.getErrorCode());
        verify(productVariantRepository, never()).saveAndFlush(any(ProductVariant.class));
    }

    @Test
    @DisplayName("TC-13: updateVariant_Success_AttributeKeyCanShareKeyWithProductSpecs")
    void updateVariant_Success_AttributeKeyCanShareKeyWithProductSpecs() {
        // Specs chung đã có RAM, variant attribute cũng có RAM (cho phép và tự động sanitize trim)
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                20,
                "https://cdn.techhub.vn/variant-natural.jpg",
                Map.of("  ram  ", "16GB"),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals("16GB", response.attributes().get("ram"));
        verify(productVariantRepository).saveAndFlush(any(ProductVariant.class));
    }

    @Test
    @DisplayName("TC-14: updateVariant_Fail_ShopBanned_ThrowsShopNotActive")
    void updateVariant_Fail_ShopBanned_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.BANNED);

        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                20,
                null,
                Collections.emptyMap(),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProductVariant(userId, productId, variantId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: updateVariant_Fail_ShopSuspended_ThrowsShopNotActive")
    void updateVariant_Fail_ShopSuspended_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.SUSPENDED);

        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("29990000.00"),
                20,
                null,
                Collections.emptyMap(),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProductVariant(userId, productId, variantId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-16: updateVariant_Fail_ProductNotFound")
    void updateVariant_Fail_ProductNotFound() {
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB",
                new BigDecimal("29990000.00"),
                20,
                null,
                Collections.emptyMap(),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProductVariant(userId, productId, variantId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-17: updateVariant_Fail_ProductSoftDeleted")
    void updateVariant_Fail_ProductSoftDeleted() {
        sampleProduct.setDeleted(true);

        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB",
                new BigDecimal("29990000.00"),
                20,
                null,
                Collections.emptyMap(),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProductVariant(userId, productId, variantId, request)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-18: updateVariant_Fail_VariantNotFoundOrNotBelongToProduct")
    void updateVariant_Fail_VariantNotFoundOrNotBelongToProduct() {
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB",
                new BigDecimal("29990000.00"),
                20,
                null,
                Collections.emptyMap(),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProductVariant(userId, productId, variantId, request)
        );

        assertEquals(ErrorCode.VARIANT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-19: updateVariant_Fail_SellerNotApproved")
    void updateVariant_Fail_SellerNotApproved() {
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);

        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB",
                new BigDecimal("29990000.00"),
                20,
                null,
                Collections.emptyMap(),
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProductVariant(userId, productId, variantId, request)
        );

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-20: updateVariant_Success_WhenAttributesNull_PreservesExistingAttributesAndKeepsApproved")
    void updateVariant_Success_WhenAttributesNull_PreservesExistingAttributesAndKeepsApproved() {
        // Request gửi attributes = null (omitted trong JSON)
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("28500000.00"),
                40,
                "https://cdn.techhub.vn/variant-natural.jpg",
                null, // Omitted attributes
                ProductVariantStatus.ACTIVE
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsByProductIdAndSkuIgnoreCaseAndIdNot(productId, "IP15PM-256-NAT", variantId)).thenReturn(false);
        when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        ProductVariantResponse response = sellerProductService.updateProductVariant(userId, productId, variantId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("28500000.00"), response.price());
        assertEquals(40, response.stockQuantity());
        // Bảo toàn attributes cũ không bị xóa trắng về empty
        assertEquals(Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"), response.attributes());
        // Không bị tính nhầm là Content Change
        assertEquals(ProductApprovalStatus.APPROVED, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.PUBLISHED, sampleProduct.getVisibilityStatus());

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("TC-21: updateVariant_Fail_VariantAlreadySoftDeleted")
    void updateVariant_Fail_VariantAlreadySoftDeleted() {
        UpdateProductVariantRequest request = buildRequest(
                "IP15PM-256-NAT",
                "Titan Tự Nhiên 256GB",
                "Bản 256GB tiêu chuẩn",
                new BigDecimal("28500000.00"),
                40,
                "https://cdn.techhub.vn/variant-natural.jpg",
                null,
                ProductVariantStatus.ACTIVE
        );

        existingVariant.setDeleted(true);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.updateProductVariant(userId, productId, variantId, request));

        assertEquals(ErrorCode.VARIANT_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).saveAndFlush(any());
    }
}

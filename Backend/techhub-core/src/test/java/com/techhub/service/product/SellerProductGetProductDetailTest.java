package com.techhub.service.product;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.ProductDetailResponse;
import com.techhub.model.entity.*;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerProductServiceImpl (GET /api/seller/products/{id})")
class SellerProductGetProductDetailTest {

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
    private UUID productId;
    private Seller seller;
    private Shop shop;
    private Category category;
    private Product sampleProduct;
    private List<ProductImage> sampleImages;
    private List<ProductVariant> sampleVariants;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
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
                .id(categoryId)
                .name("Điện thoại thông minh")
                .status(CategoryStatus.ACTIVE)
                .build();

        sampleProduct = Product.builder()
                .id(productId)
                .shop(shop)
                .category(category)
                .name("iPhone 15 Pro Max 256GB")
                .slug("iphone-15-pro-max-256gb-abc12345")
                .brandName("Apple")
                .description("<p>Thiết kế titan cao cấp...</p>")
                .attributes(Map.of("RAM", "8GB", "Chipset", "Apple A17 Pro"))
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 10, 2, 12, 0))
                .build();

        ProductImage coverImage = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .imageUrl("https://cdn.techhub.com/products/ip15-cover.jpg")
                .displayOrder(0)
                .build();

        ProductImage subImage = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .imageUrl("https://cdn.techhub.com/products/ip15-back.jpg")
                .displayOrder(1)
                .build();

        sampleImages = List.of(coverImage, subImage);

        ProductVariant v1 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .sku("IP15PM-256-NAT")
                .variantName("Titan Tự Nhiên - 256GB")
                .price(new BigDecimal("29490000.00"))
                .stockQuantity(45)
                .imageUrl("https://cdn.techhub.com/products/variants/ip15-natural.jpg")
                .attributes(Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"))
                .status(ProductVariantStatus.ACTIVE)
                .build();

        ProductVariant v2 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .sku("IP15PM-256-BLU")
                .variantName("Titan Xanh - 256GB")
                .price(new BigDecimal("28990000.00"))
                .stockQuantity(12)
                .imageUrl("https://cdn.techhub.com/products/variants/ip15-blue.jpg")
                .attributes(Map.of("Màu sắc", "Titan Xanh", "Dung lượng", "256GB"))
                .status(ProductVariantStatus.ACTIVE)
                .build();

        sampleVariants = List.of(v1, v2);
    }

    // ==========================================
    // TC-01: getProductDetail_Success_ShopActive
    // ==========================================
    @Test
    @DisplayName("TC-01: Lấy chi tiết sản phẩm thành công khi Shop đang ACTIVE")
    void getProductDetail_Success_ShopActive() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(sampleImages);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        ProductDetailResponse response = sellerProductService.getProductDetail(userId, productId);

        assertNotNull(response);
        assertEquals(productId, response.id());
        assertEquals(shopId, response.shopId());
        assertEquals(categoryId, response.categoryId());
        assertEquals("Điện thoại thông minh", response.categoryName());
        assertEquals("Apple", response.brandName());
        assertEquals("iPhone 15 Pro Max 256GB", response.name());
        assertEquals("iphone-15-pro-max-256gb-abc12345", response.slug());
        assertEquals(ProductApprovalStatus.APPROVED, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.PUBLISHED, response.visibilityStatus());
        assertFalse(response.isSuspended());
        assertFalse(response.isDeleted());
        assertEquals(2, response.images().size());
        assertEquals(0, response.images().get(0).displayOrder());
        assertEquals(2, response.variants().size());
        assertEquals("IP15PM-256-NAT", response.variants().get(0).sku());

        verify(sellerRepository).findByUserId(userId);
        verify(shopRepository).findBySellerId(sellerId);
        verify(productRepository).findByIdAndShopId(productId, shopId);
        verify(productImageRepository).findByProductIdOrderByDisplayOrderAsc(productId);
        verify(productVariantRepository).findByProductIdOrderByCreatedAtAsc(productId);
    }

    // ==========================================
    // TC-02: getProductDetail_Success_ShopPaused
    // ==========================================
    @Test
    @DisplayName("TC-02: Lấy chi tiết sản phẩm thành công khi Shop đang PAUSED (Chủ shop tạm nghỉ)")
    void getProductDetail_Success_ShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(sampleImages);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        ProductDetailResponse response = sellerProductService.getProductDetail(userId, productId);

        assertNotNull(response);
        assertEquals(productId, response.id());
        assertEquals(shopId, response.shopId());
        verify(productRepository).findByIdAndShopId(productId, shopId);
    }

    // ==========================================
    // TC-03: getProductDetail_Success_ShopSuspended_ReadonlyAllowedForDispute
    // ==========================================
    @Test
    @DisplayName("TC-03: Shop bị SUSPENDED vẫn được phép xem chi tiết sản phẩm ở chế độ Read-only để đối soát/kháng cáo (BR-07)")
    void getProductDetail_Success_ShopSuspended_ReadonlyAllowedForDispute() {
        shop.setStatus(ShopStatus.SUSPENDED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(sampleImages);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        ProductDetailResponse response = sellerProductService.getProductDetail(userId, productId);

        assertNotNull(response);
        assertEquals(productId, response.id());
        verify(productRepository).findByIdAndShopId(productId, shopId);
    }

    // ==========================================
    // TC-04: getProductDetail_Success_ProductWithSingleVariant
    // ==========================================
    @Test
    @DisplayName("TC-04: Lấy chi tiết sản phẩm đơn giản chỉ có đúng 1 biến thể và 1 ảnh Cover")
    void getProductDetail_Success_ProductWithSingleVariant() {
        ProductVariant singleVariant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .sku("SIMPLE-001")
                .variantName("Tiêu chuẩn")
                .price(new BigDecimal("1500000.00"))
                .stockQuantity(100)
                .status(ProductVariantStatus.ACTIVE)
                .build();

        ProductImage singleImg = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .imageUrl("https://cdn.techhub.com/cover.jpg")
                .displayOrder(0)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(singleImg));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(singleVariant));

        ProductDetailResponse response = sellerProductService.getProductDetail(userId, productId);

        assertNotNull(response);
        assertEquals(1, response.images().size());
        assertEquals(1, response.variants().size());
        assertEquals("SIMPLE-001", response.variants().get(0).sku());
    }

    // ==========================================
    // TC-05: getProductDetail_Success_ProductWith50VariantsAnd6Images
    // ==========================================
    @Test
    @DisplayName("TC-05: Lấy chi tiết sản phẩm có tối đa 50 biến thể và 6 hình ảnh")
    void getProductDetail_Success_ProductWith50VariantsAnd6Images() {
        List<ProductImage> sixImages = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sixImages.add(ProductImage.builder()
                    .id(UUID.randomUUID())
                    .product(sampleProduct)
                    .imageUrl("https://cdn.techhub.com/img" + i + ".jpg")
                    .displayOrder(i)
                    .build());
        }

        List<ProductVariant> fiftyVariants = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            fiftyVariants.add(ProductVariant.builder()
                    .id(UUID.randomUUID())
                    .product(sampleProduct)
                    .sku("SKU-" + String.format("%03d", i))
                    .variantName("Biến thể " + i)
                    .price(new BigDecimal("500000.00"))
                    .stockQuantity(10)
                    .status(ProductVariantStatus.ACTIVE)
                    .build());
        }

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(sixImages);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(fiftyVariants);

        ProductDetailResponse response = sellerProductService.getProductDetail(userId, productId);

        assertNotNull(response);
        assertEquals(6, response.images().size());
        assertEquals(50, response.variants().size());
    }

    // ==========================================
    // TC-06: getProductDetail_Success_ProductSoftDeleted
    // ==========================================
    @Test
    @DisplayName("TC-06: Lấy chi tiết sản phẩm đã bị xóa mềm (isDeleted = true) để xem lại lịch sử lưu trữ")
    void getProductDetail_Success_ProductSoftDeleted() {
        sampleProduct.setDeleted(true);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(sampleImages);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        ProductDetailResponse response = sellerProductService.getProductDetail(userId, productId);

        assertNotNull(response);
        assertTrue(response.isDeleted());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
    }

    // ==========================================
    // TC-07: getProductDetail_Success_ProductSuspended
    // ==========================================
    @Test
    @DisplayName("TC-07: Lấy chi tiết sản phẩm đang bị Admin đình chỉ vi phạm (isSuspended = true)")
    void getProductDetail_Success_ProductSuspended() {
        sampleProduct.setSuspended(true);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(sampleImages);
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(sampleVariants);

        ProductDetailResponse response = sellerProductService.getProductDetail(userId, productId);

        assertNotNull(response);
        assertTrue(response.isSuspended());
    }

    // ==========================================
    // TC-08: getProductDetail_Fail_ShopBanned_ThrowsShopNotActive
    // ==========================================
    @Test
    @DisplayName("TC-08: Shop bị BANNED bị từ chối truy cập với lỗi SHOP_NOT_ACTIVE (BR-07 Hard Lockout)")
    void getProductDetail_Fail_ShopBanned_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.BANNED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProductDetail(userId, productId)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).findByIdAndShopId(any(), any());
    }

    // ==========================================
    // TC-09: getProductDetail_Fail_ProductNotFound
    // ==========================================
    @Test
    @DisplayName("TC-09: Sản phẩm không tồn tại trong DB -> ném PRODUCT_NOT_FOUND (HTTP 404)")
    void getProductDetail_Fail_ProductNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProductDetail(userId, productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productImageRepository, never()).findByProductIdOrderByDisplayOrderAsc(any());
        verify(productVariantRepository, never()).findByProductIdOrderByCreatedAtAsc(any());
    }

    // ==========================================
    // TC-10: getProductDetail_Fail_CrossTenantAccess_ProductBelongsToAnotherShop
    // ==========================================
    @Test
    @DisplayName("TC-10: Sản phẩm thuộc Shop khác -> ném PRODUCT_NOT_FOUND (HTTP 404 - Cách ly dữ liệu đa người thuê)")
    void getProductDetail_Fail_CrossTenantAccess_ProductBelongsToAnotherShop() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        // findByIdAndShopId returns empty because the product has a different shopId
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProductDetail(userId, productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productRepository).findByIdAndShopId(productId, shopId);
    }

    // ==========================================
    // TC-11: getProductDetail_Fail_SellerNotFound
    // ==========================================
    @Test
    @DisplayName("TC-11: Không tìm thấy hồ sơ Seller theo userId -> ném SELLER_NOT_FOUND (HTTP 404)")
    void getProductDetail_Fail_SellerNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProductDetail(userId, productId)
        );

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    // ==========================================
    // TC-12: getProductDetail_Fail_SellerPending
    // ==========================================
    @Test
    @DisplayName("TC-12: Hồ sơ Seller đang PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void getProductDetail_Fail_SellerPending() {
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProductDetail(userId, productId)
        );

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    // ==========================================
    // TC-13: getProductDetail_Fail_SellerRejected
    // ==========================================
    @Test
    @DisplayName("TC-13: Hồ sơ Seller bị REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void getProductDetail_Fail_SellerRejected() {
        seller.setVerificationStatus(SellerVerificationStatus.REJECTED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProductDetail(userId, productId)
        );

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    // ==========================================
    // TC-14: getProductDetail_Fail_ShopNotFound
    // ==========================================
    @Test
    @DisplayName("TC-14: Không tìm thấy Shop tương ứng với Seller -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getProductDetail_Fail_ShopNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.getProductDetail(userId, productId)
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(productRepository, never()).findByIdAndShopId(any(), any());
    }
}

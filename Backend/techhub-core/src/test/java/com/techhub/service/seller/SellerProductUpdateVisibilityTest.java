package com.techhub.service.seller;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateProductVisibilityRequest;
import com.techhub.model.dto.response.ProductDetailResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.seller.impl.SellerProductServiceImpl;
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
@DisplayName("Unit Tests cho SellerProductServiceImpl (PATCH /api/seller/products/{id}/visibility)")
class SellerProductUpdateVisibilityTest {

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
    private ProductVariant sampleVariant;
    private ProductImage sampleCoverImage;

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
                .name("iPhone 15 Pro Max")
                .slug("iphone-15-pro-max-k8f2a1")
                .description("Flagship Apple")
                .attributes(Map.of("RAM", "8GB"))
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.UNPUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();

        sampleVariant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .sku("IP15PM-256")
                .variantName("Titan Tự Nhiên 256GB")
                .price(new BigDecimal("29990000.00"))
                .stockQuantity(10)
                .status(ProductVariantStatus.ACTIVE)
                .build();

        sampleCoverImage = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .imageUrl("https://cdn.techhub.vn/products/cover.jpg")
                .displayOrder(0)
                .build();
    }

    @Test
    @DisplayName("updateVisibility_Success_PublishProduct_WhenApprovedAndActiveShop: Mở bán thành công khi sản phẩm APPROVED và Shop ACTIVE")
    void updateVisibility_Success_PublishProduct_WhenApprovedAndActiveShop() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(1L);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(sampleCoverImage));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(sampleVariant));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        ProductDetailResponse response = sellerProductService.updateProductVisibility(userId, productId, request);

        assertNotNull(response);
        assertEquals(ProductVisibilityStatus.PUBLISHED, response.visibilityStatus());
        assertEquals(ProductApprovalStatus.APPROVED, response.approvalStatus());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("updateVisibility_Success_UnpublishProduct_WhenCurrentlyPublished: Ẩn sản phẩm thành công khi đang PUBLISHED")
    void updateVisibility_Success_UnpublishProduct_WhenCurrentlyPublished() {
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(sampleCoverImage));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(sampleVariant));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.UNPUBLISHED);
        ProductDetailResponse response = sellerProductService.updateProductVisibility(userId, productId, request);

        assertNotNull(response);
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
        assertEquals(ProductApprovalStatus.APPROVED, response.approvalStatus());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("updateVisibility_Success_UnpublishProduct_WhenShopPaused: Cho phép ẩn sản phẩm khi Shop đang PAUSED (BR-07)")
    void updateVisibility_Success_UnpublishProduct_WhenShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(sampleCoverImage));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(sampleVariant));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.UNPUBLISHED);
        ProductDetailResponse response = sellerProductService.updateProductVisibility(userId, productId, request);

        assertNotNull(response);
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("updateVisibility_Success_PublishProduct_WhenAllVariantsStockZero: Cho phép mở bán khi tất cả biến thể có tồn kho = 0 (BR-11)")
    void updateVisibility_Success_PublishProduct_WhenAllVariantsStockZero() {
        sampleVariant.setStockQuantity(0);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(1L);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(sampleCoverImage));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(sampleVariant));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        ProductDetailResponse response = sellerProductService.updateProductVisibility(userId, productId, request);

        assertNotNull(response);
        assertEquals(ProductVisibilityStatus.PUBLISHED, response.visibilityStatus());
    }

    @Test
    @DisplayName("updateVisibility_Success_Idempotent_WhenTargetStatusSameAsCurrent: Trả về trạng thái hiện tại khi request trùng lặp (Idempotent)")
    void updateVisibility_Success_Idempotent_WhenTargetStatusSameAsCurrent() {
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)).thenReturn(List.of(sampleCoverImage));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId)).thenReturn(List.of(sampleVariant));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        ProductDetailResponse response = sellerProductService.updateProductVisibility(userId, productId, request);

        assertNotNull(response);
        assertEquals(ProductVisibilityStatus.PUBLISHED, response.visibilityStatus());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateVisibility_Fail_PublishProduct_WhenApprovalStatusDraft: Chặn mở bán khi sản phẩm DRAFT")
    void updateVisibility_Fail_PublishProduct_WhenApprovalStatusDraft() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.DRAFT);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.PRODUCT_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_PublishProduct_WhenApprovalStatusPendingApproval: Chặn mở bán khi sản phẩm PENDING_APPROVAL")
    void updateVisibility_Fail_PublishProduct_WhenApprovalStatusPendingApproval() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.PRODUCT_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_PublishProduct_WhenApprovalStatusRejected: Chặn mở bán khi sản phẩm REJECTED")
    void updateVisibility_Fail_PublishProduct_WhenApprovalStatusRejected() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.REJECTED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.PRODUCT_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_PublishProduct_WhenProductSuspended: Chặn mở bán khi sản phẩm bị đình chỉ (isSuspended = true)")
    void updateVisibility_Fail_PublishProduct_WhenProductSuspended() {
        sampleProduct.setSuspended(true);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.PRODUCT_SUSPENDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_PublishProduct_WhenShopPaused: Chặn mở bán khi Shop đang PAUSED (BR-07)")
    void updateVisibility_Fail_PublishProduct_WhenShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_PublishProduct_WhenAllVariantsInactive: Chặn mở bán khi 100% biến thể là INACTIVE (BR-11B)")
    void updateVisibility_Fail_PublishProduct_WhenAllVariantsInactive() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(0L);

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.NO_ACTIVE_VARIANT, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_PublishProduct_WhenCategoryInactive: Chặn mở bán khi danh mục cha INACTIVE (BR-11A)")
    void updateVisibility_Fail_PublishProduct_WhenCategoryInactive() {
        category.setStatus(CategoryStatus.INACTIVE);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.CATEGORY_INACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_ShopBanned_ThrowsShopNotActive: Chặn thao tác khi Shop BANNED (BR-07)")
    void updateVisibility_Fail_ShopBanned_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.BANNED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.UNPUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_ShopSuspended_ThrowsShopNotActive: Chặn thao tác khi Shop SUSPENDED (BR-07)")
    void updateVisibility_Fail_ShopSuspended_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.SUSPENDED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.UNPUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_ProductNotFound: Ném PRODUCT_NOT_FOUND khi ID không tồn tại")
    void updateVisibility_Fail_ProductNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(nonExistentId, shopId)).thenReturn(Optional.empty());

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, nonExistentId, request));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_CrossTenantAccess_ProductBelongsToAnotherShop: Ném 404 khi truy cập sản phẩm Shop khác")
    void updateVisibility_Fail_CrossTenantAccess_ProductBelongsToAnotherShop() {
        UUID otherShopProductId = UUID.randomUUID();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(otherShopProductId, shopId)).thenReturn(Optional.empty());

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, otherShopProductId, request));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_ProductSoftDeleted: Ném 404 khi sản phẩm đã bị xóa mềm")
    void updateVisibility_Fail_ProductSoftDeleted() {
        sampleProduct.setDeleted(true);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateVisibility_Fail_SellerNotApproved: Ném NOT_A_SELLER khi hồ sơ Seller chưa được duyệt (BR-06)")
    void updateVisibility_Fail_SellerNotApproved() {
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

        UpdateProductVisibilityRequest request = new UpdateProductVisibilityRequest(ProductVisibilityStatus.PUBLISHED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.updateProductVisibility(userId, productId, request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
    }
}

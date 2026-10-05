package com.techhub.service.seller;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
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
@DisplayName("Unit Tests cho SellerProductServiceImpl (PATCH /api/seller/products/{id}/submit-approval)")
class SellerProductSubmitApprovalTest {

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
                .approvalStatus(ProductApprovalStatus.DRAFT)
                .visibilityStatus(ProductVisibilityStatus.UNPUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();

        sampleVariant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .sku("IP15PM-256-NAT")
                .variantName("Titan Tự Nhiên 256GB")
                .price(new BigDecimal("29990000"))
                .stockQuantity(50)
                .imageUrl("https://cdn.techhub.vn/var.jpg")
                .attributes(Map.of("Màu sắc", "Titan"))
                .status(ProductVariantStatus.ACTIVE)
                .build();

        sampleCoverImage = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .imageUrl("https://cdn.techhub.vn/cover.jpg")
                .displayOrder(0)
                .build();
    }

    private void mockSellerAndShop() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
    }

    private void mockValidReadiness() {
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId))
                .thenReturn(List.of(sampleVariant));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId))
                .thenReturn(List.of(sampleCoverImage));
    }

    @Test
    @DisplayName("TC-SUB-01: Gửi duyệt thành công sản phẩm từ trạng thái DRAFT")
    void submitApproval_Success_FromDraft_TransitionsToPendingApproval() {
        // Arrange
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        mockValidReadiness();
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductDetailResponse response = sellerProductService.submitProductForApproval(userId, productId);

        // Assert
        assertNotNull(response);
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, response.approvalStatus(),
                "approvalStatus phải đổi sang PENDING_APPROVAL");
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus(),
                "visibilityStatus phải luôn là UNPUBLISHED khi đang PENDING_APPROVAL");
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-SUB-02: Gửi duyệt thành công sản phẩm từ trạng thái REJECTED sau khi Seller sửa nội dung")
    void submitApproval_Success_FromRejected_TransitionsToPendingApproval() {
        // Arrange
        mockSellerAndShop();
        sampleProduct.setApprovalStatus(ProductApprovalStatus.REJECTED);
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        mockValidReadiness();
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductDetailResponse response = sellerProductService.submitProductForApproval(userId, productId);

        // Assert
        assertNotNull(response);
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, response.approvalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, response.visibilityStatus());
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-SUB-03: Gửi duyệt thành công khi Shop đang ở trạng thái PAUSED (BR-07)")
    void submitApproval_Success_WhenShopPaused() {
        // Arrange
        shop.setStatus(ShopStatus.PAUSED);
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        mockValidReadiness();
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductDetailResponse response = sellerProductService.submitProductForApproval(userId, productId);

        // Assert
        assertNotNull(response);
        assertEquals(ProductApprovalStatus.PENDING_APPROVAL, response.approvalStatus());
    }

    @Test
    @DisplayName("TC-SUB-04: Thất bại khi sản phẩm đã đang ở trạng thái PENDING_APPROVAL (BR-08)")
    void submitApproval_Fail_WhenAlreadyPendingApproval_ThrowsInvalidStatusTransition() {
        // Arrange
        mockSellerAndShop();
        sampleProduct.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SUB-05: Thất bại khi sản phẩm đã đang ở trạng thái APPROVED (BR-08)")
    void submitApproval_Fail_WhenAlreadyApproved_ThrowsInvalidStatusTransition() {
        // Arrange
        mockSellerAndShop();
        sampleProduct.setApprovalStatus(ProductApprovalStatus.APPROVED);
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SUB-06: Thất bại khi danh mục sản phẩm đang ở trạng thái INACTIVE (BR-11A)")
    void submitApproval_Fail_WhenCategoryInactive_ThrowsCategoryInactive() {
        // Arrange
        mockSellerAndShop();
        category.setStatus(CategoryStatus.INACTIVE);
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.CATEGORY_INACTIVE, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SUB-07: Thất bại khi sản phẩm không có biến thể nào (BR-11 Minimum Variant Invariant)")
    void submitApproval_Fail_WhenNoVariants_ThrowsInvalidVariantCount() {
        // Arrange
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.INVALID_VARIANT_COUNT, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SUB-08: Thất bại khi sản phẩm không có ảnh nào trong product_images (BR-11 Image Invariant)")
    void submitApproval_Fail_WhenNoImages_ThrowsInvalidProductImages() {
        // Arrange
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId))
                .thenReturn(List.of(sampleVariant));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SUB-09: Thất bại khi sản phẩm có ảnh nhưng không có ảnh mang displayOrder = 0 (BR-11 Cover Invariant)")
    void submitApproval_Fail_WhenMissingCoverImage_ThrowsInvalidProductImages() {
        // Arrange
        mockSellerAndShop();
        ProductImage nonCoverImage = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(sampleProduct)
                .imageUrl("https://cdn.techhub.vn/img1.jpg")
                .displayOrder(1) // Không phải cover
                .build();

        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId))
                .thenReturn(List.of(sampleVariant));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId))
                .thenReturn(List.of(nonCoverImage));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.INVALID_PRODUCT_IMAGES, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SUB-10: Shop bị cấm BANNED ném lỗi SHOP_NOT_ACTIVE (BR-07)")
    void submitApproval_Fail_ShopBanned_ThrowsShopNotActive() {
        // Arrange
        shop.setStatus(ShopStatus.BANNED);
        mockSellerAndShop();

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).findByIdAndShopId(any(), any());
    }

    @Test
    @DisplayName("TC-SUB-11: Shop bị đình chỉ SUSPENDED ném lỗi SHOP_NOT_ACTIVE (BR-07)")
    void submitApproval_Fail_ShopSuspended_ThrowsShopNotActive() {
        // Arrange
        shop.setStatus(ShopStatus.SUSPENDED);
        mockSellerAndShop();

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).findByIdAndShopId(any(), any());
    }

    @Test
    @DisplayName("TC-SUB-12: Sản phẩm không tồn tại ném PRODUCT_NOT_FOUND")
    void submitApproval_Fail_ProductNotFound() {
        // Arrange
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-SUB-13: Sản phẩm thuộc Shop khác ném PRODUCT_NOT_FOUND (Anti-ID Enumeration BR-11B)")
    void submitApproval_Fail_CrossTenantAccess_ProductBelongsToAnotherShop() {
        // Arrange
        mockSellerAndShop();
        UUID otherShopProductId = UUID.randomUUID();
        when(productRepository.findByIdAndShopId(otherShopProductId, shopId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, otherShopProductId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-SUB-14: Sản phẩm đã bị xóa mềm ném PRODUCT_NOT_FOUND")
    void submitApproval_Fail_ProductSoftDeleted() {
        // Arrange
        mockSellerAndShop();
        sampleProduct.setDeleted(true);
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-SUB-15: Hồ sơ Seller chưa được duyệt ném NOT_A_SELLER (BR-06)")
    void submitApproval_Fail_SellerNotApproved() {
        // Arrange
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.submitProductForApproval(userId, productId));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }
}

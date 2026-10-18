package com.techhub.service.product;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
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
@DisplayName("Unit Tests cho SellerProductServiceImpl (DELETE /api/seller/products/{productId}/variants/{variantId})")
class SellerProductDeleteVariantTest {

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
                .name("iPhone 15 Pro Max")
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isDeleted(false)
                .build();

        existingVariant = ProductVariant.builder()
                .id(variantId)
                .product(sampleProduct)
                .sku("IP15PM-256-NAT")
                .variantName("Titan Tự Nhiên 256GB")
                .price(new BigDecimal("29990000"))
                .stockQuantity(50)
                .status(ProductVariantStatus.ACTIVE)
                .build();
    }

    private void mockSellerAndShop() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
    }

    @Test
    @DisplayName("TC-DEL-01: Xóa thành công biến thể khi sản phẩm có nhiều hơn 1 biến thể")
    void deleteVariant_Success_WhenMultipleVariants_DeletesSuccessfully() {
        // Arrange
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.countByProductId(productId)).thenReturn(3L);
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(2L);

        // Act
        assertDoesNotThrow(() -> sellerProductService.deleteProductVariant(userId, productId, variantId));

        // Assert
        verify(productVariantRepository).saveAndFlush(existingVariant);
        assertTrue(existingVariant.isDeleted(), "Biến thể phải được đánh dấu xóa mềm isDeleted = true");
        assertEquals(ProductVariantStatus.INACTIVE, existingVariant.getStatus(), "Biến thể xóa mềm phải có status = INACTIVE");
        assertEquals(ProductApprovalStatus.APPROVED, sampleProduct.getApprovalStatus(),
                "BR-08: approvalStatus phải được giữ nguyên APPROVED khi xóa biến thể");
        assertEquals(ProductVisibilityStatus.PUBLISHED, sampleProduct.getVisibilityStatus(),
                "visibilityStatus phải giữ nguyên PUBLISHED khi vẫn còn biến thể ACTIVE");
    }

    @Test
    @DisplayName("TC-DEL-02: Xóa biến thể khi sản phẩm đang APPROVED giữ nguyên APPROVED và PUBLISHED (BR-08 Decommission Policy)")
    void deleteVariant_Success_WhenApproved_KeepsApprovedAndPublished() {
        // Arrange
        mockSellerAndShop();
        sampleProduct.setApprovalStatus(ProductApprovalStatus.APPROVED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.countByProductId(productId)).thenReturn(2L);
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(1L);

        // Act
        sellerProductService.deleteProductVariant(userId, productId, variantId);

        // Assert
        verify(productVariantRepository).saveAndFlush(existingVariant);
        assertTrue(existingVariant.isDeleted(), "Biến thể phải được đánh dấu xóa mềm isDeleted = true");
        assertEquals(ProductVariantStatus.INACTIVE, existingVariant.getStatus(), "Biến thể xóa mềm phải có status = INACTIVE");
        assertEquals(ProductApprovalStatus.APPROVED, sampleProduct.getApprovalStatus(),
                "approvalStatus không được bị reset về PENDING_APPROVAL");
        assertEquals(ProductVisibilityStatus.PUBLISHED, sampleProduct.getVisibilityStatus(),
                "visibilityStatus giữ nguyên PUBLISHED");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("TC-DEL-03: Xóa biến thể khi sản phẩm ở trạng thái DRAFT giữ nguyên DRAFT và UNPUBLISHED")
    void deleteVariant_Success_WhenDraft_KeepsDraftAndUnpublished() {
        // Arrange
        mockSellerAndShop();
        sampleProduct.setApprovalStatus(ProductApprovalStatus.DRAFT);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);

        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.countByProductId(productId)).thenReturn(2L);

        // Act
        sellerProductService.deleteProductVariant(userId, productId, variantId);

        // Assert
        verify(productVariantRepository).saveAndFlush(existingVariant);
        assertTrue(existingVariant.isDeleted(), "Biến thể phải được đánh dấu xóa mềm isDeleted = true");
        assertEquals(ProductVariantStatus.INACTIVE, existingVariant.getStatus(), "Biến thể xóa mềm phải có status = INACTIVE");
        assertEquals(ProductApprovalStatus.DRAFT, sampleProduct.getApprovalStatus());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());
        verify(productVariantRepository, never()).countByProductIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("TC-DEL-04: Xóa biến thể khi Shop đang ở trạng thái PAUSED thành công (BR-07)")
    void deleteVariant_Success_WhenShopPaused() {
        // Arrange
        shop.setStatus(ShopStatus.PAUSED);
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.countByProductId(productId)).thenReturn(2L);
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> sellerProductService.deleteProductVariant(userId, productId, variantId));

        // Assert
        verify(productVariantRepository).saveAndFlush(existingVariant);
        assertTrue(existingVariant.isDeleted(), "Biến thể phải được đánh dấu xóa mềm isDeleted = true");
        assertEquals(ProductVariantStatus.INACTIVE, existingVariant.getStatus(), "Biến thể xóa mềm phải có status = INACTIVE");
    }

    @Test
    @DisplayName("TC-DEL-05: 100% biến thể còn lại là INACTIVE kích hoạt tự động UNPUBLISH (BR-11B)")
    void deleteVariant_Success_RemainingVariantsAllInactive_TriggersAutoUnpublish() {
        // Arrange
        mockSellerAndShop();
        sampleProduct.setApprovalStatus(ProductApprovalStatus.APPROVED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.countByProductId(productId)).thenReturn(2L);
        // Sau khi xóa biến thể ACTIVE này, số biến thể ACTIVE còn lại = 0
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(0L);

        // Act
        sellerProductService.deleteProductVariant(userId, productId, variantId);

        // Assert
        verify(productVariantRepository).saveAndFlush(existingVariant);
        assertTrue(existingVariant.isDeleted(), "Biến thể phải được đánh dấu xóa mềm isDeleted = true");
        assertEquals(ProductVariantStatus.INACTIVE, existingVariant.getStatus(), "Biến thể xóa mềm phải có status = INACTIVE");
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus(),
                "Tất cả biến thể còn lại là INACTIVE -> visibilityStatus phải đổi sang UNPUBLISHED");
        assertEquals(ProductApprovalStatus.APPROVED, sampleProduct.getApprovalStatus(),
                "approvalStatus vẫn phải giữ nguyên APPROVED");
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("TC-DEL-06: Vẫn còn biến thể ACTIVE khác sau khi xóa không unpublish sản phẩm")
    void deleteVariant_Success_SomeRemainingVariantsStillActive_DoesNotAutoUnpublish() {
        // Arrange
        mockSellerAndShop();
        sampleProduct.setApprovalStatus(ProductApprovalStatus.APPROVED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.countByProductId(productId)).thenReturn(3L);
        when(productVariantRepository.countByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)).thenReturn(2L);

        // Act
        sellerProductService.deleteProductVariant(userId, productId, variantId);

        // Assert
        verify(productVariantRepository).saveAndFlush(existingVariant);
        assertTrue(existingVariant.isDeleted(), "Biến thể phải được đánh dấu xóa mềm isDeleted = true");
        assertEquals(ProductVariantStatus.INACTIVE, existingVariant.getStatus(), "Biến thể xóa mềm phải có status = INACTIVE");
        assertEquals(ProductVisibilityStatus.PUBLISHED, sampleProduct.getVisibilityStatus());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("TC-DEL-07: Chặn xóa biến thể duy nhất còn lại của sản phẩm (BR-11 Minimum Invariant)")
    void deleteVariant_Fail_CannotDeleteLastVariant_ThrowsException() {
        // Arrange
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.countByProductId(productId)).thenReturn(1L);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.deleteProductVariant(userId, productId, variantId));

        assertEquals(ErrorCode.CANNOT_DELETE_LAST_VARIANT, ex.getErrorCode());
        verify(productVariantRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC-DEL-08: Shop bị cấm BANNED ném lỗi SHOP_NOT_ACTIVE (BR-07)")
    void deleteVariant_Fail_ShopBanned_ThrowsShopNotActive() {
        // Arrange
        shop.setStatus(ShopStatus.BANNED);
        mockSellerAndShop();

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.deleteProductVariant(userId, productId, variantId));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).findByIdAndShopId(any(), any());
    }

    @Test
    @DisplayName("TC-DEL-09: Shop bị đình chỉ SUSPENDED ném lỗi SHOP_NOT_ACTIVE (BR-07)")
    void deleteVariant_Fail_ShopSuspended_ThrowsShopNotActive() {
        // Arrange
        shop.setStatus(ShopStatus.SUSPENDED);
        mockSellerAndShop();

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.deleteProductVariant(userId, productId, variantId));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).findByIdAndShopId(any(), any());
    }

    @Test
    @DisplayName("TC-DEL-10: Sản phẩm không tồn tại ném PRODUCT_NOT_FOUND")
    void deleteVariant_Fail_ProductNotFound() {
        // Arrange
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.deleteProductVariant(userId, productId, variantId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).findByIdAndProductId(any(), any());
    }

    @Test
    @DisplayName("TC-DEL-11: Sản phẩm thuộc Shop khác ném PRODUCT_NOT_FOUND (Anti-ID Enumeration BR-11B)")
    void deleteVariant_Fail_CrossTenantAccess_ProductBelongsToAnotherShop() {
        // Arrange
        mockSellerAndShop();
        UUID otherShopProductId = UUID.randomUUID();
        when(productRepository.findByIdAndShopId(otherShopProductId, shopId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.deleteProductVariant(userId, otherShopProductId, variantId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).findByIdAndProductId(any(), any());
    }

    @Test
    @DisplayName("TC-DEL-12: Sản phẩm đã bị xóa mềm ném PRODUCT_NOT_FOUND")
    void deleteVariant_Fail_ProductSoftDeleted() {
        // Arrange
        mockSellerAndShop();
        sampleProduct.setDeleted(true);
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.deleteProductVariant(userId, productId, variantId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).findByIdAndProductId(any(), any());
    }

    @Test
    @DisplayName("TC-DEL-13: Biến thể không tồn tại hoặc không thuộc sản phẩm ném VARIANT_NOT_FOUND")
    void deleteVariant_Fail_VariantNotFoundOrNotBelongToProduct() {
        // Arrange
        mockSellerAndShop();
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.deleteProductVariant(userId, productId, variantId));

        assertEquals(ErrorCode.VARIANT_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC-DEL-14: Hồ sơ Seller chưa được duyệt ném NOT_A_SELLER (BR-06)")
    void deleteVariant_Fail_SellerNotApproved() {
        // Arrange
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.deleteProductVariant(userId, productId, variantId));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("TC-DEL-15: Biến thể đã bị xóa mềm trước đó ném VARIANT_NOT_FOUND")
    void deleteVariant_Fail_VariantAlreadySoftDeleted() {
        // Arrange
        mockSellerAndShop();
        existingVariant.setDeleted(true);
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(existingVariant));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerProductService.deleteProductVariant(userId, productId, variantId));

        assertEquals(ErrorCode.VARIANT_NOT_FOUND, ex.getErrorCode());
        verify(productVariantRepository, never()).saveAndFlush(any());
    }
}

package com.techhub.service.seller;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerProductServiceImpl (DELETE /api/seller/products/{id})")
class SellerProductDeleteProductTest {

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
                .approvalStatus(ProductApprovalStatus.DRAFT)
                .visibilityStatus(ProductVisibilityStatus.UNPUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("deleteProduct: Thành công khi sản phẩm đang DRAFT -> isDeleted=true, visibilityStatus=UNPUBLISHED")
    void deleteProduct_Success_WhenDraft() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> sellerProductService.deleteProduct(userId, productId));

        assertTrue(sampleProduct.isDeleted());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("deleteProduct: Thành công khi sản phẩm đang APPROVED & PUBLISHED -> isDeleted=true, tự động chuyển UNPUBLISHED (BR-11B)")
    void deleteProduct_Success_WhenApprovedAndPublished() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.APPROVED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> sellerProductService.deleteProduct(userId, productId));

        assertTrue(sampleProduct.isDeleted());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("deleteProduct: Thành công khi sản phẩm đang REJECTED -> isDeleted=true, visibilityStatus=UNPUBLISHED")
    void deleteProduct_Success_WhenRejected() {
        sampleProduct.setApprovalStatus(ProductApprovalStatus.REJECTED);
        sampleProduct.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> sellerProductService.deleteProduct(userId, productId));

        assertTrue(sampleProduct.isDeleted());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("deleteProduct: Thành công khi Shop đang PAUSED (BR-07: Shop tạm nghỉ được dọn dẹp hàng nội bộ)")
    void deleteProduct_Success_WhenShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> sellerProductService.deleteProduct(userId, productId));

        assertTrue(sampleProduct.isDeleted());
        assertEquals(ProductVisibilityStatus.UNPUBLISHED, sampleProduct.getVisibilityStatus());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("deleteProduct: Thất bại khi Shop BANNED -> ném SHOP_NOT_ACTIVE (BR-07)")
    void deleteProduct_Fail_ShopBanned_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.BANNED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.deleteProduct(userId, productId));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).findByIdAndShopId(any(), any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProduct: Thất bại khi Shop SUSPENDED -> ném SHOP_NOT_ACTIVE (BR-07)")
    void deleteProduct_Fail_ShopSuspended_ThrowsShopNotActive() {
        shop.setStatus(ShopStatus.SUSPENDED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.deleteProduct(userId, productId));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(productRepository, never()).findByIdAndShopId(any(), any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProduct: Thất bại khi không tìm thấy Product -> ném PRODUCT_NOT_FOUND (404)")
    void deleteProduct_Fail_ProductNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.deleteProduct(userId, productId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProduct: Thất bại khi IDOR truy cập chéo sản phẩm của Shop khác -> ném PRODUCT_NOT_FOUND (Anti-ID Enumeration)")
    void deleteProduct_Fail_CrossTenantAccess_ProductBelongsToAnotherShop() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.deleteProduct(userId, productId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProduct: Thất bại khi sản phẩm đã bị xóa mềm từ trước (isDeleted=true) -> ném PRODUCT_NOT_FOUND (Double Delete)")
    void deleteProduct_Fail_ProductAlreadyDeleted() {
        sampleProduct.setDeleted(true);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdAndShopId(productId, shopId)).thenReturn(Optional.of(sampleProduct));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.deleteProduct(userId, productId));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProduct: Thất bại khi Seller chưa được duyệt (status=PENDING) -> ném NOT_A_SELLER (403)")
    void deleteProduct_Fail_SellerNotApproved() {
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.deleteProduct(userId, productId));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("deleteProduct: Thất bại khi không tìm thấy Seller -> ném SELLER_NOT_FOUND (404)")
    void deleteProduct_Fail_SellerNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.deleteProduct(userId, productId));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("deleteProduct: Thất bại khi không tìm thấy Shop của Seller -> ném SHOP_NOT_FOUND (404)")
    void deleteProduct_Fail_ShopNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerProductService.deleteProduct(userId, productId));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(productRepository, never()).findByIdAndShopId(any(), any());
    }
}

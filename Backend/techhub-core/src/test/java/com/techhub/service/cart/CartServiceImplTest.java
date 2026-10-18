package com.techhub.service.cart;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.AddToCartRequest;
import com.techhub.model.dto.request.UpdateCartItemRequest;
import com.techhub.model.dto.response.CartCountResponse;
import com.techhub.model.dto.response.CartItemResponse;
import com.techhub.model.dto.response.CartResponse;
import com.techhub.model.dto.response.ShopCartGroupResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.cart.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho CartService (GET /api/cart, GET /api/cart/count, POST /api/cart/items)")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private ShippingOptionRepository shippingOptionRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private UUID userId;
    private User buyerUser;
    private UUID cartId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        buyerUser = new User();
        buyerUser.setId(userId);
        buyerUser.setEmail("buyer@techhub.vn");
        buyerUser.setStatus(UserStatus.ACTIVE);

        cartId = UUID.randomUUID();
        cart = Cart.builder()
                .id(cartId)
                .user(buyerUser)
                .build();
    }

    private Product createValidProduct(UUID shopId, String shopName, ShopStatus shopStatus, UserStatus ownerStatus) {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner-" + shopName.toLowerCase() + "@techhub.vn");
        owner.setStatus(ownerStatus);

        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(owner)
                .sellerName("seller-" + shopName.toLowerCase())
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(shopId)
                .seller(seller)
                .name(shopName)
                .status(shopStatus)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        return Product.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .category(category)
                .name("Laptop Apple MacBook Air M3")
                .slug("laptop-apple-macbook-air-m3")
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isSuspended(false)
                .isDeleted(false)
                .build();
    }

    private ProductVariant createValidVariant(Product product, BigDecimal price, int stockQuantity, String imageUrl) {
        return ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .sku("MAC-M3-16-512")
                .variantName("Space Gray 16GB/512GB")
                .price(price)
                .stockQuantity(stockQuantity)
                .imageUrl(imageUrl)
                .status(ProductVariantStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    private CartItem createCartItem(Cart cart, Product product, ProductVariant variant, int quantity) {
        return CartItem.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .product(product)
                .productVariant(variant)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("TC-01: getCart_Success_EmptyCart_LazyInit - User chưa có giỏ hàng, tự động lazy init tạo giỏ hàng mới")
    void getCart_Success_EmptyCart_LazyInit() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(buyerUser));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart saved = invocation.getArgument(0);
            saved.setId(cartId);
            return saved;
        });
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(Collections.emptyList());

        CartResponse response = cartService.getCartDetails(userId);

        assertNotNull(response);
        assertEquals(cartId, response.id());
        assertEquals(0, response.totalItemTypes());
        assertEquals(0, response.totalQuantity());
        assertTrue(response.shops().isEmpty());

        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).findByCartIdWithDetails(cartId);
    }

    @Test
    @DisplayName("TC-02: getCart_Success_ExistingEmptyCart - User đã có giỏ hàng nhưng không có sản phẩm nào")
    void getCart_Success_ExistingEmptyCart() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(Collections.emptyList());

        CartResponse response = cartService.getCartDetails(userId);

        assertNotNull(response);
        assertEquals(cartId, response.id());
        assertEquals(0, response.totalItemTypes());
        assertEquals(0, response.totalQuantity());
        assertTrue(response.shops().isEmpty());

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("TC-03: getCart_Success_SingleShop_SingleItem - Giỏ hàng có 1 sản phẩm khả dụng của 1 Shop")
    void getCart_Success_SingleShop_SingleItem() {
        Product product = createValidProduct(UUID.randomUUID(), "Apple Flagship Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "https://cdn.com/var.jpg");
        CartItem item = createCartItem(cart, product, variant, 2);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        assertNotNull(response);
        assertEquals(1, response.totalItemTypes());
        assertEquals(2, response.totalQuantity());
        assertEquals(1, response.shops().size());

        ShopCartGroupResponse shopGroup = response.shops().get(0);
        assertEquals("Apple Flagship Store", shopGroup.shopName());
        assertFalse(shopGroup.isShopPaused());
        assertEquals(1, shopGroup.items().size());

        CartItemResponse itemResp = shopGroup.items().get(0);
        assertEquals(new BigDecimal("100000.00"), itemResp.price());
        assertEquals(2, itemResp.quantity());
        assertEquals(new BigDecimal("200000.00"), itemResp.itemSubtotal());
        assertEquals("https://cdn.com/var.jpg", itemResp.imageUrl());
        assertTrue(itemResp.isAvailable());
        assertFalse(itemResp.isOutOfStock());
        assertNull(itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-04: getCart_Success_MultipleItems_GroupedByShop - Giỏ hàng có 3 món thuộc 2 Shop khác nhau")
    void getCart_Success_MultipleItems_GroupedByShop() {
        UUID shopAId = UUID.randomUUID();
        Product productA1 = createValidProduct(shopAId, "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variantA1 = createValidVariant(productA1, BigDecimal.valueOf(50000), 10, "imgA1.jpg");
        CartItem item1 = createCartItem(cart, productA1, variantA1, 1);

        ProductVariant variantA2 = createValidVariant(productA1, BigDecimal.valueOf(70000), 5, "imgA2.jpg");
        CartItem item2 = createCartItem(cart, productA1, variantA2, 2);

        UUID shopBId = UUID.randomUUID();
        Product productB = createValidProduct(shopBId, "Shop B", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variantB = createValidVariant(productB, BigDecimal.valueOf(120000), 8, "imgB.jpg");
        CartItem item3 = createCartItem(cart, productB, variantB, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item1, item2, item3));

        CartResponse response = cartService.getCartDetails(userId);

        assertNotNull(response);
        assertEquals(3, response.totalItemTypes());
        assertEquals(4, response.totalQuantity()); // 1 + 2 + 1
        assertEquals(2, response.shops().size());

        ShopCartGroupResponse shopGroupA = response.shops().get(0);
        assertEquals("Shop A", shopGroupA.shopName());
        assertEquals(2, shopGroupA.items().size());

        ShopCartGroupResponse shopGroupB = response.shops().get(1);
        assertEquals("Shop B", shopGroupB.shopName());
        assertEquals(1, shopGroupB.items().size());
    }

    @Test
    @DisplayName("TC-05: getCart_Success_LivePriceReflected - Đơn giá và thành tiền phản ánh Live Price mới nhất")
    void getCart_Success_LivePriceReflected() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(150000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 3);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertEquals(new BigDecimal("150000.00"), itemResp.price());
        assertEquals(3, itemResp.quantity());
        assertEquals(new BigDecimal("450000.00"), itemResp.itemSubtotal());
    }

    @Test
    @DisplayName("TC-06: getCart_Success_FallbackToProductCoverImage - Fallback sang ảnh bìa sản phẩm khi variant không có ảnh")
    void getCart_Success_FallbackToProductCoverImage() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, null); // imageUrl is null
        CartItem item = createCartItem(cart, product, variant, 1);

        ProductImage coverImage = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .imageUrl("https://cdn.com/cover-fallback.jpg")
                .displayOrder(0)
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));
        when(productImageRepository.findByProductIdInAndDisplayOrder(Set.of(product.getId()), 0))
                .thenReturn(List.of(coverImage));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertEquals("https://cdn.com/cover-fallback.jpg", itemResp.imageUrl());
    }

    @Test
    @DisplayName("TC-07: getCart_Success_VariantImageNotNull_NoFallbackNeeded - Variant có ảnh riêng, giữ nguyên không cần fallback")
    void getCart_Success_VariantImageNotNull_NoFallbackNeeded() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "https://cdn.com/variant-custom.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertEquals("https://cdn.com/variant-custom.jpg", itemResp.imageUrl());
        verify(productImageRepository, never()).findByProductIdInAndDisplayOrder(any(), anyInt());
    }

    @Test
    @DisplayName("TC-08: getCart_Success_VariantOutOfStock_MarkedUnavailable - Tồn kho = 0 đánh dấu OUT_OF_STOCK")
    void getCart_Success_VariantOutOfStock_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 0, "img.jpg"); // stock 0
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertTrue(itemResp.isOutOfStock());
        assertFalse(itemResp.isAvailable());
        assertEquals("OUT_OF_STOCK", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-09: getCart_Success_QuantityExceedsStock_MarkedUnavailable - Số lượng trong giỏ lớn hơn tồn kho hiện có")
    void getCart_Success_QuantityExceedsStock_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 3, "img.jpg"); // stock 3
        CartItem item = createCartItem(cart, product, variant, 5); // qty 5

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isOutOfStock());
        assertFalse(itemResp.isAvailable());
        assertEquals(3, itemResp.stockQuantity());
        assertEquals("EXCEEDS_AVAILABLE_STOCK", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-10: getCart_Success_ShopPaused_MarkedUnavailable - Shop ở trạng thái PAUSED")
    void getCart_Success_ShopPaused_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.PAUSED, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        ShopCartGroupResponse shopGroup = response.shops().get(0);
        assertTrue(shopGroup.isShopPaused());
        assertEquals(ShopStatus.PAUSED, shopGroup.shopStatus());

        CartItemResponse itemResp = shopGroup.items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("SHOP_IS_PAUSED", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-11: getCart_Success_ShopSuspended_MarkedUnavailable - Shop ở trạng thái SUSPENDED")
    void getCart_Success_ShopSuspended_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.SUSPENDED, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("SHOP_NOT_ACTIVE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-12: getCart_Success_ShopBanned_MarkedUnavailable - Shop ở trạng thái BANNED")
    void getCart_Success_ShopBanned_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.BANNED, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("SHOP_NOT_ACTIVE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-13: getCart_Success_ShopOwnerBanned_MarkedUnavailable - Tài khoản chủ Shop bị BANNED")
    void getCart_Success_ShopOwnerBanned_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.BANNED); // owner BANNED
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("SHOP_NOT_ACTIVE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-14: getCart_Success_CategoryInactive_MarkedUnavailable - Danh mục cha bị INACTIVE")
    void getCart_Success_CategoryInactive_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.getCategory().setStatus(CategoryStatus.INACTIVE); // category INACTIVE
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("CATEGORY_INACTIVE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-15: getCart_Success_ProductUnpublished_MarkedUnavailable - Sản phẩm có visibilityStatus = UNPUBLISHED")
    void getCart_Success_ProductUnpublished_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("PRODUCT_UNAVAILABLE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-16: getCart_Success_ProductSuspended_MarkedUnavailable - Sản phẩm bị Admin đình chỉ isSuspended = true")
    void getCart_Success_ProductSuspended_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setSuspended(true);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("PRODUCT_UNAVAILABLE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-17: getCart_Success_ProductDeleted_MarkedUnavailable - Sản phẩm bị Seller xóa mềm isDeleted = true")
    void getCart_Success_ProductDeleted_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setDeleted(true);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("PRODUCT_UNAVAILABLE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-18: getCart_Success_ProductPendingApproval_MarkedUnavailable - Sản phẩm chưa được duyệt approvalStatus != APPROVED")
    void getCart_Success_ProductPendingApproval_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("PRODUCT_UNAVAILABLE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-19: getCart_Success_VariantSoftDeleted_MarkedUnavailable - Biến thể bị Seller xóa mềm isDeleted = true")
    void getCart_Success_VariantSoftDeleted_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        variant.setDeleted(true); // Soft-deleted variant
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("VARIANT_UNAVAILABLE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-20: getCart_Success_VariantInactive_MarkedUnavailable - Biến thể có trạng thái INACTIVE")
    void getCart_Success_VariantInactive_MarkedUnavailable() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        variant.setStatus(ProductVariantStatus.INACTIVE);
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertFalse(itemResp.isAvailable());
        assertEquals("VARIANT_UNAVAILABLE", itemResp.unavailableReason());
    }

    @Test
    @DisplayName("TC-21: getCart_Performance_StrictThreeQueries - Đảm bảo O(1) queries không bị N+1")
    void getCart_Performance_StrictThreeQueries() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, null);
        CartItem item = createCartItem(cart, product, variant, 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));
        when(productImageRepository.findByProductIdInAndDisplayOrder(any(), eq(0)))
                .thenReturn(Collections.emptyList());

        cartService.getCartDetails(userId);

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartItemRepository, times(1)).findByCartIdWithDetails(cartId);
        verify(productImageRepository, times(1)).findByProductIdInAndDisplayOrder(any(), eq(0));
    }

    @Test
    @DisplayName("TC-22: getCart_MonetaryPrecision_RoundingHalfUp - Giá lẻ làm tròn chuẩn 2 chữ số thập phân HALF_UP")
    void getCart_MonetaryPrecision_RoundingHalfUp() {
        Product product = createValidProduct(UUID.randomUUID(), "Shop A", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        // 19.995 round up -> 20.00
        ProductVariant variant = createValidVariant(product, new BigDecimal("19.995"), 10, "img.jpg");
        CartItem item = createCartItem(cart, product, variant, 3);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdWithDetails(cartId)).thenReturn(List.of(item));

        CartResponse response = cartService.getCartDetails(userId);

        CartItemResponse itemResp = response.shops().get(0).items().get(0);
        assertEquals(new BigDecimal("20.00"), itemResp.price());
        assertEquals(new BigDecimal("60.00"), itemResp.itemSubtotal());
    }

    @Test
    @DisplayName("Extra: getCart_UserNotFound_ThrowsException - Khi lazy init nếu user không tồn tại ném USER_NOT_FOUND")
    void getCart_UserNotFound_ThrowsException() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.getCartDetails(userId));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // UNIT TESTS CHO GET /api/cart/count
    // ==========================================

    @Test
    @DisplayName("TC-01: getCartCount_Success_NoCart_ReturnsZero - User chưa có giỏ hàng trả về 0 và không gọi cartRepository")
    void getCartCount_Success_NoCart_ReturnsZero() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(0);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(0, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
        verifyNoInteractions(cartRepository);
    }

    @Test
    @DisplayName("TC-02: getCartCount_Success_ExistingEmptyCart_ReturnsZero - Giỏ hàng rỗng trả về 0")
    void getCartCount_Success_ExistingEmptyCart_ReturnsZero() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(0);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(0, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
        verifyNoInteractions(cartRepository);
    }

    @Test
    @DisplayName("TC-03: getCartCount_Success_SingleItem_ReturnsOne - Giỏ có 1 mặt hàng (quantity = 1) trả về 1")
    void getCartCount_Success_SingleItem_ReturnsOne() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(1);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(1, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
    }

    @Test
    @DisplayName("TC-04: getCartCount_Success_SingleItem_MultipleQuantity_ReturnsOne - Giỏ có 1 mặt hàng số lượng lớn (qty = 5) vẫn trả về 1 dòng")
    void getCartCount_Success_SingleItem_MultipleQuantity_ReturnsOne() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(1);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(1, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
    }

    @Test
    @DisplayName("TC-05: getCartCount_Success_MultipleItems_ReturnsCorrectTypesCount - Giỏ có 3 mặt hàng khác nhau trả về 3")
    void getCartCount_Success_MultipleItems_ReturnsCorrectTypesCount() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(3);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(3, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
    }

    @Test
    @DisplayName("TC-06: getCartCount_Success_MultipleShops - Giỏ có 4 mặt hàng từ nhiều Shop khác nhau đếm chính xác")
    void getCartCount_Success_MultipleShops() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(4);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(4, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
    }

    @Test
    @DisplayName("TC-07: getCartCount_Success_IncludesOutOfStockItems - Mặt hàng hết hàng vẫn được tính trong badge cho đến khi Buyer xóa")
    void getCartCount_Success_IncludesOutOfStockItems() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(2);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(2, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
    }

    @Test
    @DisplayName("TC-08: getCartCount_Success_IncludesPausedShopItems - Mặt hàng thuộc Shop tạm nghỉ vẫn được tính trong badge")
    void getCartCount_Success_IncludesPausedShopItems() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(1);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(1, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
    }

    @Test
    @DisplayName("TC-09: getCartCount_Success_IncludesSoftDeletedVariants - Biến thể bị xóa mềm vẫn được tính trong badge")
    void getCartCount_Success_IncludesSoftDeletedVariants() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(1);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(1, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
    }

    @Test
    @DisplayName("TC-10: getCartCount_Performance_ZeroEntityLoading - Endpoint chỉ chạy aggregate index 1 query duy nhất, không nạp entity quan hệ nặng")
    void getCartCount_Performance_ZeroEntityLoading() {
        when(cartItemRepository.countByUserId(userId)).thenReturn(5);

        CartCountResponse response = cartService.getCartCount(userId);

        assertNotNull(response);
        assertEquals(5, response.totalItemTypes());

        verify(cartItemRepository, times(1)).countByUserId(userId);
        verifyNoInteractions(cartRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(productImageRepository);
    }

    @Test
    @DisplayName("TC-Extra: getCartCount_NullUserId_ReturnsZero - Defensive check khi userId null trả về 0")
    void getCartCount_NullUserId_ReturnsZero() {
        CartCountResponse response = cartService.getCartCount(null);

        assertNotNull(response);
        assertEquals(0, response.totalItemTypes());

        verifyNoInteractions(cartRepository);
        verifyNoInteractions(cartItemRepository);
    }

    // ==========================================
    // UNIT TESTS CHO POST /api/cart/items
    // ==========================================

    @Test
    @DisplayName("TC-01: addToCart_Success_NewItem_NewCartCreated - User mới chưa có giỏ hàng, lazy init Cart mới và thêm sản phẩm")
    void addToCart_Success_NewItem_NewCartCreated() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(2190000), 45, "variant.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 2);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(buyerUser));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart saved = invocation.getArgument(0);
            saved.setId(cartId);
            return saved;
        });

        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.countByCartId(cartId)).thenReturn(0);
        when(cartItemRepository.saveAndFlush(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        CartItemResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals(variant.getId(), response.variantId());
        assertEquals(2, response.quantity());
        assertEquals(new BigDecimal("2190000.00"), response.price());
        assertEquals(new BigDecimal("4380000.00"), response.itemSubtotal());
        assertEquals(45, response.stockQuantity());
        assertFalse(response.isOutOfStock());
        assertTrue(response.isAvailable());
        assertNull(response.unavailableReason());
        assertEquals("variant.jpg", response.imageUrl());

        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(cartItemRepository, times(1)).saveAndFlush(any(CartItem.class));
    }

    @Test
    @DisplayName("TC-02: addToCart_Success_NewItem_ExistingCart - User đã có giỏ hàng, thêm SKU mới thành công")
    void addToCart_Success_NewItem_ExistingCart() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(1500000), 20, "variant.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.countByCartId(cartId)).thenReturn(5);
        when(cartItemRepository.saveAndFlush(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        CartItemResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals(1, response.quantity());
        assertEquals(new BigDecimal("1500000.00"), response.itemSubtotal());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("TC-03: addToCart_Success_ExistingItem_AccumulatesQuantity - Thêm biến thể đã có sẵn trong giỏ, cộng dồn số lượng")
    void addToCart_Success_ExistingItem_AccumulatesQuantity() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(2000000), 10, "variant.jpg");
        CartItem existingItem = createCartItem(cart, product, variant, 2);
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 3);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId())).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.saveAndFlush(existingItem)).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals(5, response.quantity());
        assertEquals(new BigDecimal("10000000.00"), response.itemSubtotal());
        verify(cartItemRepository, never()).countByCartId(any());
    }

    @Test
    @DisplayName("TC-04: addToCart_Success_FallbackProductCoverImage - Biến thể không có ảnh riêng, fallback ảnh bìa sản phẩm")
    void addToCart_Success_FallbackProductCoverImage() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(1000000), 10, null); // no variant image
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        ProductImage coverImage = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .imageUrl("https://cdn.techhub.vn/cover.jpg")
                .displayOrder(0)
                .build();

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.countByCartId(cartId)).thenReturn(0);
        when(cartItemRepository.saveAndFlush(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
        when(productImageRepository.findByProductIdInAndDisplayOrder(Collections.singleton(product.getId()), 0))
                .thenReturn(List.of(coverImage));

        CartItemResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals("https://cdn.techhub.vn/cover.jpg", response.imageUrl());
    }

    @Test
    @DisplayName("TC-05: addToCart_SelfBuying_ThrowsCannotAddOwnProduct - Chủ shop cố tình thêm sản phẩm của gian hàng mình")
    void addToCart_SelfBuying_ThrowsCannotAddOwnProduct() {
        Product product = createValidProduct(UUID.randomUUID(), "My Own Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        // Set seller user id to current userId
        product.getShop().getSeller().getUser().setId(userId);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.CANNOT_ADD_OWN_PRODUCT, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-06: addToCart_ShopPaused_ThrowsShopIsPaused - Shop tạm nghỉ ném SHOP_IS_PAUSED")
    void addToCart_ShopPaused_ThrowsShopIsPaused() {
        Product product = createValidProduct(UUID.randomUUID(), "Paused Shop", ShopStatus.PAUSED, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-07: addToCart_ShopSuspended_ThrowsShopNotActive - Shop bị đình chỉ ném SHOP_NOT_ACTIVE")
    void addToCart_ShopSuspended_ThrowsShopNotActive() {
        Product product = createValidProduct(UUID.randomUUID(), "Suspended Shop", ShopStatus.SUSPENDED, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-08: addToCart_ShopOwnerBanned_ThrowsShopNotActive - Chủ Shop bị cấm ném SHOP_NOT_ACTIVE")
    void addToCart_ShopOwnerBanned_ThrowsShopNotActive() {
        Product product = createValidProduct(UUID.randomUUID(), "Banned Owner Shop", ShopStatus.ACTIVE, UserStatus.BANNED);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-09: addToCart_CategoryInactive_ThrowsCategoryInactive - Danh mục INACTIVE ném CATEGORY_INACTIVE")
    void addToCart_CategoryInactive_ThrowsCategoryInactive() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.getCategory().setStatus(CategoryStatus.INACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.CATEGORY_INACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: addToCart_ProductUnpublished_ThrowsProductNotFound - Sản phẩm chưa công khai ném PRODUCT_NOT_FOUND")
    void addToCart_ProductUnpublished_ThrowsProductNotFound() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: addToCart_ProductSuspended_ThrowsProductSuspended - Sản phẩm bị sàn đình chỉ ném PRODUCT_SUSPENDED")
    void addToCart_ProductSuspended_ThrowsProductSuspended() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setSuspended(true);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.PRODUCT_SUSPENDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: addToCart_ProductDeleted_ThrowsProductNotFound - Sản phẩm bị xóa mềm ném PRODUCT_NOT_FOUND")
    void addToCart_ProductDeleted_ThrowsProductNotFound() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setDeleted(true);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: addToCart_ProductNotApproved_ThrowsProductNotApproved - Sản phẩm chưa được duyệt ném PRODUCT_NOT_APPROVED")
    void addToCart_ProductNotApproved_ThrowsProductNotApproved() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.PRODUCT_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: addToCart_VariantNotFound_ThrowsException - Biến thể không tồn tại trong DB ném VARIANT_NOT_FOUND")
    void addToCart_VariantNotFound_ThrowsException() {
        UUID nonExistentVariantId = UUID.randomUUID();
        AddToCartRequest request = new AddToCartRequest(nonExistentVariantId, 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(nonExistentVariantId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.VARIANT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: addToCart_VariantSoftDeleted_ThrowsVariantNotFound - Biến thể bị xóa mềm ném VARIANT_NOT_FOUND")
    void addToCart_VariantSoftDeleted_ThrowsVariantNotFound() {
        UUID deletedVariantId = UUID.randomUUID();
        AddToCartRequest request = new AddToCartRequest(deletedVariantId, 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        // Soft-deleted variant filtered out by findByIdWithDetails query
        when(productVariantRepository.findByIdWithDetails(deletedVariantId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.VARIANT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-16: addToCart_VariantInactive_ThrowsVariantNotFound - Biến thể có status INACTIVE ném VARIANT_NOT_FOUND")
    void addToCart_VariantInactive_ThrowsVariantNotFound() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "img.jpg");
        variant.setStatus(ProductVariantStatus.INACTIVE);
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.VARIANT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-17: addToCart_InsufficientStock_NewItem_ThrowsException - Thêm SKU mới vượt quá tồn kho khả dụng")
    void addToCart_InsufficientStock_NewItem_ThrowsException() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 5, "img.jpg"); // stock = 5
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 10); // requested 10

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.countByCartId(cartId)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-18: addToCart_InsufficientStock_Accumulated_ThrowsException - Cộng dồn số lượng vượt quá tồn kho khả dụng")
    void addToCart_InsufficientStock_Accumulated_ThrowsException() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 5, "img.jpg"); // stock = 5
        CartItem existingItem = createCartItem(cart, product, variant, 4); // already has 4
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 2); // adding 2 -> 6 > 5

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId())).thenReturn(Optional.of(existingItem));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-19: addToCart_CartCapacityLimitExceeded_ThrowsException - Giỏ hàng đã có 50 items, thêm SKU thứ 51 bị chặn")
    void addToCart_CartCapacityLimitExceeded_ThrowsException() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.countByCartId(cartId)).thenReturn(50); // already 50

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.CART_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-20: addToCart_CartCapacityLimit_ExistingItem_Allowed - Giỏ hàng đã có 50 items nhưng tăng số lượng SKU có sẵn thì cho phép")
    void addToCart_CartCapacityLimit_ExistingItem_Allowed() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "img.jpg");
        CartItem existingItem = createCartItem(cart, product, variant, 2);
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 1);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId())).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.saveAndFlush(existingItem)).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals(3, response.quantity());
    }

    @Test
    @DisplayName("TC-21: addToCart_TotalQuantityExceeds999_ThrowsInvalidQuantity - Cộng dồn số lượng vượt quá 999 ném INVALID_QUANTITY")
    void addToCart_TotalQuantityExceeds999_ThrowsInvalidQuantity() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 2000, "img.jpg"); // stock plenty
        CartItem existingItem = createCartItem(cart, product, variant, 990);
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 15); // total 1005 > 999

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId())).thenReturn(Optional.of(existingItem));

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addToCart(userId, request));
        assertEquals(ErrorCode.INVALID_QUANTITY, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-22: addToCart_ConcurrentDuplicate_CatchesDataIntegrityViolation_MergesQuantity - Race condition double insert bắt DataIntegrityViolationException và merge")
    void addToCart_ConcurrentDuplicate_CatchesDataIntegrityViolation_MergesQuantity() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 20, "img.jpg");
        AddToCartRequest request = new AddToCartRequest(variant.getId(), 2);

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findByIdWithDetails(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variant.getId()))
                .thenReturn(Optional.empty()) // First check: thought it was new
                .thenReturn(Optional.of(createCartItem(cart, product, variant, 1))); // Second check after exception: found existing with qty 1
        when(cartItemRepository.countByCartId(cartId)).thenReturn(0);

        // First save throws DataIntegrityViolationException, second save succeeds
        CartItem mergedItem = createCartItem(cart, product, variant, 3);
        when(cartItemRepository.saveAndFlush(any(CartItem.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"))
                .thenReturn(mergedItem);

        CartItemResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals(3, response.quantity());
    }

    // ==========================================
    // UNIT TESTS CHO PUT /api/cart/items/{itemId}
    // ==========================================

    @Test
    @DisplayName("TC-01: updateCartItem_Success_IncrementQuantity - Tăng số lượng thành công (2 lên 3)")
    void updateCartItem_Success_IncrementQuantity() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(2190000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemResponse response = cartService.updateCartItem(userId, itemId, request);

        assertNotNull(response);
        assertEquals(itemId, response.id());
        assertEquals(3, response.quantity());
        assertEquals(new BigDecimal("6570000.00"), response.itemSubtotal());
        assertEquals(new BigDecimal("2190000.00"), response.price());
        verify(cartItemRepository).save(item);
    }

    @Test
    @DisplayName("TC-02: updateCartItem_Success_DecrementQuantity - Giảm số lượng thành công (5 xuống 4)")
    void updateCartItem_Success_DecrementQuantity() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 5);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(4);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemResponse response = cartService.updateCartItem(userId, itemId, request);

        assertNotNull(response);
        assertEquals(4, response.quantity());
        assertEquals(new BigDecimal("400000.00"), response.itemSubtotal());
        verify(cartItemRepository).save(item);
    }

    @Test
    @DisplayName("TC-03: updateCartItem_Success_ExactMaxStockQuantity - Cập nhật đúng bằng tồn kho tối đa (10/10)")
    void updateCartItem_Success_ExactMaxStockQuantity() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 10, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 5);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(10);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemResponse response = cartService.updateCartItem(userId, itemId, request);

        assertNotNull(response);
        assertEquals(10, response.quantity());
        assertEquals(10, response.stockQuantity());
    }

    @Test
    @DisplayName("TC-04: updateCartItem_Success_RecalculatesPriceAndSubtotal - Tính lại đơn giá Live Price và subtotal chuẩn HALF_UP")
    void updateCartItem_Success_RecalculatesPriceAndSubtotal() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, new BigDecimal("2190000.00"), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 1);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(2);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemResponse response = cartService.updateCartItem(userId, itemId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("2190000.00"), response.price());
        assertEquals(new BigDecimal("4380000.00"), response.itemSubtotal());
    }

    @Test
    @DisplayName("TC-05: updateCartItem_Success_FallbackProductCoverImage - Fallback ảnh bìa sản phẩm khi variant không có ảnh riêng")
    void updateCartItem_Success_FallbackProductCoverImage() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, null); // variant image null
        CartItem item = createCartItem(cart, product, variant, 1);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(2);

        ProductImage coverImg = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(product)
                .imageUrl("https://cdn.techhub.vn/cover.jpg")
                .displayOrder(0)
                .build();

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageRepository.findByProductIdInAndDisplayOrder(Collections.singleton(product.getId()), 0))
                .thenReturn(List.of(coverImg));

        CartItemResponse response = cartService.updateCartItem(userId, itemId, request);

        assertNotNull(response);
        assertEquals("https://cdn.techhub.vn/cover.jpg", response.imageUrl());
    }

    @Test
    @DisplayName("TC-06: updateCartItem_Idempotent_SameQuantity_Success - Request cùng số lượng thành công bất biến (Idempotent)")
    void updateCartItem_Idempotent_SameQuantity_Success() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 3);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemResponse response = cartService.updateCartItem(userId, itemId, request);

        assertNotNull(response);
        assertEquals(3, response.quantity());
    }

    @Test
    @DisplayName("TC-07: updateCartItem_ItemNotFound_ThrowsCartItemNotFound - ItemId không tồn tại ném CART_ITEM_NOT_FOUND")
    void updateCartItem_ItemNotFound_ThrowsCartItemNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        UpdateCartItemRequest request = new UpdateCartItemRequest(2);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(nonExistentId, userId))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, nonExistentId, request));
        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-08: updateCartItem_BelongsToAnotherUser_ThrowsCartItemNotFound - IDOR cố tình sửa item của user khác ném CART_ITEM_NOT_FOUND")
    void updateCartItem_BelongsToAnotherUser_ThrowsCartItemNotFound() {
        UUID otherUserItemId = UUID.randomUUID();
        UpdateCartItemRequest request = new UpdateCartItemRequest(2);

        // findByIdAndUserIdWithDetailsForUpdate filters by userId, returns empty for other user's item
        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(otherUserItemId, userId))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, otherUserItemId, request));
        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-09: updateCartItem_InsufficientStock_ThrowsInsufficientStock - Vượt tồn kho ném INSUFFICIENT_STOCK")
    void updateCartItem_InsufficientStock_ThrowsInsufficientStock() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 5, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(6); // 6 > 5 stock

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: updateCartItem_ShopPaused_ThrowsShopIsPaused - Shop PAUSED ném SHOP_IS_PAUSED")
    void updateCartItem_ShopPaused_ThrowsShopIsPaused() {
        Product product = createValidProduct(UUID.randomUUID(), "Paused Shop", ShopStatus.PAUSED, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: updateCartItem_ShopSuspended_ThrowsShopNotActive - Shop SUSPENDED ném SHOP_NOT_ACTIVE")
    void updateCartItem_ShopSuspended_ThrowsShopNotActive() {
        Product product = createValidProduct(UUID.randomUUID(), "Suspended Shop", ShopStatus.SUSPENDED, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: updateCartItem_ShopOwnerBanned_ThrowsShopNotActive - Chủ Shop BANNED ném SHOP_NOT_ACTIVE")
    void updateCartItem_ShopOwnerBanned_ThrowsShopNotActive() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.BANNED);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: updateCartItem_CategoryInactive_ThrowsCategoryInactive - Category INACTIVE ném CATEGORY_INACTIVE")
    void updateCartItem_CategoryInactive_ThrowsCategoryInactive() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.getCategory().setStatus(CategoryStatus.INACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.CATEGORY_INACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: updateCartItem_ProductUnpublished_ThrowsProductNotFound - Product UNPUBLISHED ném PRODUCT_NOT_FOUND")
    void updateCartItem_ProductUnpublished_ThrowsProductNotFound() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: updateCartItem_ProductSuspended_ThrowsProductSuspended - Product bị đình chỉ ném PRODUCT_SUSPENDED")
    void updateCartItem_ProductSuspended_ThrowsProductSuspended() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setSuspended(true);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.PRODUCT_SUSPENDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-16: updateCartItem_ProductDeleted_ThrowsProductNotFound - Product bị xóa mềm ném PRODUCT_NOT_FOUND")
    void updateCartItem_ProductDeleted_ThrowsProductNotFound() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setDeleted(true);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-17: updateCartItem_ProductNotApproved_ThrowsProductNotApproved - Product chưa duyệt ném PRODUCT_NOT_APPROVED")
    void updateCartItem_ProductNotApproved_ThrowsProductNotApproved() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.PRODUCT_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-18: updateCartItem_VariantInactive_ThrowsVariantNotFound - Variant INACTIVE ném VARIANT_NOT_FOUND")
    void updateCartItem_VariantInactive_ThrowsVariantNotFound() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        variant.setStatus(ProductVariantStatus.INACTIVE);
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.VARIANT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-19: updateCartItem_VariantSoftDeleted_ThrowsVariantNotFound - Variant bị xóa mềm ném VARIANT_NOT_FOUND")
    void updateCartItem_VariantSoftDeleted_ThrowsVariantNotFound() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        variant.setDeleted(true);
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.updateCartItem(userId, itemId, request));
        assertEquals(ErrorCode.VARIANT_NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // UNIT TESTS CHO DELETE /api/cart/items/{itemId}
    // ==========================================

    @Test
    @DisplayName("TC-01: deleteCartItem_Success_HappyPath - Xóa món hàng thành công trong điều kiện bình thường")
    void deleteCartItem_Success_HappyPath() {
        Product product = createValidProduct(UUID.randomUUID(), "Keychron Store", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        cartService.deleteCartItem(userId, itemId);

        verify(cartItemRepository, times(1)).findByIdAndUserId(itemId, userId);
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-02: deleteCartItem_Success_WhenShopPaused - Xóa thành công kể cả khi Shop đang PAUSED (BR-12C)")
    void deleteCartItem_Success_WhenShopPaused() {
        Product product = createValidProduct(UUID.randomUUID(), "Paused Shop", ShopStatus.PAUSED, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-03: deleteCartItem_Success_WhenShopSuspended - Xóa thành công kể cả khi Shop bị SUSPENDED (BR-12C)")
    void deleteCartItem_Success_WhenShopSuspended() {
        Product product = createValidProduct(UUID.randomUUID(), "Suspended Shop", ShopStatus.SUSPENDED, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-04: deleteCartItem_Success_WhenShopOwnerBanned - Xóa thành công kể cả khi Chủ Shop bị BANNED (BR-12C)")
    void deleteCartItem_Success_WhenShopOwnerBanned() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.BANNED);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-05: deleteCartItem_Success_WhenProductSuspended - Xóa thành công kể cả khi Sản phẩm bị Admin đình chỉ (BR-12C)")
    void deleteCartItem_Success_WhenProductSuspended() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setSuspended(true);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-06: deleteCartItem_Success_WhenProductDeleted - Xóa thành công kể cả khi Sản phẩm bị xóa mềm (BR-12C)")
    void deleteCartItem_Success_WhenProductDeleted() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setDeleted(true);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-07: deleteCartItem_Success_WhenProductUnpublished - Xóa thành công kể cả khi Sản phẩm bị gỡ UNPUBLISHED (BR-12C)")
    void deleteCartItem_Success_WhenProductUnpublished() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setVisibilityStatus(ProductVisibilityStatus.UNPUBLISHED);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-08: deleteCartItem_Success_WhenProductNotApproved - Xóa thành công kể cả khi Sản phẩm PENDING_APPROVAL (BR-12C)")
    void deleteCartItem_Success_WhenProductNotApproved() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.setApprovalStatus(ProductApprovalStatus.PENDING_APPROVAL);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-09: deleteCartItem_Success_WhenVariantDeleted - Xóa thành công kể cả khi Biến thể bị Seller xóa mềm (BR-12C)")
    void deleteCartItem_Success_WhenVariantDeleted() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        variant.setDeleted(true);
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-10: deleteCartItem_Success_WhenVariantInactive - Xóa thành công kể cả khi Biến thể INACTIVE (BR-12C)")
    void deleteCartItem_Success_WhenVariantInactive() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        variant.setStatus(ProductVariantStatus.INACTIVE);
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-11: deleteCartItem_Success_WhenCategoryInactive - Xóa thành công kể cả khi Danh mục cha INACTIVE (BR-12C)")
    void deleteCartItem_Success_WhenCategoryInactive() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        product.getCategory().setStatus(CategoryStatus.INACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 50, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-12: deleteCartItem_Success_WhenOutOfStock - Xóa thành công kể cả khi Sản phẩm hết hàng tồn kho 0 (BR-12C)")
    void deleteCartItem_Success_WhenOutOfStock() {
        Product product = createValidProduct(UUID.randomUUID(), "Active Shop", ShopStatus.ACTIVE, UserStatus.ACTIVE);
        ProductVariant variant = createValidVariant(product, BigDecimal.valueOf(100000), 0, "variant.png");
        CartItem item = createCartItem(cart, product, variant, 2);
        UUID itemId = item.getId();

        when(cartItemRepository.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        assertDoesNotThrow(() -> cartService.deleteCartItem(userId, itemId));
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("TC-13: deleteCartItem_ItemNotFound_ThrowsCartItemNotFound - ItemId không tồn tại ném CART_ITEM_NOT_FOUND (HTTP 404)")
    void deleteCartItem_ItemNotFound_ThrowsCartItemNotFound() {
        UUID randomItemId = UUID.randomUUID();

        when(cartItemRepository.findByIdAndUserId(randomItemId, userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.deleteCartItem(userId, randomItemId));
        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    @DisplayName("TC-14: deleteCartItem_BelongsToAnotherUser_ThrowsCartItemNotFound - IDOR cố tình xóa item của user khác ném CART_ITEM_NOT_FOUND (HTTP 404)")
    void deleteCartItem_BelongsToAnotherUser_ThrowsCartItemNotFound() {
        UUID otherUserItemId = UUID.randomUUID();

        when(cartItemRepository.findByIdAndUserId(otherUserItemId, userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.deleteCartItem(userId, otherUserItemId));
        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }
}


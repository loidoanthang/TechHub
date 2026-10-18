package com.techhub.service.cart;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CartPreviewRequest;
import com.techhub.model.dto.response.CartPreviewResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho CartService.previewCart (POST /api/cart/preview)")
class CartPreviewServiceImplTest {

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
    private Shop activeShop1;
    private Shop activeShop2;
    private Product product1;
    private Product product2;
    private ProductVariant variant1;
    private ProductVariant variant2;
    private CartItem cartItem1;
    private CartItem cartItem2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(Collections.emptyList());

        // Shop 1
        activeShop1 = Shop.builder()
                .id(UUID.randomUUID())
                .name("Apple Official Store")
                .status(ShopStatus.ACTIVE)
                .build();

        product1 = Product.builder()
                .id(UUID.randomUUID())
                .name("iPhone 15")
                .shop(activeShop1)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isDeleted(false)
                .isSuspended(false)
                .build();

        variant1 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product1)
                .variantName("128GB Black")
                .sku("IP15-128-BLK")
                .price(BigDecimal.valueOf(1000000.00)) // 1,000,000 VND
                .stockQuantity(10)
                .isDeleted(false)
                .status(ProductVariantStatus.ACTIVE)
                .build();

        cartItem1 = CartItem.builder()
                .id(UUID.randomUUID())
                .product(product1)
                .productVariant(variant1)
                .quantity(1)
                .build();

        // Shop 2
        activeShop2 = Shop.builder()
                .id(UUID.randomUUID())
                .name("Samsung Official Store")
                .status(ShopStatus.ACTIVE)
                .build();

        product2 = Product.builder()
                .id(UUID.randomUUID())
                .name("Galaxy S24")
                .shop(activeShop2)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .visibilityStatus(ProductVisibilityStatus.PUBLISHED)
                .isDeleted(false)
                .isSuspended(false)
                .build();

        variant2 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product2)
                .variantName("256GB Gray")
                .sku("S24-256-GRY")
                .price(BigDecimal.valueOf(1000000.00)) // 1,000,000 VND
                .stockQuantity(10)
                .isDeleted(false)
                .status(ProductVariantStatus.ACTIVE)
                .build();

        cartItem2 = CartItem.builder()
                .id(UUID.randomUUID())
                .product(product2)
                .productVariant(variant2)
                .quantity(1)
                .build();
    }

    private Voucher createShopVoucher(Shop shop, String code, DiscountType discountType,
                                      BigDecimal discountValue, BigDecimal minOrder, BigDecimal maxCap) {
        return Voucher.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .code(code)
                .name("Voucher " + code)
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderAmount(minOrder)
                .maxDiscountAmount(maxCap)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .usageLimit(100)
                .usedCount(0)
                .reservedCount(0)
                .build();
    }

    private Voucher createPlatformVoucher(String code, DiscountType discountType,
                                          BigDecimal discountValue, BigDecimal minOrder, BigDecimal maxCap) {
        return Voucher.builder()
                .id(UUID.randomUUID())
                .shop(null)
                .code(code)
                .name("Platform Voucher " + code)
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderAmount(minOrder)
                .maxDiscountAmount(maxCap)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .usageLimit(100)
                .usedCount(0)
                .reservedCount(0)
                .build();
    }

    private Voucher createShopShippingVoucher(Shop shop, String code, DiscountType discountType,
                                              BigDecimal discountValue, BigDecimal minOrder, BigDecimal maxCap) {
        return Voucher.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .code(code)
                .name("Shipping Voucher " + code)
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.SHIPPING_FEE)
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderAmount(minOrder)
                .maxDiscountAmount(maxCap)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .usageLimit(100)
                .usedCount(0)
                .reservedCount(0)
                .build();
    }

    private Voucher createPlatformShippingVoucher(String code, DiscountType discountType,
                                                  BigDecimal discountValue, BigDecimal minOrder, BigDecimal maxCap) {
        return Voucher.builder()
                .id(UUID.randomUUID())
                .shop(null)
                .code(code)
                .name("FreeShip " + code)
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.SHIPPING_FEE)
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderAmount(minOrder)
                .maxDiscountAmount(maxCap)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .usageLimit(100)
                .usedCount(0)
                .reservedCount(0)
                .build();
    }

    private ShippingOption createShippingOption(Shop shop, BigDecimal fee) {
        return ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(fee)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Test 1: previewCart_Success_NoVouchers - Tính toán chính xác tổng tiền khi không chọn voucher")
    void previewCart_Success_NoVouchers() {
        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId(), cartItem2.getId())))
                .thenReturn(List.of(cartItem1, cartItem2));

        CartPreviewRequest request = new CartPreviewRequest(
                List.of(cartItem1.getId(), cartItem2.getId()),
                null,
                null
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("2000000.00"), response.totalProductAmount());
        assertEquals(new BigDecimal("0.00"), response.totalShopDiscountAmount());
        assertEquals(new BigDecimal("0.00"), response.totalPlatformDiscountAmount());
        assertEquals(new BigDecimal("0.00"), response.totalSavedAmount());
        assertEquals(new BigDecimal("2000000.00"), response.finalTotalAmount());
        assertEquals(2, response.totalSelectedItems());
        assertEquals(2, response.shops().size());
        assertTrue(response.warnings().isEmpty());
    }

    @Test
    @DisplayName("Test 2: previewCart_Success_WithShopVoucher_FixedAmount - Áp voucher Shop cố định 50k thành công")
    void previewCart_Success_WithShopVoucher_FixedAmount() {
        Voucher shopVoucher = createShopVoucher(activeShop1, "APPLE50K", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(300000), null);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId())))
                .thenReturn(List.of(cartItem1));
        when(voucherRepository.findAllWithShopByIdIn(List.of(shopVoucher.getId())))
                .thenReturn(List.of(shopVoucher));

        CartPreviewRequest request = new CartPreviewRequest(
                List.of(cartItem1.getId()),
                List.of(shopVoucher.getId()),
                null
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1000000.00"), response.totalProductAmount());
        assertEquals(new BigDecimal("50000.00"), response.totalShopDiscountAmount());
        assertEquals(new BigDecimal("50000.00"), response.totalSavedAmount());
        assertEquals(new BigDecimal("950000.00"), response.finalTotalAmount());
        assertNotNull(response.shops().get(0).appliedShopVoucher());
        assertEquals("APPLE50K", response.shops().get(0).appliedShopVoucher().code());
    }

    @Test
    @DisplayName("Test 3: previewCart_Success_WithShopVoucher_PercentageWithCap - Áp voucher Shop % chạm trần tối đa")
    void previewCart_Success_WithShopVoucher_PercentageWithCap() {
        // 10% của 1,000,000 là 100,000 nhưng trần maxDiscountAmount là 60,000
        Voucher shopVoucher = createShopVoucher(activeShop1, "APPLE10PCT", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), BigDecimal.valueOf(500000), BigDecimal.valueOf(60000));

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId())))
                .thenReturn(List.of(cartItem1));
        when(voucherRepository.findAllWithShopByIdIn(List.of(shopVoucher.getId())))
                .thenReturn(List.of(shopVoucher));

        CartPreviewRequest request = new CartPreviewRequest(
                List.of(cartItem1.getId()),
                List.of(shopVoucher.getId()),
                null
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        assertEquals(new BigDecimal("60000.00"), response.totalShopDiscountAmount());
        assertEquals(new BigDecimal("940000.00"), response.finalTotalAmount());
    }

    @Test
    @DisplayName("Test 4: previewCart_Success_WithPlatformVoucher - Áp voucher toàn sàn 100k thành công")
    void previewCart_Success_WithPlatformVoucher() {
        Voucher platVoucher = createPlatformVoucher("TECHHUB100K", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(1500000), null);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId(), cartItem2.getId())))
                .thenReturn(List.of(cartItem1, cartItem2));
        when(voucherRepository.findWithShopById(platVoucher.getId()))
                .thenReturn(Optional.of(platVoucher));

        CartPreviewRequest request = new CartPreviewRequest(
                List.of(cartItem1.getId(), cartItem2.getId()),
                null,
                platVoucher.getId()
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        assertEquals(new BigDecimal("2000000.00"), response.totalProductAmount());
        assertEquals(new BigDecimal("100000.00"), response.totalPlatformDiscountAmount());
        assertEquals(new BigDecimal("1900000.00"), response.finalTotalAmount());
    }

    @Test
    @DisplayName("Test 5: previewCart_Success_StackingShopAndPlatformVouchers - Xếp chồng Voucher Shop và Sàn chuẩn BR-16C")
    void previewCart_Success_StackingShopAndPlatformVouchers() {
        Voucher shopVoucher = createShopVoucher(activeShop1, "APPLE50K", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(500000), null);
        Voucher platVoucher = createPlatformVoucher("TECHHUB100K", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(1500000), null);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId(), cartItem2.getId())))
                .thenReturn(List.of(cartItem1, cartItem2));
        when(voucherRepository.findAllWithShopByIdIn(List.of(shopVoucher.getId())))
                .thenReturn(List.of(shopVoucher));
        when(voucherRepository.findWithShopById(platVoucher.getId()))
                .thenReturn(Optional.of(platVoucher));

        CartPreviewRequest request = new CartPreviewRequest(
                List.of(cartItem1.getId(), cartItem2.getId()),
                List.of(shopVoucher.getId()),
                platVoucher.getId()
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        assertEquals(new BigDecimal("2000000.00"), response.totalProductAmount());
        assertEquals(new BigDecimal("50000.00"), response.totalShopDiscountAmount());
        assertEquals(new BigDecimal("100000.00"), response.totalPlatformDiscountAmount());
        assertEquals(new BigDecimal("150000.00"), response.totalSavedAmount());
        assertEquals(new BigDecimal("1850000.00"), response.finalTotalAmount());
    }

    @Test
    @DisplayName("Test 6: previewCart_DiscountExceedsProductAmount_ClampedToZero - Mức giảm vượt quá tiền hàng tự động clamp về 0đ (BR-14)")
    void previewCart_DiscountExceedsProductAmount_ClampedToZero() {
        // Hàng 1,000,000đ nhưng voucher giảm 2,000,000đ
        Voucher shopVoucher = createShopVoucher(activeShop1, "HUGE2M", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(2000000), BigDecimal.valueOf(500000), null);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId())))
                .thenReturn(List.of(cartItem1));
        when(voucherRepository.findAllWithShopByIdIn(List.of(shopVoucher.getId())))
                .thenReturn(List.of(shopVoucher));

        CartPreviewRequest request = new CartPreviewRequest(
                List.of(cartItem1.getId()),
                List.of(shopVoucher.getId()),
                null
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        assertEquals(new BigDecimal("1000000.00"), response.totalShopDiscountAmount());
        assertEquals(new BigDecimal("0.00"), response.finalTotalAmount());
    }

    @Test
    @DisplayName("Test 7: previewCart_ShopVoucher_MinOrderNotMet_Warning - Đơn chưa đủ điều kiện voucher shop thì trả về warning và discount = 0")
    void previewCart_ShopVoucher_MinOrderNotMet_Warning() {
        // Tiền hàng 1,000,000đ nhưng voucher yêu cầu tối thiểu 5,000,000đ
        Voucher shopVoucher = createShopVoucher(activeShop1, "APPLE_VIP", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(200000), BigDecimal.valueOf(5000000), null);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId())))
                .thenReturn(List.of(cartItem1));
        when(voucherRepository.findAllWithShopByIdIn(List.of(shopVoucher.getId())))
                .thenReturn(List.of(shopVoucher));

        CartPreviewRequest request = new CartPreviewRequest(
                List.of(cartItem1.getId()),
                List.of(shopVoucher.getId()),
                null
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        assertEquals(new BigDecimal("0.00"), response.totalShopDiscountAmount());
        assertEquals(new BigDecimal("1000000.00"), response.finalTotalAmount());
        assertFalse(response.warnings().isEmpty());
        assertTrue(response.warnings().get(0).contains("APPLE_VIP"));
    }

    @Test
    @DisplayName("Test 8: previewCart_PlatformVoucher_BaseAmountNotMet_Warning - Chưa đủ min order sàn thì warning và discount = 0")
    void previewCart_PlatformVoucher_BaseAmountNotMet_Warning() {
        // Tiền hàng 1,000,000đ nhưng voucher sàn yêu cầu 3,000,000đ
        Voucher platVoucher = createPlatformVoucher("TECHHUB_BIG", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(300000), BigDecimal.valueOf(3000000), null);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId())))
                .thenReturn(List.of(cartItem1));
        when(voucherRepository.findWithShopById(platVoucher.getId()))
                .thenReturn(Optional.of(platVoucher));

        CartPreviewRequest request = new CartPreviewRequest(
                List.of(cartItem1.getId()),
                null,
                platVoucher.getId()
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        assertEquals(new BigDecimal("0.00"), response.totalPlatformDiscountAmount());
        assertFalse(response.warnings().isEmpty());
        assertTrue(response.warnings().get(0).contains("TECHHUB_BIG"));
    }

    @Test
    @DisplayName("Test 9: previewCart_Error_EmptySelectedCartItemIds - Ném lỗi khi danh sách chọn rỗng")
    void previewCart_Error_EmptySelectedCartItemIds() {
        CartPreviewRequest request = new CartPreviewRequest(Collections.emptyList(), null, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                cartService.previewCart(userId, request));

        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("Test 10: previewCart_Error_CartItemNotFound_Or_IDOR - Ném lỗi 404 khi có item không thuộc về user")
    void previewCart_Error_CartItemNotFound_Or_IDOR() {
        UUID fakeItemId = UUID.randomUUID();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(fakeItemId)))
                .thenReturn(Collections.emptyList());

        CartPreviewRequest request = new CartPreviewRequest(List.of(fakeItemId), null, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                cartService.previewCart(userId, request));

        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Test 11: previewCart_Error_InsufficientStock - Ném lỗi khi số lượng mua vượt tồn kho (BR-11C)")
    void previewCart_Error_InsufficientStock() {
        variant1.setStockQuantity(2);
        cartItem1.setQuantity(5); // Vượt quá tồn kho

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId())))
                .thenReturn(List.of(cartItem1));

        CartPreviewRequest request = new CartPreviewRequest(List.of(cartItem1.getId()), null, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                cartService.previewCart(userId, request));

        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
    }

    @Test
    @DisplayName("Test 12: previewCart_Error_ShopIsPaused - Ném lỗi khi Shop đang tạm đóng cửa (BR-07)")
    void previewCart_Error_ShopIsPaused() {
        activeShop1.setStatus(ShopStatus.PAUSED);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId())))
                .thenReturn(List.of(cartItem1));

        CartPreviewRequest request = new CartPreviewRequest(List.of(cartItem1.getId()), null, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                cartService.previewCart(userId, request));

        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Test 13: previewCart_Error_ShopNotActive - Ném lỗi khi Shop bị khóa (BR-07)")
    void previewCart_Error_ShopNotActive() {
        activeShop1.setStatus(ShopStatus.BANNED);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId())))
                .thenReturn(List.of(cartItem1));

        CartPreviewRequest request = new CartPreviewRequest(List.of(cartItem1.getId()), null, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                cartService.previewCart(userId, request));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("Test 14: previewCart_ZeroDatabaseMutation_Verification - Xác nhận hoàn toàn Zero DB Mutation")
    void previewCart_ZeroDatabaseMutation_Verification() {
        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId())))
                .thenReturn(List.of(cartItem1));

        CartPreviewRequest request = new CartPreviewRequest(List.of(cartItem1.getId()), null, null);

        cartService.previewCart(userId, request);

        // Xác nhận không có bất kỳ method save, delete hay mutation nào được gọi
        verify(cartRepository, never()).save(any());
        verify(cartItemRepository, never()).save(any());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 15: previewCart_Success_ShopeeTwoTierProRata - Phân bổ chiết khấu 2 tầng Pro-Rata chuẩn Shopee")
    void previewCart_Success_ShopeeTwoTierProRata() {
        // Setup Shop 1 items: Quần bò (224k x 1), Áo (199k x 2)
        Shop shop1 = Shop.builder().id(UUID.randomUUID()).name("Shop Thời Trang 1").status(ShopStatus.ACTIVE).build();
        Product pQuanBo = Product.builder().id(UUID.randomUUID()).name("Quần Bò").shop(shop1)
                .approvalStatus(ProductApprovalStatus.APPROVED).visibilityStatus(ProductVisibilityStatus.PUBLISHED).build();
        ProductVariant vQuanBo = ProductVariant.builder().id(UUID.randomUUID()).product(pQuanBo)
                .price(new BigDecimal("224000.00")).stockQuantity(10).status(ProductVariantStatus.ACTIVE).build();
        CartItem itemQuanBo = CartItem.builder().id(UUID.randomUUID()).product(pQuanBo).productVariant(vQuanBo).quantity(1).build();

        Product pAo = Product.builder().id(UUID.randomUUID()).name("Áo").shop(shop1)
                .approvalStatus(ProductApprovalStatus.APPROVED).visibilityStatus(ProductVisibilityStatus.PUBLISHED).build();
        ProductVariant vAo = ProductVariant.builder().id(UUID.randomUUID()).product(pAo)
                .price(new BigDecimal("199000.00")).stockQuantity(10).status(ProductVariantStatus.ACTIVE).build();
        CartItem itemAo = CartItem.builder().id(UUID.randomUUID()).product(pAo).productVariant(vAo).quantity(2).build();

        // Setup Shop 2 items: Quần Jean (189k x 1)
        Shop shop2 = Shop.builder().id(UUID.randomUUID()).name("Shop Thời Trang 2").status(ShopStatus.ACTIVE).build();
        Product pQuanJean = Product.builder().id(UUID.randomUUID()).name("Quần Jean").shop(shop2)
                .approvalStatus(ProductApprovalStatus.APPROVED).visibilityStatus(ProductVisibilityStatus.PUBLISHED).build();
        ProductVariant vQuanJean = ProductVariant.builder().id(UUID.randomUUID()).product(pQuanJean)
                .price(new BigDecimal("189000.00")).stockQuantity(10).status(ProductVariantStatus.ACTIVE).build();
        CartItem itemQuanJean = CartItem.builder().id(UUID.randomUUID()).product(pQuanJean).productVariant(vQuanJean).quantity(1).build();

        // Vouchers
        Voucher shop1Voucher = createShopVoucher(shop1, "SHOP1_10K", DiscountType.FIXED_AMOUNT,
                new BigDecimal("10000.00"), new BigDecimal("300000.00"), null);
        Voucher shop2Voucher = createShopVoucher(shop2, "SHOP2_7K", DiscountType.FIXED_AMOUNT,
                new BigDecimal("7000.00"), new BigDecimal("149000.00"), null);
        Voucher platVoucher = createPlatformVoucher("PLATFORM_15K", DiscountType.FIXED_AMOUNT,
                new BigDecimal("15000.00"), BigDecimal.ZERO, null);

        List<CartItem> allItems = List.of(itemQuanBo, itemAo, itemQuanJean);
        List<UUID> itemIds = allItems.stream().map(CartItem::getId).toList();

        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, itemIds)).thenReturn(allItems);
        when(voucherRepository.findAllWithShopByIdIn(List.of(shop1Voucher.getId(), shop2Voucher.getId())))
                .thenReturn(List.of(shop1Voucher, shop2Voucher));
        when(voucherRepository.findWithShopById(platVoucher.getId())).thenReturn(Optional.of(platVoucher));

        CartPreviewRequest request = new CartPreviewRequest(
                itemIds,
                List.of(shop1Voucher.getId(), shop2Voucher.getId()),
                platVoucher.getId(),
                null
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        // Verification tổng quan
        assertNotNull(response);
        assertEquals(new BigDecimal("811000.00"), response.totalProductAmount());
        assertEquals(new BigDecimal("17000.00"), response.totalShopDiscountAmount());
        assertEquals(new BigDecimal("15000.00"), response.totalPlatformDiscountAmount());
        assertEquals(new BigDecimal("32000.00"), response.totalSavedAmount());
        assertEquals(new BigDecimal("779000.00"), response.finalProductAmount());
        assertEquals(new BigDecimal("779000.00"), response.finalTotalAmount());
        assertEquals(4, response.totalSelectedItems());
        assertEquals(2, response.shops().size());

        // Check Shop 1: subtotal 622,000, shop discount 10,000, plat discount 11,561.71, final 600,438.29
        var shop1Group = response.shops().stream().filter(s -> s.shopId().equals(shop1.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("622000.00"), shop1Group.shopSubtotal());
        assertEquals(new BigDecimal("10000.00"), shop1Group.shopProductDiscount());
        assertEquals(new BigDecimal("11561.71"), shop1Group.allocatedPlatformProductDiscount());
        assertEquals(new BigDecimal("600438.29"), shop1Group.shopFinalAmount());

        // Check items in Shop 1:
        // Quần bò: 224k - 7,764.99 = 216,235.01
        var resQuanBo = shop1Group.items().stream().filter(i -> i.cartItemId().equals(itemQuanBo.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("224000.00"), resQuanBo.originalSubtotal());
        assertEquals(new BigDecimal("7764.99"), resQuanBo.itemDiscountAmount());
        assertEquals(new BigDecimal("216235.01"), resQuanBo.finalItemSubtotal());
        assertEquals(new BigDecimal("216235.01"), resQuanBo.discountedUnitPrice());

        // Áo (qty 2): 398k - 13,796.72 = 384,203.28 (unit price: 192,101.64)
        var resAo = shop1Group.items().stream().filter(i -> i.cartItemId().equals(itemAo.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("398000.00"), resAo.originalSubtotal());
        assertEquals(new BigDecimal("13796.72"), resAo.itemDiscountAmount());
        assertEquals(new BigDecimal("384203.28"), resAo.finalItemSubtotal());
        assertEquals(new BigDecimal("192101.64"), resAo.discountedUnitPrice());

        // Check Shop 2: subtotal 189,000, shop discount 7,000, plat discount 3,438.29, final 178,561.71
        var shop2Group = response.shops().stream().filter(s -> s.shopId().equals(shop2.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("189000.00"), shop2Group.shopSubtotal());
        assertEquals(new BigDecimal("7000.00"), shop2Group.shopProductDiscount());
        assertEquals(new BigDecimal("3438.29"), shop2Group.allocatedPlatformProductDiscount());
        assertEquals(new BigDecimal("178561.71"), shop2Group.shopFinalAmount());

        // Quần Jean: 189k - 10,438.29 = 178,561.71
        var resQuanJean = shop2Group.items().stream().filter(i -> i.cartItemId().equals(itemQuanJean.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("189000.00"), resQuanJean.originalSubtotal());
        assertEquals(new BigDecimal("10438.29"), resQuanJean.itemDiscountAmount());
        assertEquals(new BigDecimal("178561.71"), resQuanJean.finalItemSubtotal());
        assertEquals(new BigDecimal("178561.71"), resQuanJean.discountedUnitPrice());

        // Total of items matches finalProductAmount perfectly
        BigDecimal sumItems = resQuanBo.finalItemSubtotal().add(resAo.finalItemSubtotal()).add(resQuanJean.finalItemSubtotal());
        assertEquals(response.finalProductAmount(), sumItems);
    }

    @Test
    @DisplayName("Test 16: previewCart_Success_ShippingFeeAndFreeShipVouchers - Tính toán phí vận chuyển và phân bổ FreeShip chuẩn BR-16D")
    void previewCart_Success_ShippingFeeAndFreeShipVouchers() {
        // Shop 1 has shipping fee 25k, Shop 2 has shipping fee 20k
        ShippingOption opt1 = createShippingOption(activeShop1, new BigDecimal("25000.00"));
        ShippingOption opt2 = createShippingOption(activeShop2, new BigDecimal("20000.00"));

        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(opt1, opt2));
        when(cartItemRepository.findByUserIdAndIdInWithDetails(userId, List.of(cartItem1.getId(), cartItem2.getId())))
                .thenReturn(List.of(cartItem1, cartItem2));

        // Shop 1 shipping voucher: 10k off shipping
        Voucher shop1ShipVoucher = createShopShippingVoucher(activeShop1, "FREESHIP_SHOP1", DiscountType.FIXED_AMOUNT,
                new BigDecimal("10000.00"), new BigDecimal("500000.00"), null);

        // Platform FreeShip voucher: 20k off shipping
        Voucher platShipVoucher = createPlatformShippingVoucher("FREESHIP_PLAT", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20000.00"), new BigDecimal("1000000.00"), null);

        when(voucherRepository.findAllWithShopByIdIn(List.of(shop1ShipVoucher.getId())))
                .thenReturn(List.of(shop1ShipVoucher));
        when(voucherRepository.findWithShopById(platShipVoucher.getId()))
                .thenReturn(Optional.of(platShipVoucher));

        CartPreviewRequest request = new CartPreviewRequest(
                List.of(cartItem1.getId(), cartItem2.getId()),
                List.of(shop1ShipVoucher.getId()),
                null,
                platShipVoucher.getId()
        );

        CartPreviewResponse response = cartService.previewCart(userId, request);

        assertNotNull(response);
        // Products: 1,000,000 + 1,000,000 = 2,000,000 (no product vouchers)
        assertEquals(new BigDecimal("2000000.00"), response.totalProductAmount());
        assertEquals(new BigDecimal("2000000.00"), response.finalProductAmount());

        // Shipping Fee calculations:
        // Shop 1 fee = 25,000, Shop 2 fee = 20,000 -> Total shipping fee = 45,000
        assertEquals(new BigDecimal("45000.00"), response.totalShippingFee());
        // Shop 1 shipping voucher = 10,000
        assertEquals(new BigDecimal("10000.00"), response.totalShopDiscountAmount());
        // Platform FreeShip voucher = 20,000
        assertEquals(new BigDecimal("20000.00"), response.totalPlatformDiscountAmount());
        // Total saved = 10,000 + 20,000 = 30,000
        assertEquals(new BigDecimal("30000.00"), response.totalSavedAmount());
        // Final shipping fee = 45,000 - 10,000 - 20,000 = 15,000
        assertEquals(new BigDecimal("15000.00"), response.finalShippingFee());
        // Final total amount = 2,000,000 + 15,000 = 2,015,000
        assertEquals(new BigDecimal("2015000.00"), response.finalTotalAmount());

        // Verify Shop 1 shipping breakdown:
        // Base shipping fee after shop voucher = 25,000 - 10,000 = 15,000
        // Shop 2 base shipping fee = 20,000
        // Total base shipping fee = 35,000
        // Shop 1 allocated platform freeship = 20,000 * 15,000 / 35,000 = 8,571.43
        // Shop 1 final shipping fee = 15,000 - 8,571.43 = 6,428.57
        var shop1Group = response.shops().stream().filter(s -> s.shopId().equals(activeShop1.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("25000.00"), shop1Group.shippingFee());
        assertEquals(new BigDecimal("10000.00"), shop1Group.shopShippingDiscount());
        assertEquals(new BigDecimal("8571.43"), shop1Group.allocatedPlatformShippingDiscount());
        assertEquals(new BigDecimal("6428.57"), shop1Group.finalShippingFee());
        assertEquals(new BigDecimal("1006428.57"), shop1Group.shopFinalAmount());

        // Verify Shop 2 shipping breakdown:
        // Shop 2 allocated platform freeship = 20,000 - 8,571.43 = 11,428.57
        // Shop 2 final shipping fee = 20,000 - 11,428.57 = 8,571.43
        var shop2Group = response.shops().stream().filter(s -> s.shopId().equals(activeShop2.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("20000.00"), shop2Group.shippingFee());
        assertEquals(BigDecimal.ZERO, shop2Group.shopShippingDiscount());
        assertEquals(new BigDecimal("11428.57"), shop2Group.allocatedPlatformShippingDiscount());
        assertEquals(new BigDecimal("8571.43"), shop2Group.finalShippingFee());
        assertEquals(new BigDecimal("1008571.43"), shop2Group.shopFinalAmount());

        // Sum of shopFinalAmount equals finalTotalAmount
        assertEquals(response.finalTotalAmount(), shop1Group.shopFinalAmount().add(shop2Group.shopFinalAmount()));
    }
}

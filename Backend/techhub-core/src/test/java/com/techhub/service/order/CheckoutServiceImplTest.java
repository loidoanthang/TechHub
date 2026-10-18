package com.techhub.service.order;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CheckoutPreviewRequest;
import com.techhub.model.dto.request.PlaceOrderRequest;
import com.techhub.model.dto.request.ShopOrderRequest;
import com.techhub.model.dto.response.CartItemPreviewResponse;
import com.techhub.model.dto.response.CheckoutPreviewResponse;
import com.techhub.model.dto.response.PlaceOrderResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.order.impl.CheckoutServiceImpl;
import com.techhub.service.payment.VNPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho CheckoutService (POST /api/checkout/preview - 24 Test Cases)")
class CheckoutServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ShippingOptionRepository shippingOptionRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherRedemptionRepository voucherRedemptionRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private VNPayService vnPayService;

    @InjectMocks
    private CheckoutServiceImpl checkoutService;

    private UUID userId;
    private User buyer;
    private Address defaultAddress;
    private Shop shopA;
    private Shop shopB;
    private Seller sellerA;
    private Seller sellerB;
    private User sellerUserA;
    private User sellerUserB;
    private Product productA;
    private ProductVariant variantA;
    private CartItem cartItemA;
    private Cart cartA;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        buyer = new User();
        buyer.setId(userId);
        buyer.setEmail("buyer@techhub.vn");
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setEmailVerified(true);

        defaultAddress = new Address();
        defaultAddress.setId(UUID.randomUUID());
        defaultAddress.setUser(buyer);
        defaultAddress.setRecipientName("Nguyễn Văn A");
        defaultAddress.setPhone("0912345678");
        defaultAddress.setProvince("TP. Hồ Chí Minh");
        defaultAddress.setDistrict("Quận 1");
        defaultAddress.setWard("Phường Bến Nghé");
        defaultAddress.setStreetAddress("123 Lê Duẩn");
        defaultAddress.setDefault(true);

        // Seller A & Shop A
        sellerUserA = new User();
        sellerUserA.setId(UUID.randomUUID());
        sellerA = new Seller();
        sellerA.setId(UUID.randomUUID());
        sellerA.setUser(sellerUserA);

        shopA = new Shop();
        shopA.setId(UUID.randomUUID());
        shopA.setName("Apple Official Store");
        shopA.setStatus(ShopStatus.ACTIVE);
        shopA.setSeller(sellerA);

        // Seller B & Shop B
        sellerUserB = new User();
        sellerUserB.setId(UUID.randomUUID());
        sellerB = new Seller();
        sellerB.setId(UUID.randomUUID());
        sellerB.setUser(sellerUserB);

        shopB = new Shop();
        shopB.setId(UUID.randomUUID());
        shopB.setName("Logitech Store");
        shopB.setStatus(ShopStatus.ACTIVE);
        shopB.setSeller(sellerB);

        productA = new Product();
        productA.setId(UUID.randomUUID());
        productA.setName("Củ sạc Apple 20W");
        productA.setShop(shopA);
        productA.setDeleted(false);
        productA.setSuspended(false);
        productA.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);
        productA.setApprovalStatus(ProductApprovalStatus.APPROVED);

        variantA = new ProductVariant();
        variantA.setId(UUID.randomUUID());
        variantA.setVariantName("Màu Trắng");
        variantA.setPrice(BigDecimal.valueOf(500000));
        variantA.setStockQuantity(50);
        variantA.setDeleted(false);
        variantA.setStatus(ProductVariantStatus.ACTIVE);
        variantA.setImageUrl("https://cdn.techhub.com/charger.png");

        cartA = new Cart();
        cartA.setId(UUID.randomUUID());
        cartA.setUser(buyer);

        cartItemA = new CartItem();
        cartItemA.setId(UUID.randomUUID());
        cartItemA.setCart(cartA);
        cartItemA.setProduct(productA);
        cartItemA.setProductVariant(variantA);
        cartItemA.setQuantity(1);
    }

    private void mockActiveUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(buyer));
    }

    private ShippingOption createShippingOption(Shop shop, BigDecimal fee) {
        ShippingOption so = new ShippingOption();
        so.setId(UUID.randomUUID());
        so.setShop(shop);
        so.setShippingFee(fee);
        so.setActive(true);
        return so;
    }

    @Test
    @DisplayName("TC-01: previewCheckout_Success_SingleShop_NoVoucher")
    void previewCheckout_Success_SingleShop_NoVoucher() {
        mockActiveUser();
        when(addressRepository.findDefaultAddressByUserId(userId)).thenReturn(Optional.of(defaultAddress));
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, null, null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertNotNull(response);
        assertEquals(0, BigDecimal.valueOf(500000).setScale(2, RoundingMode.HALF_UP).compareTo(response.totalProductAmount()));
        assertEquals(0, BigDecimal.valueOf(30000).setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShippingFee()));
        assertEquals(0, BigDecimal.valueOf(530000).setScale(2, RoundingMode.HALF_UP).compareTo(response.finalTotalAmount()));
        assertEquals(1, response.totalSelectedItems());
        assertEquals("Nguyễn Văn A", response.deliveryAddress().recipientName());
        assertTrue(response.warnings().isEmpty());
    }

    @Test
    @DisplayName("TC-02: previewCheckout_Success_MultiShop_WithShopVouchers")
    void previewCheckout_Success_MultiShop_WithShopVouchers() {
        mockActiveUser();
        when(addressRepository.findDefaultAddressByUserId(userId)).thenReturn(Optional.of(defaultAddress));

        Product productB = new Product();
        productB.setId(UUID.randomUUID());
        productB.setName("Chuột Logitech MX Master 3S");
        productB.setShop(shopB);
        productB.setDeleted(false);
        productB.setSuspended(false);
        productB.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);
        productB.setApprovalStatus(ProductApprovalStatus.APPROVED);

        ProductVariant variantB = new ProductVariant();
        variantB.setId(UUID.randomUUID());
        variantB.setVariantName("Màu Đen");
        variantB.setPrice(BigDecimal.valueOf(2000000));
        variantB.setStockQuantity(20);
        variantB.setDeleted(false);
        variantB.setStatus(ProductVariantStatus.ACTIVE);

        CartItem cartItemB = new CartItem();
        cartItemB.setId(UUID.randomUUID());
        cartItemB.setCart(cartA);
        cartItemB.setProduct(productB);
        cartItemB.setProductVariant(variantB);
        cartItemB.setQuantity(1);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA, cartItemB));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(
                createShippingOption(shopA, BigDecimal.valueOf(30000)),
                createShippingOption(shopB, BigDecimal.valueOf(25000))
        ));

        // Shop A Product Voucher: giảm 20k
        Voucher shopVoucherA = new Voucher();
        shopVoucherA.setId(UUID.randomUUID());
        shopVoucherA.setCode("APPLE20K");
        shopVoucherA.setName("Giảm 20k đơn từ 300k");
        shopVoucherA.setType(VoucherType.SHOP);
        shopVoucherA.setShop(shopA);
        shopVoucherA.setAppliesTo(VoucherAppliesTo.PRODUCT);
        shopVoucherA.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopVoucherA.setDiscountValue(BigDecimal.valueOf(20000));
        shopVoucherA.setMinOrderAmount(BigDecimal.valueOf(300000));
        shopVoucherA.setActive(true);
        shopVoucherA.setDeleted(false);

        // Shop B Shipping Voucher: giảm 15k
        Voucher shopVoucherB = new Voucher();
        shopVoucherB.setId(UUID.randomUUID());
        shopVoucherB.setCode("LOGISHIP15K");
        shopVoucherB.setName("Trợ giá ship 15k");
        shopVoucherB.setType(VoucherType.SHOP);
        shopVoucherB.setShop(shopB);
        shopVoucherB.setAppliesTo(VoucherAppliesTo.SHIPPING_FEE);
        shopVoucherB.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopVoucherB.setDiscountValue(BigDecimal.valueOf(15000));
        shopVoucherB.setMinOrderAmount(BigDecimal.valueOf(1000000));
        shopVoucherB.setActive(true);
        shopVoucherB.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(shopVoucherA, shopVoucherB));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(
                List.of(cartItemA.getId(), cartItemB.getId()),
                null,
                List.of(shopVoucherA.getId(), shopVoucherB.getId()),
                null,
                null
        );

        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.valueOf(2500000).setScale(2, RoundingMode.HALF_UP).compareTo(response.totalProductAmount()));
        assertEquals(0, BigDecimal.valueOf(55000).setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShippingFee()));
        assertEquals(0, BigDecimal.valueOf(35000).setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShopDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(2520000).setScale(2, RoundingMode.HALF_UP).compareTo(response.finalTotalAmount()));
        assertEquals(2, response.shops().size());
    }

    @Test
    @DisplayName("TC-03: previewCheckout_Success_PlatformVoucher_ProRataTier1")
    void previewCheckout_Success_PlatformVoucher_ProRataTier1() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher platVoucher = new Voucher();
        platVoucher.setId(UUID.randomUUID());
        platVoucher.setCode("PLAT30K");
        platVoucher.setType(VoucherType.PLATFORM);
        platVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        platVoucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        platVoucher.setDiscountValue(BigDecimal.valueOf(30000));
        platVoucher.setMinOrderAmount(BigDecimal.valueOf(200000));
        platVoucher.setActive(true);
        platVoucher.setDeleted(false);

        when(voucherRepository.findWithShopById(platVoucher.getId())).thenReturn(Optional.of(platVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(
                List.of(cartItemA.getId()), null, null, platVoucher.getId(), null
        );

        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.valueOf(30000).setScale(2, RoundingMode.HALF_UP).compareTo(response.totalPlatformDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(470000).setScale(2, RoundingMode.HALF_UP).compareTo(response.finalProductAmount()));
    }

    @Test
    @DisplayName("TC-04: previewCheckout_Success_PlatformFreeShip_ProRata")
    void previewCheckout_Success_PlatformFreeShip_ProRata() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher freeShip = new Voucher();
        freeShip.setId(UUID.randomUUID());
        freeShip.setCode("FREESHIP20K");
        freeShip.setType(VoucherType.PLATFORM);
        freeShip.setAppliesTo(VoucherAppliesTo.SHIPPING_FEE);
        freeShip.setDiscountType(DiscountType.FIXED_AMOUNT);
        freeShip.setDiscountValue(BigDecimal.valueOf(20000));
        freeShip.setMinOrderAmount(BigDecimal.ZERO);
        freeShip.setActive(true);
        freeShip.setDeleted(false);

        when(voucherRepository.findWithShopById(freeShip.getId())).thenReturn(Optional.of(freeShip));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(
                List.of(cartItemA.getId()), null, null, null, freeShip.getId()
        );

        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.valueOf(20000).setScale(2, RoundingMode.HALF_UP).compareTo(response.totalPlatformDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP).compareTo(response.finalShippingFee()));
    }

    @Test
    @DisplayName("TC-05: previewCheckout_Success_CustomAddressId_BelongsToUser")
    void previewCheckout_Success_CustomAddressId_BelongsToUser() {
        mockActiveUser();
        UUID customAddrId = UUID.randomUUID();
        Address customAddress = new Address();
        customAddress.setId(customAddrId);
        customAddress.setUser(buyer);
        customAddress.setRecipientName("Văn phòng");
        customAddress.setPhone("0987654321");
        customAddress.setStreetAddress("Tòa nhà Bitexco");
        customAddress.setWard("Bến Nghé");
        customAddress.setDistrict("Quận 1");
        customAddress.setProvince("TP. Hồ Chí Minh");
        customAddress.setDefault(false);

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(customAddrId, userId)).thenReturn(Optional.of(customAddress));
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), customAddrId, null, null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertNotNull(response.deliveryAddress());
        assertEquals("Văn phòng", response.deliveryAddress().recipientName());
        assertFalse(response.deliveryAddress().isDefault());
    }

    @Test
    @DisplayName("TC-06: previewCheckout_Success_NoDefaultAddress_ReturnsNullAddress")
    void previewCheckout_Success_NoDefaultAddress_ReturnsNullAddress() {
        mockActiveUser();
        when(addressRepository.findDefaultAddressByUserId(userId)).thenReturn(Optional.empty());
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, null, null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertNull(response.deliveryAddress());
        assertEquals(0, BigDecimal.valueOf(530000).setScale(2, RoundingMode.HALF_UP).compareTo(response.finalTotalAmount()));
    }

    @Test
    @DisplayName("TC-07: previewCheckout_ZeroNegative_CapAtZero")
    void previewCheckout_ZeroNegative_CapAtZero() {
        mockActiveUser();
        variantA.setPrice(BigDecimal.valueOf(50000));
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(10000))));

        Voucher shopVoucher = new Voucher();
        shopVoucher.setId(UUID.randomUUID());
        shopVoucher.setCode("HUGE100K");
        shopVoucher.setType(VoucherType.SHOP);
        shopVoucher.setShop(shopA);
        shopVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        shopVoucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopVoucher.setDiscountValue(BigDecimal.valueOf(100000)); // Lớn hơn tiền hàng 50k
        shopVoucher.setActive(true);
        shopVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(shopVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, List.of(shopVoucher.getId()), null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(response.finalProductAmount()));
        assertEquals(0, BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP).compareTo(response.finalTotalAmount()));
        assertTrue(response.finalTotalAmount().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    @DisplayName("TC-08: previewCheckout_RoundingDiscrepancy_AbsorbedByLargestItem")
    void previewCheckout_RoundingDiscrepancy_AbsorbedByLargestItem() {
        mockActiveUser();
        CartItem item1 = new CartItem();
        item1.setId(UUID.randomUUID());
        item1.setCart(cartA);
        item1.setProduct(productA);
        item1.setProductVariant(variantA);
        item1.setQuantity(1);

        ProductVariant var2 = new ProductVariant();
        var2.setId(UUID.randomUUID());
        var2.setPrice(BigDecimal.valueOf(500000));
        var2.setStockQuantity(50);
        var2.setStatus(ProductVariantStatus.ACTIVE);

        CartItem item2 = new CartItem();
        item2.setId(UUID.randomUUID());
        item2.setCart(cartA);
        item2.setProduct(productA);
        item2.setProductVariant(var2);
        item2.setQuantity(1);

        ProductVariant var3 = new ProductVariant();
        var3.setId(UUID.randomUUID());
        var3.setPrice(BigDecimal.valueOf(500000));
        var3.setStockQuantity(50);
        var3.setStatus(ProductVariantStatus.ACTIVE);

        CartItem item3 = new CartItem();
        item3.setId(UUID.randomUUID());
        item3.setCart(cartA);
        item3.setProduct(productA);
        item3.setProductVariant(var3);
        item3.setQuantity(1);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(item1, item2, item3));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher shopVoucher = new Voucher();
        shopVoucher.setId(UUID.randomUUID());
        shopVoucher.setCode("VOUCHER100K");
        shopVoucher.setType(VoucherType.SHOP);
        shopVoucher.setShop(shopA);
        shopVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        shopVoucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopVoucher.setDiscountValue(BigDecimal.valueOf(100000)); // 100k chia cho 3 items -> 33.333,33 mỗi item -> tổng 99.999,99 -> bù 0,01
        shopVoucher.setActive(true);
        shopVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(shopVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(
                List.of(item1.getId(), item2.getId(), item3.getId()), null, List.of(shopVoucher.getId()), null, null
        );

        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        BigDecimal sumItemDiscounts = response.shops().get(0).items().stream()
                .map(CartItemPreviewResponse::itemDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, BigDecimal.valueOf(100000).setScale(2, RoundingMode.HALF_UP).compareTo(sumItemDiscounts));
    }

    @Test
    @DisplayName("TC-09: previewCheckout_Throw_WhenCartItemIdsEmpty")
    void previewCheckout_Throw_WhenCartItemIdsEmpty() {
        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(), null, null, null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.previewCheckout(userId, request));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: previewCheckout_Throw_WhenCartItemNotFoundOrBelongsToOtherUser")
    void previewCheckout_Throw_WhenCartItemNotFoundOrBelongsToOtherUser() {
        mockActiveUser();
        UUID fakeId = UUID.randomUUID();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(fakeId), null, null, null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.previewCheckout(userId, request));
        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: previewCheckout_Throw_WhenAddressNotFoundOrBelongsToOtherUser")
    void previewCheckout_Throw_WhenAddressNotFoundOrBelongsToOtherUser() {
        mockActiveUser();
        UUID otherUserAddrId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(otherUserAddrId, userId)).thenReturn(Optional.empty());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), otherUserAddrId, null, null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.previewCheckout(userId, request));
        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: previewCheckout_Throw_WhenProductVariantOutOfStock")
    void previewCheckout_Throw_WhenProductVariantOutOfStock() {
        mockActiveUser();
        variantA.setStockQuantity(2);
        cartItemA.setQuantity(5);
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, null, null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.previewCheckout(userId, request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: previewCheckout_Throw_WhenShopPausedOrSuspended")
    void previewCheckout_Throw_WhenShopPausedOrSuspended() {
        mockActiveUser();
        shopA.setStatus(ShopStatus.PAUSED);
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, null, null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.previewCheckout(userId, request));
        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: previewCheckout_Warning_WhenVoucherMinOrderNotMet")
    void previewCheckout_Warning_WhenVoucherMinOrderNotMet() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA)); // 500k
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher shopVoucher = new Voucher();
        shopVoucher.setId(UUID.randomUUID());
        shopVoucher.setCode("APPLE1M");
        shopVoucher.setType(VoucherType.SHOP);
        shopVoucher.setShop(shopA);
        shopVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        shopVoucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopVoucher.setDiscountValue(BigDecimal.valueOf(100000));
        shopVoucher.setMinOrderAmount(BigDecimal.valueOf(1000000)); // Đơn tối thiểu 1 triệu
        shopVoucher.setActive(true);
        shopVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(shopVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, List.of(shopVoucher.getId()), null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShopDiscountAmount()));
        assertFalse(response.warnings().isEmpty());
        assertTrue(response.warnings().get(0).contains("tối thiểu thêm 500.000₫"));
    }

    @Test
    @DisplayName("TC-15: previewCheckout_Warning_WhenVoucherExpired")
    void previewCheckout_Warning_WhenVoucherExpired() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher expiredVoucher = new Voucher();
        expiredVoucher.setId(UUID.randomUUID());
        expiredVoucher.setCode("EXPIRED50");
        expiredVoucher.setType(VoucherType.SHOP);
        expiredVoucher.setShop(shopA);
        expiredVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        expiredVoucher.setEndDate(LocalDateTime.now().minusDays(1)); // Đã hết hạn hôm qua
        expiredVoucher.setActive(true);
        expiredVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(expiredVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, List.of(expiredVoucher.getId()), null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShopDiscountAmount()));
        assertTrue(response.warnings().stream().anyMatch(w -> w.contains("đã hết hạn sử dụng")));
    }

    @Test
    @DisplayName("TC-16: previewCheckout_Warning_WhenVoucherNotStartedYet")
    void previewCheckout_Warning_WhenVoucherNotStartedYet() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher futureVoucher = new Voucher();
        futureVoucher.setId(UUID.randomUUID());
        futureVoucher.setCode("FUTURE50");
        futureVoucher.setType(VoucherType.SHOP);
        futureVoucher.setShop(shopA);
        futureVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        futureVoucher.setStartDate(LocalDateTime.now().plusDays(2)); // 2 ngày nữa mới bắt đầu
        futureVoucher.setActive(true);
        futureVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(futureVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, List.of(futureVoucher.getId()), null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShopDiscountAmount()));
        assertTrue(response.warnings().stream().anyMatch(w -> w.contains("chưa đến thời gian áp dụng")));
    }

    @Test
    @DisplayName("TC-17: previewCheckout_Warning_WhenVoucherOutOfGlobalLimit_HET_SLOT")
    void previewCheckout_Warning_WhenVoucherOutOfGlobalLimit_HET_SLOT() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher outOfStockVoucher = new Voucher();
        outOfStockVoucher.setId(UUID.randomUUID());
        outOfStockVoucher.setCode("HETSLOT");
        outOfStockVoucher.setType(VoucherType.SHOP);
        outOfStockVoucher.setShop(shopA);
        outOfStockVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        outOfStockVoucher.setUsageLimit(100);
        outOfStockVoucher.setUsedCount(90);
        outOfStockVoucher.setReservedCount(10); // 90 + 10 = 100 -> Hết slot
        outOfStockVoucher.setActive(true);
        outOfStockVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(outOfStockVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, List.of(outOfStockVoucher.getId()), null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShopDiscountAmount()));
        assertTrue(response.warnings().stream().anyMatch(w -> w.contains("Hết slot")));
    }

    @Test
    @DisplayName("TC-18: previewCheckout_Warning_WhenVoucherUserLimitExceeded_HET_LUOT")
    void previewCheckout_Warning_WhenVoucherUserLimitExceeded_HET_LUOT() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher userLimitVoucher = new Voucher();
        userLimitVoucher.setId(UUID.randomUUID());
        userLimitVoucher.setCode("HETLUOT");
        userLimitVoucher.setType(VoucherType.SHOP);
        userLimitVoucher.setShop(shopA);
        userLimitVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        userLimitVoucher.setUsageLimitPerUser(1); // Chỉ cho phép 1 lượt/user
        userLimitVoucher.setActive(true);
        userLimitVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(userLimitVoucher));
        List<Object[]> userRedemptionCounts = new ArrayList<>();
        userRedemptionCounts.add(new Object[]{userLimitVoucher.getId(), 1L});
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any()))
                .thenReturn(userRedemptionCounts); // Buyer đã dùng 1 lần

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, List.of(userLimitVoucher.getId()), null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShopDiscountAmount()));
        assertTrue(response.warnings().stream().anyMatch(w -> w.contains("đã sử dụng hết số lượt cho phép")));
    }

    @Test
    @DisplayName("TC-19: previewCheckout_Warning_WhenVoucherShopMismatch")
    void previewCheckout_Warning_WhenVoucherShopMismatch() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA)); // Giỏ chỉ có Shop A
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher shopBVoucher = new Voucher();
        shopBVoucher.setId(UUID.randomUUID());
        shopBVoucher.setCode("VOUCHER_SHOP_B");
        shopBVoucher.setType(VoucherType.SHOP);
        shopBVoucher.setShop(shopB); // Voucher thuộc Shop B
        shopBVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        shopBVoucher.setActive(true);
        shopBVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(shopBVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, List.of(shopBVoucher.getId()), null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShopDiscountAmount()));
        assertTrue(response.warnings().stream().anyMatch(w -> w.contains("không thuộc gian hàng trong đơn")));
    }

    @Test
    @DisplayName("TC-20: previewCheckout_Warning_WhenVoucherDeactivatedOrDeleted")
    void previewCheckout_Warning_WhenVoucherDeactivatedOrDeleted() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher deletedVoucher = new Voucher();
        deletedVoucher.setId(UUID.randomUUID());
        deletedVoucher.setCode("DELETED_VOUCHER");
        deletedVoucher.setType(VoucherType.SHOP);
        deletedVoucher.setShop(shopA);
        deletedVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        deletedVoucher.setActive(false); // Bị tắt
        deletedVoucher.setDeleted(true);  // Bị xóa

        when(voucherRepository.findAllWithShopByIdIn(any())).thenReturn(List.of(deletedVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, List.of(deletedVoucher.getId()), null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShopDiscountAmount()));
        assertTrue(response.warnings().stream().anyMatch(w -> w.contains("không khả dụng hoặc đã bị vô hiệu hóa")));
    }

    @Test
    @DisplayName("TC-21: previewCheckout_Throw_WhenBuyerAccountBannedOrSuspended")
    void previewCheckout_Throw_WhenBuyerAccountBannedOrSuspended() {
        buyer.setStatus(UserStatus.BANNED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(buyer));

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, null, null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.previewCheckout(userId, request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-22: previewCheckout_Throw_WhenBuyerBuysFromOwnShop_SelfBuying")
    void previewCheckout_Throw_WhenBuyerBuysFromOwnShop_SelfBuying() {
        mockActiveUser();
        // Buyer chính là Seller của Shop A
        sellerUserA.setId(userId);
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, null, null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.previewCheckout(userId, request));
        assertEquals(ErrorCode.CANNOT_ADD_OWN_PRODUCT, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-23: previewCheckout_Throw_WhenVariantPriceZeroOrNegative")
    void previewCheckout_Throw_WhenVariantPriceZeroOrNegative() {
        mockActiveUser();
        variantA.setPrice(BigDecimal.ZERO); // Giá 0₫
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, null, null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.previewCheckout(userId, request));
        assertEquals(ErrorCode.INVALID_PRICE_RANGE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-24: previewCheckout_Success_FallbackShippingFee_WhenShopOptionMissing")
    void previewCheckout_Success_FallbackShippingFee_WhenShopOptionMissing() {
        mockActiveUser();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        // Shop chưa cấu hình phí ship -> trả về mảng rỗng
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of());

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(cartItemA.getId()), null, null, null, null);
        CheckoutPreviewResponse response = checkoutService.previewCheckout(userId, request);

        assertNotNull(response);
        // Tự động gán cước mặc định sàn 30.000₫
        assertEquals(0, BigDecimal.valueOf(30000).setScale(2, RoundingMode.HALF_UP).compareTo(response.totalShippingFee()));
        assertEquals(0, BigDecimal.valueOf(530000).setScale(2, RoundingMode.HALF_UP).compareTo(response.finalTotalAmount()));
    }

    // =========================================================================
    // POST /api/checkout/place-order Unit Tests (TC-01 -> TC-18)
    // =========================================================================

    private void mockAddress() {
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(defaultAddress.getId(), userId))
                .thenReturn(Optional.of(defaultAddress));
    }

    private void mockOrderPersistence() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("TC-01: placeOrder_Success_COD_SingleShop_NoVoucher")
    void placeOrder_Success_COD_SingleShop_NoVoucher() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), "Giao nhanh giúp mình"))
        );

        PlaceOrderResponse response = checkoutService.placeOrder(userId, request);

        assertNotNull(response);
        assertEquals(PaymentMethod.COD, response.paymentMethod());
        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
        assertEquals(1, response.totalOrdersCount());
        assertEquals(OrderStatus.PENDING_CONFIRMATION, response.orders().get(0).status());
        assertEquals(49, variantA.getStockQuantity()); // Tồn kho giảm từ 50 xuống 49
        assertNull(response.paymentUrl());
        verify(cartItemRepository, times(1)).deleteByCartIdAndIdIn(eq(cartA.getId()), any());
    }

    @Test
    @DisplayName("TC-02: placeOrder_Success_COD_MultiShop_WithVouchers")
    void placeOrder_Success_COD_MultiShop_WithVouchers() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();

        Product productB = new Product();
        productB.setId(UUID.randomUUID());
        productB.setName("Chuột Logitech MX Master 3S");
        productB.setShop(shopB);
        productB.setDeleted(false);
        productB.setSuspended(false);
        productB.setVisibilityStatus(ProductVisibilityStatus.PUBLISHED);
        productB.setApprovalStatus(ProductApprovalStatus.APPROVED);

        ProductVariant variantB = new ProductVariant();
        variantB.setId(UUID.randomUUID());
        variantB.setVariantName("Màu Đen");
        variantB.setPrice(BigDecimal.valueOf(1000000));
        variantB.setStockQuantity(20);
        variantB.setDeleted(false);
        variantB.setStatus(ProductVariantStatus.ACTIVE);

        CartItem cartItemB = new CartItem();
        cartItemB.setId(UUID.randomUUID());
        cartItemB.setCart(cartA);
        cartItemB.setProduct(productB);
        cartItemB.setProductVariant(variantB);
        cartItemB.setQuantity(1);

        // Shop A Voucher (Giảm 20k tiền hàng)
        Voucher shopVoucherA = new Voucher();
        shopVoucherA.setId(UUID.randomUUID());
        shopVoucherA.setCode("SHOPA20K");
        shopVoucherA.setType(VoucherType.SHOP);
        shopVoucherA.setShop(shopA);
        shopVoucherA.setAppliesTo(VoucherAppliesTo.PRODUCT);
        shopVoucherA.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopVoucherA.setDiscountValue(BigDecimal.valueOf(20000));
        shopVoucherA.setActive(true);
        shopVoucherA.setDeleted(false);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA, cartItemB));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA, variantB));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(
                createShippingOption(shopA, BigDecimal.valueOf(30000)),
                createShippingOption(shopB, BigDecimal.valueOf(25000))
        ));
        when(voucherRepository.findAllWithShopByIdInForUpdate(any())).thenReturn(List.of(shopVoucherA));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId(), cartItemB.getId()),
                List.of(
                        new ShopOrderRequest(shopA.getId(), List.of(shopVoucherA.getId()), "Shop A note"),
                        new ShopOrderRequest(shopB.getId(), List.of(), "Shop B note")
                )
        );

        PlaceOrderResponse response = checkoutService.placeOrder(userId, request);

        assertNotNull(response);
        assertEquals(2, response.totalOrdersCount());
        assertEquals(OrderStatus.PENDING_CONFIRMATION, response.orders().get(0).status());
        assertEquals(OrderStatus.PENDING_CONFIRMATION, response.orders().get(1).status());
        verify(voucherRedemptionRepository, times(1)).saveAll(any());
        assertEquals(1, shopVoucherA.getUsedCount()); // usedCount tăng 1
    }

    @Test
    @DisplayName("TC-03: placeOrder_Success_ZeroTotalAmount_AutoConfirmed")
    void placeOrder_Success_ZeroTotalAmount_AutoConfirmed() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();

        variantA.setPrice(BigDecimal.valueOf(50000)); // Giá 50k
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher shopVoucher = new Voucher();
        shopVoucher.setId(UUID.randomUUID());
        shopVoucher.setCode("FULLDISCOUNT");
        shopVoucher.setType(VoucherType.SHOP);
        shopVoucher.setShop(shopA);
        shopVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        shopVoucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopVoucher.setDiscountValue(BigDecimal.valueOf(100000)); // Giảm 100k > 50k
        shopVoucher.setActive(true);
        shopVoucher.setDeleted(false);

        Voucher freeShipVoucher = new Voucher();
        freeShipVoucher.setId(UUID.randomUUID());
        freeShipVoucher.setCode("FREESHIP");
        freeShipVoucher.setType(VoucherType.SHOP);
        freeShipVoucher.setShop(shopA);
        freeShipVoucher.setAppliesTo(VoucherAppliesTo.SHIPPING_FEE);
        freeShipVoucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        freeShipVoucher.setDiscountValue(BigDecimal.valueOf(50000)); // Giảm ship 50k > 30k
        freeShipVoucher.setActive(true);
        freeShipVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdInForUpdate(any())).thenReturn(List.of(shopVoucher, freeShipVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.VNPAY, // Khách chọn VNPAY nhưng đơn 0 đồng
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(shopVoucher.getId(), freeShipVoucher.getId()), null))
        );

        PlaceOrderResponse response = checkoutService.placeOrder(userId, request);

        assertNotNull(response);
        assertEquals(0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).compareTo(response.finalTotalAmount()));
        assertEquals(PaymentStatus.PAID, response.paymentStatus()); // Đơn 0 đồng auto PAID
        assertEquals(OrderStatus.PENDING_CONFIRMATION, response.orders().get(0).status()); // Auto PENDING_CONFIRMATION
        assertNull(response.paymentUrl()); // Không gọi cổng thanh toán
    }

    @Test
    @DisplayName("TC-04: placeOrder_Success_VNPay_PendingPayment_ReservedVoucher")
    void placeOrder_Success_VNPay_PendingPayment_ReservedVoucher() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher shopVoucher = new Voucher();
        shopVoucher.setId(UUID.randomUUID());
        shopVoucher.setCode("VNPAY20K");
        shopVoucher.setType(VoucherType.SHOP);
        shopVoucher.setShop(shopA);
        shopVoucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        shopVoucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopVoucher.setDiscountValue(BigDecimal.valueOf(20000));
        shopVoucher.setActive(true);
        shopVoucher.setDeleted(false);

        when(voucherRepository.findAllWithShopByIdInForUpdate(any())).thenReturn(List.of(shopVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());
        when(vnPayService.createPaymentUrl(any(), any(), any())).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?mock_vnpay=123");

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.VNPAY,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(shopVoucher.getId()), null))
        );

        PlaceOrderResponse response = checkoutService.placeOrder(userId, request);

        assertNotNull(response);
        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.orders().get(0).status());
        assertEquals(1, shopVoucher.getReservedCount()); // reservedCount tăng 1
        assertNotNull(response.paymentUrl());
        assertTrue(response.paymentUrl().contains("vnpay"));
    }

    @Test
    @DisplayName("TC-04B: placeOrder_Success_VNPay_WithSpecificBankCode")
    void placeOrder_Success_VNPay_WithSpecificBankCode() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));
        when(vnPayService.createPaymentUrl(any(), eq("VNPAYQR"), any())).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_BankCode=VNPAYQR");

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.VNPAY,
                "VNPAYQR",
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null)),
                null,
                null,
                null
        );

        PlaceOrderResponse response = checkoutService.placeOrder(userId, request);

        assertNotNull(response);
        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
        assertEquals("VNPAYQR", response.bankCode());
        assertNotNull(response.paymentUrl());
        assertTrue(response.paymentUrl().contains("VNPAYQR"));
    }

    @Test
    @DisplayName("TC-04C: placeOrder_Throw_WhenInvalidBankCode")
    void placeOrder_Throw_WhenInvalidBankCode() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.VNPAY,
                "INVALID_BANK",
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null)),
                null,
                null,
                null
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.INVALID_BANK_CODE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-05: placeOrder_Throw_WhenInsufficientStock")
    void placeOrder_Throw_WhenInsufficientStock() {
        mockActiveUser();
        mockAddress();
        variantA.setStockQuantity(1); // Tồn kho chỉ còn 1
        cartItemA.setQuantity(2);     // Khách đặt mua 2
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-06: placeOrder_Throw_WhenVoucherExpired")
    void placeOrder_Throw_WhenVoucherExpired() {
        mockActiveUser();
        mockAddress();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher expiredVoucher = new Voucher();
        expiredVoucher.setId(UUID.randomUUID());
        expiredVoucher.setCode("EXPIRED10K");
        expiredVoucher.setType(VoucherType.SHOP);
        expiredVoucher.setShop(shopA);
        expiredVoucher.setActive(true);
        expiredVoucher.setDeleted(false);
        expiredVoucher.setEndDate(LocalDateTime.now().minusDays(1)); // Đã hết hạn

        when(voucherRepository.findAllWithShopByIdInForUpdate(any())).thenReturn(List.of(expiredVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(expiredVoucher.getId()), null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.VOUCHER_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-07: placeOrder_Throw_WhenVoucherOutOfSlots")
    void placeOrder_Throw_WhenVoucherOutOfSlots() {
        mockActiveUser();
        mockAddress();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher outOfSlotVoucher = new Voucher();
        outOfSlotVoucher.setId(UUID.randomUUID());
        outOfSlotVoucher.setCode("OUTOFSLOT");
        outOfSlotVoucher.setType(VoucherType.SHOP);
        outOfSlotVoucher.setShop(shopA);
        outOfSlotVoucher.setActive(true);
        outOfSlotVoucher.setDeleted(false);
        outOfSlotVoucher.setUsageLimit(10);
        outOfSlotVoucher.setUsedCount(10); // Hết slot (usedCount = usageLimit)

        when(voucherRepository.findAllWithShopByIdInForUpdate(any())).thenReturn(List.of(outOfSlotVoucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(outOfSlotVoucher.getId()), null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.VOUCHER_OUT_OF_STOCK, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-08: placeOrder_Throw_WhenVoucherUserLimitExceeded")
    void placeOrder_Throw_WhenVoucherUserLimitExceeded() {
        mockActiveUser();
        mockAddress();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        Voucher voucher = new Voucher();
        voucher.setId(UUID.randomUUID());
        voucher.setCode("1USEPERUSER");
        voucher.setType(VoucherType.SHOP);
        voucher.setShop(shopA);
        voucher.setActive(true);
        voucher.setDeleted(false);
        voucher.setUsageLimitPerUser(1);

        when(voucherRepository.findAllWithShopByIdInForUpdate(any())).thenReturn(List.of(voucher));
        // Buyer đã dùng 1 lần trong voucher_redemptions
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{voucher.getId(), 1L}));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(voucher.getId()), null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.VOUCHER_USAGE_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-09: placeOrder_Throw_WhenSelfBuyingOwnProduct")
    void placeOrder_Throw_WhenSelfBuyingOwnProduct() {
        mockActiveUser();
        mockAddress();
        sellerUserA.setId(userId); // Buyer chính là Seller của Shop A
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.CANNOT_ADD_OWN_PRODUCT, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: placeOrder_Throw_WhenAddressNotFoundOrIDOR")
    void placeOrder_Throw_WhenAddressNotFoundOrIDOR() {
        mockActiveUser();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(any(), any())).thenReturn(Optional.empty());

        PlaceOrderRequest request = new PlaceOrderRequest(
                UUID.randomUUID(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: placeOrder_Throw_WhenCartItemNotFoundOrIDOR")
    void placeOrder_Throw_WhenCartItemNotFoundOrIDOR() {
        mockActiveUser();
        mockAddress();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of()); // Không thấy items

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: placeOrder_Throw_WhenShopPausedOrBanned")
    void placeOrder_Throw_WhenShopPausedOrBanned() {
        mockActiveUser();
        mockAddress();
        shopA.setStatus(ShopStatus.PAUSED);
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: placeOrder_Concurrency_PessimisticLock_NoOverselling")
    void placeOrder_Concurrency_PessimisticLock_NoOverselling() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();
        variantA.setStockQuantity(1); // Tồn kho duy nhất = 1
        cartItemA.setQuantity(1);
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        PlaceOrderResponse response = checkoutService.placeOrder(userId, request);

        assertNotNull(response);
        assertEquals(0, variantA.getStockQuantity()); // Trừ kho về đúng 0, không bị âm
    }

    @Test
    @DisplayName("TC-14: placeOrder_Concurrency_DeadlockFree_SortedVariantIds")
    void placeOrder_Concurrency_DeadlockFree_SortedVariantIds() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();

        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        ProductVariant v1 = new ProductVariant();
        v1.setId(id1);
        v1.setVariantName("V1");
        v1.setPrice(BigDecimal.valueOf(100000));
        v1.setStockQuantity(10);
        v1.setStatus(ProductVariantStatus.ACTIVE);

        ProductVariant v2 = new ProductVariant();
        v2.setId(id2);
        v2.setVariantName("V2");
        v2.setPrice(BigDecimal.valueOf(200000));
        v2.setStockQuantity(10);
        v2.setStatus(ProductVariantStatus.ACTIVE);

        CartItem item1 = new CartItem();
        item1.setId(UUID.randomUUID());
        item1.setCart(cartA);
        item1.setProduct(productA);
        item1.setProductVariant(v1);
        item1.setQuantity(1);

        CartItem item2 = new CartItem();
        item2.setId(UUID.randomUUID());
        item2.setCart(cartA);
        item2.setProduct(productA);
        item2.setProductVariant(v2);
        item2.setQuantity(1);

        // Gửi items theo thứ tự ngược: item2 rồi mới đến item1
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(item2, item1));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(v1, v2));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(item2.getId(), item1.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        checkoutService.placeOrder(userId, request);

        // Verify rằng list ID truyền vào findAllByIdInForUpdate phải được sắp xếp tăng dần id1 < id2
        org.mockito.ArgumentCaptor<List<UUID>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(productVariantRepository).findAllByIdInForUpdate(captor.capture());
        List<UUID> capturedIds = captor.getValue();
        assertEquals(id1, capturedIds.get(0));
        assertEquals(id2, capturedIds.get(1));
    }

    @Test
    @DisplayName("TC-15: placeOrder_CartCleanup_OnlySelectedItemsDeleted")
    void placeOrder_CartCleanup_OnlySelectedItemsDeleted() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        checkoutService.placeOrder(userId, request);

        // Chỉ xóa cartItemA đã chọn, không xóa các item khác
        verify(cartItemRepository, times(1)).deleteByCartIdAndIdIn(eq(cartA.getId()), eq(List.of(cartItemA.getId())));
    }

    @Test
    @DisplayName("TC-16: placeOrder_SnapshotIntegrity_OrderItemsAndAddress")
    void placeOrder_SnapshotIntegrity_OrderItemsAndAddress() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();
        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(cartItemA));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(variantA));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        checkoutService.placeOrder(userId, request);

        org.mockito.ArgumentCaptor<Order> orderCaptor = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertNotNull(savedOrder.getDeliveryAddressSnapshot());
        assertTrue(savedOrder.getDeliveryAddressSnapshot().contains("123 Lê Duẩn"));
        assertTrue(savedOrder.getDeliveryAddressSnapshot().contains("Nguyễn Văn A"));

        org.mockito.ArgumentCaptor<List<OrderItem>> itemsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(itemsCaptor.capture());
        OrderItem savedItem = itemsCaptor.getValue().get(0);

        assertEquals("Củ sạc Apple 20W", savedItem.getProductNameSnapshot());
        assertEquals(0, BigDecimal.valueOf(500000).setScale(2, RoundingMode.HALF_UP).compareTo(savedItem.getOriginalPrice()));
    }

    @Test
    @DisplayName("TC-17: placeOrder_ProRataRoundingDiscrepancy_AbsorbedByLargestItem")
    void placeOrder_ProRataRoundingDiscrepancy_AbsorbedByLargestItem() {
        mockActiveUser();
        mockAddress();
        mockOrderPersistence();

        ProductVariant v1 = new ProductVariant();
        v1.setId(UUID.randomUUID());
        v1.setVariantName("V1");
        v1.setPrice(BigDecimal.valueOf(100000));
        v1.setStockQuantity(10);
        v1.setStatus(ProductVariantStatus.ACTIVE);

        ProductVariant v2 = new ProductVariant();
        v2.setId(UUID.randomUUID());
        v2.setVariantName("V2");
        v2.setPrice(BigDecimal.valueOf(100000));
        v2.setStockQuantity(10);
        v2.setStatus(ProductVariantStatus.ACTIVE);

        ProductVariant v3 = new ProductVariant();
        v3.setId(UUID.randomUUID());
        v3.setVariantName("V3");
        v3.setPrice(BigDecimal.valueOf(100000));
        v3.setStockQuantity(10);
        v3.setStatus(ProductVariantStatus.ACTIVE);

        CartItem item1 = new CartItem();
        item1.setId(UUID.randomUUID());
        item1.setCart(cartA);
        item1.setProduct(productA);
        item1.setProductVariant(v1);
        item1.setQuantity(1);

        CartItem item2 = new CartItem();
        item2.setId(UUID.randomUUID());
        item2.setCart(cartA);
        item2.setProduct(productA);
        item2.setProductVariant(v2);
        item2.setQuantity(1);

        CartItem item3 = new CartItem();
        item3.setId(UUID.randomUUID());
        item3.setCart(cartA);
        item3.setProduct(productA);
        item3.setProductVariant(v3);
        item3.setQuantity(1);

        // Voucher giảm 100k cho đơn 300k (100k chia 3 = 33.333,33₫ mỗi item)
        Voucher voucher = new Voucher();
        voucher.setId(UUID.randomUUID());
        voucher.setCode("DISCOUNT100K");
        voucher.setType(VoucherType.SHOP);
        voucher.setShop(shopA);
        voucher.setAppliesTo(VoucherAppliesTo.PRODUCT);
        voucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        voucher.setDiscountValue(BigDecimal.valueOf(100000));
        voucher.setActive(true);
        voucher.setDeleted(false);

        when(cartItemRepository.findByUserIdAndIdInWithDetails(eq(userId), any())).thenReturn(List.of(item1, item2, item3));
        when(productVariantRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(v1, v2, v3));
        when(shippingOptionRepository.findByShopIdIn(any())).thenReturn(List.of(createShippingOption(shopA, BigDecimal.valueOf(30000))));
        when(voucherRepository.findAllWithShopByIdInForUpdate(any())).thenReturn(List.of(voucher));
        when(voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(any(), any(), any())).thenReturn(List.of());

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(item1.getId(), item2.getId(), item3.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(voucher.getId()), null))
        );

        checkoutService.placeOrder(userId, request);

        org.mockito.ArgumentCaptor<List<OrderItem>> itemsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(itemsCaptor.capture());
        List<OrderItem> savedItems = itemsCaptor.getValue();

        BigDecimal sumDiscount = savedItems.stream()
                .map(OrderItem::getItemDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tổng chiết khấu của 3 items đúng chính xác bằng 100.000,00₫ (phần dư làm tròn 1 xu được hấp thụ)
        assertEquals(0, BigDecimal.valueOf(100000).setScale(2, RoundingMode.HALF_UP).compareTo(sumDiscount));
    }

    @Test
    @DisplayName("TC-18: placeOrder_Throw_WhenUserBannedOrSuspended")
    void placeOrder_Throw_WhenUserBannedOrSuspended() {
        buyer.setStatus(UserStatus.BANNED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(buyer));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> checkoutService.placeOrder(userId, request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-19: placeOrder_Idempotency_ReturnExistingOrderWhenDuplicateRequest")
    void placeOrder_Idempotency_ReturnExistingOrderWhenDuplicateRequest() {
        String idemKey = "idem-test-key-12345";
        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.valueOf(530000))
                .transactionId(idemKey)
                .createdAt(LocalDateTime.now())
                .build();

        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-20261015-EXISTING")
                .shop(shopA)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(530000))
                .shippingFee(BigDecimal.valueOf(30000))
                .items(List.of())
                .build();

        when(paymentRepository.findByTransactionId(idemKey)).thenReturn(Optional.of(existingPayment));
        when(orderRepository.findByPaymentId(existingPayment.getId())).thenReturn(List.of(existingOrder));

        PlaceOrderRequest request = new PlaceOrderRequest(
                defaultAddress.getId(),
                PaymentMethod.COD,
                List.of(cartItemA.getId()),
                List.of(new ShopOrderRequest(shopA.getId(), List.of(), null)),
                null,
                null,
                idemKey
        );

        PlaceOrderResponse response = checkoutService.placeOrder(userId, request);

        assertNotNull(response);
        assertEquals(existingPayment.getId(), response.paymentId());
        assertEquals(1, response.totalOrdersCount());
        assertEquals("ORD-20261015-EXISTING", response.orders().get(0).orderNumber());
        // Không tạo thêm đơn, không trừ thêm kho
        verify(productVariantRepository, never()).findAllByIdInForUpdate(any());
    }
}

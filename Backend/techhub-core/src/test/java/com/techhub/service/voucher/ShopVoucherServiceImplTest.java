package com.techhub.service.voucher;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.CartShopVoucherResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.*;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.repository.VoucherRepository;
import com.techhub.service.voucher.impl.ShopVoucherServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ShopVoucherServiceImpl - Chọn xem Voucher Shop trong Giỏ Hàng & Checkout (GET /api/shops/{sellerName}/vouchers)")
class ShopVoucherServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private ShopVoucherServiceImpl shopVoucherService;

    private User sampleUser;
    private Seller sampleSeller;
    private Shop sampleShop;
    private Voucher eligibleVoucher;
    private Voucher ineligibleVoucher;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setStatus(UserStatus.ACTIVE);

        sampleSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("laptop-hadong")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        sampleShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(sampleSeller)
                .name("Laptop Hà Đông")
                .status(ShopStatus.ACTIVE)
                .build();

        eligibleVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(sampleShop)
                .code("LAPTOP50K")
                .name("Giảm 50.000₫ đơn từ 200k")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .maxDiscountAmount(null)
                .usageLimit(500)
                .usedCount(10)
                .reservedCount(0)
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        ineligibleVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(sampleShop)
                .code("VIP1000K")
                .name("Giảm 1.000.000₫ đơn từ 50 triệu")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(1000000))
                .minOrderAmount(BigDecimal.valueOf(50000000))
                .maxDiscountAmount(null)
                .usageLimit(100)
                .usedCount(5)
                .reservedCount(0)
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("TC-01: Lấy voucher thành công theo slug, phân loại chính xác Đủ điều kiện và Chưa đủ điều kiện kèm upsell")
    void getShopVouchers_Success_EligibleAndIneligible() {
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(voucherRepository.findAvailableShopVouchers(eq(sampleShop.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(eligibleVoucher, ineligibleVoucher));

        BigDecimal orderAmount = BigDecimal.valueOf(45600000); // 45.6 triệu
        List<CartShopVoucherResponse> results = shopVoucherService.getShopVouchersForCart("laptop-hadong", orderAmount);

        assertEquals(2, results.size());

        // Voucher 1: Eligible
        CartShopVoucherResponse res1 = results.get(0);
        assertEquals("LAPTOP50K", res1.code());
        assertTrue(res1.isEligible());
        assertEquals(BigDecimal.valueOf(50000), res1.estimatedDiscount());
        assertEquals(BigDecimal.ZERO, res1.upsellRemainingAmount());
        assertNull(res1.ineligibilityReason());

        // Voucher 2: Ineligible
        CartShopVoucherResponse res2 = results.get(1);
        assertEquals("VIP1000K", res2.code());
        assertFalse(res2.isEligible());
        assertEquals(BigDecimal.ZERO, res2.estimatedDiscount());
        assertEquals(BigDecimal.valueOf(4400000), res2.upsellRemainingAmount()); // 50tr - 45.6tr = 4.4tr
        assertNotNull(res2.ineligibilityReason());
        assertTrue(res2.ineligibilityReason().contains("Mua thêm 4.400.000₫ để dùng mã này"));
    }

    @Test
    @DisplayName("TC-02: Lấy voucher thành công bằng Shop UUID trực tiếp (Cart/Checkout Integration)")
    void getShopVouchers_Success_ByShopIdUUID() {
        UUID shopId = sampleShop.getId();
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(sampleShop));
        when(voucherRepository.findAvailableShopVouchers(eq(shopId), any(LocalDateTime.class)))
                .thenReturn(List.of(eligibleVoucher));

        List<CartShopVoucherResponse> results = shopVoucherService.getShopVouchersForCart(
                shopId.toString(), BigDecimal.valueOf(500000));

        assertEquals(1, results.size());
        assertEquals("LAPTOP50K", results.get(0).code());
        assertTrue(results.get(0).isEligible());
        verify(shopRepository, times(1)).findById(shopId);
        verify(sellerRepository, never()).findBySellerName(any());
    }

    @Test
    @DisplayName("TC-03: Không truyền orderAmount -> Trả về danh sách đầy đủ với các cờ đánh giá là null")
    void getShopVouchers_WithoutOrderAmount() {
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(voucherRepository.findAvailableShopVouchers(eq(sampleShop.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(eligibleVoucher));

        List<CartShopVoucherResponse> results = shopVoucherService.getShopVouchersForCart("laptop-hadong", null);

        assertEquals(1, results.size());
        CartShopVoucherResponse res = results.get(0);
        assertNull(res.isEligible());
        assertNull(res.estimatedDiscount());
        assertNull(res.upsellRemainingAmount());
        assertNull(res.ineligibilityReason());
    }

    @Test
    @DisplayName("TC-04: BR-07 - Shop đang PAUSED -> Trả về mảng rỗng [] (200 OK)")
    void getShopVouchers_ShopPaused_ReturnsEmptyList() {
        sampleShop.setStatus(ShopStatus.PAUSED);
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));

        List<CartShopVoucherResponse> results = shopVoucherService.getShopVouchersForCart(
                "laptop-hadong", BigDecimal.valueOf(500000));

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(voucherRepository, never()).findAvailableShopVouchers(any(), any());
    }

    @Test
    @DisplayName("TC-05: BR-07 - Shop bị SUSPENDED -> Ném SHOP_NOT_FOUND (404)")
    void getShopVouchers_ShopSuspended_Returns404() {
        sampleShop.setStatus(ShopStatus.SUSPENDED);
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopVoucherService.getShopVouchersForCart("laptop-hadong", BigDecimal.valueOf(500000)));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).findAvailableShopVouchers(any(), any());
    }

    @Test
    @DisplayName("TC-06: BR-07 - Shop bị BANNED -> Ném SHOP_NOT_FOUND (404)")
    void getShopVouchers_ShopBanned_Returns404() {
        sampleShop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopVoucherService.getShopVouchersForCart("laptop-hadong", BigDecimal.valueOf(500000)));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).findAvailableShopVouchers(any(), any());
    }

    @Test
    @DisplayName("TC-07: Slug không tồn tại trong hệ thống -> Ném SHOP_NOT_FOUND (404)")
    void getShopVouchers_ShopNotFound_Returns404() {
        when(sellerRepository.findBySellerName("unknown-slug")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopVoucherService.getShopVouchersForCart("unknown-slug", null));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-08: Seller chưa được phê duyệt (verificationStatus != APPROVED) -> Ném SHOP_NOT_FOUND (404)")
    void getShopVouchers_SellerNotApproved_Returns404() {
        sampleSeller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopVoucherService.getShopVouchersForCart("laptop-hadong", null));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-09: Tài khoản User của Seller bị BANNED -> Ném SHOP_NOT_FOUND (404)")
    void getShopVouchers_UserBanned_Returns404() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopVoucherService.getShopVouchersForCart("laptop-hadong", null));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: Tài khoản User của Seller bị DELETED -> Ném SHOP_NOT_FOUND (404)")
    void getShopVouchers_UserDeleted_Returns404() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopVoucherService.getShopVouchersForCart("laptop-hadong", null));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: Slug không hợp lệ (quá ngắn, ký tự đặc biệt) -> Ném SHOP_NOT_FOUND (404)")
    void getShopVouchers_InvalidSlugPattern_Returns404() {
        assertThrows(BusinessException.class, () ->
                shopVoucherService.getShopVouchersForCart("ab", null));

        assertThrows(BusinessException.class, () ->
                shopVoucherService.getShopVouchersForCart("shop@name", null));

        assertThrows(BusinessException.class, () ->
                shopVoucherService.getShopVouchersForCart(null, null));
    }

    @Test
    @DisplayName("TC-12: orderAmount truyền số âm -> Chuẩn hóa an toàn về ZERO, đánh giá không đủ điều kiện")
    void getShopVouchers_NegativeOrderAmount_NormalizedToZero() {
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(voucherRepository.findAvailableShopVouchers(eq(sampleShop.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(eligibleVoucher));

        List<CartShopVoucherResponse> results = shopVoucherService.getShopVouchersForCart(
                "laptop-hadong", BigDecimal.valueOf(-50000));

        assertEquals(1, results.size());
        CartShopVoucherResponse res = results.get(0);
        assertFalse(res.isEligible());
        assertEquals(BigDecimal.ZERO, res.estimatedDiscount());
        assertEquals(BigDecimal.valueOf(200000), res.upsellRemainingAmount());
    }

    @Test
    @DisplayName("TC-13: Voucher giảm theo % có trần maxDiscountAmount -> Giảm đúng mức trần và không vượt quá đơn hàng")
    void getShopVouchers_PercentageWithMaxCap() {
        Voucher percentageVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(sampleShop)
                .code("SALE10PERCENT")
                .name("Giảm 10% tối đa 500k")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .minOrderAmount(BigDecimal.valueOf(1000000))
                .maxDiscountAmount(BigDecimal.valueOf(500000))
                .usageLimit(1000)
                .usedCount(10)
                .reservedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(voucherRepository.findAvailableShopVouchers(eq(sampleShop.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(percentageVoucher));

        // Đơn hàng 20 triệu: 10% = 2 triệu, nhưng maxDiscountAmount = 500k -> giảm 500k
        BigDecimal orderAmount = BigDecimal.valueOf(20000000);
        List<CartShopVoucherResponse> results = shopVoucherService.getShopVouchersForCart("laptop-hadong", orderAmount);

        assertEquals(1, results.size());
        CartShopVoucherResponse res = results.get(0);
        assertTrue(res.isEligible());
        assertEquals(BigDecimal.valueOf(500000), res.estimatedDiscount());
    }

    @Test
    @DisplayName("TC-14: Shop ACTIVE nhưng không có voucher nào khả dụng -> Trả về danh sách rỗng [] (200 OK)")
    void getShopVouchers_EmptyResult() {
        when(sellerRepository.findBySellerName("laptop-hadong")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(voucherRepository.findAvailableShopVouchers(eq(sampleShop.getId()), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<CartShopVoucherResponse> results = shopVoucherService.getShopVouchersForCart(
                "laptop-hadong", BigDecimal.valueOf(1000000));

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("TC-15: Chuỗi có dấu gạch ngang nhưng không phải 36 ký tự (VD: a-b-c-d-e) -> Xử lý như slug, không bị nhầm thành UUID")
    void getShopVouchers_SlugWithHexHyphenPattern_TreatedAsSlugNotUUID() {
        when(sellerRepository.findBySellerName("a-b-c-d-e")).thenReturn(Optional.of(sampleSeller));
        when(shopRepository.findBySellerId(sampleSeller.getId())).thenReturn(Optional.of(sampleShop));
        when(voucherRepository.findAvailableShopVouchers(eq(sampleShop.getId()), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<CartShopVoucherResponse> results = shopVoucherService.getShopVouchersForCart(
                "a-b-c-d-e", BigDecimal.valueOf(1000000));

        assertNotNull(results);
        verify(sellerRepository, times(1)).findBySellerName("a-b-c-d-e");
        verify(shopRepository, never()).findById(any());
    }
}

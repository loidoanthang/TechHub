package com.techhub.service.voucher;

import com.techhub.model.dto.response.CartPlatformVoucherResponse;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherType;
import com.techhub.repository.VoucherRepository;
import com.techhub.service.voucher.impl.BuyerVoucherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho BuyerVoucherServiceImpl - Chọn xem Voucher Toàn Sàn trong Giỏ Hàng & Checkout (GET /api/vouchers/platform)")
class BuyerVoucherServiceImplTest {

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private BuyerVoucherServiceImpl buyerVoucherService;

    private Voucher freeshipVoucher;
    private Voucher productVoucher;
    private Voucher megaVoucher;

    @BeforeEach
    void setUp() {
        freeshipVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(null)
                .code("FREESHIP30K")
                .name("Miễn phí vận chuyển tối đa 30k")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.SHIPPING_FEE)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(30000))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .maxDiscountAmount(null)
                .usageLimit(1000)
                .usedCount(50)
                .reservedCount(0)
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        productVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(null)
                .code("TECHHUB40K")
                .name("Giảm 40k đơn từ 500k toàn sàn")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(40000))
                .minOrderAmount(BigDecimal.valueOf(500000))
                .maxDiscountAmount(null)
                .usageLimit(500)
                .usedCount(10)
                .reservedCount(0)
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        megaVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(null)
                .code("TECHHUBMEGA1M")
                .name("Giảm 1 triệu đơn từ 50 triệu")
                .type(VoucherType.PLATFORM)
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
    @DisplayName("TC-01: Lấy danh sách voucher toàn sàn thành công, phân loại Đủ điều kiện và Chưa đủ điều kiện kèm upsell")
    void getPlatformVouchers_Success_EligibleAndIneligible() {
        when(voucherRepository.findAvailablePlatformVouchers(eq(null), any(LocalDateTime.class)))
                .thenReturn(List.of(freeshipVoucher, productVoucher, megaVoucher));

        BigDecimal orderAmount = BigDecimal.valueOf(45600000); // 45.6 triệu
        BigDecimal shippingFee = BigDecimal.valueOf(30000);    // 30k

        List<CartPlatformVoucherResponse> results = buyerVoucherService.getPlatformVouchersForCart(
                orderAmount, shippingFee, null);

        assertEquals(3, results.size());

        // Voucher 1: FreeShip - Eligible
        CartPlatformVoucherResponse res1 = results.get(0);
        assertEquals("FREESHIP30K", res1.code());
        assertEquals(VoucherAppliesTo.SHIPPING_FEE, res1.appliesTo());
        assertTrue(res1.isEligible());
        assertEquals(BigDecimal.valueOf(30000), res1.estimatedDiscount());
        assertEquals(BigDecimal.ZERO, res1.upsellRemainingAmount());
        assertNull(res1.ineligibilityReason());

        // Voucher 2: Product discount - Eligible
        CartPlatformVoucherResponse res2 = results.get(1);
        assertEquals("TECHHUB40K", res2.code());
        assertEquals(VoucherAppliesTo.PRODUCT, res2.appliesTo());
        assertTrue(res2.isEligible());
        assertEquals(BigDecimal.valueOf(40000), res2.estimatedDiscount());
        assertEquals(BigDecimal.ZERO, res2.upsellRemainingAmount());
        assertNull(res2.ineligibilityReason());

        // Voucher 3: Mega - Ineligible (50tr > 45.6tr)
        CartPlatformVoucherResponse res3 = results.get(2);
        assertEquals("TECHHUBMEGA1M", res3.code());
        assertFalse(res3.isEligible());
        assertEquals(BigDecimal.ZERO, res3.estimatedDiscount());
        assertEquals(BigDecimal.valueOf(4400000), res3.upsellRemainingAmount());
        assertNotNull(res3.ineligibilityReason());
        assertTrue(res3.ineligibilityReason().contains("Mua thêm 4.400.000₫ để dùng mã này"));
    }

    @Test
    @DisplayName("TC-02: Lọc theo appliesTo = PRODUCT -> Chỉ trả về voucher giảm tiền hàng")
    void getPlatformVouchers_FilterByAppliesTo_Product() {
        when(voucherRepository.findAvailablePlatformVouchers(eq(VoucherAppliesTo.PRODUCT), any(LocalDateTime.class)))
                .thenReturn(List.of(productVoucher));

        List<CartPlatformVoucherResponse> results = buyerVoucherService.getPlatformVouchersForCart(
                BigDecimal.valueOf(1000000), null, VoucherAppliesTo.PRODUCT);

        assertEquals(1, results.size());
        assertEquals("TECHHUB40K", results.get(0).code());
        assertEquals(VoucherAppliesTo.PRODUCT, results.get(0).appliesTo());
        verify(voucherRepository, times(1)).findAvailablePlatformVouchers(eq(VoucherAppliesTo.PRODUCT), any());
    }

    @Test
    @DisplayName("TC-03: Lọc theo appliesTo = SHIPPING_FEE -> Chỉ trả về voucher freeship")
    void getPlatformVouchers_FilterByAppliesTo_ShippingFee() {
        when(voucherRepository.findAvailablePlatformVouchers(eq(VoucherAppliesTo.SHIPPING_FEE), any(LocalDateTime.class)))
                .thenReturn(List.of(freeshipVoucher));

        List<CartPlatformVoucherResponse> results = buyerVoucherService.getPlatformVouchersForCart(
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(30000), VoucherAppliesTo.SHIPPING_FEE);

        assertEquals(1, results.size());
        assertEquals("FREESHIP30K", results.get(0).code());
        assertEquals(VoucherAppliesTo.SHIPPING_FEE, results.get(0).appliesTo());
        verify(voucherRepository, times(1)).findAvailablePlatformVouchers(eq(VoucherAppliesTo.SHIPPING_FEE), any());
    }

    @Test
    @DisplayName("TC-04: Không truyền orderAmount -> Trả về danh sách với các cờ đánh giá mang giá trị null")
    void getPlatformVouchers_WithoutOrderAmount() {
        when(voucherRepository.findAvailablePlatformVouchers(eq(null), any(LocalDateTime.class)))
                .thenReturn(List.of(freeshipVoucher, productVoucher));

        List<CartPlatformVoucherResponse> results = buyerVoucherService.getPlatformVouchersForCart(
                null, null, null);

        assertEquals(2, results.size());
        assertNull(results.get(0).isEligible());
        assertNull(results.get(0).estimatedDiscount());
        assertNull(results.get(0).upsellRemainingAmount());
        assertNull(results.get(0).ineligibilityReason());
    }

    @Test
    @DisplayName("TC-05: BR-14 - Phí ship thực tế thấp hơn mức giảm tối đa của mã -> Chặn trần đúng bằng phí ship")
    void getPlatformVouchers_ShippingFeeCapped() {
        when(voucherRepository.findAvailablePlatformVouchers(eq(null), any(LocalDateTime.class)))
                .thenReturn(List.of(freeshipVoucher));

        // Phí ship thực tế 20k, mã giảm tối đa 30k -> giảm 20k
        BigDecimal shippingFee = BigDecimal.valueOf(20000);
        BigDecimal orderAmount = BigDecimal.valueOf(500000);

        List<CartPlatformVoucherResponse> results = buyerVoucherService.getPlatformVouchersForCart(
                orderAmount, shippingFee, null);

        assertEquals(1, results.size());
        CartPlatformVoucherResponse res = results.get(0);
        assertTrue(res.isEligible());
        assertEquals(BigDecimal.valueOf(20000), res.estimatedDiscount());
    }

    @Test
    @DisplayName("TC-06: Voucher giảm % có trần maxDiscountAmount -> Chặn đúng trần trần tối đa")
    void getPlatformVouchers_PercentageWithMaxCap() {
        Voucher percentageVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(null)
                .code("MEGA10PERCENT")
                .name("Giảm 10% tối đa 100k")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .minOrderAmount(BigDecimal.valueOf(500000))
                .maxDiscountAmount(BigDecimal.valueOf(100000))
                .usageLimit(1000)
                .usedCount(5)
                .reservedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findAvailablePlatformVouchers(eq(null), any(LocalDateTime.class)))
                .thenReturn(List.of(percentageVoucher));

        // Đơn hàng 5 triệu: 10% = 500k, nhưng trần 100k -> giảm 100k
        BigDecimal orderAmount = BigDecimal.valueOf(5000000);
        List<CartPlatformVoucherResponse> results = buyerVoucherService.getPlatformVouchersForCart(
                orderAmount, null, null);

        assertEquals(1, results.size());
        CartPlatformVoucherResponse res = results.get(0);
        assertTrue(res.isEligible());
        assertEquals(BigDecimal.valueOf(100000), res.estimatedDiscount());
    }

    @Test
    @DisplayName("TC-07: orderAmount truyền số âm -> Chuẩn hóa an toàn về ZERO, đánh giá không đủ điều kiện")
    void getPlatformVouchers_NegativeOrderAmount_NormalizedToZero() {
        when(voucherRepository.findAvailablePlatformVouchers(eq(null), any(LocalDateTime.class)))
                .thenReturn(List.of(productVoucher));

        List<CartPlatformVoucherResponse> results = buyerVoucherService.getPlatformVouchersForCart(
                BigDecimal.valueOf(-50000), null, null);

        assertEquals(1, results.size());
        CartPlatformVoucherResponse res = results.get(0);
        assertFalse(res.isEligible());
        assertEquals(BigDecimal.ZERO, res.estimatedDiscount());
        assertEquals(BigDecimal.valueOf(500000), res.upsellRemainingAmount());
    }

    @Test
    @DisplayName("TC-08: Sàn không có voucher khả dụng -> Trả về mảng rỗng [] (200 OK)")
    void getPlatformVouchers_EmptyResult() {
        when(voucherRepository.findAvailablePlatformVouchers(eq(null), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<CartPlatformVoucherResponse> results = buyerVoucherService.getPlatformVouchersForCart(
                BigDecimal.valueOf(1000000), null, null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("TC-09: Voucher freeship giảm theo % có trần maxDiscountAmount -> Tính theo phí ship")
    void getPlatformVouchers_PercentageShippingFee() {
        Voucher percentageShippingVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(null)
                .code("SHIP50PERCENT")
                .name("Giảm 50% phí ship tối đa 25k")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.SHIPPING_FEE)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(50))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .maxDiscountAmount(BigDecimal.valueOf(25000))
                .usageLimit(1000)
                .usedCount(5)
                .reservedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findAvailablePlatformVouchers(eq(null), any(LocalDateTime.class)))
                .thenReturn(List.of(percentageShippingVoucher));

        // Phí ship 40k: 50% = 20k <= maxDiscount 25k -> giảm 20k
        List<CartPlatformVoucherResponse> results = buyerVoucherService.getPlatformVouchersForCart(
                BigDecimal.valueOf(300000), BigDecimal.valueOf(40000), null);

        assertEquals(1, results.size());
        CartPlatformVoucherResponse res = results.get(0);
        assertTrue(res.isEligible());
        assertEquals(new BigDecimal("20000.00"), res.estimatedDiscount());
    }
}

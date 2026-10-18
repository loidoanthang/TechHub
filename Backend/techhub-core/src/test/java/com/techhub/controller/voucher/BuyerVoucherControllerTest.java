package com.techhub.controller.voucher;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.CartPlatformVoucherResponse;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherType;
import com.techhub.service.voucher.BuyerVoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho BuyerVoucherController - Endpoint công khai xem voucher toàn sàn trong giỏ hàng (GET /api/vouchers/platform)")
class BuyerVoucherControllerTest {

    @Mock
    private BuyerVoucherService buyerVoucherService;

    @InjectMocks
    private BuyerVoucherController buyerVoucherController;

    @Test
    @DisplayName("getPlatformVouchersForCart: Lấy danh sách voucher toàn sàn thành công trả về HTTP 200 OK")
    void getPlatformVouchersForCart_Success() {
        CartPlatformVoucherResponse responseDto = new CartPlatformVoucherResponse(
                UUID.randomUUID(),
                "FREESHIP30K",
                "Miễn phí vận chuyển 30k",
                VoucherType.PLATFORM,
                VoucherAppliesTo.SHIPPING_FEE,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(200000),
                null,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10),
                true,
                BigDecimal.valueOf(30000),
                BigDecimal.ZERO,
                null
        );

        when(buyerVoucherService.getPlatformVouchersForCart(
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), null))
                .thenReturn(List.of(responseDto));

        ApiResponse<List<CartPlatformVoucherResponse>> response =
                buyerVoucherController.getPlatformVouchersForCart(
                        BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), null);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Cart platform vouchers retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals("FREESHIP30K", response.getData().get(0).code());
        assertTrue(response.getData().get(0).isEligible());

        verify(buyerVoucherService, times(1)).getPlatformVouchersForCart(
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), null);
    }

    @Test
    @DisplayName("getPlatformVouchersForCart: Endpoint công khai không yêu cầu @PreAuthorize (PermitAll)")
    void getPlatformVouchersForCart_NoPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = BuyerVoucherController.class.getMethod(
                "getPlatformVouchersForCart", BigDecimal.class, BigDecimal.class, VoucherAppliesTo.class);
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNull(preAuthorize, "Method getPlatformVouchersForCart là endpoint công khai permitAll, không được có @PreAuthorize");
    }
}

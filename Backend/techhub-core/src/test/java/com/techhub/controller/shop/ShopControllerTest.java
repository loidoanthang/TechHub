package com.techhub.controller.shop;

import com.techhub.common.ApiResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.PublicShopResponse;
import com.techhub.model.enums.ShopStatus;
import com.techhub.service.shop.ShopService;
import com.techhub.model.dto.response.CartShopVoucherResponse;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherType;
import com.techhub.service.voucher.ShopVoucherService;
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
@DisplayName("Unit Tests cho ShopController - Endpoint công khai xem trang gian hàng & voucher trong giỏ hàng (GET /api/shops/{sellerName})")
class ShopControllerTest {

    @Mock
    private ShopService shopService;

    @Mock
    private ShopVoucherService shopVoucherService;

    @InjectMocks
    private ShopController shopController;

    @Test
    @DisplayName("getPublicShopProfile: Lấy thông tin Shop thành công trả về HTTP 200 OK")
    void getPublicShopProfile_Success() {
        PublicShopResponse responseDto = new PublicShopResponse(
                UUID.randomUUID(),
                "apple-store-vn",
                "Apple Store VN",
                "Cửa hàng phân phối sản phẩm Apple chính hãng",
                "https://cdn.techhub.com/avatar.png",
                "https://cdn.techhub.com/banner.png",
                ShopStatus.ACTIVE,
                "TP. Hồ Chí Minh",
                "Quận 1",
                LocalDateTime.now().minusDays(30)
        );

        when(shopService.getPublicShopProfile("apple-store-vn")).thenReturn(responseDto);

        ApiResponse<PublicShopResponse> response = shopController.getPublicShopProfile("apple-store-vn");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shop profile retrieved successfully", response.getMessage());
        assertEquals(responseDto, response.getData());
        assertNull(response.getMetadata());

        verify(shopService, times(1)).getPublicShopProfile("apple-store-vn");
    }

    @Test
    @DisplayName("getPublicShopProfile: Service ném BusinessException (SHOP_NOT_FOUND) -> Controller propagate ngoại lệ")
    void getPublicShopProfile_ShopNotFound_Propagates() {
        when(shopService.getPublicShopProfile("unknown-shop"))
                .thenThrow(new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopController.getPublicShopProfile("unknown-shop"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopService, times(1)).getPublicShopProfile("unknown-shop");
    }

    @Test
    @DisplayName("getPublicShopProfile: Endpoint công khai không yêu cầu @PreAuthorize (PermitAll)")
    void getPublicShopProfile_NoPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = ShopController.class.getMethod("getPublicShopProfile", String.class);
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNull(preAuthorize, "Method getPublicShopProfile là endpoint công khai permitAll, không được có @PreAuthorize");
    }

    @Test
    @DisplayName("getShopVouchersForCart: Lấy danh sách voucher thành công trả về HTTP 200 OK")
    void getShopVouchersForCart_Success() {
        UUID shopId = UUID.randomUUID();
        CartShopVoucherResponse voucherResponse = new CartShopVoucherResponse(
                UUID.randomUUID(),
                shopId,
                "LAPTOP50K",
                "Giảm 50k",
                VoucherType.SHOP,
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(200000),
                null,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10),
                true,
                BigDecimal.valueOf(50000),
                BigDecimal.ZERO,
                null
        );

        when(shopVoucherService.getShopVouchersForCart("apple-store-vn", BigDecimal.valueOf(500000)))
                .thenReturn(List.of(voucherResponse));

        ApiResponse<List<CartShopVoucherResponse>> response =
                shopController.getShopVouchersForCart("apple-store-vn", BigDecimal.valueOf(500000));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Cart shop vouchers retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals("LAPTOP50K", response.getData().get(0).code());
        assertTrue(response.getData().get(0).isEligible());

        verify(shopVoucherService, times(1)).getShopVouchersForCart("apple-store-vn", BigDecimal.valueOf(500000));
    }

    @Test
    @DisplayName("getShopVouchersForCart: Service ném BusinessException (SHOP_NOT_FOUND) -> Controller propagate ngoại lệ")
    void getShopVouchersForCart_ShopNotFound_Propagates() {
        when(shopVoucherService.getShopVouchersForCart("unknown-shop", null))
                .thenThrow(new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopController.getShopVouchersForCart("unknown-shop", null));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopVoucherService, times(1)).getShopVouchersForCart("unknown-shop", null);
    }

    @Test
    @DisplayName("getShopVouchersForCart: Endpoint công khai không yêu cầu @PreAuthorize (PermitAll)")
    void getShopVouchersForCart_NoPreAuthorizeAnnotation() throws NoSuchMethodException {
        var method = ShopController.class.getMethod("getShopVouchersForCart", String.class, BigDecimal.class);
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertNull(preAuthorize, "Method getShopVouchersForCart là endpoint công khai permitAll, không được có @PreAuthorize");
    }
}

package com.techhub.controller.shop;

import com.techhub.common.ApiResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.PublicShopResponse;
import com.techhub.model.enums.ShopStatus;
import com.techhub.service.shop.ShopService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ShopController - Endpoint công khai xem trang gian hàng (GET /api/shops/{sellerName})")
class ShopControllerTest {

    @Mock
    private ShopService shopService;

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

        ResponseEntity<ApiResponse<PublicShopResponse>> response = shopController.getPublicShopProfile("apple-store-vn");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Shop profile retrieved successfully", response.getBody().getMessage());
        assertEquals(responseDto, response.getBody().getData());
        assertNull(response.getBody().getMetadata());

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
}

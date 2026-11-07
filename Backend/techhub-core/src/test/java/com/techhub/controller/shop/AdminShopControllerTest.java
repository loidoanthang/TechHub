package com.techhub.controller.shop;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.AdminUpdateShopStatusRequest;
import com.techhub.model.dto.response.AdminShopResponse;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.service.shop.AdminShopService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminShopController (GET /api/admin/shops)")
class AdminShopControllerTest {

    @Mock
    private AdminShopService adminShopService;

    @InjectMocks
    private AdminShopController adminShopController;

    @Test
    @DisplayName("TC-19: Phân quyền bảo mật - Controller bắt buộc có @PreAuthorize(\"hasRole('ADMIN')\")")
    void controller_hasPreAuthorizeAdminAnnotation() {
        PreAuthorize annotation = AdminShopController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "AdminShopController phải được cấu hình @PreAuthorize");
        assertEquals("hasRole('ADMIN')", annotation.value(), "Annotation @PreAuthorize phải yêu cầu role ADMIN");
    }

    @Test
    @DisplayName("TC-20: Cấu hình RequestMapping - Controller bắt buộc có @RequestMapping(\"/api/admin/shops\")")
    void controller_hasRequestMappingAnnotation() {
        RequestMapping annotation = AdminShopController.class.getAnnotation(RequestMapping.class);
        assertNotNull(annotation, "AdminShopController phải được cấu hình @RequestMapping");
        assertArrayEquals(new String[]{"/api/admin/shops"}, annotation.value());
    }

    @Test
    @DisplayName("TC-21: Gọi Service đúng tham số và đóng gói ApiResponse thành công")
    void controller_getShops_callsServiceWithCorrectParamsAndReturnsApiResponse() {
        AdminShopResponse shopResponse = new AdminShopResponse(
                UUID.randomUUID(),
                "Official Tech Store",
                "Description",
                "https://cdn.techhub.com/avatar.png",
                "https://cdn.techhub.com/banner.png",
                ShopStatus.ACTIVE,
                UUID.randomUUID(),
                "tech_official",
                SellerVerificationStatus.APPROVED,
                UUID.randomUUID(),
                "Nguyen Van An",
                "an.nguyen@techstore.vn",
                "0987654321",
                UserStatus.ACTIVE,
                new BigDecimal("150000000.00"),
                25L,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now()
        );

        PageResponse<AdminShopResponse> mockPage = new PageResponse<>(
                List.of(shopResponse),
                0,
                10,
                1L,
                1,
                true
        );

        when(adminShopService.getShops("tech", ShopStatus.ACTIVE, 0, 10, "name", "asc"))
                .thenReturn(mockPage);

        ApiResponse<PageResponse<AdminShopResponse>> apiResponse = adminShopController.getShops(
                "tech", ShopStatus.ACTIVE, 0, 10, "name", "asc"
        );

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("Shop list retrieved successfully.", apiResponse.getMessage());
        assertEquals(mockPage, apiResponse.getData());
        assertNull(apiResponse.getMetadata());

        verify(adminShopService).getShops("tech", ShopStatus.ACTIVE, 0, 10, "name", "asc");
    }

    @Test
    @DisplayName("TC-22: Gọi Service với các tham số mặc định")
    void controller_getShops_defaultParameters() {
        PageResponse<AdminShopResponse> mockPage = new PageResponse<>(
                List.of(),
                0,
                20,
                0L,
                0,
                true
        );

        when(adminShopService.getShops(null, null, 0, 20, "createdAt", "desc"))
                .thenReturn(mockPage);

        ApiResponse<PageResponse<AdminShopResponse>> apiResponse = adminShopController.getShops(
                null, null, 0, 20, "createdAt", "desc"
        );

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals(0, apiResponse.getData().totalElements());

        verify(adminShopService).getShops(null, null, 0, 20, "createdAt", "desc");
    }

    @Test
    @DisplayName("TC-23: Controller gọi Service getShopDetail đúng tham số và đóng gói ApiResponse")
    void controller_getShopDetail_success() {
        UUID shopId = UUID.randomUUID();
        com.techhub.model.dto.response.AdminShopDetailResponse detailResponse = new com.techhub.model.dto.response.AdminShopDetailResponse(
                new com.techhub.model.dto.response.AdminShopDetailResponse.ShopCommercialResponse(
                        shopId,
                        "Official Tech Store",
                        "Description",
                        "avatar.png",
                        "banner.png",
                        ShopStatus.ACTIVE,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ),
                null,
                null,
                null,
                null,
                new com.techhub.model.dto.response.AdminShopDetailResponse.ShopMetricsResponse(10L, 8L, 5L, new BigDecimal("1000000.00"))
        );

        when(adminShopService.getShopDetail(shopId)).thenReturn(detailResponse);

        ApiResponse<com.techhub.model.dto.response.AdminShopDetailResponse> response = adminShopController.getShopDetail(shopId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shop details retrieved successfully.", response.getMessage());
        assertEquals(detailResponse, response.getData());
        assertNull(response.getMetadata());

        verify(adminShopService).getShopDetail(shopId);
    }

    // =========================================================================
    // API-09: PATCH /api/admin/shops/{id}/status - Controller Tests
    // =========================================================================

    @Test
    @DisplayName("TC-24 (API-09): Controller gọi đúng Service updateShopStatus và đóng gói ApiResponse")
    void controller_updateShopStatus_success() {
        UUID shopId = UUID.randomUUID();
        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(
                ShopStatus.SUSPENDED,
                "Tạm khóa kiểm tra"
        );

        AdminShopResponse shopResponse = new AdminShopResponse(
                shopId,
                "Official Tech Store",
                "Description",
                "https://cdn.techhub.com/avatar.png",
                "https://cdn.techhub.com/banner.png",
                ShopStatus.SUSPENDED,
                UUID.randomUUID(),
                "tech_official",
                SellerVerificationStatus.APPROVED,
                UUID.randomUUID(),
                "Nguyễn Văn An",
                "an.nguyen@techstore.vn",
                "0987654321",
                UserStatus.ACTIVE,
                new BigDecimal("154200000.00"),
                10L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminShopService.updateShopStatus(shopId, request)).thenReturn(shopResponse);

        ApiResponse<AdminShopResponse> response = adminShopController.updateShopStatus(shopId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shop status updated successfully.", response.getMessage());
        assertEquals(shopResponse, response.getData());
        assertNull(response.getMetadata());

        verify(adminShopService).updateShopStatus(shopId, request);
    }

    @Test
    @DisplayName("TC-25 (API-09): Phương thức updateShopStatus có annotation @PatchMapping(\"/{id}/status\")")
    void controller_updateShopStatus_hasPatchMappingAnnotation() throws NoSuchMethodException {
        Method method = AdminShopController.class.getMethod(
                "updateShopStatus",
                UUID.class,
                AdminUpdateShopStatusRequest.class
        );
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        assertNotNull(patchMapping, "Phương thức updateShopStatus phải có @PatchMapping");
        assertArrayEquals(new String[]{"/{id}/status"}, patchMapping.value());
    }

    @Test
    @DisplayName("TC-26 (API-09): Bean Validation - Chặn request khi status là null (@NotNull)")
    void requestDto_statusNull_validationFails() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(null, "Lý do");
        Set<ConstraintViolation<AdminUpdateShopStatusRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Validation phải phát hiện lỗi khi status null");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Shop status is required")));
    }

    @Test
    @DisplayName("TC-27 (API-09): Bean Validation - Chặn request khi reason > 500 ký tự (@Size)")
    void requestDto_reasonTooLong_validationFails() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        String longReason = "a".repeat(501);
        AdminUpdateShopStatusRequest request = new AdminUpdateShopStatusRequest(ShopStatus.SUSPENDED, longReason);
        Set<ConstraintViolation<AdminUpdateShopStatusRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Validation phải phát hiện lỗi khi reason > 500 ký tự");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Reason must not exceed 500 characters")));
    }
}

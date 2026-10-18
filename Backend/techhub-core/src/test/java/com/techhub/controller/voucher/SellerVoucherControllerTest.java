package com.techhub.controller.voucher;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherStatusRequest;
import com.techhub.model.dto.response.ShopVoucherDetailResponse;
import com.techhub.model.dto.response.ShopVoucherResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.voucher.SellerVoucherService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerVoucherController (POST, GET, GET by ID & PUT /api/seller/vouchers)")
class SellerVoucherControllerTest {

    @Mock
    private SellerVoucherService sellerVoucherService;

    @InjectMocks
    private SellerVoucherController sellerVoucherController;

    private Validator validator;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        userId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("seller@techhub.vn");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setRoles(Set.of(Role.SELLER));
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC_SVC_01: Controller tạo voucher thành công, gọi service với userId chính xác")
    void controller_createShopVoucher_Success() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "TECH10",
                "Giảm 10% đơn hàng",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("50000.00"),
                100,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(7)
        );

        ShopVoucherResponse mockResponse = new ShopVoucherResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "TECH10",
                "Giảm 10% đơn hàng",
                VoucherType.SHOP,
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("50000.00"),
                100,
                1,
                0,
                0,
                request.startDate(),
                request.endDate(),
                true,
                VoucherStatus.UPCOMING,
                LocalDateTime.now()
        );

        when(sellerVoucherService.createShopVoucher(userId, request)).thenReturn(mockResponse);

        // Act
        ApiResponse<ShopVoucherResponse> response = sellerVoucherController.createShopVoucher(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shop voucher created successfully", response.getMessage());
        assertEquals(mockResponse, response.getData());
        verify(sellerVoucherService).createShopVoucher(userId, request);
    }

    @Test
    @DisplayName("TC_SVC_02: Kiểm tra các Annotation bắt buộc trên Controller và Handler method")
    void controller_annotations_Valid() throws NoSuchMethodException {
        // Controller-level annotations
        assertTrue(SellerVoucherController.class.isAnnotationPresent(RestController.class));
        RequestMapping reqMapping = SellerVoucherController.class.getAnnotation(RequestMapping.class);
        assertNotNull(reqMapping);
        assertArrayEquals(new String[]{"/api/seller/vouchers"}, reqMapping.value());

        // Method-level annotations
        Method method = SellerVoucherController.class.getMethod("createShopVoucher", CreateShopVoucherRequest.class);
        assertTrue(method.isAnnotationPresent(PostMapping.class));

        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('SELLER')", preAuth.value());

        ResponseStatus resStatus = method.getAnnotation(ResponseStatus.class);
        assertNotNull(resStatus);
        assertEquals(HttpStatus.CREATED, resStatus.value());
    }

    @Test
    @DisplayName("TC_SVC_03: DTO validation hợp lệ không có lỗi vi phạm")
    void validation_validRequest_NoViolations() {
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "CODE123",
                "Tên Voucher Hợp Lệ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("15.00"),
                BigDecimal.ZERO,
                new BigDecimal("50000.00"),
                100,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        Set<ConstraintViolation<CreateShopVoucherRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("TC_SVC_04: DTO validation vi phạm khi thiếu mã, sai định dạng regex mã hoặc rỗng")
    void validation_invalidCode_HasViolations() {
        // Blank code
        CreateShopVoucherRequest req1 = new CreateShopVoucherRequest(
                "", "Name", VoucherAppliesTo.PRODUCT, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), BigDecimal.ZERO, null, 10, 1,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(2)
        );
        Set<ConstraintViolation<CreateShopVoucherRequest>> v1 = validator.validate(req1);
        assertFalse(v1.isEmpty());

        // Invalid characters (spaces, special characters not allowed)
        CreateShopVoucherRequest req2 = new CreateShopVoucherRequest(
                "CODE with SPACE", "Name", VoucherAppliesTo.PRODUCT, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), BigDecimal.ZERO, null, 10, 1,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(2)
        );
        Set<ConstraintViolation<CreateShopVoucherRequest>> v2 = validator.validate(req2);
        assertFalse(v2.isEmpty());
    }

    @Test
    @DisplayName("TC_SVC_05: DTO validation vi phạm khi thiếu các trường bắt buộc (name, appliesTo, discountType, dates)")
    void validation_missingRequiredFields_HasViolations() {
        CreateShopVoucherRequest req = new CreateShopVoucherRequest(
                "CODE12",
                null, // Missing name
                null, // Missing appliesTo
                null, // Missing discountType
                null, // Missing discountValue
                BigDecimal.ZERO,
                null,
                10,
                null, // Missing usageLimitPerUser
                null, // Missing startDate
                null  // Missing endDate
        );

        Set<ConstraintViolation<CreateShopVoucherRequest>> violations = validator.validate(req);
        assertEquals(7, violations.size());
    }

    @Test
    @DisplayName("TC_SVC_06: DTO validation vi phạm khi discountValue hoặc minOrderAmount âm/không hợp lệ")
    void validation_invalidNumericalValues_HasViolations() {
        CreateShopVoucherRequest req = new CreateShopVoucherRequest(
                "CODE12",
                "Name",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("0.00"), // Min is 0.01
                new BigDecimal("-1000.00"), // Min is 0.00
                new BigDecimal("0.00"), // Max discount amount min is 0.01
                0, // Usage limit min is 1
                0, // Usage limit per user min is 1
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(2)
        );

        Set<ConstraintViolation<CreateShopVoucherRequest>> violations = validator.validate(req);
        assertEquals(5, violations.size());
    }

    @Test
    @DisplayName("TC_SVC_07: DTO validation vi phạm khi discountValue vượt quá ngưỡng tối đa DecimalMax")
    void validation_numericalValuesExceedDecimalMax_HasViolations() {
        CreateShopVoucherRequest req = new CreateShopVoucherRequest(
                "CODE12",
                "Name",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("1000000000000.00"), // Vượt quá 999,999,999,999.99
                new BigDecimal("1000000000000.00"), // Vượt quá 999,999,999,999.99
                new BigDecimal("1000000000000.00"), // Vượt quá 999,999,999,999.99
                100,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(2)
        );

        Set<ConstraintViolation<CreateShopVoucherRequest>> violations = validator.validate(req);
        assertEquals(3, violations.size());
    }

    // ==========================================
    // Tests cho GET /api/seller/vouchers (List Shop Vouchers)
    // ==========================================

    @Test
    @DisplayName("TC_SVC_GET_01: Controller lấy danh sách voucher thành công và chuyển đủ tham số xuống service")
    void controller_getShopVouchers_Success() {
        // Arrange
        ShopVoucherResponse item = new ShopVoucherResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "FLASH50",
                "Flash Sale 50k",
                VoucherType.SHOP,
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                null,
                50,
                1,
                0,
                0,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                true,
                VoucherStatus.UPCOMING,
                LocalDateTime.now()
        );
        PageResponse<ShopVoucherResponse> mockPage = new PageResponse<>(
                List.of(item), 0, 10, 1, 1, true
        );

        when(sellerVoucherService.getShopVouchers(
                userId, "flash", VoucherAppliesTo.PRODUCT, DiscountType.FIXED_AMOUNT, VoucherStatus.UPCOMING, 0, 10, "createdAt", "desc"
        )).thenReturn(mockPage);

        // Act
        ApiResponse<PageResponse<ShopVoucherResponse>> response = sellerVoucherController.getShopVouchers(
                "flash", VoucherAppliesTo.PRODUCT, DiscountType.FIXED_AMOUNT, VoucherStatus.UPCOMING, 0, 10, "createdAt", "desc"
        );

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched shop vouchers successfully", response.getMessage());
        assertEquals(mockPage, response.getData());
        assertEquals(1, response.getData().content().size());
        assertEquals("FLASH50", response.getData().content().get(0).code());
        verify(sellerVoucherService).getShopVouchers(
                userId, "flash", VoucherAppliesTo.PRODUCT, DiscountType.FIXED_AMOUNT, VoucherStatus.UPCOMING, 0, 10, "createdAt", "desc"
        );
    }

    @Test
    @DisplayName("TC_SVC_GET_02: Kiểm tra các Annotation bắt buộc trên Handler method GET /api/seller/vouchers")
    void controller_getShopVouchers_Annotations_Valid() throws NoSuchMethodException {
        Method method = SellerVoucherController.class.getMethod(
                "getShopVouchers",
                String.class, VoucherAppliesTo.class, DiscountType.class, VoucherStatus.class,
                int.class, int.class, String.class, String.class
        );
        assertTrue(method.isAnnotationPresent(GetMapping.class));

        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('SELLER')", preAuth.value());
    }

    @Test
    @DisplayName("TC_SVC_GET_03: Controller trả về PageResponse rỗng khi Shop chưa có voucher")
    void controller_getShopVouchers_EmptyResult() {
        // Arrange
        PageResponse<ShopVoucherResponse> emptyPage = new PageResponse<>(
                Collections.emptyList(), 0, 10, 0, 0, true
        );

        when(sellerVoucherService.getShopVouchers(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        )).thenReturn(emptyPage);

        // Act
        ApiResponse<PageResponse<ShopVoucherResponse>> response = sellerVoucherController.getShopVouchers(
                null, null, null, null, 0, 10, "createdAt", "desc"
        );

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getData().content().isEmpty());
        assertEquals(0, response.getData().totalElements());
    }

    @Test
    @DisplayName("TC_SVC_GET_04: Controller gọi service với giá trị phân trang mặc định (page=0, size=10, sortBy=createdAt, sortDir=desc)")
    void controller_getShopVouchers_DefaultParameters() {
        // Arrange
        PageResponse<ShopVoucherResponse> mockPage = new PageResponse<>(
                Collections.emptyList(), 0, 10, 0, 0, true
        );
        when(sellerVoucherService.getShopVouchers(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        )).thenReturn(mockPage);

        // Act
        ApiResponse<PageResponse<ShopVoucherResponse>> response = sellerVoucherController.getShopVouchers(
                null, null, null, null, 0, 10, "createdAt", "desc"
        );

        // Assert
        assertNotNull(response);
        verify(sellerVoucherService).getShopVouchers(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        );
    }

    // ==========================================
    // Tests cho GET /api/seller/vouchers/{id} (View Shop Voucher Details)
    // ==========================================

    @Test
    @DisplayName("TC_SVDC_01: Controller lấy chi tiết voucher thành công, gọi service với đúng userId và voucherId")
    void controller_getShopVoucherDetail_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        ShopVoucherDetailResponse mockDetail = new ShopVoucherDetailResponse(
                voucherId,
                UUID.randomUUID(),
                "FLASH50",
                "Flash Sale 50k",
                VoucherType.SHOP,
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                5,
                20,
                80,
                20.00,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5),
                true,
                VoucherStatus.ONGOING,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1)
        );

        when(sellerVoucherService.getShopVoucherDetail(userId, voucherId)).thenReturn(mockDetail);

        // Act
        ApiResponse<ShopVoucherDetailResponse> response = sellerVoucherController.getShopVoucherDetail(voucherId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched shop voucher detail successfully", response.getMessage());
        assertEquals(mockDetail, response.getData());
        assertEquals(voucherId, response.getData().id());
        assertEquals(80, response.getData().remainingCount());
        assertEquals(20.00, response.getData().redemptionRate());
        verify(sellerVoucherService).getShopVoucherDetail(userId, voucherId);
    }

    @Test
    @DisplayName("TC_SVDC_02: Kiểm tra các Annotation bắt buộc trên Handler method GET /api/seller/vouchers/{id}")
    void controller_getShopVoucherDetail_Annotations_Valid() throws NoSuchMethodException {
        Method method = SellerVoucherController.class.getMethod("getShopVoucherDetail", UUID.class);
        assertTrue(method.isAnnotationPresent(GetMapping.class));
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/{id}"}, getMapping.value());

        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('SELLER')", preAuth.value());
    }

    // ==========================================
    // Tests cho PUT /api/seller/vouchers/{id} (Update Shop Voucher)
    // ==========================================

    @Test
    @DisplayName("TC_SVUC_01: Controller cập nhật voucher thành công và kiểm tra đầy đủ Annotations")
    void controller_updateShopVoucher_Success() throws NoSuchMethodException {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Updated Name",
                DiscountType.PERCENTAGE,
                new BigDecimal("15.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("30000.00"),
                200,
                1,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        ShopVoucherDetailResponse mockResponse = new ShopVoucherDetailResponse(
                voucherId,
                UUID.randomUUID(),
                "CODE10",
                "Updated Name",
                VoucherType.SHOP,
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("15.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("30000.00"),
                200,
                1,
                0,
                0,
                200,
                0.00,
                request.startDate(),
                request.endDate(),
                true,
                VoucherStatus.UPCOMING,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(sellerVoucherService.updateShopVoucher(userId, voucherId, request)).thenReturn(mockResponse);

        // Act
        ApiResponse<ShopVoucherDetailResponse> response = sellerVoucherController.updateShopVoucher(voucherId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shop voucher updated successfully", response.getMessage());
        assertEquals(mockResponse, response.getData());
        verify(sellerVoucherService).updateShopVoucher(userId, voucherId, request);

        // Check annotations
        Method method = SellerVoucherController.class.getMethod("updateShopVoucher", UUID.class, UpdateShopVoucherRequest.class);
        assertTrue(method.isAnnotationPresent(PutMapping.class));
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        assertArrayEquals(new String[]{"/{id}"}, putMapping.value());

        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('SELLER')", preAuth.value());
    }

    @Test
    @DisplayName("TC_SVUC_02: DTO validation cho UpdateShopVoucherRequest vi phạm khi name blank hoặc discountValue âm hoặc thiếu endDate")
    void validation_updateShopVoucher_Violations() {
        // Case 1: Blank name, negative discount value, missing discountType, missing endDate
        UpdateShopVoucherRequest invalidReq = new UpdateShopVoucherRequest(
                "",
                null,
                new BigDecimal("-5.00"),
                new BigDecimal("-100.00"),
                new BigDecimal("0.00"),
                0,
                0,
                null,
                null
        );

        Set<ConstraintViolation<UpdateShopVoucherRequest>> violations = validator.validate(invalidReq);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("discountType")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("discountValue")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("endDate")));
    }

    // ==========================================
    // Tests cho PATCH /api/seller/vouchers/{id}/status (Toggle Shop Voucher Status)
    // ==========================================

    @Test
    @DisplayName("TC_SVSC_01: Controller cập nhật trạng thái voucher thành công và kiểm tra đầy đủ Annotations")
    void controller_updateShopVoucherStatus_Success() throws NoSuchMethodException {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        ShopVoucherDetailResponse mockResponse = new ShopVoucherDetailResponse(
                voucherId,
                UUID.randomUUID(),
                "SUMMER2026",
                "Summer Sale",
                VoucherType.SHOP,
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                2,
                15,
                85,
                15.00,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5),
                false,
                VoucherStatus.DISABLED,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now()
        );

        when(sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request)).thenReturn(mockResponse);

        // Act
        ApiResponse<ShopVoucherDetailResponse> response = sellerVoucherController.updateShopVoucherStatus(voucherId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shop voucher status updated successfully", response.getMessage());
        assertEquals(mockResponse, response.getData());
        assertFalse(response.getData().isActive());
        assertEquals(VoucherStatus.DISABLED, response.getData().status());
        verify(sellerVoucherService).updateShopVoucherStatus(userId, voucherId, request);

        // Check annotations
        Method method = SellerVoucherController.class.getMethod("updateShopVoucherStatus", UUID.class, UpdateShopVoucherStatusRequest.class);
        assertTrue(method.isAnnotationPresent(PatchMapping.class));
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        assertArrayEquals(new String[]{"/{id}/status"}, patchMapping.value());

        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('SELLER')", preAuth.value());
    }

    @Test
    @DisplayName("TC_SVSC_02: DTO validation vi phạm khi thiếu trường isActive (null)")
    void validation_updateShopVoucherStatus_NullIsActive() {
        UpdateShopVoucherStatusRequest invalidReq = new UpdateShopVoucherStatusRequest(null);
        Set<ConstraintViolation<UpdateShopVoucherStatusRequest>> violations = validator.validate(invalidReq);

        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("isActive")));
    }

    @Test
    @DisplayName("TC_SVDC_01: Seller gọi DELETE /api/seller/vouchers/{id} thành công -> HTTP 200, message thành công, data null")
    void deleteShopVoucher_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();

        // Act
        ApiResponse<Void> response = sellerVoucherController.deleteShopVoucher(voucherId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shop voucher deleted successfully", response.getMessage());
        assertNull(response.getData());
        verify(sellerVoucherService).deleteShopVoucher(userId, voucherId);
    }

    @Test
    @DisplayName("TC_SVDC_02: Kiểm tra Controller annotations (@DeleteMapping('/{id}'), @PreAuthorize('hasRole('SELLER')') qua Reflection")
    void deleteShopVoucher_AnnotationsCheck() throws NoSuchMethodException {
        Method method = SellerVoucherController.class.getMethod("deleteShopVoucher", UUID.class);
        assertTrue(method.isAnnotationPresent(DeleteMapping.class));
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        assertArrayEquals(new String[]{"/{id}"}, deleteMapping.value());

        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('SELLER')", preAuth.value());
    }
}

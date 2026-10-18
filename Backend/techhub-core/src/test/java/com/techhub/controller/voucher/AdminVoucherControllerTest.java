package com.techhub.controller.voucher;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreatePlatformVoucherRequest;
import com.techhub.model.dto.request.UpdatePlatformVoucherRequest;
import com.techhub.model.dto.request.UpdatePlatformVoucherStatusRequest;
import com.techhub.model.dto.response.AdminVoucherDetailResponse;
import com.techhub.model.dto.response.AdminVoucherResponse;
import com.techhub.model.dto.response.PlatformVoucherResponse;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;
import com.techhub.service.voucher.AdminVoucherService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminVoucherController (POST /api/admin/vouchers)")
class AdminVoucherControllerTest {

    @Mock
    private AdminVoucherService adminVoucherService;

    @InjectMocks
    private AdminVoucherController adminVoucherController;

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Kiểm tra controller annotations: @RestController, @RequestMapping, @PreAuthorize")
    void testControllerAnnotations() {
        assertTrue(AdminVoucherController.class.isAnnotationPresent(RestController.class));

        RequestMapping mapping = AdminVoucherController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/admin/vouchers"}, mapping.value());

        PreAuthorize preAuth = AdminVoucherController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('ADMIN')", preAuth.value());
    }

    @Test
    @DisplayName("Kiểm tra method annotations: @PostMapping, @ResponseStatus(HttpStatus.CREATED)")
    void testMethodAnnotations() throws NoSuchMethodException {
        Method method = AdminVoucherController.class.getMethod("createPlatformVoucher", CreatePlatformVoucherRequest.class);
        assertTrue(method.isAnnotationPresent(PostMapping.class));

        ResponseStatus status = method.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.CREATED, status.value());
    }

    @Test
    @DisplayName("createPlatformVoucher: Gọi service thành công và trả về ApiResponse 201 Created")
    void testCreatePlatformVoucher_Success() {
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "TECHHUB50K",
                "Giảm 50K đơn từ 300K",
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(300000),
                null,
                100,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        PlatformVoucherResponse mockResponse = new PlatformVoucherResponse(
                UUID.randomUUID(),
                "TECHHUB50K",
                "Giảm 50K đơn từ 300K",
                VoucherType.PLATFORM,
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(300000),
                null,
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

        when(adminVoucherService.createPlatformVoucher(any(CreatePlatformVoucherRequest.class))).thenReturn(mockResponse);

        ApiResponse<PlatformVoucherResponse> apiResponse = adminVoucherController.createPlatformVoucher(request);

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("Platform voucher created successfully", apiResponse.getMessage());
        assertEquals("TECHHUB50K", apiResponse.getData().code());
        assertEquals(VoucherType.PLATFORM, apiResponse.getData().type());
        verify(adminVoucherService, times(1)).createPlatformVoucher(request);
    }

    @Test
    @DisplayName("Bean Validation: DTO hợp lệ không có violation nào")
    void testBeanValidation_ValidRequest() {
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "VALID_CODE-123",
                "Tên hợp lệ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(20000),
                50,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        Set<ConstraintViolation<CreatePlatformVoucherRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: code blank hoặc sai regex sinh violation")
    void testBeanValidation_InvalidCode() {
        CreatePlatformVoucherRequest blankCode = new CreatePlatformVoucherRequest(
                "",
                "Tên hợp lệ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        Set<ConstraintViolation<CreatePlatformVoucherRequest>> violations = validator.validate(blankCode);
        assertFalse(violations.isEmpty());

        CreatePlatformVoucherRequest invalidChar = new CreatePlatformVoucherRequest(
                "CODE@#$%",
                "Tên hợp lệ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        violations = validator.validate(invalidChar);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: name blank hoặc ngắn hơn 2 ký tự sinh violation")
    void testBeanValidation_InvalidName() {
        CreatePlatformVoucherRequest shortName = new CreatePlatformVoucherRequest(
                "CODE123",
                "A",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        Set<ConstraintViolation<CreatePlatformVoucherRequest>> violations = validator.validate(shortName);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: discountValue <= 0 hoặc minOrderAmount < 0 sinh violation")
    void testBeanValidation_InvalidValues() {
        CreatePlatformVoucherRequest zeroDiscount = new CreatePlatformVoucherRequest(
                "CODE123",
                "Tên voucher",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.ZERO,
                null,
                null,
                null,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        Set<ConstraintViolation<CreatePlatformVoucherRequest>> violations = validator.validate(zeroDiscount);
        assertFalse(violations.isEmpty());

        CreatePlatformVoucherRequest negativeMinOrder = new CreatePlatformVoucherRequest(
                "CODE123",
                "Tên voucher",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.TEN,
                BigDecimal.valueOf(-1),
                null,
                null,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        violations = validator.validate(negativeMinOrder);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: usageLimitPerUser null hoặc < 1 sinh violation")
    void testBeanValidation_InvalidUsageLimitPerUser() {
        CreatePlatformVoucherRequest nullLimit = new CreatePlatformVoucherRequest(
                "CODE123",
                "Tên voucher",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        Set<ConstraintViolation<CreatePlatformVoucherRequest>> violations = validator.validate(nullLimit);
        assertFalse(violations.isEmpty());

        CreatePlatformVoucherRequest zeroLimit = new CreatePlatformVoucherRequest(
                "CODE123",
                "Tên voucher",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.TEN,
                null,
                null,
                null,
                0,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        violations = validator.validate(zeroLimit);
        assertFalse(violations.isEmpty());
    }

    // ==========================================
    // Tests for GET /api/admin/vouchers
    // ==========================================

    @Test
    @DisplayName("Kiểm tra method annotations của getVouchers: @GetMapping")
    void testGetVouchersMethodAnnotation() throws NoSuchMethodException {
        Method method = AdminVoucherController.class.getMethod(
                "getVouchers",
                VoucherType.class,
                UUID.class,
                String.class,
                VoucherAppliesTo.class,
                DiscountType.class,
                VoucherStatus.class,
                int.class,
                int.class,
                String.class,
                String.class
        );
        assertTrue(method.isAnnotationPresent(GetMapping.class));
    }

    @Test
    @DisplayName("getVouchers: Gọi service với đầy đủ query parameters và trả về ApiResponse 200 OK")
    void testGetVouchers_Success() {
        UUID shopId = UUID.randomUUID();
        AdminVoucherResponse sampleItem = new AdminVoucherResponse(
                UUID.randomUUID(),
                shopId,
                "Tech Official Store",
                com.techhub.model.enums.ShopStatus.ACTIVE,
                "TECH50K",
                "Giảm 50K cho đồ công nghệ",
                VoucherType.SHOP,
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(500000),
                null,
                100,
                1,
                0,
                12,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5),
                true,
                false,
                VoucherStatus.ONGOING,
                LocalDateTime.now().minusDays(2)
        );

        PageResponse<AdminVoucherResponse> mockPageResponse = new PageResponse<>(
                List.of(sampleItem),
                0,
                20,
                1L,
                1,
                true
        );

        when(adminVoucherService.getVouchers(
                VoucherType.SHOP,
                shopId,
                "TECH",
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                VoucherStatus.ONGOING,
                0,
                20,
                "createdAt",
                "desc"
        )).thenReturn(mockPageResponse);

        ApiResponse<PageResponse<AdminVoucherResponse>> response = adminVoucherController.getVouchers(
                VoucherType.SHOP,
                shopId,
                "TECH",
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                VoucherStatus.ONGOING,
                0,
                20,
                "createdAt",
                "desc"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched admin vouchers successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().content().size());
        assertEquals("TECH50K", response.getData().content().get(0).code());
        assertEquals("Tech Official Store", response.getData().content().get(0).shopName());
        assertEquals(com.techhub.model.enums.ShopStatus.ACTIVE, response.getData().content().get(0).shopStatus());

        verify(adminVoucherService, times(1)).getVouchers(
                VoucherType.SHOP,
                shopId,
                "TECH",
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                VoucherStatus.ONGOING,
                0,
                20,
                "createdAt",
                "desc"
        );
    }

    @Test
    @DisplayName("Kiểm tra method annotations cho getVoucherDetail: @GetMapping(\"/{id}\")")
    void testMethodAnnotations_GetVoucherDetail() throws NoSuchMethodException {
        Method method = AdminVoucherController.class.getMethod("getVoucherDetail", UUID.class);
        assertTrue(method.isAnnotationPresent(GetMapping.class));
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/{id}"}, mapping.value());
    }

    @Test
    @DisplayName("getVoucherDetail: Gọi service thành công và trả về ApiResponse 200 OK")
    void testGetVoucherDetail_Success() {
        UUID voucherId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        AdminVoucherDetailResponse mockDetail = new AdminVoucherDetailResponse(
                voucherId,
                "TECHVIP100",
                "Voucher VIP 100K",
                VoucherType.SHOP,
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(500000),
                null,
                200,
                2,
                10,
                50,
                140,
                25.0,
                shopId,
                "Official Tech Store",
                com.techhub.model.enums.ShopStatus.ACTIVE,
                "https://cdn.techhub.vn/shop/avatar.png",
                "SHOP",
                "Người bán tự chi trả 100% ngân sách khuyến mãi",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().plusDays(5),
                true,
                VoucherStatus.ONGOING,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(1)
        );

        when(adminVoucherService.getVoucherDetail(voucherId)).thenReturn(mockDetail);

        ApiResponse<AdminVoucherDetailResponse> response = adminVoucherController.getVoucherDetail(voucherId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched admin voucher detail successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(voucherId, response.getData().id());
        assertEquals("TECHVIP100", response.getData().code());
        assertEquals(VoucherType.SHOP, response.getData().type());
        assertEquals(shopId, response.getData().shopId());
        assertEquals("Official Tech Store", response.getData().shopName());
        assertEquals(25.0, response.getData().redemptionRate());
        assertEquals(140, response.getData().remainingCount());
        assertEquals("SHOP", response.getData().sponsor());

        verify(adminVoucherService, times(1)).getVoucherDetail(voucherId);
    }

    @Test
    @DisplayName("Kiểm tra method annotations cho updatePlatformVoucher: @PutMapping(\"/{id}\")")
    void testMethodAnnotations_UpdatePlatformVoucher() throws NoSuchMethodException {
        Method method = AdminVoucherController.class.getMethod("updatePlatformVoucher", UUID.class, UpdatePlatformVoucherRequest.class);
        assertTrue(method.isAnnotationPresent(PutMapping.class));
        PutMapping mapping = method.getAnnotation(PutMapping.class);
        assertArrayEquals(new String[]{"/{id}"}, mapping.value());
    }

    @Test
    @DisplayName("updatePlatformVoucher: Gọi service thành công và trả về ApiResponse 200 OK")
    void testUpdatePlatformVoucher_Success() {
        UUID voucherId = UUID.randomUUID();
        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Mã giảm giá hè 2026 update",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(300000),
                null,
                200,
                2,
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusDays(10)
        );

        AdminVoucherDetailResponse mockDetail = new AdminVoucherDetailResponse(
                voucherId,
                "SUMMER50K",
                "Mã giảm giá hè 2026 update",
                VoucherType.PLATFORM,
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(300000),
                null,
                200,
                2,
                0,
                0,
                200,
                0.0,
                null,
                null,
                null,
                null,
                "PLATFORM",
                "Sàn TechHub tài trợ 100% ngân sách khuyến mãi",
                request.startDate(),
                request.endDate(),
                true,
                VoucherStatus.UPCOMING,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );

        when(adminVoucherService.updatePlatformVoucher(voucherId, request)).thenReturn(mockDetail);

        ApiResponse<AdminVoucherDetailResponse> response = adminVoucherController.updatePlatformVoucher(voucherId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Platform voucher updated successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(voucherId, response.getData().id());
        assertEquals("SUMMER50K", response.getData().code());
        assertEquals("Mã giảm giá hè 2026 update", response.getData().name());
        assertEquals(200, response.getData().usageLimit());

        verify(adminVoucherService, times(1)).updatePlatformVoucher(voucherId, request);
    }

    @Test
    @DisplayName("Bean Validation: UpdatePlatformVoucherRequest hợp lệ không có violation nào")
    void testUpdateVoucherValidation_Valid() {
        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher Hợp Lệ",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(150000),
                BigDecimal.valueOf(30000),
                100,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        Set<ConstraintViolation<UpdatePlatformVoucherRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: UpdatePlatformVoucherRequest name blank hoặc > 200 ký tự sinh violation")
    void testUpdateVoucherValidation_InvalidName() {
        UpdatePlatformVoucherRequest blankName = new UpdatePlatformVoucherRequest(
                "   ",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(15),
                null,
                null,
                null,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );
        Set<ConstraintViolation<UpdatePlatformVoucherRequest>> violations = validator.validate(blankName);
        assertFalse(violations.isEmpty());

        UpdatePlatformVoucherRequest longName = new UpdatePlatformVoucherRequest(
                "A".repeat(201),
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(15),
                null,
                null,
                null,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );
        violations = validator.validate(longName);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: UpdatePlatformVoucherRequest discountType null hoặc discountValue <= 0 sinh violation")
    void testUpdateVoucherValidation_InvalidDiscount() {
        UpdatePlatformVoucherRequest nullType = new UpdatePlatformVoucherRequest(
                "Voucher Name",
                null,
                BigDecimal.valueOf(15),
                null,
                null,
                null,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );
        Set<ConstraintViolation<UpdatePlatformVoucherRequest>> violations = validator.validate(nullType);
        assertFalse(violations.isEmpty());

        UpdatePlatformVoucherRequest zeroValue = new UpdatePlatformVoucherRequest(
                "Voucher Name",
                DiscountType.PERCENTAGE,
                BigDecimal.ZERO,
                null,
                null,
                null,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );
        violations = validator.validate(zeroValue);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: UpdatePlatformVoucherRequest minOrderAmount hoặc maxDiscountAmount âm sinh violation")
    void testUpdateVoucherValidation_NegativeAmounts() {
        UpdatePlatformVoucherRequest negativeMinOrder = new UpdatePlatformVoucherRequest(
                "Voucher Name",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(-1),
                null,
                null,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );
        Set<ConstraintViolation<UpdatePlatformVoucherRequest>> violations = validator.validate(negativeMinOrder);
        assertFalse(violations.isEmpty());

        UpdatePlatformVoucherRequest negativeMaxDiscount = new UpdatePlatformVoucherRequest(
                "Voucher Name",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                BigDecimal.valueOf(-1000),
                null,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );
        violations = validator.validate(negativeMaxDiscount);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: UpdatePlatformVoucherRequest usageLimit hoặc userLimit < 1 sinh violation")
    void testUpdateVoucherValidation_InvalidLimits() {
        UpdatePlatformVoucherRequest invalidUsage = new UpdatePlatformVoucherRequest(
                "Voucher Name",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                0,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );
        Set<ConstraintViolation<UpdatePlatformVoucherRequest>> violations = validator.validate(invalidUsage);
        assertFalse(violations.isEmpty());

        UpdatePlatformVoucherRequest invalidUser = new UpdatePlatformVoucherRequest(
                "Voucher Name",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                0,
                null,
                LocalDateTime.now().plusDays(5)
        );
        violations = validator.validate(invalidUser);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: UpdatePlatformVoucherRequest endDate null sinh violation")
    void testUpdateVoucherValidation_NullEndDate() {
        UpdatePlatformVoucherRequest nullEndDate = new UpdatePlatformVoucherRequest(
                "Voucher Name",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                null,
                null
        );
        Set<ConstraintViolation<UpdatePlatformVoucherRequest>> violations = validator.validate(nullEndDate);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Kiểm tra method annotations cho updatePlatformVoucherStatus: @PatchMapping(\"/{id}/status\")")
    void testMethodAnnotations_UpdatePlatformVoucherStatus() throws NoSuchMethodException {
        Method method = AdminVoucherController.class.getMethod("updatePlatformVoucherStatus", UUID.class, UpdatePlatformVoucherStatusRequest.class);
        assertTrue(method.isAnnotationPresent(PatchMapping.class));
        PatchMapping mapping = method.getAnnotation(PatchMapping.class);
        assertArrayEquals(new String[]{"/{id}/status"}, mapping.value());
    }

    @Test
    @DisplayName("updatePlatformVoucherStatus: Gọi service thành công và trả về ApiResponse 200 OK")
    void testUpdatePlatformVoucherStatus_Success() {
        UUID voucherId = UUID.randomUUID();
        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        AdminVoucherDetailResponse mockDetail = new AdminVoucherDetailResponse(
                voucherId,
                "SUMMER50K",
                "Mã giảm giá hè 2026",
                VoucherType.PLATFORM,
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(300000),
                null,
                1000,
                2,
                15,
                142,
                843,
                14.20,
                null,
                null,
                null,
                null,
                "TECHHUB_PLATFORM",
                "Sàn TechHub tài trợ 100% chiết khấu ngân sách (BR-16D)",
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().plusDays(10),
                false,
                VoucherStatus.DISABLED,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now()
        );

        when(adminVoucherService.updatePlatformVoucherStatus(voucherId, request)).thenReturn(mockDetail);

        ApiResponse<AdminVoucherDetailResponse> response = adminVoucherController.updatePlatformVoucherStatus(voucherId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Platform voucher status updated successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(voucherId, response.getData().id());
        assertFalse(response.getData().isActive());
        assertEquals(VoucherStatus.DISABLED, response.getData().status());

        verify(adminVoucherService, times(1)).updatePlatformVoucherStatus(voucherId, request);
    }

    @Test
    @DisplayName("Bean Validation: UpdatePlatformVoucherStatusRequest hợp lệ không có violation nào")
    void testUpdatePlatformVoucherStatusValidation_Valid() {
        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(true);
        Set<ConstraintViolation<UpdatePlatformVoucherStatusRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());

        UpdatePlatformVoucherStatusRequest requestFalse = new UpdatePlatformVoucherStatusRequest(false);
        violations = validator.validate(requestFalse);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Bean Validation: UpdatePlatformVoucherStatusRequest có isActive null sinh violation")
    void testUpdatePlatformVoucherStatusValidation_NullIsActive() {
        UpdatePlatformVoucherStatusRequest invalidRequest = new UpdatePlatformVoucherStatusRequest(null);
        Set<ConstraintViolation<UpdatePlatformVoucherStatusRequest>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Kiểm tra method annotations cho deletePlatformVoucher: @DeleteMapping(\"/{id}\")")
    void testMethodAnnotations_DeletePlatformVoucher() throws NoSuchMethodException {
        Method method = AdminVoucherController.class.getMethod("deletePlatformVoucher", UUID.class);
        assertTrue(method.isAnnotationPresent(DeleteMapping.class));
        DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
        assertArrayEquals(new String[]{"/{id}"}, mapping.value());
    }

    @Test
    @DisplayName("deletePlatformVoucher: Gọi service thành công và trả về ApiResponse 200 OK")
    void testDeletePlatformVoucher_Success() {
        UUID voucherId = UUID.randomUUID();
        doNothing().when(adminVoucherService).deletePlatformVoucher(voucherId);

        ApiResponse<Void> response = adminVoucherController.deletePlatformVoucher(voucherId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Platform voucher deleted successfully", response.getMessage());
        assertNull(response.getData());

        verify(adminVoucherService, times(1)).deletePlatformVoucher(voucherId);
    }
}

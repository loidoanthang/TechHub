package com.techhub.controller.payout;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.payout.ProcessPayoutRequest;
import com.techhub.model.dto.response.payout.AdminPayoutResponse;
import com.techhub.model.enums.PayoutProcessAction;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.security.SecurityUtils;
import com.techhub.service.payout.AdminPayoutService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminPayoutController (GET & PATCH /api/admin/payouts)")
class AdminPayoutControllerTest {

    private static Validator validator;

    @Mock
    private AdminPayoutService adminPayoutService;

    @InjectMocks
    private AdminPayoutController controller;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("TC-ADM-PAY-01: Controller Happy Path - Lấy danh sách yêu cầu rút tiền thành công trả về 200 OK")
    void getPayoutRequests_Success_Returns200() {
        AdminPayoutResponse item = AdminPayoutResponse.builder()
                .id(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .sellerName("TechHub Official Store")
                .shopName("TechHub Store")
                .amount(new BigDecimal("2500000.00"))
                .bankName("Vietcombank")
                .bankAccountNumber("1234567890")
                .bankAccountHolder("NGUYEN VAN A")
                .status(PayoutStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<AdminPayoutResponse> mockPage = new PageResponse<>(
                List.of(item), 0, 20, 1, 1, true
        );

        when(adminPayoutService.getPayoutRequests(null, null, null, null, 0, 20, "createdAt", "DESC"))
                .thenReturn(mockPage);

        ApiResponse<PageResponse<AdminPayoutResponse>> response = controller.getPayoutRequests(
                null, null, null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Payout requests retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().content().size());
        assertEquals(1, response.getData().totalElements());
        verify(adminPayoutService, times(1)).getPayoutRequests(
                null, null, null, null, 0, 20, "createdAt", "DESC"
        );
    }

    @Test
    @DisplayName("TC-ADM-PAY-18: Validation - Page index âm bị từ chối (@Min(0))")
    void getPayoutRequests_ValidationError_NegativePage() throws NoSuchMethodException {
        ExecutableValidator execValidator = validator.forExecutables();
        Method method = AdminPayoutController.class.getMethod("getPayoutRequests",
                PayoutStatus.class, String.class, LocalDate.class, LocalDate.class, int.class, int.class, String.class, String.class);

        Set<ConstraintViolation<AdminPayoutController>> violations = execValidator.validateParameters(
                controller, method, new Object[]{null, null, null, null, -1, 20, "createdAt", "DESC"}
        );

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Page index must not be negative")));
    }

    @Test
    @DisplayName("TC-ADM-PAY-19: Validation - Page size <= 0 bị từ chối (@Min(1))")
    void getPayoutRequests_ValidationError_ZeroOrNegativeSize() throws NoSuchMethodException {
        ExecutableValidator execValidator = validator.forExecutables();
        Method method = AdminPayoutController.class.getMethod("getPayoutRequests",
                PayoutStatus.class, String.class, LocalDate.class, LocalDate.class, int.class, int.class, String.class, String.class);

        Set<ConstraintViolation<AdminPayoutController>> violations = execValidator.validateParameters(
                controller, method, new Object[]{null, null, null, null, 0, 0, "createdAt", "DESC"}
        );

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Page size must be at least 1")));
    }

    @Test
    @DisplayName("TC-ADM-PAY-19b: Validation - Page size vượt quá 100 bị từ chối (@Max(100))")
    void getPayoutRequests_ValidationError_SizeExceedingMax() throws NoSuchMethodException {
        ExecutableValidator execValidator = validator.forExecutables();
        Method method = AdminPayoutController.class.getMethod("getPayoutRequests",
                PayoutStatus.class, String.class, LocalDate.class, LocalDate.class, int.class, int.class, String.class, String.class);

        Set<ConstraintViolation<AdminPayoutController>> violations = execValidator.validateParameters(
                controller, method, new Object[]{null, null, null, null, 0, 101, "createdAt", "DESC"}
        );

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Page size must not exceed 100")));
    }

    @Test
    @DisplayName("TC-ADM-PAY-20: Authorization & Mapping - Bắt buộc @PreAuthorize(\"hasRole('ADMIN')\") và mapping đúng")
    void getPayoutRequests_PreAuthorize_RequiresRoleAdmin() throws NoSuchMethodException {
        Method method = AdminPayoutController.class.getMethod("getPayoutRequests",
                PayoutStatus.class, String.class, LocalDate.class, LocalDate.class, int.class, int.class, String.class, String.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getPayoutRequests phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('ADMIN')"),
                "@PreAuthorize phải yêu cầu hasRole('ADMIN')");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "Method getPayoutRequests phải có annotation @GetMapping");

        RequestMapping classMapping = AdminPayoutController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/api/admin/payouts"}, classMapping.value());
    }

    // =========================================================================
    // UNIT TESTS CHO PROCESS PAYOUT REQUEST (PATCH /api/admin/payouts/{id}/process)
    // =========================================================================

    @Test
    @DisplayName("TC-ADM-PROC-01b: Controller Happy Path - Phê duyệt rút tiền thành công trả về 200 OK")
    void processPayoutRequest_Success_Approve() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.APPROVE, null);

        AdminPayoutResponse mockResponse = AdminPayoutResponse.builder()
                .id(payoutId)
                .amount(new BigDecimal("1000000.00"))
                .status(PayoutStatus.COMPLETED)
                .processedById(adminId)
                .processedByName("Super Admin")
                .processedAt(LocalDateTime.now())
                .build();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(adminId);
            when(adminPayoutService.processPayoutRequest(payoutId, adminId, request)).thenReturn(mockResponse);

            ApiResponse<AdminPayoutResponse> response = controller.processPayoutRequest(payoutId, request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("Payout request processed successfully", response.getMessage());
            assertEquals(payoutId, response.getData().id());
            assertEquals(PayoutStatus.COMPLETED, response.getData().status());

            verify(adminPayoutService).processPayoutRequest(payoutId, adminId, request);
        }
    }

    @Test
    @DisplayName("TC-ADM-PROC-02b: Controller Happy Path - Từ chối rút tiền thành công trả về 200 OK")
    void processPayoutRequest_Success_Reject() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.REJECT, "Sai thông tin ngân hàng");

        AdminPayoutResponse mockResponse = AdminPayoutResponse.builder()
                .id(payoutId)
                .amount(new BigDecimal("2000000.00"))
                .status(PayoutStatus.REJECTED)
                .rejectionReason("Sai thông tin ngân hàng")
                .processedById(adminId)
                .processedByName("Super Admin")
                .processedAt(LocalDateTime.now())
                .build();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(adminId);
            when(adminPayoutService.processPayoutRequest(payoutId, adminId, request)).thenReturn(mockResponse);

            ApiResponse<AdminPayoutResponse> response = controller.processPayoutRequest(payoutId, request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("Payout request processed successfully", response.getMessage());
            assertEquals(payoutId, response.getData().id());
            assertEquals(PayoutStatus.REJECTED, response.getData().status());
            assertEquals("Sai thông tin ngân hàng", response.getData().rejectionReason());

            verify(adminPayoutService).processPayoutRequest(payoutId, adminId, request);
        }
    }

    @Test
    @DisplayName("TC-ADM-PROC-08: Validation - Action bị null ném lỗi validation")
    void processPayoutRequest_ValidationError_NullAction() {
        ProcessPayoutRequest request = new ProcessPayoutRequest(null, "Lý do");
        Set<ConstraintViolation<ProcessPayoutRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Action must not be null")));
    }

    @Test
    @DisplayName("TC-ADM-PROC-10: Validation - RejectionReason vượt quá 1000 ký tự")
    void processPayoutRequest_ValidationError_LongRejectionReason() {
        String longReason = "a".repeat(1001);
        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.REJECT, longReason);
        Set<ConstraintViolation<ProcessPayoutRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Rejection reason must not exceed 1000 characters")));
    }

    @Test
    @DisplayName("TC-ADM-PROC-11: Authorization & Mapping - Bắt buộc @PreAuthorize(\"hasRole('ADMIN')\") và @PatchMapping(\"/{id}/process\")")
    void processPayoutRequest_PreAuthorize_RequiresRoleAdmin() throws NoSuchMethodException {
        Method method = AdminPayoutController.class.getMethod("processPayoutRequest", UUID.class, ProcessPayoutRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method processPayoutRequest phải có @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('ADMIN')"), "@PreAuthorize phải yêu cầu hasRole('ADMIN')");

        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        assertNotNull(patchMapping, "Method processPayoutRequest phải có @PatchMapping");
        assertArrayEquals(new String[]{"/{id}/process"}, patchMapping.value());
    }
}

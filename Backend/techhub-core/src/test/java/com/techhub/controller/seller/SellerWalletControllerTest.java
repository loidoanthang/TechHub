package com.techhub.controller.seller;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.payout.WithdrawalRequest;
import com.techhub.model.dto.response.payout.PayoutResponse;
import com.techhub.model.dto.response.wallet.SellerWalletTransactionResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.SellerWalletTransactionType;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.seller.SellerWalletService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerWalletController (POST /api/seller/wallet/withdraw)")
class SellerWalletControllerTest {

    private static Validator validator;

    @Mock
    private SellerWalletService sellerWalletService;

    @InjectMocks
    private SellerWalletController controller;

    private UUID userId;
    private User sellerUser;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userId = UUID.randomUUID();
        sellerUser = new User();
        sellerUser.setId(userId);
        sellerUser.setEmail("seller@techhub.vn");
        sellerUser.setStatus(UserStatus.ACTIVE);
        sellerUser.setRoles(Set.of(Role.SELLER));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("TC-WDR-01: Controller Happy Path - Gửi yêu cầu rút tiền thành công trả về 200 OK")
    void requestWithdrawal_Success_Returns200() {
        authenticateUser(sellerUser);
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000000.00"));

        PayoutResponse mockResponse = PayoutResponse.builder()
                .id(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .shopName("TechHub Store")
                .amount(new BigDecimal("1000000.00"))
                .bankName("Vietcombank")
                .bankAccountNumber("******7890")
                .bankAccountHolder("NGUYEN VAN A")
                .status(PayoutStatus.PENDING)
                .remainingBalance(new BigDecimal("4000000.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(sellerWalletService.requestWithdrawal(userId, request)).thenReturn(mockResponse);

        ApiResponse<PayoutResponse> response = controller.requestWithdrawal(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Withdrawal request submitted successfully", response.getMessage());
        assertEquals(mockResponse, response.getData());
        verify(sellerWalletService, times(1)).requestWithdrawal(userId, request);
    }

    @Test
    @DisplayName("TC-WDR-04: DTO Validation - Số tiền nhỏ hơn mức tối thiểu 50,000 VND (49,999.99 VND)")
    void withdrawalRequest_ValidationError_BelowMinimum() {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("49999.99"));
        Set<ConstraintViolation<WithdrawalRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must be at least 50,000 VND")));
    }

    @Test
    @DisplayName("TC-WDR-05: DTO Validation - Số tiền vượt mức tối đa 100,000,000 VND (100,000,000.01 VND)")
    void withdrawalRequest_ValidationError_AboveMaximum() {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("100000000.01"));
        Set<ConstraintViolation<WithdrawalRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 100,000,000 VND")));
    }

    @Test
    @DisplayName("TC-WDR-06: DTO Validation - Số tiền bị null")
    void withdrawalRequest_ValidationError_NullAmount() {
        WithdrawalRequest request = new WithdrawalRequest(null);
        Set<ConstraintViolation<WithdrawalRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Withdrawal amount is required")));
    }

    @Test
    @DisplayName("TC-WDR-07: DTO Validation - Số tiền <= 0 hoặc vượt quá chữ số thập phân cho phép")
    void withdrawalRequest_ValidationError_NegativeOrZeroOrInvalidDigits() {
        // Zero
        WithdrawalRequest zeroRequest = new WithdrawalRequest(BigDecimal.ZERO);
        Set<ConstraintViolation<WithdrawalRequest>> zeroViolations = validator.validate(zeroRequest);
        assertFalse(zeroViolations.isEmpty());

        // Negative
        WithdrawalRequest negativeRequest = new WithdrawalRequest(new BigDecimal("-100000.00"));
        Set<ConstraintViolation<WithdrawalRequest>> negativeViolations = validator.validate(negativeRequest);
        assertFalse(negativeViolations.isEmpty());

        // Invalid fraction (> 2 decimal places)
        WithdrawalRequest invalidDigitsRequest = new WithdrawalRequest(new BigDecimal("100000.123"));
        Set<ConstraintViolation<WithdrawalRequest>> digitViolations = validator.validate(invalidDigitsRequest);
        assertFalse(digitViolations.isEmpty());
        assertTrue(digitViolations.stream().anyMatch(v -> v.getMessage().contains("Withdrawal amount format is invalid")));
    }

    @Test
    @DisplayName("TC-WDR-16: Security - Request không có JWT Authentication bị từ chối")
    void requestWithdrawal_ThrowsUnauthenticated_WhenNoSecurityContext() {
        SecurityContextHolder.clearContext();
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000000.00"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                controller.requestWithdrawal(request));

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verifyNoInteractions(sellerWalletService);
    }

    @Test
    @DisplayName("TC-WDR-17: Authorization - Bắt buộc @PreAuthorize(\"hasRole('SELLER')\") và mapping đúng")
    void requestWithdrawal_PreAuthorize_RequiresRoleSeller() throws NoSuchMethodException {
        Method method = SellerWalletController.class.getMethod("requestWithdrawal", WithdrawalRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method requestWithdrawal phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('SELLER')"),
                "@PreAuthorize phải yêu cầu hasRole('SELLER')");

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "Method requestWithdrawal phải có annotation @PostMapping");
        assertArrayEquals(new String[]{"/withdraw"}, postMapping.value());

        RequestMapping classMapping = SellerWalletController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/api/seller/wallet"}, classMapping.value());
    }

    // =========================================================================
    // API 2: GET /api/seller/wallet/transactions (TC-LEDGER-01 đến TC-LEDGER-17)
    // =========================================================================

    @Test
    @DisplayName("TC-LEDGER-01: Controller Happy Path - Lấy danh sách giao dịch ví trả về 200 OK")
    void getWalletTransactions_Success_Returns200() {
        authenticateUser(sellerUser);
        SellerWalletTransactionResponse item = SellerWalletTransactionResponse.builder()
                .id(UUID.randomUUID())
                .type(SellerWalletTransactionType.ORDER_SETTLEMENT)
                .amount(new BigDecimal("1000000.00"))
                .balanceBefore(new BigDecimal("2000000.00"))
                .balanceAfter(new BigDecimal("3000000.00"))
                .referenceId(UUID.randomUUID())
                .description("Order settlement")
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<SellerWalletTransactionResponse> mockPage = new PageResponse<>(
                List.of(item), 0, 20, 1, 1, true
        );

        when(sellerWalletService.getWalletTransactions(userId, null, null, null, 0, 20, "DESC"))
                .thenReturn(mockPage);

        ApiResponse<PageResponse<SellerWalletTransactionResponse>> response = controller.getWalletTransactions(
                null, null, null, 0, 20, "DESC"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Wallet transactions retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().content().size());
        assertEquals(1, response.getData().totalElements());
        verify(sellerWalletService, times(1)).getWalletTransactions(
                userId, null, null, null, 0, 20, "DESC"
        );
    }

    @Test
    @DisplayName("TC-LEDGER-13: Validation - Page index âm bị từ chối (@Min(0))")
    void getWalletTransactions_ValidationError_NegativePage() throws NoSuchMethodException {
        ExecutableValidator execValidator = validator.forExecutables();
        Method method = SellerWalletController.class.getMethod("getWalletTransactions",
                SellerWalletTransactionType.class, LocalDate.class, LocalDate.class, int.class, int.class, String.class);

        Set<ConstraintViolation<SellerWalletController>> violations = execValidator.validateParameters(
                controller, method, new Object[]{null, null, null, -1, 20, "DESC"}
        );

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Page index must not be negative")));
    }

    @Test
    @DisplayName("TC-LEDGER-14: Validation - Page size <= 0 bị từ chối (@Min(1))")
    void getWalletTransactions_ValidationError_ZeroOrNegativeSize() throws NoSuchMethodException {
        ExecutableValidator execValidator = validator.forExecutables();
        Method method = SellerWalletController.class.getMethod("getWalletTransactions",
                SellerWalletTransactionType.class, LocalDate.class, LocalDate.class, int.class, int.class, String.class);

        Set<ConstraintViolation<SellerWalletController>> violations = execValidator.validateParameters(
                controller, method, new Object[]{null, null, null, 0, 0, "DESC"}
        );

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Page size must be at least 1")));
    }

    @Test
    @DisplayName("TC-LEDGER-15: Validation - Page size vượt quá 100 bị từ chối (@Max(100))")
    void getWalletTransactions_ValidationError_SizeExceedingMax() throws NoSuchMethodException {
        ExecutableValidator execValidator = validator.forExecutables();
        Method method = SellerWalletController.class.getMethod("getWalletTransactions",
                SellerWalletTransactionType.class, LocalDate.class, LocalDate.class, int.class, int.class, String.class);

        Set<ConstraintViolation<SellerWalletController>> violations = execValidator.validateParameters(
                controller, method, new Object[]{null, null, null, 0, 101, "DESC"}
        );

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Page size must not exceed 100")));
    }

    @Test
    @DisplayName("TC-LEDGER-17: Security - Request không có JWT Authentication bị từ chối")
    void getWalletTransactions_ThrowsUnauthenticated_WhenNoSecurityContext() {
        SecurityContextHolder.clearContext();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                controller.getWalletTransactions(null, null, null, 0, 20, "DESC"));

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verifyNoInteractions(sellerWalletService);
    }

    @Test
    @DisplayName("TC-LEDGER-17b: Authorization - Bắt buộc @PreAuthorize(\"hasRole('SELLER')\") và mapping đúng")
    void getWalletTransactions_PreAuthorize_RequiresRoleSeller() throws NoSuchMethodException {
        Method method = SellerWalletController.class.getMethod("getWalletTransactions",
                SellerWalletTransactionType.class, LocalDate.class, LocalDate.class, int.class, int.class, String.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getWalletTransactions phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('SELLER')"),
                "@PreAuthorize phải yêu cầu hasRole('SELLER')");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "Method getWalletTransactions phải có annotation @GetMapping");
        assertArrayEquals(new String[]{"/transactions"}, getMapping.value());
    }
}

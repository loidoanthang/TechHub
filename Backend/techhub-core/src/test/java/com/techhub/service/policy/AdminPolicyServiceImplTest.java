package com.techhub.service.policy;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.policy.UpdatePlatformPolicyRequest;
import com.techhub.model.dto.response.policy.PlatformPolicyResponse;
import com.techhub.model.entity.TransactionPolicy;
import com.techhub.model.entity.User;
import com.techhub.repository.TransactionPolicyRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.policy.impl.AdminPolicyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminPolicyServiceImpl (Platform Policies)")
class AdminPolicyServiceImplTest {

    @Mock
    private TransactionPolicyRepository transactionPolicyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminPolicyServiceImpl adminPolicyService;

    private User testAdminUser;
    private TransactionPolicy policyCommission;
    private TransactionPolicy policyTimeout;

    @BeforeEach
    void setUp() {
        testAdminUser = new User();
        testAdminUser.setId(UUID.randomUUID());
        testAdminUser.setFirstName("Admin");
        testAdminUser.setLastName("Super");
        testAdminUser.setEmail("admin@techhub.vn");

        policyCommission = TransactionPolicy.builder()
                .id(UUID.randomUUID())
                .policyKey("PLATFORM_COMMISSION_RATE")
                .policyValue("0.05")
                .description("Tỷ lệ hoa hồng chiết khấu sàn áp dụng cho Seller (5%)")
                .updatedBy(testAdminUser)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();

        policyTimeout = TransactionPolicy.builder()
                .id(UUID.randomUUID())
                .policyKey("VNPAY_PAYMENT_TIMEOUT_MINUTES")
                .policyValue("15")
                .description("Thời gian chờ thanh toán đơn hàng VNPAY trước khi tự hủy (phút)")
                .updatedBy(null)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();
    }

    @Test
    @DisplayName("TC-ADM-POL-01: Happy Path - Lấy danh sách chính sách sàn thành công khi DB đã có dữ liệu")
    void getAllPolicies_Success_WhenDataExists() {
        when(transactionPolicyRepository.findAllWithUpdatedByOrderByPolicyKeyAsc())
                .thenReturn(List.of(policyCommission, policyTimeout));

        List<PlatformPolicyResponse> result = adminPolicyService.getAllPolicies();

        assertNotNull(result);
        assertEquals(2, result.size());

        PlatformPolicyResponse first = result.get(0);
        assertEquals("PLATFORM_COMMISSION_RATE", first.policyKey());
        assertEquals("0.05", first.policyValue());
        assertEquals(testAdminUser.getId(), first.updatedById());
        assertEquals("Super Admin", first.updatedByName());

        PlatformPolicyResponse second = result.get(1);
        assertEquals("VNPAY_PAYMENT_TIMEOUT_MINUTES", second.policyKey());
        assertEquals("15", second.policyValue());
        assertNull(second.updatedById());
        assertNull(second.updatedByName());

        verify(transactionPolicyRepository, times(1)).findAllWithUpdatedByOrderByPolicyKeyAsc();
        verify(transactionPolicyRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("TC-ADM-POL-02: Tự động khởi tạo giá trị mặc định khi DB rỗng (Auto-seed Happy Path)")
    void getAllPolicies_Success_AutoSeedWhenEmpty() {
        when(transactionPolicyRepository.findAllWithUpdatedByOrderByPolicyKeyAsc())
                .thenReturn(Collections.emptyList());

        when(transactionPolicyRepository.saveAll(any())).thenAnswer(invocation -> {
            List<TransactionPolicy> list = invocation.getArgument(0);
            return list.stream().peek(p -> p.setId(UUID.randomUUID())).toList();
        });

        List<PlatformPolicyResponse> result = adminPolicyService.getAllPolicies();

        assertNotNull(result);
        assertEquals(5, result.size());

        ArgumentCaptor<List<TransactionPolicy>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionPolicyRepository).saveAll(captor.capture());
        List<TransactionPolicy> savedList = captor.getValue();
        assertEquals(5, savedList.size());

        assertTrue(savedList.stream().anyMatch(p -> "PLATFORM_COMMISSION_RATE".equals(p.getPolicyKey()) && "0.05".equals(p.getPolicyValue())));
        assertTrue(savedList.stream().anyMatch(p -> "VNPAY_PAYMENT_TIMEOUT_MINUTES".equals(p.getPolicyKey()) && "15".equals(p.getPolicyValue())));
        assertTrue(savedList.stream().anyMatch(p -> "ORDER_AUTO_COMPLETE_HOURS".equals(p.getPolicyKey()) && "72".equals(p.getPolicyValue())));
        assertTrue(savedList.stream().anyMatch(p -> "RETURN_REQUEST_WINDOW_HOURS".equals(p.getPolicyKey()) && "72".equals(p.getPolicyValue())));
        assertTrue(savedList.stream().anyMatch(p -> "AUTO_APPROVE_RETURN_HOURS".equals(p.getPolicyKey()) && "72".equals(p.getPolicyValue())));
    }

    @Test
    @DisplayName("TC-ADM-POL-03: Hiển thị đầy đủ thông tin kiểm toán updatedBy khi chính sách đã được cập nhật")
    void getAllPolicies_Success_AuditInformationMapped() {
        when(transactionPolicyRepository.findAllWithUpdatedByOrderByPolicyKeyAsc())
                .thenReturn(List.of(policyCommission));

        List<PlatformPolicyResponse> result = adminPolicyService.getAllPolicies();

        assertEquals(1, result.size());
        assertEquals("Super Admin", result.get(0).updatedByName());
        assertEquals(testAdminUser.getId(), result.get(0).updatedById());
    }

    @Test
    @DisplayName("TC-ADM-POL-04: Xử lý an toàn khi updatedBy là null (chính sách mặc định chưa từng sửa)")
    void getAllPolicies_Success_NullUpdatedByHandledSafely() {
        when(transactionPolicyRepository.findAllWithUpdatedByOrderByPolicyKeyAsc())
                .thenReturn(List.of(policyTimeout));

        List<PlatformPolicyResponse> result = adminPolicyService.getAllPolicies();

        assertEquals(1, result.size());
        assertNull(result.get(0).updatedById());
        assertNull(result.get(0).updatedByName());
    }

    @Test
    @DisplayName("TC-ADM-POL-05: Fallback hiển thị email khi Admin chưa cập nhật firstName/lastName")
    void getAllPolicies_Success_FallbackToEmailWhenNoName() {
        User emailOnlyAdmin = new User();
        emailOnlyAdmin.setId(UUID.randomUUID());
        emailOnlyAdmin.setEmail("moderator@techhub.vn");

        TransactionPolicy policy = TransactionPolicy.builder()
                .id(UUID.randomUUID())
                .policyKey("CUSTOM_POLICY")
                .policyValue("100")
                .description("Custom")
                .updatedBy(emailOnlyAdmin)
                .build();

        when(transactionPolicyRepository.findAllWithUpdatedByOrderByPolicyKeyAsc())
                .thenReturn(List.of(policy));

        List<PlatformPolicyResponse> result = adminPolicyService.getAllPolicies();

        assertEquals(1, result.size());
        assertEquals("moderator@techhub.vn", result.get(0).updatedByName());
    }

    @Test
    @DisplayName("TC-ADM-POL-06: Sắp xếp danh sách chính sách theo thứ tự bảng chữ cái policyKey ASC")
    void getAllPolicies_Success_OrderingPreserved() {
        TransactionPolicy polA = TransactionPolicy.builder().id(UUID.randomUUID()).policyKey("A_KEY").policyValue("1").build();
        TransactionPolicy polB = TransactionPolicy.builder().id(UUID.randomUUID()).policyKey("B_KEY").policyValue("2").build();
        TransactionPolicy polC = TransactionPolicy.builder().id(UUID.randomUUID()).policyKey("C_KEY").policyValue("3").build();

        when(transactionPolicyRepository.findAllWithUpdatedByOrderByPolicyKeyAsc())
                .thenReturn(List.of(polA, polB, polC));

        List<PlatformPolicyResponse> result = adminPolicyService.getAllPolicies();

        assertEquals(3, result.size());
        assertEquals("A_KEY", result.get(0).policyKey());
        assertEquals("B_KEY", result.get(1).policyKey());
        assertEquals("C_KEY", result.get(2).policyKey());
    }

    // =========================================================================
    // UNIT TESTS CHO UPDATE PLATFORM POLICY (PUT /api/admin/policies)
    // =========================================================================

    @Test
    @DisplayName("TC-ADM-UPOL-01: Happy Path - Cập nhật tỷ lệ hoa hồng PLATFORM_COMMISSION_RATE thành công")
    void updatePolicy_Success_CommissionRate() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "PLATFORM_COMMISSION_RATE", "0.08", "Tăng hoa hồng lên 8%"
        );

        when(transactionPolicyRepository.findForUpdateByPolicyKey("PLATFORM_COMMISSION_RATE"))
                .thenReturn(Optional.of(policyCommission));
        when(userRepository.findById(testAdminUser.getId()))
                .thenReturn(Optional.of(testAdminUser));
        when(transactionPolicyRepository.save(any(TransactionPolicy.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PlatformPolicyResponse response = adminPolicyService.updatePolicy(testAdminUser.getId(), request);

        assertNotNull(response);
        assertEquals("PLATFORM_COMMISSION_RATE", response.policyKey());
        assertEquals("0.08", response.policyValue());
        assertEquals("Tăng hoa hồng lên 8%", response.description());
        assertEquals(testAdminUser.getId(), response.updatedById());
        assertEquals("Super Admin", response.updatedByName());

        verify(transactionPolicyRepository).findForUpdateByPolicyKey("PLATFORM_COMMISSION_RATE");
        verify(transactionPolicyRepository).save(policyCommission);
    }

    @Test
    @DisplayName("TC-ADM-UPOL-02: Happy Path - Cập nhật timeout thanh toán VNPAY_PAYMENT_TIMEOUT_MINUTES thành công")
    void updatePolicy_Success_VnpayTimeout() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "VNPAY_PAYMENT_TIMEOUT_MINUTES", "30", "Tăng thời gian giữ đơn lên 30 phút"
        );

        when(transactionPolicyRepository.findForUpdateByPolicyKey("VNPAY_PAYMENT_TIMEOUT_MINUTES"))
                .thenReturn(Optional.of(policyTimeout));
        when(userRepository.findById(testAdminUser.getId()))
                .thenReturn(Optional.of(testAdminUser));
        when(transactionPolicyRepository.save(any(TransactionPolicy.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PlatformPolicyResponse response = adminPolicyService.updatePolicy(testAdminUser.getId(), request);

        assertNotNull(response);
        assertEquals("VNPAY_PAYMENT_TIMEOUT_MINUTES", response.policyKey());
        assertEquals("30", response.policyValue());
        assertEquals("Tăng thời gian giữ đơn lên 30 phút", response.description());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-03: Happy Path - Tự động trim khoảng trắng thừa ở policyKey, policyValue và description")
    void updatePolicy_Success_TrimWhitespace() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "  PLATFORM_COMMISSION_RATE  ", "  0.10  ", "  Khoảng trắng hai đầu  "
        );

        when(transactionPolicyRepository.findForUpdateByPolicyKey("PLATFORM_COMMISSION_RATE"))
                .thenReturn(Optional.of(policyCommission));
        when(userRepository.findById(testAdminUser.getId()))
                .thenReturn(Optional.of(testAdminUser));
        when(transactionPolicyRepository.save(any(TransactionPolicy.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PlatformPolicyResponse response = adminPolicyService.updatePolicy(testAdminUser.getId(), request);

        assertEquals("0.10", response.policyValue());
        assertEquals("Khoảng trắng hai đầu", response.description());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-04: Thất bại - PLATFORM_COMMISSION_RATE là số âm ném INVALID_POLICY_VALUE")
    void updatePolicy_ThrowsException_InvalidCommissionRateNegative() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "PLATFORM_COMMISSION_RATE", "-0.01", "Invalid negative"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), request)
        );
        assertEquals(ErrorCode.INVALID_POLICY_VALUE, ex.getErrorCode());
        verify(transactionPolicyRepository, never()).findForUpdateByPolicyKey(any());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-05: Thất bại - PLATFORM_COMMISSION_RATE vượt quá 1.0 (100%) ném INVALID_POLICY_VALUE")
    void updatePolicy_ThrowsException_InvalidCommissionRateExceedsOne() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "PLATFORM_COMMISSION_RATE", "1.05", "Exceeds 100%"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), request)
        );
        assertEquals(ErrorCode.INVALID_POLICY_VALUE, ex.getErrorCode());
        verify(transactionPolicyRepository, never()).findForUpdateByPolicyKey(any());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-06: Thất bại - PLATFORM_COMMISSION_RATE không phải định dạng số ném INVALID_POLICY_VALUE")
    void updatePolicy_ThrowsException_InvalidCommissionRateNonNumeric() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "PLATFORM_COMMISSION_RATE", "not_a_number", "Invalid format"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), request)
        );
        assertEquals(ErrorCode.INVALID_POLICY_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-07: Thất bại - VNPAY_PAYMENT_TIMEOUT_MINUTES dưới ngưỡng 5 phút ném INVALID_POLICY_VALUE")
    void updatePolicy_ThrowsException_InvalidVnpayTimeoutTooSmall() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "VNPAY_PAYMENT_TIMEOUT_MINUTES", "4", "Too small"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), request)
        );
        assertEquals(ErrorCode.INVALID_POLICY_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-08: Thất bại - VNPAY_PAYMENT_TIMEOUT_MINUTES vượt quá 1440 phút (24h) ném INVALID_POLICY_VALUE")
    void updatePolicy_ThrowsException_InvalidVnpayTimeoutTooLarge() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "VNPAY_PAYMENT_TIMEOUT_MINUTES", "1441", "Too large"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), request)
        );
        assertEquals(ErrorCode.INVALID_POLICY_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-09: Thất bại - ORDER_AUTO_COMPLETE_HOURS ngoài phạm vi 12-720 giờ ném INVALID_POLICY_VALUE")
    void updatePolicy_ThrowsException_InvalidAutoCompleteHours() {
        UpdatePlatformPolicyRequest requestTooSmall = new UpdatePlatformPolicyRequest(
                "ORDER_AUTO_COMPLETE_HOURS", "11", "Too small"
        );
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), requestTooSmall)
        );
        assertEquals(ErrorCode.INVALID_POLICY_VALUE, ex1.getErrorCode());

        UpdatePlatformPolicyRequest requestTooLarge = new UpdatePlatformPolicyRequest(
                "ORDER_AUTO_COMPLETE_HOURS", "721", "Too large"
        );
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), requestTooLarge)
        );
        assertEquals(ErrorCode.INVALID_POLICY_VALUE, ex2.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-10: Thất bại - RETURN_REQUEST_WINDOW_HOURS dưới 24 giờ ném INVALID_POLICY_VALUE")
    void updatePolicy_ThrowsException_InvalidReturnWindowHours() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "RETURN_REQUEST_WINDOW_HOURS", "23", "Too small"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), request)
        );
        assertEquals(ErrorCode.INVALID_POLICY_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-11: Thất bại - AUTO_APPROVE_RETURN_HOURS vượt quá 720 giờ ném INVALID_POLICY_VALUE")
    void updatePolicy_ThrowsException_InvalidAutoApproveReturnHours() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "AUTO_APPROVE_RETURN_HOURS", "721", "Too large"
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), request)
        );
        assertEquals(ErrorCode.INVALID_POLICY_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-12: Thất bại - Không tìm thấy policyKey trong DB ném POLICY_NOT_FOUND (404)")
    void updatePolicy_ThrowsException_PolicyNotFound() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "NON_EXISTING_KEY", "100", "Unknown key"
        );

        when(transactionPolicyRepository.findForUpdateByPolicyKey("NON_EXISTING_KEY"))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), request)
        );
        assertEquals(ErrorCode.POLICY_NOT_FOUND, ex.getErrorCode());
        verify(transactionPolicyRepository).findForUpdateByPolicyKey("NON_EXISTING_KEY");
        verify(transactionPolicyRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-ADM-UPOL-13: Thất bại - Admin User không tồn tại ném USER_NOT_FOUND")
    void updatePolicy_ThrowsException_AdminUserNotFound() {
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "PLATFORM_COMMISSION_RATE", "0.06", "Valid update"
        );

        when(transactionPolicyRepository.findForUpdateByPolicyKey("PLATFORM_COMMISSION_RATE"))
                .thenReturn(Optional.of(policyCommission));
        when(userRepository.findById(testAdminUser.getId()))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPolicyService.updatePolicy(testAdminUser.getId(), request)
        );
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(transactionPolicyRepository, never()).save(any());
    }
}

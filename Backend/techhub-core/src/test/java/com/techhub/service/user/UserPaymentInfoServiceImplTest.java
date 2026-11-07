package com.techhub.service.user;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreatePaymentInfoRequest;
import com.techhub.model.dto.response.PaymentInfoResponse;
import com.techhub.model.entity.User;
import com.techhub.model.entity.UserPaymentInfo;
import com.techhub.model.enums.PaymentType;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.UserPaymentInfoRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.user.impl.UserPaymentInfoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho UserPaymentInfoServiceImpl (POST /api/users/payment-info)")
class UserPaymentInfoServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPaymentInfoRepository userPaymentInfoRepository;

    @InjectMocks
    private UserPaymentInfoServiceImpl paymentInfoService;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = new User();
        activeUser.setId(userId);
        activeUser.setEmail("buyer@techhub.com");
        activeUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("TC-01: createPaymentInfo_Success_FirstAccount_AutoDefault - Tài khoản đầu tiên tự động thành mặc định")
    void createPaymentInfo_Success_FirstAccount_AutoDefault() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                "9876543210123",
                "NGUYEN VAN AN",
                null,
                false // Client gửi false nhưng là tài khoản đầu tiên
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(0L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                eq(userId), eq("Vietcombank"), eq("9876543210123"))).thenReturn(false);

        when(userPaymentInfoRepository.saveAndFlush(any(UserPaymentInfo.class))).thenAnswer(invocation -> {
            UserPaymentInfo entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });

        PaymentInfoResponse response = paymentInfoService.createPaymentInfo(userId, request);

        assertNotNull(response);
        assertEquals("Vietcombank", response.provider());
        assertEquals("NGUYEN VAN AN", response.accountHolderName());
        assertTrue(response.isDefault(), "Tài khoản đầu tiên bắt buộc phải là mặc định");
        assertEquals("*********0123", response.accountNumber(), "Số tài khoản phải được mask an toàn");

        verify(userPaymentInfoRepository, times(1)).resetDefaultByUserId(userId);
        verify(userPaymentInfoRepository, times(1)).saveAndFlush(any(UserPaymentInfo.class));
    }

    @Test
    @DisplayName("TC-02: createPaymentInfo_Success_SecondAccount_NonDefault - Tài khoản thứ 2 với isDefault=false không làm mất mặc định cũ")
    void createPaymentInfo_Success_SecondAccount_NonDefault() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.E_WALLET,
                "MoMo",
                "0901234567",
                "NGUYEN VAN AN",
                null,
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                eq(userId), eq("MoMo"), eq("0901234567"))).thenReturn(false);

        when(userPaymentInfoRepository.saveAndFlush(any(UserPaymentInfo.class))).thenAnswer(invocation -> {
            UserPaymentInfo entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        PaymentInfoResponse response = paymentInfoService.createPaymentInfo(userId, request);

        assertNotNull(response);
        assertFalse(response.isDefault(), "Tài khoản thứ 2 phải mang isDefault=false");
        assertEquals("******4567", response.accountNumber());

        verify(userPaymentInfoRepository, never()).resetDefaultByUserId(any());
    }

    @Test
    @DisplayName("TC-03: createPaymentInfo_Success_SwitchDefault - Tài khoản mới với isDefault=true reset tài khoản cũ")
    void createPaymentInfo_Success_SwitchDefault() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.BANK_ACCOUNT,
                "Techcombank",
                "1903333333333",
                "NGUYEN VAN AN",
                null,
                true
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(2L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                eq(userId), eq("Techcombank"), eq("1903333333333"))).thenReturn(false);

        when(userPaymentInfoRepository.saveAndFlush(any(UserPaymentInfo.class))).thenAnswer(invocation -> {
            UserPaymentInfo entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PaymentInfoResponse response = paymentInfoService.createPaymentInfo(userId, request);

        assertNotNull(response);
        assertTrue(response.isDefault(), "Tài khoản mới phải có isDefault = true");

        verify(userPaymentInfoRepository, times(1)).resetDefaultByUserId(userId);
    }

    @Test
    @DisplayName("TC-04: createPaymentInfo_Success_CreditCard_ValidExpiry - Thêm thẻ tín dụng với hạn thẻ hợp lệ")
    void createPaymentInfo_Success_CreditCard_ValidExpiry() {
        YearMonth futureExpiry = YearMonth.now().plusYears(2);
        String expiryString = String.format("%02d/%d", futureExpiry.getMonthValue(), futureExpiry.getYear());

        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.CREDIT_CARD,
                "Visa",
                "4111222233334444",
                "NGUYEN VAN AN",
                expiryString,
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                eq(userId), eq("Visa"), eq("4111222233334444"))).thenReturn(false);

        when(userPaymentInfoRepository.saveAndFlush(any(UserPaymentInfo.class))).thenAnswer(invocation -> {
            UserPaymentInfo entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PaymentInfoResponse response = paymentInfoService.createPaymentInfo(userId, request);

        assertNotNull(response);
        assertEquals(PaymentType.CREDIT_CARD, response.paymentType());
        assertEquals(expiryString, response.cardExpiry());
        assertEquals("************4444", response.accountNumber());
    }

    @Test
    @DisplayName("TC-05: createPaymentInfo_Success_SanitizeAccountNumber - Xóa sạch khoảng trắng và dấu gạch ngang trong số tài khoản")
    void createPaymentInfo_Success_SanitizeAccountNumber() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                " 9704 2292 1234 5678 ",
                "nguyen van an",
                null,
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                eq(userId), eq("Vietcombank"), eq("9704229212345678"))).thenReturn(false);

        when(userPaymentInfoRepository.saveAndFlush(any(UserPaymentInfo.class))).thenAnswer(invocation -> {
            UserPaymentInfo entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PaymentInfoResponse response = paymentInfoService.createPaymentInfo(userId, request);

        ArgumentCaptor<UserPaymentInfo> captor = ArgumentCaptor.forClass(UserPaymentInfo.class);
        verify(userPaymentInfoRepository).saveAndFlush(captor.capture());

        assertEquals("9704229212345678", captor.getValue().getAccountNumber(), "Số tài khoản lưu DB phải sạch khoảng trắng");
        assertEquals("************5678", response.accountNumber());
    }

    @Test
    @DisplayName("TC-06: createPaymentInfo_Success_NormalizeHolderName - Chuẩn hóa tên chủ tài khoản thành IN HOA")
    void createPaymentInfo_Success_NormalizeHolderName() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.BANK_ACCOUNT,
                "MBBank",
                "1234567890",
                " nguyen van an ",
                null,
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                eq(userId), eq("MBBank"), eq("1234567890"))).thenReturn(false);

        when(userPaymentInfoRepository.saveAndFlush(any(UserPaymentInfo.class))).thenAnswer(invocation -> {
            UserPaymentInfo entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PaymentInfoResponse response = paymentInfoService.createPaymentInfo(userId, request);

        assertEquals("NGUYEN VAN AN", response.accountHolderName(), "Tên chủ tài khoản phải được in hoa và trim sạch sẽ");
    }

    @Test
    @DisplayName("TC-07: createPaymentInfo_Failed_LimitExceeded - Ném PAYMENT_INFO_LIMIT_EXCEEDED khi đã đạt tối đa 5 tài khoản")
    void createPaymentInfo_Failed_LimitExceeded() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                "1234567890",
                "NGUYEN VAN AN",
                null,
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(5L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.createPaymentInfo(userId, request)
        );

        assertEquals(ErrorCode.PAYMENT_INFO_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC-08: createPaymentInfo_Failed_DuplicateAccount - Ném DUPLICATE_PAYMENT_INFO khi cùng provider và số tài khoản")
    void createPaymentInfo_Failed_DuplicateAccount() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                "9876543210123",
                "NGUYEN VAN AN",
                null,
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                eq(userId), eq("Vietcombank"), eq("9876543210123"))).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.createPaymentInfo(userId, request)
        );

        assertEquals(ErrorCode.DUPLICATE_PAYMENT_INFO, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC-09: createPaymentInfo_Failed_CreditCardMissingExpiry - CREDIT_CARD thiếu cardExpiry ném INVALID_INPUT_DATA")
    void createPaymentInfo_Failed_CreditCardMissingExpiry() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.CREDIT_CARD,
                "Visa",
                "4111222233334444",
                "NGUYEN VAN AN",
                null, // Thiếu expiry
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                any(), any(), any())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.createPaymentInfo(userId, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: createPaymentInfo_Failed_CreditCardExpired - Thẻ tín dụng đã hết hạn ném INVALID_CARD_EXPIRY")
    void createPaymentInfo_Failed_CreditCardExpired() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.CREDIT_CARD,
                "Visa",
                "4111222233334444",
                "NGUYEN VAN AN",
                "01/2020", // Quá khứ
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                any(), any(), any())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.createPaymentInfo(userId, request)
        );

        assertEquals(ErrorCode.INVALID_CARD_EXPIRY, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: createPaymentInfo_Failed_CreditCardInvalidExpiryFormat - Hạn thẻ sai định dạng regex ném INVALID_CARD_EXPIRY")
    void createPaymentInfo_Failed_CreditCardInvalidExpiryFormat() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.CREDIT_CARD,
                "Visa",
                "4111222233334444",
                "NGUYEN VAN AN",
                "13/2029", // Tháng 13 không hợp lệ
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                any(), any(), any())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.createPaymentInfo(userId, request)
        );

        assertEquals(ErrorCode.INVALID_CARD_EXPIRY, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: createPaymentInfo_Sanitize_CardExpiryNullForBankAccount - BANK_ACCOUNT có cardExpiry tự động bị gán null")
    void createPaymentInfo_Sanitize_CardExpiryNullForBankAccount() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                "1234567890",
                "NGUYEN VAN AN",
                "12/2028", // Client truyền thừa
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                any(), any(), any())).thenReturn(false);

        when(userPaymentInfoRepository.saveAndFlush(any(UserPaymentInfo.class))).thenAnswer(invocation -> {
            UserPaymentInfo entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PaymentInfoResponse response = paymentInfoService.createPaymentInfo(userId, request);

        ArgumentCaptor<UserPaymentInfo> captor = ArgumentCaptor.forClass(UserPaymentInfo.class);
        verify(userPaymentInfoRepository).saveAndFlush(captor.capture());

        assertNull(captor.getValue().getCardExpiry(), "Tài khoản ngân hàng phải lưu cardExpiry = null");
        assertNull(response.cardExpiry());
    }

    @Test
    @DisplayName("TC-15: createPaymentInfo_Failed_ConcurrentDuplicate - Bắt DataIntegrityViolationException và ném DUPLICATE_PAYMENT_INFO")
    void createPaymentInfo_Failed_ConcurrentDuplicate() {
        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                "9876543210123",
                "NGUYEN VAN AN",
                null,
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserId(userId)).thenReturn(1L);
        when(userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                eq(userId), eq("Vietcombank"), eq("9876543210123"))).thenReturn(false);

        when(userPaymentInfoRepository.saveAndFlush(any(UserPaymentInfo.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint uk_user_payment_info_user_provider_account violation"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.createPaymentInfo(userId, request)
        );

        assertEquals(ErrorCode.DUPLICATE_PAYMENT_INFO, ex.getErrorCode());
    }

    // ==========================================
    // TESTS FOR GET /api/users/payment-info
    // ==========================================

    @Test
    @DisplayName("TC-16: getPaymentInfos_Success_MultipleAccountsWithDefaultFirst - Trả về danh sách tài khoản với Default đứng đầu")
    void getPaymentInfos_Success_MultipleAccountsWithDefaultFirst() {
        UserPaymentInfo account1 = UserPaymentInfo.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .paymentType(PaymentType.BANK_ACCOUNT)
                .provider("Vietcombank")
                .accountNumber("9876543210123")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry(null)
                .isDefault(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        UserPaymentInfo account2 = UserPaymentInfo.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .paymentType(PaymentType.E_WALLET)
                .provider("MoMo")
                .accountNumber("0987654321")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry(null)
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserPaymentInfo account3 = UserPaymentInfo.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .paymentType(PaymentType.CREDIT_CARD)
                .provider("Techcombank")
                .accountNumber("4111222233334444")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry("12/2028")
                .isDefault(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId))
                .thenReturn(List.of(account1, account2, account3));

        List<PaymentInfoResponse> result = paymentInfoService.getPaymentInfos(userId);

        assertNotNull(result);
        assertEquals(3, result.size());

        // Kiểm tra phần tử đầu tiên là default
        PaymentInfoResponse first = result.get(0);
        assertTrue(first.isDefault());
        assertEquals("Vietcombank", first.provider());
        assertEquals("*********0123", first.accountNumber());
        assertNull(first.cardExpiry());

        // Kiểm tra phần tử thứ 2
        PaymentInfoResponse second = result.get(1);
        assertFalse(second.isDefault());
        assertEquals("MoMo", second.provider());
        assertEquals("******4321", second.accountNumber());

        // Kiểm tra phần tử thứ 3 (Credit card)
        PaymentInfoResponse third = result.get(2);
        assertFalse(third.isDefault());
        assertEquals("Techcombank", third.provider());
        assertEquals("************4444", third.accountNumber());
        assertEquals("12/2028", third.cardExpiry());

        verify(userPaymentInfoRepository, times(1)).findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Test
    @DisplayName("TC-17: getPaymentInfos_Success_EmptyList - Người dùng chưa lưu tài khoản nào trả về list rỗng")
    void getPaymentInfos_Success_EmptyList() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId))
                .thenReturn(Collections.emptyList());

        List<PaymentInfoResponse> result = paymentInfoService.getPaymentInfos(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userPaymentInfoRepository, times(1)).findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Test
    @DisplayName("TC-18: getPaymentInfos_Success_SingleDefaultAccount - Người dùng có 1 tài khoản mặc định")
    void getPaymentInfos_Success_SingleDefaultAccount() {
        UserPaymentInfo account = UserPaymentInfo.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .paymentType(PaymentType.BANK_ACCOUNT)
                .provider("MBBank")
                .accountNumber("1234567890123")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry(null)
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId))
                .thenReturn(List.of(account));

        List<PaymentInfoResponse> result = paymentInfoService.getPaymentInfos(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isDefault());
        assertEquals("MBBank", result.get(0).provider());
    }

    @Test
    @DisplayName("TC-19: getPaymentInfos_Success_DataMaskingVerified - Che giấu số tài khoản an toàn")
    void getPaymentInfos_Success_DataMaskingVerified() {
        UserPaymentInfo account = UserPaymentInfo.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .paymentType(PaymentType.BANK_ACCOUNT)
                .provider("ACB")
                .accountNumber("1234567890")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry(null)
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId))
                .thenReturn(List.of(account));

        List<PaymentInfoResponse> result = paymentInfoService.getPaymentInfos(userId);

        assertNotNull(result);
        assertEquals("******7890", result.get(0).accountNumber());
        assertFalse(result.get(0).accountNumber().contains("123456"));
    }

    @Test
    @DisplayName("TC-20: getPaymentInfos_Success_ShortAccountNumberMasking - Số tài khoản ngắn <= 4 ký tự mask toàn bộ")
    void getPaymentInfos_Success_ShortAccountNumberMasking() {
        UserPaymentInfo account = UserPaymentInfo.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .paymentType(PaymentType.BANK_ACCOUNT)
                .provider("CustomBank")
                .accountNumber("123")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry(null)
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId))
                .thenReturn(List.of(account));

        List<PaymentInfoResponse> result = paymentInfoService.getPaymentInfos(userId);

        assertNotNull(result);
        assertEquals("***", result.get(0).accountNumber());
    }

    @Test
    @DisplayName("TC-21: getPaymentInfos_Success_CreditCardExpiryPresent - Thẻ tín dụng hiển thị hạn sử dụng")
    void getPaymentInfos_Success_CreditCardExpiryPresent() {
        UserPaymentInfo card = UserPaymentInfo.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .paymentType(PaymentType.CREDIT_CARD)
                .provider("VPBank")
                .accountNumber("5123456789012345")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry("10/2029")
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId))
                .thenReturn(List.of(card));

        List<PaymentInfoResponse> result = paymentInfoService.getPaymentInfos(userId);

        assertNotNull(result);
        assertEquals("10/2029", result.get(0).cardExpiry());
    }

    @Test
    @DisplayName("TC-22: getPaymentInfos_Success_BankAccountExpiryIsNull - Tài khoản ngân hàng cardExpiry là null")
    void getPaymentInfos_Success_BankAccountExpiryIsNull() {
        UserPaymentInfo bank = UserPaymentInfo.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .paymentType(PaymentType.BANK_ACCOUNT)
                .provider("BIDV")
                .accountNumber("12410001234567")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry(null)
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId))
                .thenReturn(List.of(bank));

        List<PaymentInfoResponse> result = paymentInfoService.getPaymentInfos(userId);

        assertNotNull(result);
        assertNull(result.get(0).cardExpiry());
    }

    @Test
    @DisplayName("TC-23: getPaymentInfos_Failed_UserNotFound - Ném USER_NOT_FOUND khi không tìm thấy Buyer")
    void getPaymentInfos_Failed_UserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.getPaymentInfos(userId)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).findByUserIdOrderByIsDefaultDescCreatedAtDesc(any());
    }

    @Test
    @DisplayName("TC-24: getPaymentInfos_Failed_UserDeactivated - Ném ACCESS_DENIED khi Buyer bị khóa hoặc xóa")
    void getPaymentInfos_Failed_UserDeactivated() {
        User bannedUser = new User();
        bannedUser.setId(userId);
        bannedUser.setEmail("banned@techhub.com");
        bannedUser.setStatus(UserStatus.BANNED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(bannedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.getPaymentInfos(userId)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).findByUserIdOrderByIsDefaultDescCreatedAtDesc(any());
    }

    @Test
    @DisplayName("TC-27: getPaymentInfos_Failed_UserSuspended - Ném ACCESS_DENIED khi Buyer bị tạm ngưng (SUSPENDED)")
    void getPaymentInfos_Failed_UserSuspended() {
        User suspendedUser = new User();
        suspendedUser.setId(userId);
        suspendedUser.setEmail("suspended@techhub.com");
        suspendedUser.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.getPaymentInfos(userId)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).findByUserIdOrderByIsDefaultDescCreatedAtDesc(any());
    }

    // ==========================================
    // TESTS FOR PATCH /api/users/payment-info/{id}/default
    // ==========================================

    @Test
    @DisplayName("TC-28: setDefaultPaymentInfo_Success_SwitchFromOtherDefault - Chuyển đổi tài khoản mặc định thành công")
    void setDefaultPaymentInfo_Success_SwitchFromOtherDefault() {
        UUID paymentInfoId = UUID.randomUUID();
        UserPaymentInfo accountB = UserPaymentInfo.builder()
                .id(paymentInfoId)
                .user(activeUser)
                .paymentType(PaymentType.BANK_ACCOUNT)
                .provider("Vietcombank")
                .accountNumber("9876543210123")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry(null)
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(paymentInfoId, userId))
                .thenReturn(Optional.of(accountB));
        when(userPaymentInfoRepository.save(any(UserPaymentInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInfoResponse response = paymentInfoService.setDefaultPaymentInfo(userId, paymentInfoId);

        assertNotNull(response);
        assertTrue(response.isDefault());
        assertEquals("Vietcombank", response.provider());
        assertEquals("*********0123", response.accountNumber());

        verify(userPaymentInfoRepository, times(1)).resetDefaultByUserId(userId);
        verify(userPaymentInfoRepository, times(1)).save(accountB);
    }

    @Test
    @DisplayName("TC-29: setDefaultPaymentInfo_Success_AlreadyDefault_Idempotent - Đã là mặc định sẵn thì trả về ngay (Idempotent)")
    void setDefaultPaymentInfo_Success_AlreadyDefault_Idempotent() {
        UUID paymentInfoId = UUID.randomUUID();
        UserPaymentInfo accountA = UserPaymentInfo.builder()
                .id(paymentInfoId)
                .user(activeUser)
                .paymentType(PaymentType.E_WALLET)
                .provider("MoMo")
                .accountNumber("0987654321")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry(null)
                .isDefault(true) // Đã là mặc định
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(paymentInfoId, userId))
                .thenReturn(Optional.of(accountA));

        PaymentInfoResponse response = paymentInfoService.setDefaultPaymentInfo(userId, paymentInfoId);

        assertNotNull(response);
        assertTrue(response.isDefault());
        assertEquals("MoMo", response.provider());

        // Đảm bảo không gọi reset hoặc update thừa
        verify(userPaymentInfoRepository, never()).resetDefaultByUserId(any());
        verify(userPaymentInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-30: setDefaultPaymentInfo_Success_DataMaskingVerified - Số tài khoản trong response được che giấu an toàn")
    void setDefaultPaymentInfo_Success_DataMaskingVerified() {
        UUID paymentInfoId = UUID.randomUUID();
        UserPaymentInfo account = UserPaymentInfo.builder()
                .id(paymentInfoId)
                .user(activeUser)
                .paymentType(PaymentType.CREDIT_CARD)
                .provider("Techcombank")
                .accountNumber("4111222233334444")
                .accountHolderName("NGUYEN VAN AN")
                .cardExpiry("12/2028")
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(paymentInfoId, userId))
                .thenReturn(Optional.of(account));
        when(userPaymentInfoRepository.save(any(UserPaymentInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInfoResponse response = paymentInfoService.setDefaultPaymentInfo(userId, paymentInfoId);

        assertNotNull(response);
        assertEquals("************4444", response.accountNumber());
        assertEquals("12/2028", response.cardExpiry());
        assertTrue(response.isDefault());
    }

    @Test
    @DisplayName("TC-31: setDefaultPaymentInfo_Failed_PaymentInfoNotFound - Ném PAYMENT_INFO_NOT_FOUND khi id không tồn tại")
    void setDefaultPaymentInfo_Failed_PaymentInfoNotFound() {
        UUID randomId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(randomId, userId))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.setDefaultPaymentInfo(userId, randomId)
        );

        assertEquals(ErrorCode.PAYMENT_INFO_NOT_FOUND, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).resetDefaultByUserId(any());
        verify(userPaymentInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-32: setDefaultPaymentInfo_Failed_IDOR_BelongsToAnotherUser - Ném PAYMENT_INFO_NOT_FOUND khi tài khoản thuộc user khác")
    void setDefaultPaymentInfo_Failed_IDOR_BelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        UUID otherPaymentInfoId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        // findByIdAndUserId lọc theo userId của người gọi, trả về empty khi id thuộc user khác
        when(userPaymentInfoRepository.findByIdAndUserId(otherPaymentInfoId, userId))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.setDefaultPaymentInfo(userId, otherPaymentInfoId)
        );

        assertEquals(ErrorCode.PAYMENT_INFO_NOT_FOUND, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).resetDefaultByUserId(any());
        verify(userPaymentInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-33: setDefaultPaymentInfo_Failed_UserNotFound - Ném USER_NOT_FOUND khi Buyer không tồn tại")
    void setDefaultPaymentInfo_Failed_UserNotFound() {
        UUID paymentInfoId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.setDefaultPaymentInfo(userId, paymentInfoId)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("TC-34: setDefaultPaymentInfo_Failed_UserBanned - Ném ACCESS_DENIED khi Buyer bị BANNED")
    void setDefaultPaymentInfo_Failed_UserBanned() {
        UUID paymentInfoId = UUID.randomUUID();
        User bannedUser = new User();
        bannedUser.setId(userId);
        bannedUser.setStatus(UserStatus.BANNED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(bannedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.setDefaultPaymentInfo(userId, paymentInfoId)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("TC-35: setDefaultPaymentInfo_Failed_UserSuspended - Ném ACCESS_DENIED khi Buyer bị SUSPENDED")
    void setDefaultPaymentInfo_Failed_UserSuspended() {
        UUID paymentInfoId = UUID.randomUUID();
        User suspendedUser = new User();
        suspendedUser.setId(userId);
        suspendedUser.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.setDefaultPaymentInfo(userId, paymentInfoId)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).findByIdAndUserId(any(), any());
    }

    // ==========================================
    // TESTS FOR DELETE /api/users/payment-info/{id}
    // ==========================================

    @Test
    @DisplayName("TC-38: deletePaymentInfo_Success_NonDefaultAccount - Xóa tài khoản không phải mặc định thành công")
    void deletePaymentInfo_Success_NonDefaultAccount() {
        UUID paymentInfoId = UUID.randomUUID();
        UserPaymentInfo accountB = UserPaymentInfo.builder()
                .id(paymentInfoId)
                .user(activeUser)
                .paymentType(PaymentType.E_WALLET)
                .provider("MoMo")
                .accountNumber("0987654321")
                .isDefault(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(paymentInfoId, userId))
                .thenReturn(Optional.of(accountB));

        paymentInfoService.deletePaymentInfo(userId, paymentInfoId);

        verify(userPaymentInfoRepository, never()).findFirstByUserIdAndIdNotOrderByCreatedAtDesc(any(), any());
        verify(userPaymentInfoRepository, times(1)).delete(accountB);
    }

    @Test
    @DisplayName("TC-39: deletePaymentInfo_Success_DefaultAccount_AutoPromoteRemaining - Xóa tài khoản mặc định, tự động đôn tài khoản còn lại")
    void deletePaymentInfo_Success_DefaultAccount_AutoPromoteRemaining() {
        UUID paymentInfoId = UUID.randomUUID();
        UserPaymentInfo accountA = UserPaymentInfo.builder()
                .id(paymentInfoId)
                .user(activeUser)
                .paymentType(PaymentType.BANK_ACCOUNT)
                .provider("Vietcombank")
                .accountNumber("9876543210123")
                .isDefault(true) // Đang là mặc định
                .build();

        UUID remainingId = UUID.randomUUID();
        UserPaymentInfo accountB = UserPaymentInfo.builder()
                .id(remainingId)
                .user(activeUser)
                .paymentType(PaymentType.E_WALLET)
                .provider("MoMo")
                .accountNumber("0987654321")
                .isDefault(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(paymentInfoId, userId))
                .thenReturn(Optional.of(accountA));
        when(userPaymentInfoRepository.findFirstByUserIdAndIdNotOrderByCreatedAtDesc(userId, paymentInfoId))
                .thenReturn(Optional.of(accountB));

        paymentInfoService.deletePaymentInfo(userId, paymentInfoId);

        assertTrue(accountB.isDefault(), "Tài khoản còn lại phải được đôn lên isDefault = true");
        verify(userPaymentInfoRepository, times(1)).save(accountB);
        verify(userPaymentInfoRepository, times(1)).delete(accountA);
    }

    @Test
    @DisplayName("TC-40: deletePaymentInfo_Success_DefaultAccount_PromoteNewestRemaining - Đôn tài khoản mới nhất trong các tài khoản còn lại")
    void deletePaymentInfo_Success_DefaultAccount_PromoteNewestRemaining() {
        UUID paymentInfoId = UUID.randomUUID();
        UserPaymentInfo accountA = UserPaymentInfo.builder()
                .id(paymentInfoId)
                .user(activeUser)
                .isDefault(true)
                .build();

        UUID newestRemainingId = UUID.randomUUID();
        UserPaymentInfo accountC = UserPaymentInfo.builder()
                .id(newestRemainingId)
                .user(activeUser)
                .provider("Techcombank")
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(paymentInfoId, userId))
                .thenReturn(Optional.of(accountA));
        when(userPaymentInfoRepository.findFirstByUserIdAndIdNotOrderByCreatedAtDesc(userId, paymentInfoId))
                .thenReturn(Optional.of(accountC));

        paymentInfoService.deletePaymentInfo(userId, paymentInfoId);

        assertTrue(accountC.isDefault());
        verify(userPaymentInfoRepository, times(1)).save(accountC);
        verify(userPaymentInfoRepository, times(1)).delete(accountA);
    }

    @Test
    @DisplayName("TC-41: deletePaymentInfo_Success_SoleAccount_BecomesEmpty - Xóa tài khoản duy nhất của người dùng")
    void deletePaymentInfo_Success_SoleAccount_BecomesEmpty() {
        UUID paymentInfoId = UUID.randomUUID();
        UserPaymentInfo soleAccount = UserPaymentInfo.builder()
                .id(paymentInfoId)
                .user(activeUser)
                .isDefault(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(paymentInfoId, userId))
                .thenReturn(Optional.of(soleAccount));
        when(userPaymentInfoRepository.findFirstByUserIdAndIdNotOrderByCreatedAtDesc(userId, paymentInfoId))
                .thenReturn(Optional.empty()); // Không còn tài khoản nào khác

        paymentInfoService.deletePaymentInfo(userId, paymentInfoId);

        verify(userPaymentInfoRepository, never()).save(any());
        verify(userPaymentInfoRepository, times(1)).delete(soleAccount);
    }

    @Test
    @DisplayName("TC-42: deletePaymentInfo_Failed_PaymentInfoNotFound - Ném PAYMENT_INFO_NOT_FOUND khi id không tồn tại")
    void deletePaymentInfo_Failed_PaymentInfoNotFound() {
        UUID randomId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(randomId, userId))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.deletePaymentInfo(userId, randomId)
        );

        assertEquals(ErrorCode.PAYMENT_INFO_NOT_FOUND, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).delete(any(UserPaymentInfo.class));
    }

    @Test
    @DisplayName("TC-43: deletePaymentInfo_Failed_IDOR_BelongsToAnotherUser - Ném PAYMENT_INFO_NOT_FOUND khi id thuộc user khác")
    void deletePaymentInfo_Failed_IDOR_BelongsToAnotherUser() {
        UUID otherPaymentInfoId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserId(otherPaymentInfoId, userId))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.deletePaymentInfo(userId, otherPaymentInfoId)
        );

        assertEquals(ErrorCode.PAYMENT_INFO_NOT_FOUND, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).delete(any(UserPaymentInfo.class));
    }

    @Test
    @DisplayName("TC-44: deletePaymentInfo_Failed_UserNotFound - Ném USER_NOT_FOUND khi Buyer không tồn tại")
    void deletePaymentInfo_Failed_UserNotFound() {
        UUID paymentInfoId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.deletePaymentInfo(userId, paymentInfoId)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).findByIdAndUserId(any(), any());
        verify(userPaymentInfoRepository, never()).delete(any(UserPaymentInfo.class));
    }

    @Test
    @DisplayName("TC-45: deletePaymentInfo_Failed_UserBanned - Ném ACCESS_DENIED khi Buyer bị BANNED")
    void deletePaymentInfo_Failed_UserBanned() {
        UUID paymentInfoId = UUID.randomUUID();
        User bannedUser = new User();
        bannedUser.setId(userId);
        bannedUser.setStatus(UserStatus.BANNED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(bannedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.deletePaymentInfo(userId, paymentInfoId)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).findByIdAndUserId(any(), any());
        verify(userPaymentInfoRepository, never()).delete(any(UserPaymentInfo.class));
    }

    @Test
    @DisplayName("TC-46: deletePaymentInfo_Failed_UserSuspended - Ném ACCESS_DENIED khi Buyer bị SUSPENDED")
    void deletePaymentInfo_Failed_UserSuspended() {
        UUID paymentInfoId = UUID.randomUUID();
        User suspendedUser = new User();
        suspendedUser.setId(userId);
        suspendedUser.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentInfoService.deletePaymentInfo(userId, paymentInfoId)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(userPaymentInfoRepository, never()).findByIdAndUserId(any(), any());
        verify(userPaymentInfoRepository, never()).delete(any(UserPaymentInfo.class));
    }
}

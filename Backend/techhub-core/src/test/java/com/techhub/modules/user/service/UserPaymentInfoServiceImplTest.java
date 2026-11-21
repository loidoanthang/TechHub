package com.techhub.modules.user.service;

import com.techhub.config.properties.VNPayProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.payment.dto.response.PaymentInfoResponse;
import com.techhub.modules.payment.dto.response.VNPayIpnResponse;
import com.techhub.modules.payment.dto.response.VNPayTokenLinkResponse;
import com.techhub.modules.payment.enums.PaymentInfoStatus;
import com.techhub.modules.payment.service.VNPayService;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.user.entity.UserPaymentInfo;
import com.techhub.modules.user.enums.UserStatus;
import com.techhub.modules.user.repository.UserPaymentInfoRepository;
import com.techhub.modules.user.repository.UserRepository;
import com.techhub.modules.user.service.impl.UserPaymentInfoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho UserPaymentInfoServiceImpl (VNPAY Tokenization)")
class UserPaymentInfoServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPaymentInfoRepository userPaymentInfoRepository;

    @Mock
    private VNPayService vnPayService;

    @Mock
    private VNPayProperties vnPayProperties;

    @InjectMocks
    private UserPaymentInfoServiceImpl service;

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
    @DisplayName("initiateTokenLink: Thành công khi user active và chưa đạt giới hạn thẻ")
    void initiateTokenLink_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserIdAndStatus(userId, PaymentInfoStatus.ACTIVE)).thenReturn(1L);
        when(vnPayService.createTokenLinkUrl(eq(userId), any(), eq("127.0.0.1")))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?token_create");

        VNPayTokenLinkResponse response = service.initiateTokenLink(userId, "127.0.0.1");

        assertNotNull(response);
        assertTrue(response.redirectUrl().contains("token_create"));
        assertNotNull(response.txnRef());
    }

    @Test
    @DisplayName("initiateTokenLink: Ném PAYMENT_INFO_LIMIT_EXCEEDED khi đã đủ 5 thẻ active")
    void initiateTokenLink_LimitExceeded_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.countByUserIdAndStatus(userId, PaymentInfoStatus.ACTIVE)).thenReturn(5L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.initiateTokenLink(userId, "127.0.0.1"));

        assertEquals(ErrorCode.PAYMENT_INFO_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("getPaymentInfos: Trả về danh sách thẻ active")
    void getPaymentInfos_Success() {
        UserPaymentInfo info = UserPaymentInfo.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .bankCode("NCB")
                .cardMask("970407******1234")
                .vnpToken("VNP_TOKEN_123")
                .status(PaymentInfoStatus.ACTIVE)
                .isDefault(true)
                .build();
        when(userPaymentInfoRepository.findByUserIdAndStatusOrderByIsDefaultDescCreatedAtDesc(userId, PaymentInfoStatus.ACTIVE))
                .thenReturn(List.of(info));

        List<PaymentInfoResponse> list = service.getPaymentInfos(userId);

        assertEquals(1, list.size());
        assertEquals("NCB", list.get(0).bankCode());
        assertTrue(list.get(0).isDefault());
    }

    @Test
    @DisplayName("setDefaultPaymentInfo: Cập nhật thẻ mặc định thành công")
    void setDefaultPaymentInfo_Success() {
        UUID infoId = UUID.randomUUID();
        UserPaymentInfo info = UserPaymentInfo.builder()
                .id(infoId)
                .user(activeUser)
                .bankCode("NCB")
                .cardMask("970407******1234")
                .status(PaymentInfoStatus.ACTIVE)
                .isDefault(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserIdAndStatus(infoId, userId, PaymentInfoStatus.ACTIVE))
                .thenReturn(Optional.of(info));
        when(userPaymentInfoRepository.save(any(UserPaymentInfo.class))).thenReturn(info);

        PaymentInfoResponse response = service.setDefaultPaymentInfo(userId, infoId);

        assertNotNull(response);
        verify(userPaymentInfoRepository, times(1)).resetDefaultByUserId(userId);
    }

    @Test
    @DisplayName("deletePaymentInfo: Xóa thẻ và cập nhật status sang REMOVED")
    void deletePaymentInfo_Success() {
        UUID infoId = UUID.randomUUID();
        UserPaymentInfo info = UserPaymentInfo.builder()
                .id(infoId)
                .user(activeUser)
                .bankCode("NCB")
                .vnpToken("VNP_TOKEN_123")
                .status(PaymentInfoStatus.ACTIVE)
                .isDefault(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userPaymentInfoRepository.findByIdAndUserIdAndStatus(infoId, userId, PaymentInfoStatus.ACTIVE))
                .thenReturn(Optional.of(info));

        service.deletePaymentInfo(userId, infoId);

        assertEquals(PaymentInfoStatus.REMOVED, info.getStatus());
        verify(vnPayService, times(1)).removeToken(userId, "VNP_TOKEN_123", "127.0.0.1");
    }
}

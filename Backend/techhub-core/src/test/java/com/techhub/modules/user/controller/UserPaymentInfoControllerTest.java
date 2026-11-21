package com.techhub.modules.user.controller;

import com.techhub.common.ApiResponse;
import com.techhub.modules.payment.dto.response.PaymentInfoResponse;
import com.techhub.modules.payment.dto.response.VNPayIpnResponse;
import com.techhub.modules.payment.dto.response.VNPayTokenLinkResponse;
import com.techhub.modules.payment.enums.PaymentInfoStatus;
import com.techhub.modules.payment.enums.PaymentType;
import com.techhub.modules.user.service.UserPaymentInfoService;
import com.techhub.security.CustomUserDetails;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.user.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho UserPaymentInfoController (Tokenized VNPAY)")
class UserPaymentInfoControllerTest {

    @Mock
    private UserPaymentInfoService userPaymentInfoService;

    @InjectMocks
    private UserPaymentInfoController controller;

    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        User testUser = new User();
        testUser.setId(currentUserId);
        testUser.setEmail("buyer@techhub.com");
        testUser.setStatus(UserStatus.ACTIVE);

        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("initiateTokenLink: Phải có @PreAuthorize yêu cầu ROLE_BUYER")
    void controller_initiateTokenLink_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = UserPaymentInfoController.class.getMethod("initiateTokenLink", jakarta.servlet.http.HttpServletRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method initiateTokenLink phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"));
    }

    @Test
    @DisplayName("initiateTokenLink: Thành công trả về redirectUrl sang VNPAY")
    void controller_initiateTokenLink_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        VNPayTokenLinkResponse mockResponse = new VNPayTokenLinkResponse(
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Command=token_create",
                "LINK_123456"
        );
        when(userPaymentInfoService.initiateTokenLink(eq(currentUserId), any())).thenReturn(mockResponse);

        ApiResponse<VNPayTokenLinkResponse> response = controller.initiateTokenLink(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(mockResponse.redirectUrl(), response.getData().redirectUrl());
    }

    @Test
    @DisplayName("getPaymentInfos: Lấy danh sách thẻ liên kết thành công")
    void controller_getPaymentInfos_Success() {
        PaymentInfoResponse item = new PaymentInfoResponse(
                UUID.randomUUID(),
                PaymentType.BANK_ACCOUNT,
                "NCB",
                "970407******1234",
                "01",
                "NGUYEN VAN A",
                PaymentInfoStatus.ACTIVE,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(userPaymentInfoService.getPaymentInfos(currentUserId)).thenReturn(List.of(item));

        ApiResponse<List<PaymentInfoResponse>> response = controller.getPaymentInfos();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("NCB", response.getData().get(0).bankCode());
    }

    @Test
    @DisplayName("setDefaultPaymentInfo: Phải có @PreAuthorize yêu cầu ROLE_BUYER")
    void controller_setDefaultPaymentInfo_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = UserPaymentInfoController.class.getMethod("setDefaultPaymentInfo", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method setDefaultPaymentInfo phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"));
    }

    @Test
    @DisplayName("setDefaultPaymentInfo: Đặt mặc định thành công")
    void controller_setDefaultPaymentInfo_Success() {
        UUID infoId = UUID.randomUUID();
        PaymentInfoResponse mockResponse = new PaymentInfoResponse(
                infoId,
                PaymentType.BANK_ACCOUNT,
                "NCB",
                "970407******1234",
                "01",
                "NGUYEN VAN A",
                PaymentInfoStatus.ACTIVE,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(userPaymentInfoService.setDefaultPaymentInfo(currentUserId, infoId)).thenReturn(mockResponse);

        ApiResponse<PaymentInfoResponse> response = controller.setDefaultPaymentInfo(infoId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getData().isDefault());
    }

    @Test
    @DisplayName("deletePaymentInfo: Phải có @PreAuthorize yêu cầu ROLE_BUYER")
    void controller_deletePaymentInfo_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = UserPaymentInfoController.class.getMethod("deletePaymentInfo", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method deletePaymentInfo phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"));
    }

    @Test
    @DisplayName("deletePaymentInfo: Xóa thẻ thành công")
    void controller_deletePaymentInfo_Success() {
        UUID infoId = UUID.randomUUID();
        doNothing().when(userPaymentInfoService).deletePaymentInfo(currentUserId, infoId);

        ApiResponse<Void> response = controller.deletePaymentInfo(infoId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(userPaymentInfoService, times(1)).deletePaymentInfo(currentUserId, infoId);
    }

    @Test
    @DisplayName("processTokenCreateIpn: Xử lý IPN thành công")
    void controller_processTokenCreateIpn_Success() {
        Map<String, String> params = Map.of("vnp_ResponseCode", "00");
        when(userPaymentInfoService.processTokenCreateIpn(params)).thenReturn(VNPayIpnResponse.success());

        VNPayIpnResponse response = controller.processTokenCreateIpn(params);

        assertNotNull(response);
        assertEquals("00", response.rspCode());
    }
}

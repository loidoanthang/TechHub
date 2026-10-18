package com.techhub.controller.payment;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.VNPayRetryRequest;
import com.techhub.model.dto.response.VNPayCallbackResponse;
import com.techhub.model.dto.response.VNPayIpnResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.payment.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho PaymentController (/api/payment/vnpay)")
class PaymentControllerTest {

    @Mock
    private VNPayService vnPayService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private PaymentController paymentController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("buyer@techhub.com");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("processIpn should delegate to VNPayService and return VNPayIpnResponse")
    void testProcessIpn() {
        Map<String, String> params = Map.of(
                "vnp_TxnRef", "TXN123",
                "vnp_ResponseCode", "00"
        );
        VNPayIpnResponse expectedResponse = VNPayIpnResponse.success();
        when(vnPayService.processIpn(params)).thenReturn(expectedResponse);

        VNPayIpnResponse result = paymentController.processIpn(params);

        assertThat(result).isNotNull();
        assertThat(result.rspCode()).isEqualTo("00");
        assertThat(result.message()).isEqualTo("Confirm Success");
        verify(vnPayService).processIpn(params);
    }

    @Test
    @DisplayName("processCallback should delegate to VNPayService and return ApiResponse")
    void testProcessCallback() {
        Map<String, String> params = Map.of(
                "vnp_TxnRef", "TXN123",
                "vnp_ResponseCode", "00"
        );
        UUID paymentId = UUID.randomUUID();
        VNPayCallbackResponse serviceResponse = new VNPayCallbackResponse(
                true,
                paymentId,
                "14567890",
                BigDecimal.valueOf(250000),
                "NCB",
                "Thanh toan don hang",
                "00",
                "Giao dịch thanh toán thành công qua VNPAY",
                List.of("ORD-001")
        );
        when(vnPayService.processCallback(params)).thenReturn(serviceResponse);

        ApiResponse<VNPayCallbackResponse> result = paymentController.processCallback(params);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().paymentId()).isEqualTo(paymentId);
        assertThat(result.getData().transactionNo()).isEqualTo("14567890");
        verify(vnPayService).processCallback(params);
    }

    @Test
    @DisplayName("retryPayment should delegate to VNPayService with current user and client IP")
    void testRetryPayment() {
        UUID paymentId = UUID.randomUUID();
        VNPayRetryRequest request = new VNPayRetryRequest("VNPAYQR");
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.100");
        when(vnPayService.retryPayment(userId, paymentId, "VNPAYQR", "192.168.1.100"))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?retry=123");

        ApiResponse<Map<String, String>> result = paymentController.retryPayment(paymentId, request, httpRequest);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsKey("paymentUrl");
        assertThat(result.getData().get("paymentUrl")).contains("https://sandbox.vnpayment.vn");
        verify(vnPayService).retryPayment(userId, paymentId, "VNPAYQR", "192.168.1.100");
    }

    @Test
    @DisplayName("retryPayment should handle null request body gracefully")
    void testRetryPayment_NullRequest() {
        UUID paymentId = UUID.randomUUID();
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(vnPayService.retryPayment(userId, paymentId, null, "127.0.0.1"))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?default=1");

        ApiResponse<Map<String, String>> result = paymentController.retryPayment(paymentId, null, httpRequest);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get("paymentUrl")).contains("default=1");
        verify(vnPayService).retryPayment(userId, paymentId, null, "127.0.0.1");
    }

    @Test
    @DisplayName("Verify Spring Web and Security annotations on PaymentController")
    void testAnnotations() throws NoSuchMethodException {
        // Controller class annotations
        assertTrue(PaymentController.class.isAnnotationPresent(RestController.class));
        RequestMapping reqMapping = PaymentController.class.getAnnotation(RequestMapping.class);
        assertNotNull(reqMapping);
        assertArrayEquals(new String[]{"/api/payment/vnpay"}, reqMapping.value());

        // IPN method annotations
        Method ipnMethod = PaymentController.class.getMethod("processIpn", Map.class);
        assertTrue(ipnMethod.isAnnotationPresent(GetMapping.class));
        assertArrayEquals(new String[]{"/ipn"}, ipnMethod.getAnnotation(GetMapping.class).value());

        // Callback method annotations
        Method callbackMethod = PaymentController.class.getMethod("processCallback", Map.class);
        assertTrue(callbackMethod.isAnnotationPresent(GetMapping.class));
        assertArrayEquals(new String[]{"/callback"}, callbackMethod.getAnnotation(GetMapping.class).value());

        // Retry method annotations
        Method retryMethod = PaymentController.class.getMethod("retryPayment", UUID.class, VNPayRetryRequest.class, HttpServletRequest.class);
        assertTrue(retryMethod.isAnnotationPresent(PostMapping.class));
        assertArrayEquals(new String[]{"/retry/{paymentId}"}, retryMethod.getAnnotation(PostMapping.class).value());
        assertTrue(retryMethod.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('BUYER')", retryMethod.getAnnotation(PreAuthorize.class).value());
    }
}

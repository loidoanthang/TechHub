package com.techhub.controller.payment;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.BuyerPaymentStatusResponse;
import com.techhub.model.dto.response.PaymentChannelResponse;
import com.techhub.model.dto.response.PaymentMethodResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.payment.PaymentService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho BuyerPaymentController (/api/buyer)")
class BuyerPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private BuyerPaymentController buyerPaymentController;

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
    @DisplayName("getAvailablePaymentMethods should delegate to PaymentService and return ApiResponse")
    void testGetAvailablePaymentMethods() {
        PaymentMethodResponse cod = PaymentMethodResponse.builder()
                .code("COD")
                .name("Thanh toán khi nhận hàng")
                .isDefault(true)
                .isActive(true)
                .channels(List.of())
                .build();
        PaymentMethodResponse vnpay = PaymentMethodResponse.builder()
                .code("VNPAY")
                .name("Cổng VNPAY")
                .isDefault(false)
                .isActive(true)
                .channels(List.of(
                        PaymentChannelResponse.builder().code("VNPAYQR").name("VNPAY-QR").build()
                ))
                .build();

        when(paymentService.getAvailablePaymentMethods()).thenReturn(List.of(cod, vnpay));

        ApiResponse<List<PaymentMethodResponse>> result = buyerPaymentController.getAvailablePaymentMethods();

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).code()).isEqualTo("COD");
        assertThat(result.getData().get(1).code()).isEqualTo("VNPAY");
        verify(paymentService).getAvailablePaymentMethods();
    }

    @Test
    @DisplayName("getBuyerPaymentStatus should extract currentUserId and delegate to PaymentService")
    void testGetBuyerPaymentStatus() {
        UUID paymentId = UUID.randomUUID();
        BuyerPaymentStatusResponse expectedResponse = BuyerPaymentStatusResponse.builder()
                .paymentId(paymentId)
                .paymentMethod(PaymentMethod.VNPAY)
                .bankCode("VNPAYQR")
                .status(PaymentStatus.PAID)
                .transactionId("998877")
                .amount(BigDecimal.valueOf(350000))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .isExpired(false)
                .orderIds(List.of(UUID.randomUUID()))
                .orderNumbers(List.of("TH001"))
                .build();

        when(paymentService.getBuyerPaymentStatus(userId, paymentId)).thenReturn(expectedResponse);

        ApiResponse<BuyerPaymentStatusResponse> result = buyerPaymentController.getBuyerPaymentStatus(paymentId);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(expectedResponse);
        verify(paymentService).getBuyerPaymentStatus(userId, paymentId);
    }

    @Test
    @DisplayName("Verify Spring Web and Security annotations on BuyerPaymentController")
    void testAnnotations() throws NoSuchMethodException {
        // Controller annotations
        assertTrue(BuyerPaymentController.class.isAnnotationPresent(RestController.class));
        RequestMapping reqMapping = BuyerPaymentController.class.getAnnotation(RequestMapping.class);
        assertNotNull(reqMapping);
        assertArrayEquals(new String[]{"/api/buyer"}, reqMapping.value());

        // getAvailablePaymentMethods annotations
        Method getMethods = BuyerPaymentController.class.getMethod("getAvailablePaymentMethods");
        assertTrue(getMethods.isAnnotationPresent(GetMapping.class));
        assertArrayEquals(new String[]{"/payment-methods"}, getMethods.getAnnotation(GetMapping.class).value());
        assertTrue(getMethods.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('BUYER')", getMethods.getAnnotation(PreAuthorize.class).value());

        // getBuyerPaymentStatus annotations
        Method getStatus = BuyerPaymentController.class.getMethod("getBuyerPaymentStatus", UUID.class);
        assertTrue(getStatus.isAnnotationPresent(GetMapping.class));
        assertArrayEquals(new String[]{"/payments/{id}/status"}, getStatus.getAnnotation(GetMapping.class).value());
        assertTrue(getStatus.isAnnotationPresent(PreAuthorize.class));
        assertEquals("hasRole('BUYER')", getStatus.getAnnotation(PreAuthorize.class).value());

        // PathVariable annotation
        assertTrue(getStatus.getParameters()[0].isAnnotationPresent(PathVariable.class));
    }
}

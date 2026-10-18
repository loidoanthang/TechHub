package com.techhub.service.payment;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.BuyerPaymentStatusResponse;
import com.techhub.model.dto.response.PaymentChannelResponse;
import com.techhub.model.dto.response.PaymentMethodResponse;
import com.techhub.model.entity.Order;
import com.techhub.model.entity.Payment;
import com.techhub.model.entity.User;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.PaymentRepository;
import com.techhub.service.payment.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho PaymentServiceImpl (getAvailablePaymentMethods & getBuyerPaymentStatus)")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID userId;
    private UUID otherUserId;
    private UUID paymentId;
    private User buyer;
    private Payment payment;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        buyer = new User();
        buyer.setId(userId);
        buyer.setEmail("buyer@techhub.com");

        payment = Payment.builder()
                .id(paymentId)
                .user(buyer)
                .paymentMethod(PaymentMethod.VNPAY)
                .bankCode("VNPAYQR")
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.valueOf(450000))
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();
    }

    @Test
    @DisplayName("TC-01: getAvailablePaymentMethods should return COD as default and VNPAY with 4 direct channels")
    void getAvailablePaymentMethods_Success() {
        List<PaymentMethodResponse> methods = paymentService.getAvailablePaymentMethods();

        assertThat(methods).hasSize(2);

        // COD verification
        PaymentMethodResponse cod = methods.stream()
                .filter(m -> m.code().equals("COD"))
                .findFirst().orElseThrow();
        assertThat(cod.isDefault()).isTrue();
        assertThat(cod.isActive()).isTrue();
        assertThat(cod.channels()).isEmpty();

        // VNPAY verification
        PaymentMethodResponse vnpay = methods.stream()
                .filter(m -> m.code().equals("VNPAY"))
                .findFirst().orElseThrow();
        assertThat(vnpay.isDefault()).isFalse();
        assertThat(vnpay.isActive()).isTrue();
        assertThat(vnpay.channels()).hasSize(4);

        List<String> channelCodes = vnpay.channels().stream()
                .map(PaymentChannelResponse::code)
                .toList();
        assertThat(channelCodes).containsExactlyInAnyOrder("VNPAYQR", "VNBANK", "INTCARD", "VNPAY");
    }

    @Test
    @DisplayName("TC-03: getBuyerPaymentStatus should return PENDING status with valid expiration when within 15 minutes")
    void getBuyerPaymentStatus_Success_Pending() {
        UUID order1Id = UUID.randomUUID();
        UUID order2Id = UUID.randomUUID();
        Order order1 = Order.builder().id(order1Id).orderNumber("TH001").build();
        Order order2 = Order.builder().id(order2Id).orderNumber("TH002").build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(List.of(order1, order2));

        BuyerPaymentStatusResponse response = paymentService.getBuyerPaymentStatus(userId, paymentId);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.VNPAY);
        assertThat(response.bankCode()).isEqualTo("VNPAYQR");
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(450000));
        assertThat(response.isExpired()).isFalse();
        assertThat(response.orderIds()).containsExactly(order1Id, order2Id);
        assertThat(response.orderNumbers()).containsExactly("TH001", "TH002");

        verify(paymentRepository).findById(paymentId);
        verify(orderRepository).findByPaymentId(paymentId);
    }

    @Test
    @DisplayName("TC-04: getBuyerPaymentStatus should return PAID status and transactionId when payment completed")
    void getBuyerPaymentStatus_Success_Paid() {
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId("14567890");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(List.of());

        BuyerPaymentStatusResponse response = paymentService.getBuyerPaymentStatus(userId, paymentId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.transactionId()).isEqualTo("14567890");
        assertThat(response.isExpired()).isFalse();
        assertThat(response.orderIds()).isEmpty();
    }

    @Test
    @DisplayName("TC-05: getBuyerPaymentStatus should flag isExpired=true when PENDING payment is older than 15 minutes")
    void getBuyerPaymentStatus_Success_Expired() {
        payment.setCreatedAt(LocalDateTime.now().minusMinutes(16));

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(List.of());

        BuyerPaymentStatusResponse response = paymentService.getBuyerPaymentStatus(userId, paymentId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.isExpired()).isTrue();
    }

    @Test
    @DisplayName("TC-06: getBuyerPaymentStatus should throw PAYMENT_NOT_FOUND when payment does not exist")
    void getBuyerPaymentStatus_NotFound() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getBuyerPaymentStatus(userId, paymentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);

        verify(paymentRepository).findById(paymentId);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("TC-07: getBuyerPaymentStatus should throw ACCESS_DENIED on IDOR attempt")
    void getBuyerPaymentStatus_AccessDenied_IDOR() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getBuyerPaymentStatus(otherUserId, paymentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        verify(paymentRepository).findById(paymentId);
        verifyNoInteractions(orderRepository);
    }
}

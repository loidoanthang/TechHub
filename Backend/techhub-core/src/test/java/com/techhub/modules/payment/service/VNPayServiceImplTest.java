package com.techhub.modules.payment.service;

import com.techhub.common.VNPayUtils;
import com.techhub.config.properties.VNPayProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.payment.dto.response.VNPayCallbackResponse;
import com.techhub.modules.payment.dto.response.VNPayIpnResponse;
import com.techhub.modules.payment.dto.response.VNPayQueryDrResponse;
import com.techhub.modules.payment.dto.response.VNPayRefundResponse;
import com.techhub.modules.order.repository.OrderRepository;
import com.techhub.modules.payment.repository.PaymentRepository;
import com.techhub.modules.voucher.repository.VoucherRedemptionRepository;
import com.techhub.modules.voucher.repository.VoucherRepository;
import com.techhub.modules.payment.service.impl.VNPayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.techhub.modules.order.enums.OrderStatus;
import com.techhub.modules.payment.enums.PaymentMethod;
import com.techhub.modules.payment.enums.PaymentStatus;
import com.techhub.modules.voucher.enums.VoucherRedemptionStatus;
import com.techhub.modules.order.entity.Order;
import com.techhub.modules.payment.entity.Payment;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.voucher.entity.Voucher;
import com.techhub.modules.voucher.entity.VoucherRedemption;

@ExtendWith(MockitoExtension.class)
class VNPayServiceImplTest {

    @Mock
    private VNPayProperties vnPayProperties;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VoucherRedemptionRepository voucherRedemptionRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private VNPayServiceImpl vnPayService;

    private Payment payment;
    private User buyer;
    private final String hashSecret = "DHPMLPLWZVHLGFDDDIZKQFFRIMHDDBWW";
    private final String tmnCode = "MNACYPOK";
    private final String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private final String returnUrl = "http://localhost:3000/checkout/payment-callback";
    private final String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setId(UUID.randomUUID());
        buyer.setEmail("buyer@techhub.com");

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .user(buyer)
                .amount(BigDecimal.valueOf(250000))
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .orders(new ArrayList<>())
                .build();
    }

    private void mockVNPayProperties() {
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(vnPayProperties.getTmnCode()).thenReturn(tmnCode);
        when(vnPayProperties.getPayUrl()).thenReturn(payUrl);
        when(vnPayProperties.getReturnUrl()).thenReturn(returnUrl);
    }

    // ==========================================
    // 1. createPaymentUrl Tests
    // ==========================================
    @Test
    @DisplayName("createPaymentUrl should generate valid URL with default gateway when bankCode is null")
    void createPaymentUrl_DefaultGateway() {
        mockVNPayProperties();

        String url = vnPayService.createPaymentUrl(payment, null, "127.0.0.1");

        assertThat(url).isNotNull();
        assertThat(url).startsWith(payUrl + "?");
        assertThat(url).contains("vnp_Amount=25000000"); // 250,000 * 100
        assertThat(url).contains("vnp_TmnCode=" + tmnCode);
        assertThat(url).contains("vnp_TxnRef=" + payment.getId());
        assertThat(url).contains("vnp_SecureHash=");
        assertThat(url).doesNotContain("vnp_BankCode=");
    }

    @Test
    @DisplayName("createPaymentUrl should include vnp_BankCode when bankCode is provided")
    void createPaymentUrl_WithBankCode() {
        mockVNPayProperties();

        String url = vnPayService.createPaymentUrl(payment, "VNPAYQR", "127.0.0.1");

        assertThat(url).contains("vnp_BankCode=VNPAYQR");
    }

    @Test
    @DisplayName("createPaymentUrl should throw exception on invalid bank code")
    void createPaymentUrl_InvalidBankCode() {
        assertThatThrownBy(() -> vnPayService.createPaymentUrl(payment, "INVALID_BANK", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_BANK_CODE);
    }

    @Test
    @DisplayName("createPaymentUrl should throw exception on null or zero amount")
    void createPaymentUrl_ZeroAmount() {
        payment.setAmount(BigDecimal.ZERO);
        assertThatThrownBy(() -> vnPayService.createPaymentUrl(payment, null, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAYMENT_AMOUNT);
    }

    // ==========================================
    // 2. processIpn Tests
    // ==========================================
    @Test
    @DisplayName("processIpn should return 97 Invalid Checksum when hash mismatch")
    void processIpn_InvalidChecksum() {
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", payment.getId().toString());
        params.put("vnp_Amount", "25000000");
        params.put("vnp_SecureHash", "INVALID_HASH");

        VNPayIpnResponse response = vnPayService.processIpn(params);

        assertThat(response.rspCode()).isEqualTo("97");
        assertThat(response.message()).isEqualTo("Invalid Checksum");
    }

    @Test
    @DisplayName("processIpn should return 01 Order Not Found when payment does not exist")
    void processIpn_OrderNotFound() {
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.empty());

        Map<String, String> params = createValidVNPayParams(payment.getId().toString(), "25000000", "00", "00");

        VNPayIpnResponse response = vnPayService.processIpn(params);

        assertThat(response.rspCode()).isEqualTo("01");
        assertThat(response.message()).isEqualTo("Order not found");
    }

    @Test
    @DisplayName("processIpn should return 04 Invalid Amount when amount does not match")
    void processIpn_InvalidAmount() {
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Map<String, String> params = createValidVNPayParams(payment.getId().toString(), "99999900", "00", "00");

        VNPayIpnResponse response = vnPayService.processIpn(params);

        assertThat(response.rspCode()).isEqualTo("04");
        assertThat(response.message()).isEqualTo("Invalid Amount");
    }

    @Test
    @DisplayName("processIpn should return 02 Order Already Confirmed when status is PAID")
    void processIpn_AlreadyConfirmed() {
        payment.setStatus(PaymentStatus.PAID);
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Map<String, String> params = createValidVNPayParams(payment.getId().toString(), "25000000", "00", "00");

        VNPayIpnResponse response = vnPayService.processIpn(params);

        assertThat(response.rspCode()).isEqualTo("02");
        assertThat(response.message()).isEqualTo("Order already confirmed");
    }

    @Test
    @DisplayName("processIpn should update payment, orders, and vouchers to PAID on success")
    void processIpn_Success() {
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PENDING_PAYMENT)
                .payment(payment)
                .build();
        payment.getOrders().add(order);

        Voucher voucher = Voucher.builder()
                .id(UUID.randomUUID())
                .reservedCount(1)
                .usedCount(0)
                .build();

        VoucherRedemption redemption = VoucherRedemption.builder()
                .id(UUID.randomUUID())
                .voucher(voucher)
                .status(VoucherRedemptionStatus.RESERVED)
                .build();

        when(voucherRedemptionRepository.findByPaymentId(payment.getId())).thenReturn(List.of(redemption));

        Map<String, String> params = createValidVNPayParams(payment.getId().toString(), "25000000", "00", "00");

        VNPayIpnResponse response = vnPayService.processIpn(params);

        assertThat(response.rspCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        assertThat(redemption.getStatus()).isEqualTo(VoucherRedemptionStatus.REDEEMED);
        assertThat(voucher.getReservedCount()).isEqualTo(0);
        assertThat(voucher.getUsedCount()).isEqualTo(1);

        verify(paymentRepository).save(payment);
        verify(orderRepository).saveAll(any());
        verify(voucherRedemptionRepository).saveAll(any());
        verify(voucherRepository).saveAll(any());
    }

    @Test
    @DisplayName("processIpn should mark payment as FAILED when response code is not 00")
    void processIpn_FailedPayment() {
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Map<String, String> params = createValidVNPayParams(payment.getId().toString(), "25000000", "24", "02");

        VNPayIpnResponse response = vnPayService.processIpn(params);

        assertThat(response.rspCode()).isEqualTo("00"); // Acknowledged to VNPay
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }

    // ==========================================
    // 3. processCallback Tests
    // ==========================================
    @Test
    @DisplayName("processCallback should throw VNPAY_CHECKSUM_INVALID on signature mismatch")
    void processCallback_InvalidChecksum() {
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "WRONG_HASH");

        assertThatThrownBy(() -> vnPayService.processCallback(params))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VNPAY_CHECKSUM_INVALID);
    }

    @Test
    @DisplayName("processCallback should return successful response when response code is 00")
    void processCallback_Success() {
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Order order = Order.builder()
                .orderNumber("ORD-20261018-001")
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        payment.getOrders().add(order);

        Map<String, String> params = createValidVNPayParams(payment.getId().toString(), "25000000", "00", "00");

        VNPayCallbackResponse response = vnPayService.processCallback(params);

        assertThat(response.success()).isTrue();
        assertThat(response.paymentId()).isEqualTo(payment.getId());
        assertThat(response.responseCode()).isEqualTo("00");
        assertThat(response.orderNumbers()).containsExactly("ORD-20261018-001");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("processCallback should return failed response when customer cancelled")
    void processCallback_CustomerCancelled() {
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Map<String, String> params = createValidVNPayParams(payment.getId().toString(), "25000000", "24", "02");

        VNPayCallbackResponse response = vnPayService.processCallback(params);

        assertThat(response.success()).isFalse();
        assertThat(response.responseCode()).isEqualTo("24");
        assertThat(response.message()).contains("hủy bởi khách hàng");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // ==========================================
    // 4. retryPayment Tests
    // ==========================================
    @Test
    @DisplayName("retryPayment should throw ACCESS_DENIED if another user attempts retry")
    void retryPayment_AccessDenied() {
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        UUID anotherUserId = UUID.randomUUID();
        assertThatThrownBy(() -> vnPayService.retryPayment(anotherUserId, payment.getId(), null, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("retryPayment should throw PAYMENT_ALREADY_PAID if payment is already paid")
    void retryPayment_AlreadyPaid() {
        payment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> vnPayService.retryPayment(buyer.getId(), payment.getId(), null, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_PAID);
    }

    @Test
    @DisplayName("retryPayment should throw ORDER_ALREADY_CANCELLED if any linked order was cancelled")
    void retryPayment_OrderCancelled() {
        Order cancelledOrder = Order.builder().status(OrderStatus.CANCELLED).build();
        payment.getOrders().add(cancelledOrder);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> vnPayService.retryPayment(buyer.getId(), payment.getId(), null, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("retryPayment should generate fresh URL with retry timestamp suffix in txnRef")
    void retryPayment_Success() {
        mockVNPayProperties();
        payment.setStatus(PaymentStatus.FAILED);
        Order activeOrder = Order.builder().status(OrderStatus.PENDING_PAYMENT).build();
        payment.getOrders().add(activeOrder);

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        String retryUrl = vnPayService.retryPayment(buyer.getId(), payment.getId(), "VNBANK", "127.0.0.1");

        assertThat(retryUrl).isNotNull();
        assertThat(retryUrl).startsWith(payUrl + "?");
        assertThat(retryUrl).contains("vnp_BankCode=VNBANK");
        assertThat(retryUrl).contains("vnp_TxnRef=" + payment.getId() + "_");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getBankCode()).isEqualTo("VNBANK");
        verify(paymentRepository).save(payment);
    }

    private Map<String, String> createValidVNPayParams(String txnRef, String amount, String responseCode, String transactionStatus) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_Amount", amount);
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionStatus", transactionStatus);
        params.put("vnp_TransactionNo", "14567890");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_OrderInfo", "Thanh toan don hang " + txnRef.substring(0, 8));

        String hash = VNPayUtils.hashAllFields(params, hashSecret);
        params.put("vnp_SecureHash", hash);
        return params;
    }

    // ==========================================
    // 5. queryTransaction Tests (QueryDR)
    // ==========================================
    @Test
    @DisplayName("queryTransaction should sync payment to PAID when VNPAY reports success")
    void queryTransaction_Success_SyncPaid() {
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(vnPayProperties.getTmnCode()).thenReturn(tmnCode);
        when(vnPayProperties.getApiUrl()).thenReturn(apiUrl);

        Order order = Order.builder().status(OrderStatus.PENDING_PAYMENT).build();
        payment.getOrders().add(order);

        VNPayQueryDrResponse mockResp = VNPayQueryDrResponse.builder()
                .vnpResponseId("RESP_Q1")
                .vnpCommand("querydr")
                .vnpResponseCode("00")
                .vnpTransactionStatus("00")
                .vnpTransactionNo("14567890")
                .vnpBankCode("NCB")
                .vnpMessage("Success")
                .build();

        when(restTemplate.postForObject(eq(apiUrl), any(HttpEntity.class), eq(VNPayQueryDrResponse.class)))
                .thenReturn(mockResp);

        VNPayQueryDrResponse result = vnPayService.queryTransaction(payment.getId(), "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getTransactionId()).isEqualTo("14567890");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("queryTransaction should sync payment to FAILED when VNPAY reports failed status (02)")
    void queryTransaction_SyncFailed() {
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(vnPayProperties.getTmnCode()).thenReturn(tmnCode);
        when(vnPayProperties.getApiUrl()).thenReturn(apiUrl);

        VNPayQueryDrResponse mockResp = VNPayQueryDrResponse.builder()
                .vnpResponseId("RESP_Q2")
                .vnpCommand("querydr")
                .vnpResponseCode("00")
                .vnpTransactionStatus("02")
                .vnpTransactionNo("14567890")
                .vnpBankCode("NCB")
                .vnpMessage("Transaction Failed")
                .build();

        when(restTemplate.postForObject(eq(apiUrl), any(HttpEntity.class), eq(VNPayQueryDrResponse.class)))
                .thenReturn(mockResp);

        VNPayQueryDrResponse result = vnPayService.queryTransaction(payment.getId(), "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("queryTransaction should throw PAYMENT_NOT_FOUND when payment does not exist")
    void queryTransaction_PaymentNotFound() {
        when(paymentRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vnPayService.queryTransaction(UUID.randomUUID(), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }

    // ==========================================
    // 6. refundTransaction Tests (Refund)
    // ==========================================
    @Test
    @DisplayName("refundTransaction should execute full refund and increment payment refundedAmount")
    void refundTransaction_Success_FullRefund() {
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId("14567890");
        payment.setAmount(new BigDecimal("250000.00"));
        payment.setRefundedAmount(BigDecimal.ZERO);

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(vnPayProperties.getTmnCode()).thenReturn(tmnCode);
        when(vnPayProperties.getApiUrl()).thenReturn(apiUrl);

        VNPayRefundResponse mockResp = VNPayRefundResponse.builder()
                .vnpResponseId("RESP_REF_1")
                .vnpCommand("refund")
                .vnpResponseCode("00")
                .vnpMessage("Refund Success")
                .build();

        when(restTemplate.postForObject(eq(apiUrl), any(HttpEntity.class), eq(VNPayRefundResponse.class)))
                .thenReturn(mockResp);

        VNPayRefundResponse result = vnPayService.refundTransaction(
                payment.getId(), new BigDecimal("250000.00"), null, "Full refund reason", "admin", "127.0.0.1"
        );

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("250000.00");
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("refundTransaction should execute partial refund successfully")
    void refundTransaction_Success_PartialRefund() {
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId("14567890");
        payment.setAmount(new BigDecimal("250000.00"));
        payment.setRefundedAmount(new BigDecimal("50000.00"));

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(vnPayProperties.getHashSecret()).thenReturn(hashSecret);
        when(vnPayProperties.getTmnCode()).thenReturn(tmnCode);
        when(vnPayProperties.getApiUrl()).thenReturn(apiUrl);

        VNPayRefundResponse mockResp = VNPayRefundResponse.builder()
                .vnpResponseId("RESP_REF_2")
                .vnpCommand("refund")
                .vnpResponseCode("00")
                .vnpMessage("Partial Refund Success")
                .build();

        when(restTemplate.postForObject(eq(apiUrl), any(HttpEntity.class), eq(VNPayRefundResponse.class)))
                .thenReturn(mockResp);

        VNPayRefundResponse result = vnPayService.refundTransaction(
                payment.getId(), new BigDecimal("100000.00"), null, "Partial refund reason", "admin", "127.0.0.1"
        );

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("150000.00");
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("refundTransaction should reject when amount exceeds refundable balance")
    void refundTransaction_ExceedsRefundableBalance() {
        payment.setStatus(PaymentStatus.PAID);
        payment.setAmount(new BigDecimal("250000.00"));
        payment.setRefundedAmount(new BigDecimal("200000.00")); // Only 50k left

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> vnPayService.refundTransaction(
                payment.getId(), new BigDecimal("100000.00"), null, "Exceeds", "admin", "127.0.0.1"
        )).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REFUND_AMOUNT));
    }

    @Test
    @DisplayName("refundTransaction should reject when payment is not PAID")
    void refundTransaction_NotPaid() {
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> vnPayService.refundTransaction(
                payment.getId(), new BigDecimal("100000.00"), null, "Not paid", "admin", "127.0.0.1"
        )).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.REFUND_NOT_ALLOWED));
    }
}

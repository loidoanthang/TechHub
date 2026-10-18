package com.techhub.service.payment.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.BuyerPaymentStatusResponse;
import com.techhub.model.dto.response.PaymentChannelResponse;
import com.techhub.model.dto.response.PaymentMethodResponse;
import com.techhub.model.entity.Order;
import com.techhub.model.entity.Payment;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;
import com.techhub.model.enums.VNPayBankCode;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.PaymentRepository;
import com.techhub.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    private static final int PAYMENT_TTL_MINUTES = 15;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getAvailablePaymentMethods() {
        List<PaymentChannelResponse> vnpayChannels = Arrays.stream(VNPayBankCode.values())
                .map(code -> PaymentChannelResponse.builder()
                        .code(code.name())
                        .name(getChannelName(code))
                        .description(code.getDescription())
                        .iconUrl("/icons/payment/" + code.name().toLowerCase() + ".svg")
                        .build())
                .toList();

        PaymentMethodResponse codMethod = PaymentMethodResponse.builder()
                .code(PaymentMethod.COD.name())
                .name("Thanh toán khi nhận hàng (COD)")
                .description("Thanh toán tiền mặt trực tiếp cho nhân viên giao hàng khi nhận kiện hàng")
                .iconUrl("/icons/payment/cod.svg")
                .isDefault(true)
                .isActive(true)
                .channels(List.of())
                .build();

        PaymentMethodResponse vnpayMethod = PaymentMethodResponse.builder()
                .code(PaymentMethod.VNPAY.name())
                .name("Cổng thanh toán trực tuyến VNPAY")
                .description("Thanh toán an toàn, bảo mật tức thì qua Cổng VNPAY và 4 kênh trực tiếp")
                .iconUrl("/icons/payment/vnpay.svg")
                .isDefault(false)
                .isActive(true)
                .channels(vnpayChannels)
                .build();

        return List.of(codMethod, vnpayMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerPaymentStatusResponse getBuyerPaymentStatus(UUID userId, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUser().getId().equals(userId)) {
            log.warn("IDOR attempt: User {} tried to access payment {} of User {}",
                    userId, paymentId, payment.getUser().getId());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        List<Order> orders = orderRepository.findByPaymentId(paymentId);
        List<UUID> orderIds = orders.stream().map(Order::getId).toList();
        List<String> orderNumbers = orders.stream().map(Order::getOrderNumber).toList();

        LocalDateTime createdAt = payment.getCreatedAt() != null ? payment.getCreatedAt() : LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusMinutes(PAYMENT_TTL_MINUTES);
        boolean isExpired = payment.getStatus() == PaymentStatus.PENDING
                && LocalDateTime.now().isAfter(expiresAt);

        return BuyerPaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .paymentMethod(payment.getPaymentMethod())
                .bankCode(payment.getBankCode())
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .expiresAt(expiresAt)
                .isExpired(isExpired)
                .orderIds(orderIds)
                .orderNumbers(orderNumbers)
                .build();
    }

    private String getChannelName(VNPayBankCode code) {
        return switch (code) {
            case VNPAYQR -> "VNPAY-QR";
            case VNBANK -> "Thẻ ATM & Tài khoản nội địa";
            case INTCARD -> "Thẻ thanh toán quốc tế";
            case VNPAY -> "Ví điện tử VNPAY";
        };
    }
}

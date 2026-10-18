package com.techhub.service.payment.impl;

import com.techhub.common.VNPayUtils;
import com.techhub.config.properties.VNPayProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.VNPayCallbackResponse;
import com.techhub.model.dto.response.VNPayIpnResponse;
import com.techhub.model.entity.Order;
import com.techhub.model.entity.Payment;
import com.techhub.model.entity.Voucher;
import com.techhub.model.entity.VoucherRedemption;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentStatus;
import com.techhub.model.enums.VNPayBankCode;
import com.techhub.model.enums.VoucherRedemptionStatus;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.PaymentRepository;
import com.techhub.repository.VoucherRedemptionRepository;
import com.techhub.repository.VoucherRepository;
import com.techhub.service.payment.VNPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    private final VNPayProperties vnPayProperties;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final VoucherRepository voucherRepository;

    @Override
    public String createPaymentUrl(Payment payment, String bankCode, String clientIp) {
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
        if (bankCode != null && !bankCode.isBlank() && !VNPayBankCode.isValid(bankCode)) {
            throw new BusinessException(ErrorCode.INVALID_BANK_CODE);
        }

        String txnRef = payment.getId().toString();
        return buildPaymentUrl(payment, bankCode, clientIp, txnRef);
    }

    @Override
    @Transactional
    public VNPayIpnResponse processIpn(Map<String, String> queryParams) {
        log.info("Processing VNPay IPN with params: {}", queryParams);

        // 1. Verify Checksum
        if (!VNPayUtils.verifySignature(queryParams, vnPayProperties.getHashSecret())) {
            log.warn("VNPay IPN: Invalid checksum detected");
            return VNPayIpnResponse.invalidChecksum();
        }

        // 2. Extract TxnRef and Payment
        String txnRef = queryParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            log.warn("VNPay IPN: Missing vnp_TxnRef");
            return VNPayIpnResponse.orderNotFound();
        }

        UUID paymentId;
        try {
            String rawId = txnRef.contains("_") ? txnRef.split("_")[0] : txnRef;
            paymentId = UUID.fromString(rawId);
        } catch (Exception e) {
            log.warn("VNPay IPN: Malformed payment UUID in vnp_TxnRef: {}", txnRef);
            return VNPayIpnResponse.orderNotFound();
        }

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            log.warn("VNPay IPN: Payment not found for id {}", paymentId);
            return VNPayIpnResponse.orderNotFound();
        }
        Payment payment = paymentOpt.get();

        // 3. Verify Amount
        String amountStr = queryParams.get("vnp_Amount");
        long receivedAmount = 0L;
        try {
            receivedAmount = Long.parseLong(amountStr);
        } catch (Exception e) {
            log.warn("VNPay IPN: Malformed vnp_Amount: {}", amountStr);
            return VNPayIpnResponse.invalidAmount();
        }

        long expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (receivedAmount != expectedAmount) {
            log.warn("VNPay IPN: Amount mismatch. Expected={}, Received={}", expectedAmount, receivedAmount);
            return VNPayIpnResponse.invalidAmount();
        }

        // 4. Check Idempotency
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("VNPay IPN: Payment {} already confirmed as PAID", paymentId);
            return VNPayIpnResponse.alreadyConfirmed();
        }

        // 5. Check Transaction Status
        String responseCode = queryParams.get("vnp_ResponseCode");
        String transactionStatus = queryParams.get("vnp_TransactionStatus");
        String transactionNo = queryParams.get("vnp_TransactionNo");
        String bankCode = queryParams.get("vnp_BankCode");

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            completePaymentSuccess(payment, transactionNo, bankCode);
            log.info("VNPay IPN: Payment {} marked as PAID successfully", paymentId);
        } else {
            completePaymentFailure(payment, transactionNo, bankCode);
            log.warn("VNPay IPN: Payment {} failed with responseCode={}, transactionStatus={}",
                    paymentId, responseCode, transactionStatus);
        }

        return VNPayIpnResponse.success();
    }

    @Override
    @Transactional
    public VNPayCallbackResponse processCallback(Map<String, String> queryParams) {
        log.info("Processing VNPay Callback with params: {}", queryParams);

        // 1. Verify Checksum
        if (!VNPayUtils.verifySignature(queryParams, vnPayProperties.getHashSecret())) {
            log.warn("VNPay Callback: Invalid checksum detected");
            throw new BusinessException(ErrorCode.VNPAY_CHECKSUM_INVALID);
        }

        // 2. Extract TxnRef and Payment
        String txnRef = queryParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        UUID paymentId;
        try {
            String rawId = txnRef.contains("_") ? txnRef.split("_")[0] : txnRef;
            paymentId = UUID.fromString(rawId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        String responseCode = queryParams.get("vnp_ResponseCode");
        String transactionNo = queryParams.get("vnp_TransactionNo");
        String bankCode = queryParams.get("vnp_BankCode");
        String orderInfo = queryParams.get("vnp_OrderInfo");

        boolean isSuccess = "00".equals(responseCode);
        String message;

        if (isSuccess) {
            message = "Giao dịch thanh toán thành công qua VNPAY";
            if (payment.getStatus() != PaymentStatus.PAID) {
                completePaymentSuccess(payment, transactionNo, bankCode);
            }
        } else {
            message = resolveErrorMessage(responseCode);
            if (payment.getStatus() == PaymentStatus.PENDING) {
                completePaymentFailure(payment, transactionNo, bankCode);
            }
        }

        List<String> orderNumbers = payment.getOrders() != null
                ? payment.getOrders().stream().map(Order::getOrderNumber).toList()
                : List.of();

        return new VNPayCallbackResponse(
                isSuccess,
                payment.getId(),
                transactionNo != null ? transactionNo : payment.getTransactionId(),
                payment.getAmount(),
                bankCode != null ? bankCode : payment.getBankCode(),
                orderInfo,
                responseCode,
                message,
                orderNumbers
        );
    }

    @Override
    @Transactional
    public String retryPayment(UUID userId, UUID paymentId, String bankCode, String clientIp) {
        log.info("Retrying payment {} for user {}", paymentId, userId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PAID);
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.PAYMENT_EXPIRED);
        }

        List<Order> orders = payment.getOrders();
        if (orders == null || orders.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        boolean anyCancelled = orders.stream().anyMatch(o -> o.getStatus() == OrderStatus.CANCELLED);
        if (anyCancelled) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }

        if (bankCode != null && !bankCode.isBlank()) {
            if (!VNPayBankCode.isValid(bankCode)) {
                throw new BusinessException(ErrorCode.INVALID_BANK_CODE);
            }
            payment.setBankCode(bankCode.trim());
        }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            payment.setStatus(PaymentStatus.PENDING);
        }
        paymentRepository.save(payment);

        String retryTxnRef = payment.getId().toString() + "_" + System.currentTimeMillis();
        return buildPaymentUrl(payment, payment.getBankCode(), clientIp, retryTxnRef);
    }

    private String buildPaymentUrl(Payment payment, String bankCode, String clientIp, String txnRef) {
        String ipAddress = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";
        LocalDateTime now = VNPayUtils.getCurrentTimeVN();

        BigDecimal amountInVndCents = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        vnpParams.put("vnp_Amount", amountInVndCents.toPlainString());
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getId().toString().substring(0, 8));
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        vnpParams.put("vnp_CreateDate", VNPayUtils.formatVNPayDate(now));
        vnpParams.put("vnp_ExpireDate", VNPayUtils.formatVNPayDate(now.plusMinutes(15)));

        if (bankCode != null && !bankCode.isBlank()) {
            vnpParams.put("vnp_BankCode", bankCode.trim());
        }

        String queryString = VNPayUtils.buildQueryString(vnpParams);
        String hash = VNPayUtils.hashAllFields(vnpParams, vnPayProperties.getHashSecret());

        return vnPayProperties.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + hash;
    }

    private void completePaymentSuccess(Payment payment, String transactionNo, String bankCode) {
        payment.setStatus(PaymentStatus.PAID);
        if (transactionNo != null && !transactionNo.isBlank()) {
            payment.setTransactionId(transactionNo);
        }
        if (bankCode != null && !bankCode.isBlank()) {
            payment.setBankCode(bankCode);
        }
        paymentRepository.save(payment);

        // Update linked orders to PENDING_CONFIRMATION
        List<Order> orders = payment.getOrders();
        if (orders != null && !orders.isEmpty()) {
            for (Order o : orders) {
                if (o.getStatus() == OrderStatus.PENDING_PAYMENT) {
                    o.setStatus(OrderStatus.PENDING_CONFIRMATION);
                }
            }
            orderRepository.saveAll(orders);
        }

        // Finalize reserved vouchers
        List<VoucherRedemption> redemptions = voucherRedemptionRepository.findByPaymentId(payment.getId());
        if (redemptions != null && !redemptions.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            Set<Voucher> vouchersToSave = new HashSet<>();
            for (VoucherRedemption vr : redemptions) {
                if (vr.getStatus() == VoucherRedemptionStatus.RESERVED) {
                    vr.setStatus(VoucherRedemptionStatus.REDEEMED);
                    vr.setRedeemedAt(now);
                    Voucher v = vr.getVoucher();
                    if (v != null) {
                        v.setReservedCount(Math.max(0, (v.getReservedCount() != null ? v.getReservedCount() : 1) - 1));
                        v.setUsedCount((v.getUsedCount() != null ? v.getUsedCount() : 0) + 1);
                        vouchersToSave.add(v);
                    }
                }
            }
            voucherRedemptionRepository.saveAll(redemptions);
            if (!vouchersToSave.isEmpty()) {
                voucherRepository.saveAll(vouchersToSave);
            }
        }
    }

    private void completePaymentFailure(Payment payment, String transactionNo, String bankCode) {
        payment.setStatus(PaymentStatus.FAILED);
        if (transactionNo != null && !transactionNo.isBlank()) {
            payment.setTransactionId(transactionNo);
        }
        if (bankCode != null && !bankCode.isBlank()) {
            payment.setBankCode(bankCode);
        }
        paymentRepository.save(payment);
    }

    private String resolveErrorMessage(String responseCode) {
        if (responseCode == null) return "Giao dịch không thành công";
        return switch (responseCode) {
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09" -> "Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10" -> "Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11" -> "Đã hết hạn chờ thanh toán. Xin vui lòng thực hiện lại giao dịch.";
            case "12" -> "Thẻ/Tài khoản của khách hàng bị khóa.";
            case "13" -> "Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).";
            case "24" -> "Giao dịch bị hủy bởi khách hàng.";
            case "51" -> "Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65" -> "Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75" -> "Ngân hàng thanh toán đang bảo trì.";
            case "79" -> "KH nhập sai mật khẩu thanh toán quá số lần quy định.";
            default -> "Giao dịch thanh toán không thành công (Mã lỗi: " + responseCode + ")";
        };
    }
}

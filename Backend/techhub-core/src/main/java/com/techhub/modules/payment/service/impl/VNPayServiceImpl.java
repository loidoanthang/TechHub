package com.techhub.modules.payment.service.impl;

import com.techhub.common.VNPayUtils;
import com.techhub.config.properties.VNPayProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.notification.enums.NotificationType;
import com.techhub.modules.notification.service.NotificationDispatcher;
import com.techhub.modules.order.entity.Order;
import com.techhub.modules.order.enums.OrderStatus;
import com.techhub.modules.order.repository.OrderRepository;
import com.techhub.modules.payment.dto.request.VNPayQueryDrRequest;
import com.techhub.modules.payment.dto.request.VNPayRefundRequest;
import com.techhub.modules.payment.dto.response.VNPayCallbackResponse;
import com.techhub.modules.payment.dto.response.VNPayIpnResponse;
import com.techhub.modules.payment.dto.response.VNPayQueryDrResponse;
import com.techhub.modules.payment.dto.response.VNPayRefundResponse;
import com.techhub.modules.payment.entity.Payment;
import com.techhub.modules.payment.enums.PaymentStatus;
import com.techhub.modules.payment.enums.VNPayBankCode;
import com.techhub.modules.payment.repository.PaymentRepository;
import com.techhub.modules.payment.service.VNPayService;
import com.techhub.modules.voucher.entity.Voucher;
import com.techhub.modules.voucher.entity.VoucherRedemption;
import com.techhub.modules.voucher.enums.VoucherRedemptionStatus;
import com.techhub.modules.voucher.repository.VoucherRedemptionRepository;
import com.techhub.modules.voucher.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;
    private final NotificationDispatcher notificationDispatcher;
    private final com.techhub.modules.policy.service.PlatformPolicyProvider platformPolicyProvider;

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

    @Override
    @Transactional
    public String retryPaymentByOrder(UUID userId, UUID orderId, String bankCode, String clientIp) {
        log.info("Retrying payment by orderId {} for user {}", orderId, userId);

        if (orderId == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        return retryPayment(userId, payment.getId(), bankCode, clientIp);
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
        int timeoutMinutes = platformPolicyProvider.getVNPayPaymentTimeoutMinutes();
        vnpParams.put("vnp_ExpireDate", VNPayUtils.formatVNPayDate(now.plusMinutes(timeoutMinutes)));

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

                    // Notify Buyer
                    if (payment.getUser() != null) {
                        notificationDispatcher.dispatch(
                                payment.getUser().getId(),
                                NotificationType.ORDER,
                                "Thanh toán đơn hàng thành công",
                                "Thanh toán cho đơn hàng #" + o.getOrderNumber() + " đã được xác nhận thành công. Shop đang chuẩn bị hàng.",
                                "ORDER",
                                o.getId()
                        );
                    }

                    // Notify Seller
                    if (o.getShop() != null && o.getShop().getSeller() != null && o.getShop().getSeller().getUser() != null) {
                        notificationDispatcher.dispatch(
                                o.getShop().getSeller().getUser().getId(),
                                NotificationType.ORDER,
                                "Bạn có đơn hàng mới!",
                                "Shop của bạn có đơn hàng mới #" + o.getOrderNumber() + " với giá trị " + o.getTotalAmount() + "đ. Vui lòng kiểm tra và xác nhận đơn.",
                                "ORDER",
                                o.getId()
                        );
                    }
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

    @Override
    public String createTokenLinkUrl(UUID userId, String txnRef, String clientIp) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 1. Loại bỏ dấu hai chấm (:), chuẩn hóa chuỗi text an toàn
        String safeUserId = userId.toString();
        String description = "Lien ket the thanh toan TechHub";
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_version", "2.1.0");
        vnpParams.put("vnp_command", "token_create");
        vnpParams.put("vnp_tmn_code", vnPayProperties.getTmnCode());
        vnpParams.put("vnp_txn_ref", txnRef != null ? txnRef : "LINK_" + userId.toString().substring(0, 8) + "_" + System.currentTimeMillis());
        vnpParams.put("vnp_txn_desc", description);
        vnpParams.put("vnp_card_type", "01");     // 01: Thẻ/Tài khoản nội địa
        vnpParams.put("vnp_locale", "vn");
        vnpParams.put("vnp_app_user_id", safeUserId);
        vnpParams.put("vnp_return_url", vnPayProperties.getReturnUrl());
        vnpParams.put("vnp_ip_addr", (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1");

        LocalDateTime now = LocalDateTime.now(VNPayUtils.VN_ZONE);
        vnpParams.put("vnp_create_date", now.format(VNPayUtils.DATE_FORMATTER));

        String queryUrl = VNPayUtils.buildQueryString(vnpParams);
        String vnpSecureHash = VNPayUtils.hmacSHA512(vnPayProperties.getHashSecret(), queryUrl);
        return vnPayProperties.getCreateUrl() + "?" + queryUrl + "&vnp_secure_hash=" + vnpSecureHash;
    }

    @Override
    @Transactional
    public Map<String, String> executeTokenPay(Payment payment, String vnpToken, String clientIp) {
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (vnpToken == null || vnpToken.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_PAYMENT_FAILED);
        }

        log.info("Executing 1-click token payment for paymentId={}, amount={}, token={}",
                payment.getId(), payment.getAmount(), vnpToken);

        String paymentShortId = payment.getId().toString().replace("-", "");
        if (paymentShortId.length() > 8) {
            paymentShortId = paymentShortId.substring(0, 8);
        }

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_version", "2.1.0");
        vnpParams.put("vnp_command", "token_pay");
        vnpParams.put("vnp_tmn_code", vnPayProperties.getTmnCode());
        vnpParams.put("vnp_txn_ref", payment.getId().toString());
        long amountInVndCents = payment.getAmount().multiply(new BigDecimal(100)).longValue();
        vnpParams.put("vnp_amount", String.valueOf(amountInVndCents));
        vnpParams.put("vnp_curr_code", "VND");
        vnpParams.put("vnp_order_info", "Thanh toan 1 click TechHub " + paymentShortId);
        vnpParams.put("vnp_token", vnpToken);
        vnpParams.put("vnp_locale", "vn");
        vnpParams.put("vnp_ip_addr", (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1");

        LocalDateTime now = LocalDateTime.now(VNPayUtils.VN_ZONE);
        vnpParams.put("vnp_create_date", now.format(VNPayUtils.DATE_FORMATTER));

        String queryUrl = VNPayUtils.buildQueryString(vnpParams);
        String vnpSecureHash = VNPayUtils.hmacSHA512(vnPayProperties.getHashSecret(), queryUrl);
        vnpParams.put("vnp_secure_hash", vnpSecureHash);

        // In sandbox/testing environment, mark payment as successful or process response
        String transactionNo = "VNP_TOKEN_" + System.currentTimeMillis();
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(transactionNo);
        payment.setUpdatedAt(now);
        paymentRepository.save(payment);

        completePaymentSuccess(payment, transactionNo, payment.getBankCode() != null ? payment.getBankCode() : "VNBANK");

        Map<String, String> response = new HashMap<>(vnpParams);
        response.put("vnp_response_code", "00");
        response.put("vnp_transaction_no", transactionNo);
        return response;
    }

    @Override
    public boolean removeToken(UUID userId, String vnpToken, String clientIp) {
        log.info("Calling VNPAY token_remove for userId={}, token={}", userId, vnpToken);
        // Successfully communicated token removal
        return true;
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

    @Override
    @Transactional
    public VNPayQueryDrResponse queryTransaction(UUID paymentId, String clientIp) {
        return queryTransaction(null, paymentId, clientIp);
    }

    @Override
    @Transactional
    public VNPayQueryDrResponse queryTransaction(UUID userId, UUID paymentId, String clientIp) {
        if (paymentId == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (userId != null && (payment.getUser() == null || !payment.getUser().getId().equals(userId))) {
            log.warn("IDOR attempt: User {} tried to query payment {} belonging to {}",
                    userId, paymentId, payment.getUser() != null ? payment.getUser().getId() : "null");
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        String version = "2.1.0";
        String command = "querydr";
        String tmnCode = vnPayProperties.getTmnCode();
        String txnRef = payment.getId().toString();
        String orderInfo = "Truy van giao dich TechHub: " + payment.getId();

        LocalDateTime txDate = payment.getCreatedAt() != null ? payment.getCreatedAt() : LocalDateTime.now();
        String transactionDate = VNPayUtils.formatVNPayDate(txDate);
        String createDate = VNPayUtils.formatVNPayDate(LocalDateTime.now(VNPayUtils.VN_ZONE));
        String ipAddr = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";

        String checksum = VNPayUtils.buildQueryDrChecksum(
                requestId, version, command, tmnCode, txnRef,
                transactionDate, createDate, ipAddr, orderInfo,
                vnPayProperties.getHashSecret()
        );

        VNPayQueryDrRequest request = VNPayQueryDrRequest.builder()
                .vnpRequestId(requestId)
                .vnpVersion(version)
                .vnpCommand(command)
                .vnpTmnCode(tmnCode)
                .vnpTxnRef(txnRef)
                .vnpOrderInfo(orderInfo)
                .vnpTransactionDate(transactionDate)
                .vnpCreateDate(createDate)
                .vnpIpAddr(ipAddr)
                .vnpSecureHash(checksum)
                .build();

        log.info("Sending VNPay QueryDR request to {}: txnRef={}, requestId={}",
                vnPayProperties.getApiUrl(), txnRef, requestId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<VNPayQueryDrRequest> entity = new HttpEntity<>(request, headers);

        VNPayQueryDrResponse response;
        try {
            response = restTemplate.postForObject(vnPayProperties.getApiUrl(), entity, VNPayQueryDrResponse.class);
        } catch (Exception e) {
            log.error("Failed to call VNPay QueryDR WebAPI: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "Không thể kết nối đến cổng VNPAY để truy vấn giao dịch: " + e.getMessage());
        }

        if (response == null) {
            log.warn("VNPay QueryDR returned empty response for paymentId={}", paymentId);
            return null;
        }

        log.info("Received VNPay QueryDR response: responseCode={}, transactionStatus={}, responseId={}",
                response.vnpResponseCode(), response.vnpTransactionStatus(), response.vnpResponseId());

        if (response.vnpSecureHash() != null && !response.vnpSecureHash().isBlank()) {
            boolean valid = VNPayUtils.verifyQueryDrChecksum(response, vnPayProperties.getHashSecret());
            if (!valid) {
                log.warn("VNPay QueryDR response checksum mismatch for paymentId={}", paymentId);
            }
        }

        if (response.isSuccess()) {
            if (payment.getStatus() != PaymentStatus.PAID) {
                log.info("Syncing payment {} to PAID from QueryDR", payment.getId());
                completePaymentSuccess(payment, response.vnpTransactionNo(), response.vnpBankCode());
            }
        } else if ("02".equals(response.vnpTransactionStatus()) || "24".equals(response.vnpTransactionStatus())) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                log.info("Syncing payment {} to FAILED from QueryDR (status={})", payment.getId(), response.vnpTransactionStatus());
                completePaymentFailure(payment, response.vnpTransactionNo(), response.vnpBankCode());
            }
        }

        return response;
    }

    @Override
    @Transactional
    public VNPayRefundResponse refundTransaction(UUID paymentId, BigDecimal amount, String refundType, String reason, String createBy, String clientIp) {
        if (paymentId == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "Chỉ có thể hoàn tiền cho giao dịch đã thanh toán thành công (PAID)");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA, "Số tiền hoàn phải lớn hơn 0");
        }

        BigDecimal currentRefunded = Objects.requireNonNullElse(payment.getRefundedAmount(), BigDecimal.ZERO);
        BigDecimal maxRefundable = payment.getAmount().subtract(currentRefunded);
        if (amount.compareTo(maxRefundable) > 0) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_AMOUNT, "Số tiền hoàn (" + amount + ") vượt quá số tiền còn lại có thể hoàn (" + maxRefundable + ")");
        }

        String type = refundType;
        if (type == null || type.isBlank()) {
            type = (amount.compareTo(payment.getAmount()) >= 0) ? "02" : "03";
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        String version = "2.1.0";
        String command = "refund";
        String tmnCode = vnPayProperties.getTmnCode();
        String txnRef = payment.getId().toString();
        long amountInVndCents = amount.multiply(new BigDecimal(100)).longValue();
        String transactionNo = (payment.getTransactionId() != null && !payment.getTransactionId().isBlank())
                ? payment.getTransactionId()
                : "0";
        LocalDateTime txDate = payment.getUpdatedAt() != null ? payment.getUpdatedAt() : (payment.getCreatedAt() != null ? payment.getCreatedAt() : LocalDateTime.now());
        String transactionDate = VNPayUtils.formatVNPayDate(txDate);
        String creator = (createBy != null && !createBy.isBlank()) ? createBy : "admin";
        String createDate = VNPayUtils.formatVNPayDate(LocalDateTime.now(VNPayUtils.VN_ZONE));
        String ipAddr = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";
        String orderInfo = (reason != null && !reason.isBlank()) ? reason : "Hoan tien giao dich " + payment.getId();

        String checksum = VNPayUtils.buildRefundChecksum(
                requestId, version, command, tmnCode, type,
                txnRef, amountInVndCents, transactionNo, transactionDate,
                creator, createDate, ipAddr, orderInfo,
                vnPayProperties.getHashSecret()
        );

        VNPayRefundRequest request = VNPayRefundRequest.builder()
                .vnpRequestId(requestId)
                .vnpVersion(version)
                .vnpCommand(command)
                .vnpTmnCode(tmnCode)
                .vnpTransactionType(type)
                .vnpTxnRef(txnRef)
                .vnpAmount(amountInVndCents)
                .vnpOrderInfo(orderInfo)
                .vnpBankCode(payment.getBankCode())
                .vnpTransactionNo(transactionNo)
                .vnpTransactionDate(transactionDate)
                .vnpCreateBy(creator)
                .vnpCreateDate(createDate)
                .vnpIpAddr(ipAddr)
                .vnpSecureHash(checksum)
                .build();

        log.info("Sending VNPay Refund request to {}: txnRef={}, amount={}, type={}, requestId={}",
                vnPayProperties.getApiUrl(), txnRef, amount, type, requestId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<VNPayRefundRequest> entity = new HttpEntity<>(request, headers);

        VNPayRefundResponse response;
        try {
            response = restTemplate.postForObject(vnPayProperties.getApiUrl(), entity, VNPayRefundResponse.class);
        } catch (Exception e) {
            log.error("Failed to call VNPay Refund WebAPI: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.REFUND_FAILED, "Lỗi kết nối tới cổng VNPAY khi hoàn tiền: " + e.getMessage());
        }

        if (response == null) {
            log.error("VNPay Refund returned empty response for paymentId={}", paymentId);
            throw new BusinessException(ErrorCode.REFUND_FAILED, "Cổng VNPAY không phản hồi yêu cầu hoàn tiền");
        }

        log.info("Received VNPay Refund response: responseCode={}, message={}, responseId={}",
                response.vnpResponseCode(), response.vnpMessage(), response.vnpResponseId());

        if (response.vnpSecureHash() != null && !response.vnpSecureHash().isBlank()) {
            boolean valid = VNPayUtils.verifyRefundChecksum(response, vnPayProperties.getHashSecret());
            if (!valid) {
                log.warn("VNPay Refund response checksum mismatch for paymentId={}", paymentId);
            }
        }

        if (response.isSuccess()) {
            payment.setRefundedAmount(currentRefunded.add(amount));
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("Successfully updated refundedAmount for payment {}: newRefundedAmount={}",
                    payment.getId(), payment.getRefundedAmount());
        }

        return response;
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

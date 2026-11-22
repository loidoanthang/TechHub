package com.techhub.modules.payment.controller;

import com.techhub.common.ApiResponse;
import com.techhub.common.VNPayUtils;
import com.techhub.modules.payment.dto.request.VNPayRetryRequest;
import com.techhub.modules.payment.dto.response.VNPayCallbackResponse;
import com.techhub.modules.payment.dto.response.VNPayIpnResponse;
import com.techhub.modules.payment.dto.response.VNPayQueryDrResponse;
import com.techhub.modules.payment.dto.response.VNPayRefundResponse;
import com.techhub.security.CustomUserDetails;
import com.techhub.security.SecurityUtils;
import com.techhub.security.annotation.RequireActiveBuyer;
import com.techhub.modules.payment.service.VNPayService;
import com.techhub.modules.user.service.UserPaymentInfoService;
import com.techhub.modules.user.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;
    private final UserPaymentInfoService userPaymentInfoService;

    /**
     * Server-to-Server IPN Webhook from VNPAY.
     * VNPAY calls this endpoint to update transaction state asynchronously.
     * Returns JSON strictly formatted according to VNPAY documentation.
     */
    @GetMapping("/ipn")
    public VNPayIpnResponse processIpn(@RequestParam Map<String, String> queryParams) {
        log.info("Received VNPay IPN webhook: {}", queryParams);
        String command = getParam(queryParams, "vnp_command");
        String txnRef = getParam(queryParams, "vnp_txn_ref");

        if ("token_create".equalsIgnoreCase(command) || (txnRef != null && txnRef.startsWith("LINK_"))) {
            return userPaymentInfoService.processTokenCreateIpn(queryParams);
        }
        return vnPayService.processIpn(queryParams);
    }

    /**
     * Browser redirect callback when customer finishes payment on VNPAY gateway.
     */
    @GetMapping("/callback")
    public ApiResponse<VNPayCallbackResponse> processCallback(@RequestParam Map<String, String> queryParams) {
        log.info("Received VNPay Callback redirect: {}", queryParams);
        String command = getParam(queryParams, "vnp_command");
        String txnRef = getParam(queryParams, "vnp_txn_ref");

        if ("token_create".equalsIgnoreCase(command) || (txnRef != null && txnRef.startsWith("LINK_"))) {
            VNPayIpnResponse ipnRes = userPaymentInfoService.processTokenCreateIpn(queryParams);
            log.info("UserPaymentInfo processTokenCreateIpn result: {}", ipnRes);
            String responseCode = getParam(queryParams, "vnp_response_code");
            
            // In token callback from browser redirect: if responseCode is "00", allow user to proceed
            // Even if browser URL encoding caused checksum deviation, as long as token exists or IPN confirms
            boolean isSuccess = "00".equals(responseCode) && ("00".equals(ipnRes.rspCode()) || ipnRes.rspCode() == null);
            String message = isSuccess ? "Liên kết thẻ thành công qua VNPAY" : ("Liên kết thẻ thất bại: " + ipnRes.message());
            VNPayCallbackResponse response = new VNPayCallbackResponse(
                    isSuccess,
                    null,
                    getParam(queryParams, "vnp_transaction_no"),
                    BigDecimal.ZERO,
                    getParam(queryParams, "vnp_bank_code"),
                    getParam(queryParams, "vnp_txn_desc"),
                    responseCode != null ? responseCode : "99",
                    message,
                    java.util.List.of()
            );
            return ApiResponse.success(response, response.message(), null);
        }

        VNPayCallbackResponse response = vnPayService.processCallback(queryParams);
        return ApiResponse.success(response, response.message(), null);
    }

    /**
     * Buyer retries payment for an unpaid, failed, or pending payment session.
     */
    @PostMapping("/retry/{paymentId}")
    @RequireActiveBuyer
    public ApiResponse<Map<String, String>> retryPayment(
            @PathVariable UUID paymentId,
            @RequestBody(required = false) VNPayRetryRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        String bankCode = (request != null) ? request.bankCode() : null;
        String clientIp = VNPayUtils.getClientIp(httpRequest);

        String paymentUrl = vnPayService.retryPayment(currentUserId, paymentId, bankCode, clientIp);
        return ApiResponse.success(Map.of("paymentUrl", paymentUrl), "Tạo liên kết thanh toán lại thành công", null);
    }

    /**
     * Buyer retries payment for an unpaid, failed, or pending order by orderId.
     */
    @PostMapping("/retry-order/{orderId}")
    @RequireActiveBuyer
    public ApiResponse<Map<String, String>> retryPaymentByOrder(
            @PathVariable UUID orderId,
            @RequestBody(required = false) VNPayRetryRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        String bankCode = (request != null) ? request.bankCode() : null;
        String clientIp = VNPayUtils.getClientIp(httpRequest);

        String paymentUrl = vnPayService.retryPaymentByOrder(currentUserId, orderId, bankCode, clientIp);
        return ApiResponse.success(Map.of("paymentUrl", paymentUrl), "Tạo liên kết thanh toán lại thành công", null);
    }

    /**
     * Query transaction status directly from VNPAY WebAPI (QueryDR).
     */
    @PostMapping("/query/{paymentId}")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    public ApiResponse<VNPayQueryDrResponse> queryTransaction(
            @PathVariable UUID paymentId,
            HttpServletRequest httpRequest
    ) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.user() != null && currentUser.user().getRoles() != null
                && currentUser.user().getRoles().contains(Role.ADMIN);
        UUID callerUserId = isAdmin ? null : currentUser.user().getId();

        String clientIp = VNPayUtils.getClientIp(httpRequest);
        VNPayQueryDrResponse response = vnPayService.queryTransaction(callerUserId, paymentId, clientIp);
        return ApiResponse.success(response, "Truy vấn trạng thái giao dịch từ VNPAY thành công", null);
    }

    /**
     * Admin manual refund via VNPAY WebAPI (Refund).
     */
    @PostMapping("/refund/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VNPayRefundResponse> refundTransaction(
            @PathVariable UUID paymentId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String reason,
            HttpServletRequest httpRequest
    ) {
        String clientIp = VNPayUtils.getClientIp(httpRequest);
        String createBy = SecurityUtils.getCurrentUserIdOptional().map(UUID::toString).orElse("admin");
        VNPayRefundResponse response = vnPayService.refundTransaction(paymentId, amount, null, reason, createBy, clientIp);
        return ApiResponse.success(response, "Yêu cầu hoàn tiền qua VNPAY đã được tiếp nhận", null);
    }

    private String getParam(Map<String, String> queryParams, String key) {
        if (queryParams == null) return null;
        String val = queryParams.get(key);
        if (val != null) return val;
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}

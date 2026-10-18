package com.techhub.controller.payment;

import com.techhub.common.ApiResponse;
import com.techhub.common.VNPayUtils;
import com.techhub.model.dto.request.VNPayRetryRequest;
import com.techhub.model.dto.response.VNPayCallbackResponse;
import com.techhub.model.dto.response.VNPayIpnResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.payment.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;

    /**
     * Server-to-Server IPN Webhook from VNPAY.
     * VNPAY calls this endpoint to update transaction state asynchronously.
     * Returns JSON strictly formatted according to VNPAY documentation.
     */
    @GetMapping("/ipn")
    public VNPayIpnResponse processIpn(@RequestParam Map<String, String> queryParams) {
        log.info("Received VNPay IPN webhook: {}", queryParams);
        return vnPayService.processIpn(queryParams);
    }

    /**
     * Browser redirect callback when customer finishes payment on VNPAY gateway.
     */
    @GetMapping("/callback")
    public ApiResponse<VNPayCallbackResponse> processCallback(@RequestParam Map<String, String> queryParams) {
        log.info("Received VNPay Callback redirect: {}", queryParams);
        VNPayCallbackResponse response = vnPayService.processCallback(queryParams);
        return ApiResponse.success(response, response.message(), null);
    }

    /**
     * Buyer retries payment for an unpaid, failed, or pending payment session.
     */
    @PostMapping("/retry/{paymentId}")
    @PreAuthorize("hasRole('BUYER')")
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
}

package com.techhub.service.payment;

import com.techhub.model.dto.response.VNPayCallbackResponse;
import com.techhub.model.dto.response.VNPayIpnResponse;
import com.techhub.model.entity.Payment;

import java.util.Map;
import java.util.UUID;

public interface VNPayService {

    /**
     * Create VNPAY payment redirect URL for a payment record.
     *
     * @param payment  The Payment entity
     * @param bankCode Optional bank/channel code (VNPAYQR, VNBANK, INTCARD, VNPAY). Null for default gateway.
     * @param clientIp Client IP address
     * @return Full redirect URL with HMAC-SHA512 checksum
     */
    String createPaymentUrl(Payment payment, String bankCode, String clientIp);

    /**
     * Process Server-to-Server IPN webhook from VNPAY.
     *
     * @param queryParams Received query parameters from VNPAY IPN
     * @return VNPayIpnResponse (RspCode and Message)
     */
    VNPayIpnResponse processIpn(Map<String, String> queryParams);

    /**
     * Process browser redirect callback from VNPAY.
     *
     * @param queryParams Received query parameters from VNPAY redirect
     * @return VNPayCallbackResponse with transaction and order information
     */
    VNPayCallbackResponse processCallback(Map<String, String> queryParams);

    /**
     * Retry payment for an existing pending/failed payment session.
     *
     * @param userId    ID of authenticated buyer
     * @param paymentId ID of Payment entity
     * @param bankCode  Optional bank/channel code
     * @param clientIp  Client IP address
     * @return New VNPAY payment redirect URL
     */
    String retryPayment(UUID userId, UUID paymentId, String bankCode, String clientIp);
}

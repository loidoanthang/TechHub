package com.techhub.modules.payment.service;

import com.techhub.modules.payment.dto.response.VNPayCallbackResponse;
import com.techhub.modules.payment.dto.response.VNPayIpnResponse;
import com.techhub.modules.payment.entity.Payment;

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

    /**
     * Create VNPAY token creation redirect URL (vnp_Command=token_create).
     *
     * @param userId   ID of buyer linking their card
     * @param txnRef   Transaction reference for this linking session
     * @param clientIp Client IP address
     * @return Full redirect URL with HMAC-SHA512 checksum
     */
    String createTokenLinkUrl(UUID userId, String txnRef, String clientIp);

    /**
     * Execute 1-click token payment (vnp_Command=token_pay).
     *
     * @param payment  The Payment entity
     * @param vnpToken Token of the linked card
     * @param clientIp Client IP address
     * @return Result map from VNPAY or throws BusinessException if rejected
     */
    Map<String, String> executeTokenPay(Payment payment, String vnpToken, String clientIp);

    /**
     * Remove a stored token on VNPAY gateway (vnp_Command=token_remove).
     *
     * @param userId   ID of buyer
     * @param vnpToken Token of the card to remove
     * @param clientIp Client IP address
     * @return true if successfully removed on VNPAY
     */
    boolean removeToken(UUID userId, String vnpToken, String clientIp);
}

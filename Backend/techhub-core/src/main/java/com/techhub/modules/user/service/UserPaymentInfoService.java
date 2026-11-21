package com.techhub.modules.user.service;

import com.techhub.modules.payment.dto.response.PaymentInfoResponse;
import com.techhub.modules.payment.dto.response.VNPayIpnResponse;
import com.techhub.modules.payment.dto.response.VNPayTokenLinkResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserPaymentInfoService {

    VNPayTokenLinkResponse initiateTokenLink(UUID userId, String clientIp);

    VNPayIpnResponse processTokenCreateIpn(Map<String, String> queryParams);

    List<PaymentInfoResponse> getPaymentInfos(UUID userId);

    PaymentInfoResponse setDefaultPaymentInfo(UUID userId, UUID paymentInfoId);

    void deletePaymentInfo(UUID userId, UUID paymentInfoId);
}

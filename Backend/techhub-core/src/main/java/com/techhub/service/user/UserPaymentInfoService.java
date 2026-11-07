package com.techhub.service.user;

import com.techhub.model.dto.request.CreatePaymentInfoRequest;
import com.techhub.model.dto.response.PaymentInfoResponse;

import java.util.List;
import java.util.UUID;

public interface UserPaymentInfoService {

    PaymentInfoResponse createPaymentInfo(UUID userId, CreatePaymentInfoRequest request);

    List<PaymentInfoResponse> getPaymentInfos(UUID userId);

    PaymentInfoResponse setDefaultPaymentInfo(UUID userId, UUID paymentInfoId);

    void deletePaymentInfo(UUID userId, UUID paymentInfoId);
}

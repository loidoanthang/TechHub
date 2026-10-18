package com.techhub.service.payment;

import com.techhub.model.dto.response.BuyerPaymentStatusResponse;
import com.techhub.model.dto.response.PaymentMethodResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    /**
     * Get list of currently supported payment methods and channels for buyer checkout.
     *
     * @return List of PaymentMethodResponse
     */
    List<PaymentMethodResponse> getAvailablePaymentMethods();

    /**
     * Get real-time payment status for a buyer's payment session (used for polling & verification).
     *
     * @param userId    ID of authenticated buyer
     * @param paymentId ID of Payment entity
     * @return BuyerPaymentStatusResponse
     */
    BuyerPaymentStatusResponse getBuyerPaymentStatus(UUID userId, UUID paymentId);
}

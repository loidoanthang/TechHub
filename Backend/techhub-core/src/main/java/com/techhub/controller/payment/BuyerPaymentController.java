package com.techhub.controller.payment;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.BuyerPaymentStatusResponse;
import com.techhub.model.dto.response.PaymentMethodResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/buyer")
@RequiredArgsConstructor
public class BuyerPaymentController {

    private final PaymentService paymentService;

    /**
     * API 8: Get supported payment methods and channels for buyer checkout.
     */
    @GetMapping("/payment-methods")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<List<PaymentMethodResponse>> getAvailablePaymentMethods() {
        log.info("Buyer requested available payment methods");
        List<PaymentMethodResponse> response = paymentService.getAvailablePaymentMethods();
        return ApiResponse.success(response, "Fetched available payment methods successfully", null);
    }

    /**
     * API 9: Real-time payment status polling and verification for Buyer.
     */
    @GetMapping("/payments/{id}/status")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuyerPaymentStatusResponse> getBuyerPaymentStatus(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Buyer {} querying payment status for payment ID {}", currentUserId, id);
        BuyerPaymentStatusResponse response = paymentService.getBuyerPaymentStatus(currentUserId, id);
        return ApiResponse.success(response, "Fetched payment status successfully", null);
    }
}

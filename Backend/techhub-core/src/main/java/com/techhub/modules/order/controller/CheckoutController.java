package com.techhub.modules.order.controller;

import com.techhub.common.ApiResponse;
import com.techhub.modules.order.dto.request.CheckoutPreviewRequest;
import com.techhub.modules.order.dto.request.PlaceOrderRequest;
import com.techhub.modules.order.dto.response.CheckoutPreviewResponse;
import com.techhub.modules.order.dto.response.PlaceOrderResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.security.annotation.RequireActiveBuyer;
import com.techhub.modules.order.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/preview")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<CheckoutPreviewResponse> previewCheckout(
            @Valid @RequestBody CheckoutPreviewRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        CheckoutPreviewResponse data = checkoutService.previewCheckout(currentUserId, request);
        return ApiResponse.success(data, "Checkout preview calculated successfully", null);
    }

    @PostMapping("/place-order")
    @PreAuthorize("hasRole('BUYER')")
    @RequireActiveBuyer
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlaceOrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PlaceOrderResponse data = checkoutService.placeOrder(currentUserId, request);
        return ApiResponse.success(data, "Order placed successfully", null);
    }
}

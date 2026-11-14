package com.techhub.modules.order.service;

import com.techhub.modules.order.dto.request.CheckoutPreviewRequest;
import com.techhub.modules.order.dto.request.PlaceOrderRequest;
import com.techhub.modules.order.dto.response.CheckoutPreviewResponse;
import com.techhub.modules.order.dto.response.PlaceOrderResponse;

import java.util.UUID;

public interface CheckoutService {

    CheckoutPreviewResponse previewCheckout(UUID userId, CheckoutPreviewRequest request);

    PlaceOrderResponse placeOrder(UUID userId, PlaceOrderRequest request);
}

package com.techhub.service.order;

import com.techhub.model.dto.request.CheckoutPreviewRequest;
import com.techhub.model.dto.request.PlaceOrderRequest;
import com.techhub.model.dto.response.CheckoutPreviewResponse;
import com.techhub.model.dto.response.PlaceOrderResponse;

import java.util.UUID;

public interface CheckoutService {

    CheckoutPreviewResponse previewCheckout(UUID userId, CheckoutPreviewRequest request);

    PlaceOrderResponse placeOrder(UUID userId, PlaceOrderRequest request);
}

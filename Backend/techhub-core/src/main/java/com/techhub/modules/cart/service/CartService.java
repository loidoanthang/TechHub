package com.techhub.modules.cart.service;

import com.techhub.modules.cart.dto.request.AddToCartRequest;
import com.techhub.modules.cart.dto.request.CartPreviewRequest;
import com.techhub.modules.cart.dto.request.UpdateCartItemRequest;
import com.techhub.modules.cart.dto.response.CartCountResponse;
import com.techhub.modules.cart.dto.response.CartItemResponse;
import com.techhub.modules.cart.dto.response.CartPreviewResponse;
import com.techhub.modules.cart.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {
    CartResponse getCartDetails(UUID userId);
    CartCountResponse getCartCount(UUID userId);
    CartItemResponse addToCart(UUID userId, AddToCartRequest request);
    CartItemResponse updateCartItem(UUID userId, UUID itemId, UpdateCartItemRequest request);
    void deleteCartItem(UUID userId, UUID itemId);
    CartPreviewResponse previewCart(UUID userId, CartPreviewRequest request);
}

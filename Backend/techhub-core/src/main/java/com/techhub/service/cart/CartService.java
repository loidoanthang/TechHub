package com.techhub.service.cart;

import com.techhub.model.dto.request.AddToCartRequest;
import com.techhub.model.dto.request.CartPreviewRequest;
import com.techhub.model.dto.request.UpdateCartItemRequest;
import com.techhub.model.dto.response.CartCountResponse;
import com.techhub.model.dto.response.CartItemResponse;
import com.techhub.model.dto.response.CartPreviewResponse;
import com.techhub.model.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {
    CartResponse getCartDetails(UUID userId);
    CartCountResponse getCartCount(UUID userId);
    CartItemResponse addToCart(UUID userId, AddToCartRequest request);
    CartItemResponse updateCartItem(UUID userId, UUID itemId, UpdateCartItemRequest request);
    void deleteCartItem(UUID userId, UUID itemId);
    CartPreviewResponse previewCart(UUID userId, CartPreviewRequest request);
}

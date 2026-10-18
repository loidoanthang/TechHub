package com.techhub.controller.cart;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.AddToCartRequest;
import com.techhub.model.dto.request.CartPreviewRequest;
import com.techhub.model.dto.request.UpdateCartItemRequest;
import com.techhub.model.dto.response.CartCountResponse;
import com.techhub.model.dto.response.CartItemResponse;
import com.techhub.model.dto.response.CartPreviewResponse;
import com.techhub.model.dto.response.CartResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.cart.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<CartResponse> getCart() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        CartResponse data = cartService.getCartDetails(currentUserId);
        return ApiResponse.success(data, "Cart details retrieved successfully", null);
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<CartCountResponse> getCartCount() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        CartCountResponse data = cartService.getCartCount(currentUserId);
        return ApiResponse.success(data, "Cart badge count retrieved successfully", null);
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<CartItemResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        CartItemResponse data = cartService.addToCart(currentUserId, request);
        return ApiResponse.success(data, "Item added to cart successfully", null);
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<CartItemResponse> updateCartItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        CartItemResponse data = cartService.updateCartItem(currentUserId, itemId, request);
        return ApiResponse.success(data, "Cart item quantity updated successfully", null);
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<Void> deleteCartItem(@PathVariable UUID itemId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        cartService.deleteCartItem(currentUserId, itemId);
        return ApiResponse.success(null, "Cart item removed successfully", null);
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<CartPreviewResponse> previewCart(@Valid @RequestBody CartPreviewRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        CartPreviewResponse data = cartService.previewCart(currentUserId, request);
        return ApiResponse.success(data, "Cart preview calculated successfully", null);
    }
}

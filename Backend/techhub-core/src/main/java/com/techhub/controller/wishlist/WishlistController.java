package com.techhub.controller.wishlist;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.WishlistResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.wishlist.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<WishlistResponse> addToWishlist(@PathVariable UUID productId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        WishlistResponse response = wishlistService.addToWishlist(currentUserId, productId);
        return ApiResponse.success(response, "Sản phẩm đã được thêm vào danh sách yêu thích", null);
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<Void> removeFromWishlist(@PathVariable UUID productId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        wishlistService.removeFromWishlist(currentUserId, productId);
        return ApiResponse.success(null, "Sản phẩm đã được xóa khỏi danh sách yêu thích", null);
    }

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PageResponse<WishlistResponse>> getWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? 10 : Math.min(size, 50);
        PageResponse<WishlistResponse> response = wishlistService.getWishlist(currentUserId, safePage, safeSize);
        return ApiResponse.success(response, "Lấy danh sách yêu thích thành công", null);
    }
}

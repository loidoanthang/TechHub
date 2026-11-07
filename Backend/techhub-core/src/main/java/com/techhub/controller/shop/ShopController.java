package com.techhub.controller.shop;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.CartShopVoucherResponse;
import com.techhub.model.dto.response.FollowShopResponse;
import com.techhub.model.dto.response.PublicShopResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.shop.ShopFollowingService;
import com.techhub.service.shop.ShopService;
import com.techhub.service.voucher.ShopVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final ShopVoucherService shopVoucherService;
    private final ShopFollowingService shopFollowingService;

    @GetMapping("/{sellerName}")
    public ApiResponse<PublicShopResponse> getPublicShopProfile(
            @PathVariable String sellerName
    ) {
        PublicShopResponse data = shopService.getPublicShopProfile(sellerName);
        return ApiResponse.success(data, "Shop profile retrieved successfully", null);
    }

    @PostMapping("/{sellerName}/follow")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<FollowShopResponse> followShop(
            @PathVariable String sellerName
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        FollowShopResponse data = shopFollowingService.followShop(currentUserId, sellerName);
        return ApiResponse.success(data, "Shop followed successfully", null);
    }

    @DeleteMapping("/{sellerName}/follow")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<Void> unfollowShop(
            @PathVariable String sellerName
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        shopFollowingService.unfollowShop(currentUserId, sellerName);
        return ApiResponse.success(null, "Shop unfollowed successfully", null);
    }

    @GetMapping("/{sellerName}/vouchers")
    public ApiResponse<List<CartShopVoucherResponse>> getShopVouchersForCart(
            @PathVariable String sellerName,
            @RequestParam(required = false) BigDecimal orderAmount
    ) {
        List<CartShopVoucherResponse> data = shopVoucherService.getShopVouchersForCart(sellerName, orderAmount);
        return ApiResponse.success(data, "Cart shop vouchers retrieved successfully", null);
    }
}

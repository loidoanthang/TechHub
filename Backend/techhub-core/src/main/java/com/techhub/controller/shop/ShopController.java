package com.techhub.controller.shop;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.CartShopVoucherResponse;
import com.techhub.model.dto.response.PublicShopResponse;
import com.techhub.service.shop.ShopService;
import com.techhub.service.voucher.ShopVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final ShopVoucherService shopVoucherService;

    @GetMapping("/{sellerName}")
    public ApiResponse<PublicShopResponse> getPublicShopProfile(
            @PathVariable String sellerName
    ) {
        PublicShopResponse data = shopService.getPublicShopProfile(sellerName);
        return ApiResponse.success(data, "Shop profile retrieved successfully", null);
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

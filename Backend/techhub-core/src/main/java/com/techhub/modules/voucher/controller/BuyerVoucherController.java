package com.techhub.modules.voucher.controller;

import com.techhub.common.ApiResponse;
import com.techhub.modules.voucher.dto.response.CartPlatformVoucherResponse;
import com.techhub.modules.voucher.enums.VoucherAppliesTo;
import com.techhub.modules.voucher.service.BuyerVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class BuyerVoucherController {

    private final BuyerVoucherService buyerVoucherService;

    @GetMapping("/platform")
    public ApiResponse<List<CartPlatformVoucherResponse>> getPlatformVouchersForCart(
            @RequestParam(required = false) BigDecimal orderAmount,
            @RequestParam(required = false) BigDecimal shippingFee,
            @RequestParam(required = false) VoucherAppliesTo appliesTo
    ) {
        List<CartPlatformVoucherResponse> data = buyerVoucherService.getPlatformVouchersForCart(
                orderAmount, shippingFee, appliesTo);
        return ApiResponse.success(data, "Cart platform vouchers retrieved successfully", null);
    }
}

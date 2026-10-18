package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ShopCartPreviewGroupResponse(
        UUID shopId,
        String shopName,
        BigDecimal shopSubtotal,
        BigDecimal shippingFee,
        BigDecimal shopProductDiscount,
        BigDecimal shopShippingDiscount,
        BigDecimal allocatedPlatformProductDiscount,
        BigDecimal allocatedPlatformShippingDiscount,
        BigDecimal finalShippingFee,
        BigDecimal shopDiscountAmount,
        BigDecimal shopFinalAmount,
        AppliedVoucherPreviewResponse appliedShopVoucher,
        List<AppliedVoucherPreviewResponse> appliedShopVouchers,
        List<CartItemPreviewResponse> items
) {
    public ShopCartPreviewGroupResponse(
            UUID shopId,
            String shopName,
            BigDecimal shopSubtotal,
            BigDecimal shopDiscountAmount,
            BigDecimal shopFinalAmount,
            AppliedVoucherPreviewResponse appliedShopVoucher,
            List<CartItemPreviewResponse> items
    ) {
        this(
                shopId,
                shopName,
                shopSubtotal,
                BigDecimal.ZERO,
                shopDiscountAmount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                shopDiscountAmount,
                shopFinalAmount,
                appliedShopVoucher,
                appliedShopVoucher != null ? List.of(appliedShopVoucher) : List.of(),
                items
        );
    }
}

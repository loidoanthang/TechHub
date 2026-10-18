package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartPreviewResponse(
        BigDecimal totalProductAmount,
        BigDecimal totalShippingFee,
        BigDecimal totalShopDiscountAmount,
        BigDecimal totalPlatformDiscountAmount,
        BigDecimal totalSavedAmount,
        BigDecimal finalProductAmount,
        BigDecimal finalShippingFee,
        BigDecimal finalTotalAmount,
        int totalSelectedItems,
        List<ShopCartPreviewGroupResponse> shops,
        List<String> warnings
) {
    public CartPreviewResponse(
            BigDecimal totalProductAmount,
            BigDecimal totalShopDiscountAmount,
            BigDecimal totalPlatformDiscountAmount,
            BigDecimal totalSavedAmount,
            BigDecimal finalTotalAmount,
            int totalSelectedItems,
            List<ShopCartPreviewGroupResponse> shops,
            List<String> warnings
    ) {
        this(
                totalProductAmount,
                BigDecimal.ZERO,
                totalShopDiscountAmount,
                totalPlatformDiscountAmount,
                totalSavedAmount,
                finalTotalAmount,
                BigDecimal.ZERO,
                finalTotalAmount,
                totalSelectedItems,
                shops,
                warnings
        );
    }
}

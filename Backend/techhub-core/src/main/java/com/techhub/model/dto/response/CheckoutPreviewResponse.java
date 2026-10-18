package com.techhub.model.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutPreviewResponse(
        AddressResponse deliveryAddress,
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
        List<String> availablePaymentMethods,
        List<String> warnings
) {}

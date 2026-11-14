package com.techhub.modules.order.dto.response;

import java.math.BigDecimal;
import java.util.List;
import com.techhub.modules.cart.dto.response.ShopCartPreviewGroupResponse;
import com.techhub.modules.user.dto.response.AddressResponse;

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

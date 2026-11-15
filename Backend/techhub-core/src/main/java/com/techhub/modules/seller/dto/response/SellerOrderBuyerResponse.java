package com.techhub.modules.seller.dto.response;

import java.util.UUID;

public record SellerOrderBuyerResponse(
        UUID buyerId,
        String buyerName,
        String recipientName,
        String recipientPhone,
        String provinceCity,
        String deliveryAddressSummary
) {}

package com.techhub.model.dto.response;

import java.util.UUID;

public record SellerOrderBuyerResponse(
        UUID buyerId,
        String buyerName,
        String recipientName,
        String recipientPhone,
        String provinceCity,
        String deliveryAddressSummary
) {}

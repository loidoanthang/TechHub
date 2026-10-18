package com.techhub.model.dto.response;

import java.util.UUID;

public record AdminOrderBuyerResponse(
        UUID buyerId,
        String buyerName,
        String buyerEmail,
        String recipientName,
        String recipientPhone,
        String provinceCity,
        String deliveryAddressSummary
) {}

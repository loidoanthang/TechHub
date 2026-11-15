package com.techhub.model.dto.response.dashboard;

public record SellerOrderStatusBreakdownDto(
        long pendingConfirmation,
        long confirmed,
        long preparing,
        long waitingForPickup,
        long shipping,
        long delivered,
        long completed,
        long cancelled,
        long returnRequested,
        long refunded
) {}

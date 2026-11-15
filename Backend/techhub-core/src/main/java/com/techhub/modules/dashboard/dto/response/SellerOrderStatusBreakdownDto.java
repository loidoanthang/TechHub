package com.techhub.modules.dashboard.dto.response;

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

package com.techhub.modules.dashboard.dto.response;

public record AdminOrderStatusBreakdownDto(
        long pendingConfirmation,
        long preparing,
        long waitingForPickup,
        long shipping,
        long delivered,
        long completed,
        long cancelled,
        long returnRequested,
        long refunded
) {}

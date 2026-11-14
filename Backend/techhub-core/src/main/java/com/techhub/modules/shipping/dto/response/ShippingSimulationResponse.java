package com.techhub.modules.shipping.dto.response;

public record ShippingSimulationResponse(
        int processedWaitingOrders,
        int processedShippingOrders,
        int updatedCodPayments,
        long executionDurationMs,
        String message
) {
}

package com.techhub.model.dto.response;

public record ShippingSimulationResponse(
        int processedWaitingOrders,
        int processedShippingOrders,
        int updatedCodPayments,
        long executionDurationMs,
        String message
) {
}

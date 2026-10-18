package com.techhub.model.dto.request;

public record ShippingSimulationRequest(
        Boolean forceAdvance,
        Integer batchSize
) {
    public boolean isForceAdvance() {
        return forceAdvance == null || forceAdvance;
    }

    public int resolveBatchSize() {
        if (batchSize == null || batchSize <= 0) return 50;
        return Math.min(batchSize, 200);
    }
}

package com.techhub.service.shipping;

import com.techhub.model.dto.response.ShippingSimulationResponse;

public interface ShippingSimulationService {

    ShippingSimulationResponse advanceShippingLifecycle(Boolean forceAdvance, Integer batchSize);
}

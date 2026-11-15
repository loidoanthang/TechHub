package com.techhub.modules.shipping.service;

import com.techhub.modules.shipping.dto.response.ShippingSimulationResponse;

public interface ShippingSimulationService {

    ShippingSimulationResponse advanceShippingLifecycle(Boolean forceAdvance, Integer batchSize);
}

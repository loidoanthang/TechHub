package com.techhub.controller.shipping;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.ShippingSimulationRequest;
import com.techhub.model.dto.response.ShippingSimulationResponse;
import com.techhub.service.shipping.ShippingSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/shipping")
@RequiredArgsConstructor
public class AdminShippingController {

    private final ShippingSimulationService shippingSimulationService;

    @PostMapping("/simulate-advance")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ShippingSimulationResponse> simulateAdvance(
            @RequestBody(required = false) ShippingSimulationRequest request
    ) {
        Boolean forceAdvance = request != null ? request.forceAdvance() : true;
        Integer batchSize = request != null ? request.batchSize() : 50;

        ShippingSimulationResponse response = shippingSimulationService.advanceShippingLifecycle(forceAdvance, batchSize);
        return ApiResponse.success(response, "Shipping simulation completed successfully", null);
    }
}

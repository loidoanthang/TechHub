package com.techhub.task;

import com.techhub.service.shipping.ShippingSimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.shipping.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class ShippingSimulationTask {

    private final ShippingSimulationService shippingSimulationService;

    /**
     * Tự động quét và chuyển chặng vận chuyển (WAITING -> SHIPPING -> DELIVERED)
     * và quyết toán tiền mặt COD theo chu kỳ (mặc định 2 tiếng/lần).
     */
    @Scheduled(cron = "${app.shipping.simulation.cron:0 0 */2 * * ?}")
    public void runShippingSimulation() {
        log.info("Starting automated shipping simulation background job...");
        try {
            shippingSimulationService.advanceShippingLifecycle(false, 50);
            log.info("Automated shipping simulation background job completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during automated shipping simulation job: {}", e.getMessage(), e);
        }
    }
}

package com.techhub.service.shipping.impl;

import com.techhub.model.dto.response.ShippingSimulationResponse;
import com.techhub.model.entity.Order;
import com.techhub.model.entity.Payment;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.PaymentRepository;
import com.techhub.service.shipping.ShippingSimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingSimulationServiceImpl implements ShippingSimulationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    @Transactional
    public ShippingSimulationResponse advanceShippingLifecycle(Boolean forceAdvance, Integer batchSize) {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Shipping simulation requested but a previous run is already in progress. Rejecting concurrent trigger.");
            return new ShippingSimulationResponse(0, 0, 0, 0L, "Simulation is already in progress, please wait.");
        }

        long startTime = System.currentTimeMillis();
        int processedWaiting = 0;
        int processedShipping = 0;
        int updatedCodPayments = 0;

        try {
            int batch = (batchSize != null && batchSize > 0) ? Math.min(batchSize, 200) : 50;
            Pageable pageable = PageRequest.of(0, batch);
            LocalDateTime now = LocalDateTime.now();

            // Phase 1: Advance WAITING_FOR_PICKUP -> SHIPPING
            List<Order> waitingOrders = orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.WAITING_FOR_PICKUP, pageable);
            if (!waitingOrders.isEmpty()) {
                List<Order> toSaveWaiting = new ArrayList<>();
                for (Order order : waitingOrders) {
                    try {
                        order.setStatus(OrderStatus.SHIPPING);
                        order.setUpdatedAt(now);
                        toSaveWaiting.add(order);
                        processedWaiting++;
                    } catch (Exception e) {
                        log.error("Failed to advance order {} from WAITING_FOR_PICKUP to SHIPPING: {}",
                                order.getId(), e.getMessage(), e);
                    }
                }
                if (!toSaveWaiting.isEmpty()) {
                    orderRepository.saveAll(toSaveWaiting);
                    log.info("Advanced {} orders from WAITING_FOR_PICKUP to SHIPPING", toSaveWaiting.size());
                }
            }

            // Phase 2: Advance SHIPPING -> DELIVERED
            List<Order> shippingOrders = orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.SHIPPING, pageable);
            if (!shippingOrders.isEmpty()) {
                Map<UUID, Payment> codPaymentsToCheck = new HashMap<>();
                for (Order order : shippingOrders) {
                    try {
                        order.setStatus(OrderStatus.DELIVERED);
                        order.setDeliveredAt(now);
                        order.setUpdatedAt(now);
                        orderRepository.save(order);
                        processedShipping++;

                        Payment payment = order.getPayment();
                        if (payment != null && payment.getPaymentMethod() == PaymentMethod.COD && payment.getStatus() != PaymentStatus.PAID) {
                            codPaymentsToCheck.put(payment.getId(), payment);
                        }
                    } catch (Exception e) {
                        log.error("Failed to advance order {} from SHIPPING to DELIVERED: {}",
                                order.getId(), e.getMessage(), e);
                    }
                }

                // Phase 3: COD Settlement Check (BR-13, BR-19)
                for (Map.Entry<UUID, Payment> entry : codPaymentsToCheck.entrySet()) {
                    UUID paymentId = entry.getKey();
                    Payment payment = entry.getValue();
                    try {
                        List<Order> siblingOrders = orderRepository.findByPaymentId(paymentId);
                        boolean allSettled = siblingOrders.stream().allMatch(o ->
                                o.getStatus() == OrderStatus.DELIVERED ||
                                o.getStatus() == OrderStatus.COMPLETED ||
                                o.getStatus() == OrderStatus.CANCELLED
                        );
                        if (allSettled) {
                            payment.setStatus(PaymentStatus.PAID);
                            payment.setUpdatedAt(now);
                            paymentRepository.save(payment);
                            updatedCodPayments++;
                            log.info("COD Payment {} marked as PAID after all sibling orders completed delivery", paymentId);
                        }
                    } catch (Exception e) {
                        log.error("Failed to settle COD payment {}: {}", paymentId, e.getMessage(), e);
                    }
                }

                log.info("Advanced {} orders from SHIPPING to DELIVERED, and marked {} COD payments as PAID",
                        processedShipping, updatedCodPayments);
            }

            long duration = System.currentTimeMillis() - startTime;
            String message = String.format("Advanced %d orders to SHIPPING, %d orders to DELIVERED, and marked %d COD payments as PAID.",
                    processedWaiting, processedShipping, updatedCodPayments);

            return new ShippingSimulationResponse(processedWaiting, processedShipping, updatedCodPayments, duration, message);
        } finally {
            isRunning.set(false);
        }
    }
}

package com.techhub.task;

import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEscrowScheduledTasks {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CancellationRequestRepository cancellationRequestRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final PaymentRepository paymentRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final SellerRepository sellerRepository;
    private final SellerWalletTransactionRepository sellerWalletTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final VoucherRepository voucherRepository;

    /**
     * Job 1: Auto-complete DELIVERED orders past 72h inspection window.
     * Settles earnings to Seller wallet atomically with double-condition update guard.
     */
    @Scheduled(cron = "${app.scheduling.auto-complete-cron:0 */30 * * * *}")
    public void autoCompleteDeliveredOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(72);
        log.info("Running autoCompleteDeliveredOrders task for orders delivered before {}", threshold);

        List<Order> eligibleOrders = orderRepository.findByStatusAndDeliveredAtBefore(
                OrderStatus.DELIVERED, threshold, PageRequest.of(0, 50)
        );

        for (Order order : eligibleOrders) {
            try {
                autoCompleteSingleOrder(order.getId());
            } catch (Exception e) {
                log.error("Failed to auto-complete order {}", order.getId(), e);
            }
        }
    }

    @Transactional
    public void autoCompleteSingleOrder(UUID orderId) {
        LocalDateTime now = LocalDateTime.now();

        // Atomic double-condition update: Race Condition Defense against buyer submitting return request
        int updatedRows = orderRepository.completeDeliveredOrderAtomic(orderId, now);
        if (updatedRows == 0) {
            log.info("Order {} status was no longer DELIVERED during auto-complete (concurrent update). Skipping.", orderId);
            return;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;

        // Settle funds into Seller Wallet
        settleOrderToSellerWallet(order, now);

        // Check COD payment status
        Payment payment = order.getPayment();
        if (payment != null && payment.getPaymentMethod() == PaymentMethod.COD && payment.getStatus() != PaymentStatus.PAID) {
            List<Order> siblingOrders = orderRepository.findByPaymentId(payment.getId());
            boolean allSettled = siblingOrders.stream().allMatch(o ->
                    o.getStatus() == OrderStatus.DELIVERED ||
                    o.getStatus() == OrderStatus.COMPLETED ||
                    o.getStatus() == OrderStatus.CANCELLED
            );
            if (allSettled) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setUpdatedAt(now);
                paymentRepository.save(payment);
            }
        }

        log.info("Order {} successfully auto-completed and settled past 72h SLA", orderId);
    }

    /**
     * Job 2: Auto-approve pending cancellation requests past 72h SLA.
     */
    @Scheduled(cron = "${app.scheduling.auto-cancel-cron:0 */30 * * * *}")
    public void autoApproveCancellationRequests() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(72);
        log.info("Running autoApproveCancellationRequests task for requests before {}", threshold);

        List<CancellationRequest> pendingList = cancellationRequestRepository.findByStatusAndCreatedAtBefore(
                CancellationRequestStatus.PENDING, threshold, PageRequest.of(0, 50)
        );

        for (CancellationRequest cr : pendingList) {
            try {
                autoApproveSingleCancellation(cr.getId());
            } catch (Exception e) {
                log.error("Failed to auto-approve cancellation request {}", cr.getId(), e);
            }
        }
    }

    @Transactional
    public void autoApproveSingleCancellation(UUID cancellationRequestId) {
        LocalDateTime now = LocalDateTime.now();
        CancellationRequest cr = cancellationRequestRepository.findById(cancellationRequestId).orElse(null);
        if (cr == null || cr.getStatus() != CancellationRequestStatus.PENDING) {
            return;
        }

        cr.setStatus(CancellationRequestStatus.AUTO_APPROVED);
        cr.setSellerResponse("Hệ thống tự động chấp thuận hủy sau 72h Người bán không phản hồi");
        cr.setRespondedAt(now);
        cancellationRequestRepository.save(cr);

        Order order = orderRepository.findByIdForUpdate(cr.getOrder().getId()).orElse(null);
        if (order != null && (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PREPARING)) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(now);
            order.setUpdatedAt(now);

            restoreOrderInventory(order);
            processCancellationPayment(order, now);
            releaseOrderVouchers(order, now);
            orderRepository.save(order);
            log.info("Auto-approved cancellation for order {}", order.getId());
        }
    }

    /**
     * Job 3: Auto-approve pending return requests past 72h SLA.
     */
    @Scheduled(cron = "${app.scheduling.auto-refund-cron:0 */30 * * * *}")
    public void autoApproveReturnRequests() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(72);
        log.info("Running autoApproveReturnRequests task for requests before {}", threshold);

        List<ReturnRequest> pendingReturns = returnRequestRepository.findByStatusAndCreatedAtBefore(
                ReturnRequestStatus.PENDING, threshold, PageRequest.of(0, 50)
        );

        for (ReturnRequest rr : pendingReturns) {
            try {
                autoApproveSingleReturn(rr.getId());
            } catch (Exception e) {
                log.error("Failed to auto-approve return request {}", rr.getId(), e);
            }
        }
    }

    @Transactional
    public void autoApproveSingleReturn(UUID returnRequestId) {
        LocalDateTime now = LocalDateTime.now();
        ReturnRequest rr = returnRequestRepository.findById(returnRequestId).orElse(null);
        if (rr == null || rr.getStatus() != ReturnRequestStatus.PENDING) {
            return;
        }

        rr.setStatus(ReturnRequestStatus.AUTO_APPROVED);
        rr.setSellerNote("Hệ thống tự động chấp thuận hoàn tiền sau 72h Người bán không phản hồi");
        rr.setRespondedAt(now);
        returnRequestRepository.save(rr);

        Order order = orderRepository.findByIdForUpdate(rr.getOrder().getId()).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.RETURN_REQUESTED) {
            order.setStatus(OrderStatus.REFUNDED);
            order.setUpdatedAt(now);
            orderRepository.save(order);

            // Record Refund Transaction (Stock is NOT restored, Voucher is NOT released)
            Payment payment = order.getPayment();
            BigDecimal refundAmt = Objects.requireNonNullElse(order.getTotalAmount(), BigDecimal.ZERO);

            if (payment != null) {
                if (payment.getPaymentMethod() == PaymentMethod.COD) {
                    RefundTransaction refundTx = RefundTransaction.builder()
                            .payment(payment)
                            .order(order)
                            .returnRequest(rr)
                            .amount(refundAmt)
                            .refundMethod(RefundMethod.BANK_TRANSFER_MANUAL)
                            .status(RefundStatus.PENDING)
                            .targetBankName(rr.getBankName())
                            .targetBankAccount(rr.getBankAccountNumber())
                            .targetBankHolder(rr.getBankAccountHolder())
                            .createdAt(now)
                            .build();
                    refundTransactionRepository.save(refundTx);
                } else {
                    payment.setRefundedAmount(Objects.requireNonNullElse(payment.getRefundedAmount(), BigDecimal.ZERO).add(refundAmt));
                    payment.setUpdatedAt(now);
                    paymentRepository.save(payment);

                    RefundTransaction refundTx = RefundTransaction.builder()
                            .payment(payment)
                            .order(order)
                            .returnRequest(rr)
                            .amount(refundAmt)
                            .refundMethod(RefundMethod.GATEWAY_REFUND)
                            .status(RefundStatus.SUCCESS)
                            .gatewayRefundId("MOCK_REFUND_" + UUID.randomUUID().toString().substring(0, 8))
                            .createdAt(now)
                            .build();
                    refundTransactionRepository.save(refundTx);
                }
            }
            log.info("Auto-approved return request {} for order {}. Order status: REFUNDED", rr.getId(), order.getId());
        }
    }

    private void settleOrderToSellerWallet(Order order, LocalDateTime now) {
        if (order.getShop() == null || order.getShop().getSeller() == null) {
            return;
        }
        Seller seller = order.getShop().getSeller();
        BigDecimal earnings = order.getTotalSellerEarnings() != null ? order.getTotalSellerEarnings() : BigDecimal.ZERO;
        if (earnings.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (sellerWalletTransactionRepository.existsByReferenceIdAndType(order.getId(), SellerWalletTransactionType.ORDER_SETTLEMENT)) {
            log.info("Order {} already settled. Skipping duplicate settlement.", order.getId());
            return;
        }

        BigDecimal balanceBefore = seller.getBalance() != null ? seller.getBalance() : BigDecimal.ZERO;
        sellerRepository.incrementBalance(seller.getId(), earnings);
        BigDecimal balanceAfter = balanceBefore.add(earnings);

        SellerWalletTransaction walletTx = SellerWalletTransaction.builder()
                .seller(seller)
                .type(SellerWalletTransactionType.ORDER_SETTLEMENT)
                .amount(earnings)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceId(order.getId())
                .description("Quyết toán doanh thu đơn hàng #" + order.getOrderNumber())
                .createdAt(now)
                .build();
        sellerWalletTransactionRepository.save(walletTx);
        log.info("Settled order {} earnings (+{}) to seller {}", order.getId(), earnings, seller.getId());
    }

    private void restoreOrderInventory(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<UUID> variantIds = items.stream()
                .map(i -> i.getProductVariant() != null ? i.getProductVariant().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (!variantIds.isEmpty()) {
            List<ProductVariant> variants = productVariantRepository.findAllByIdInForUpdate(variantIds);
            Map<UUID, ProductVariant> variantMap = variants.stream().collect(Collectors.toMap(ProductVariant::getId, v -> v));
            for (OrderItem item : items) {
                if (item.getProductVariant() != null && variantMap.containsKey(item.getProductVariant().getId())) {
                    ProductVariant variant = variantMap.get(item.getProductVariant().getId());
                    variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                }
            }
            productVariantRepository.saveAll(variants);
            log.info("Restored inventory for order {}", order.getId());
        }
    }

    private void processCancellationPayment(Order order, LocalDateTime now) {
        Payment payment = order.getPayment();
        if (payment == null) return;

        if (payment.getStatus() == PaymentStatus.PAID) {
            BigDecimal refundAmt = Objects.requireNonNullElse(order.getTotalAmount(), BigDecimal.ZERO);
            BigDecimal currentRefunded = Objects.requireNonNullElse(payment.getRefundedAmount(), BigDecimal.ZERO);
            payment.setRefundedAmount(currentRefunded.add(refundAmt));
            payment.setUpdatedAt(now);
            paymentRepository.save(payment);

            RefundTransaction refundTx = RefundTransaction.builder()
                    .payment(payment)
                    .order(order)
                    .amount(refundAmt)
                    .refundMethod(RefundMethod.GATEWAY_REFUND)
                    .status(RefundStatus.SUCCESS)
                    .gatewayRefundId("MOCK_REFUND_" + UUID.randomUUID().toString().substring(0, 8))
                    .createdAt(now)
                    .build();
            refundTransactionRepository.save(refundTx);
        } else if (payment.getPaymentMethod() == PaymentMethod.COD) {
            BigDecimal orderAmt = Objects.requireNonNullElse(order.getTotalAmount(), BigDecimal.ZERO);
            BigDecimal currentAmt = Objects.requireNonNullElse(payment.getAmount(), BigDecimal.ZERO);
            payment.setAmount(currentAmt.subtract(orderAmt).max(BigDecimal.ZERO));

            List<Order> siblings = orderRepository.findByPaymentId(payment.getId());
            boolean allCancelled = siblings.stream().allMatch(o -> o.getId().equals(order.getId()) || o.getStatus() == OrderStatus.CANCELLED);
            if (allCancelled) {
                payment.setStatus(PaymentStatus.CANCELLED);
            }
            payment.setUpdatedAt(now);
            paymentRepository.save(payment);
        }
    }

    private void releaseOrderVouchers(Order order, LocalDateTime now) {
        List<VoucherRedemption> redemptions = voucherRedemptionRepository.findByOrderId(order.getId());
        for (VoucherRedemption vr : redemptions) {
            if (vr.getStatus() == VoucherRedemptionStatus.RESERVED || vr.getStatus() == VoucherRedemptionStatus.REDEEMED) {
                Voucher voucher = vr.getVoucher();
                if (voucher != null) {
                    if (vr.getStatus() == VoucherRedemptionStatus.REDEEMED && voucher.getUsedCount() != null && voucher.getUsedCount() > 0) {
                        voucher.setUsedCount(voucher.getUsedCount() - 1);
                    } else if (vr.getStatus() == VoucherRedemptionStatus.RESERVED && voucher.getReservedCount() != null && voucher.getReservedCount() > 0) {
                        voucher.setReservedCount(voucher.getReservedCount() - 1);
                    }
                    voucherRepository.save(voucher);
                }
                vr.setStatus(VoucherRedemptionStatus.RELEASED);
                vr.setReleasedAt(now);
                vr.setUpdatedAt(now);
                voucherRedemptionRepository.save(vr);
            }
        }
    }
}

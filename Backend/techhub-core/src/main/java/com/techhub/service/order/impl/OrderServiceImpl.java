package com.techhub.service.order.impl;

import com.techhub.common.PageResponse;
import com.techhub.common.SlugUtils;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.AddressSnapshotResponse;
import com.techhub.model.dto.response.BuyerOrderDetailResponse;
import com.techhub.model.dto.response.BuyerOrderItemResponse;
import com.techhub.model.dto.response.BuyerOrderResponse;
import com.techhub.model.dto.response.OrderTimelineStepResponse;
import com.techhub.model.dto.response.ReorderItemDto;
import com.techhub.model.dto.response.ReorderResponse;
import com.techhub.model.dto.response.ReorderUnavailableItemDto;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.model.dto.request.CreateCancellationRequest;
import com.techhub.model.dto.request.CancelOrderRequest;
import com.techhub.model.dto.response.CancellationRequestResponse;
import com.techhub.repository.*;
import com.techhub.service.order.OrderService;
import com.techhub.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "totalAmount", "orderNumber");

    private static final Map<String, Pattern> ADDRESS_FIELD_PATTERNS = Map.of(
            "id", Pattern.compile("\"id\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\""),
            "recipientName", Pattern.compile("\"recipientName\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\""),
            "receiverName", Pattern.compile("\"receiverName\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\""),
            "phoneNumber", Pattern.compile("\"phoneNumber\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\""),
            "phone", Pattern.compile("\"phone\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\""),
            "streetAddress", Pattern.compile("\"streetAddress\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\""),
            "ward", Pattern.compile("\"ward\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\""),
            "district", Pattern.compile("\"district\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\""),
            "provinceCity", Pattern.compile("\"provinceCity\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\""),
            "province", Pattern.compile("\"province\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
    );

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CancellationRequestRepository cancellationRequestRepository;
    private final SellerRepository sellerRepository;
    private final SellerWalletTransactionRepository sellerWalletTransactionRepository;
    private final com.techhub.repository.ProductVariantRepository productVariantRepository;
    private final com.techhub.repository.VoucherRedemptionRepository voucherRedemptionRepository;
    private final com.techhub.repository.VoucherRepository voucherRepository;
    private final com.techhub.repository.RefundTransactionRepository refundTransactionRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BuyerOrderResponse> getBuyerOrders(
            UUID userId,
            OrderStatus status,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        // 1. Gatekeeper: Validate Buyer Security & Account Status (BR-11D)
        validateBuyerAccount(userId);

        // 2. Sanitize Pagination & Sort Parameters (Defense against DoS and SQL Injection)
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);

        String validSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(direction, validSortBy));

        // 3. Build Dynamic Query Specification (IDOR Protection BR-11F + Status + Keyword)
        Specification<Order> spec = OrderSpecification.filterBuyerOrders(userId, status, keyword);

        // 4. Phase 1 Query: Paginate Orders with To-One EntityGraph (shop, payment)
        Page<Order> ordersPage = orderRepository.findAll(spec, pageable);

        if (ordersPage.isEmpty()) {
            return PageResponse.from(ordersPage, Collections.emptyList());
        }

        // 5. Phase 2 Query: Batch Fetch OrderItems by Order IDs (Zero N+1 Query)
        List<UUID> orderIds = ordersPage.getContent().stream()
                .map(Order::getId)
                .toList();

        List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
        Map<UUID, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(oi -> oi.getOrder().getId()));

        // 6. Phase 3: In-Memory Stitching & Dynamic Available Actions (BR-13, BR-15)
        List<BuyerOrderResponse> buyerOrderResponses = ordersPage.getContent().stream()
                .map(order -> mapToBuyerOrderResponse(order, itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList())))
                .toList();

        return PageResponse.from(ordersPage, buyerOrderResponses);
    }

    private BuyerOrderResponse mapToBuyerOrderResponse(Order order, List<OrderItem> orderItems) {
        List<BuyerOrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> new BuyerOrderItemResponse(
                        item.getId(),
                        item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                        item.getProductNameSnapshot(),
                        item.getVariantNameSnapshot(),
                        item.getSkuSnapshot(),
                        item.getImageUrlSnapshot(),
                        item.getOriginalPrice(),
                        item.getQuantity(),
                        item.getItemDiscountAmount(),
                        item.getDiscountedUnitPrice(),
                        item.getFinalSubtotal()
                ))
                .toList();

        int totalItemsCount = orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        LocalDateTime deliveredAt = order.getDeliveredAt();
        LocalDateTime completedAt = order.getCompletedAt();
        LocalDateTime cancelledAt = order.getCancelledAt();

        String shopName = order.getShop() != null ? order.getShop().getName() : "";
        UUID shopId = order.getShop() != null ? order.getShop().getId() : null;
        String shopSlug = (shopName != null && !shopName.isBlank()) ? SlugUtils.slugify(shopName) : "";

        return new BuyerOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                shopId,
                shopName,
                shopSlug,
                order.getStatus(),
                getStatusDescription(order.getStatus()),
                order.getPayment() != null ? order.getPayment().getPaymentMethod() : null,
                order.getPayment() != null ? order.getPayment().getStatus() : null,
                order.getTotalProductAmount(),
                order.getShippingFee(),
                order.getShopDiscountAmount(),
                order.getPlatformDiscountAmount(),
                order.getTotalAmount(),
                totalItemsCount,
                itemResponses,
                order.getCreatedAt(),
                deliveredAt,
                completedAt,
                cancelledAt,
                evaluateAvailableActions(order.getStatus())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerOrderDetailResponse getBuyerOrderDetail(UUID userId, UUID orderId) {
        // 1. Gatekeeper: Validate Buyer Security & Account Status (BR-11D)
        validateBuyerAccount(userId);

        // 2. Fetch Order with Shop and Payment via EntityGraph (IDOR Protection BR-11F)
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return buildBuyerOrderDetailResponse(order);
    }

    @Override
    @Transactional
    public BuyerOrderDetailResponse completeOrder(UUID userId, UUID orderId) {
        log.info("Processing completeOrder for userId={}, orderId={}", userId, orderId);

        // 1. Gatekeeper: Validate Buyer Security & Account Status (BR-11D)
        validateBuyerAccount(userId);

        // 2. Input validation
        if (orderId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        // 3. Fetch Order with Pessimistic Write Lock & IDOR Protection
        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 4. Idempotency Check (EC-01)
        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.info("Order {} is already COMPLETED. Returning current state (Idempotent).", orderId);
            return buildBuyerOrderDetailResponse(order);
        }

        // 5. State Machine Transition Validation (BR-15: Only DELIVERED -> COMPLETED)
        if (order.getStatus() != OrderStatus.DELIVERED) {
            log.warn("Invalid order status transition from {} to COMPLETED for order {}", order.getStatus(), orderId);
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 6. Transition to COMPLETED & Update Timestamp
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(now);
        order.setUpdatedAt(now);
        order = orderRepository.save(order);

        // 7. Settle to Seller Wallet (Chặng 6 & Issue #5)
        settleOrderToSellerWallet(order, now);

        // 8. Synchronize COD Settlement (BR-13, BR-19)
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
                log.info("COD Payment {} marked as PAID after order {} completed", payment.getId(), orderId);
            }
        }

        log.info("Order {} successfully completed by buyer {}", orderId, userId);
        return buildBuyerOrderDetailResponse(order);
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
            log.info("Order {} already settled to seller wallet. Skipping duplicate credit.", order.getId());
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

    @Override
    @Transactional
    public CancellationRequestResponse requestOrderCancellation(UUID userId, UUID orderId, CreateCancellationRequest request) {
        log.info("Processing requestOrderCancellation for userId={}, orderId={}", userId, orderId);

        // 1. Gatekeeper: Validate Buyer Security & Account Status (BR-11D)
        User user = validateBuyerAccount(userId);

        // 2. Input validation
        if (orderId == null || request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        String reason = request.reason() != null ? request.reason().trim() : "";
        CancellationReasonCode reasonCode = request.reasonCode();
        String reasonNote = request.reasonNote() != null ? request.reasonNote().trim() : null;

        if (reasonCode != null && reason.isBlank()) {
            reason = reasonCode.name();
        }
        if (reason.isBlank() || (reasonCode == null && reason.length() < 5)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        // 3. Fetch Order with Pessimistic Write Lock & IDOR Protection (BR-11F)
        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 4. Validate Order Status (BR-15: Only CONFIRMED or PREPARING can request cancellation)
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PREPARING) {
            log.warn("Cannot request cancellation for order {} in status {}", orderId, order.getStatus());
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 5. Check existing cancellation requests (BR-21, EC-01, EC-06)
        Optional<CancellationRequest> existingOpt = cancellationRequestRepository.findByOrderId(orderId);
        if (existingOpt.isPresent()) {
            CancellationRequest existing = existingOpt.get();
            if (existing.getStatus() == CancellationRequestStatus.PENDING) {
                if (existing.getReason().equals(reason)) {
                    log.info("Cancellation request already pending with same reason for order {}. Returning existing (Idempotent).", orderId);
                    return mapToCancellationRequestResponse(existing, order.getOrderNumber());
                } else {
                    log.warn("Cancellation request already pending with different reason for order {}", orderId);
                    throw new BusinessException(ErrorCode.CANCELLATION_REQUEST_ALREADY_EXISTS);
                }
            } else if (existing.getStatus() == CancellationRequestStatus.REJECTED) {
                log.warn("Cancellation request was previously rejected for order {}. Cannot submit again.", orderId);
                throw new BusinessException(ErrorCode.CANCELLATION_REQUEST_ALREADY_EXISTS);
            } else if (existing.getStatus() == CancellationRequestStatus.APPROVED || existing.getStatus() == CancellationRequestStatus.AUTO_APPROVED) {
                log.warn("Cancellation request was already approved for order {}", orderId);
                throw new BusinessException(ErrorCode.CANCELLATION_REQUEST_ALREADY_EXISTS);
            }
        }

        // 6. Create & Persist new CancellationRequest
        CancellationRequest cancellationRequest = CancellationRequest.builder()
                .order(order)
                .user(user)
                .reason(reason)
                .reasonCode(reasonCode != null ? reasonCode : CancellationReasonCode.OTHER)
                .reasonNote(reasonNote != null ? reasonNote : reason)
                .status(CancellationRequestStatus.PENDING)
                .sellerResponse(null)
                .respondedAt(null)
                .build();

        cancellationRequest = cancellationRequestRepository.save(cancellationRequest);
        log.info("Cancellation request {} created successfully for order {}", cancellationRequest.getId(), orderId);

        return mapToCancellationRequestResponse(cancellationRequest, order.getOrderNumber());
    }

    @Override
    @Transactional
    public BuyerOrderDetailResponse cancelBuyerOrder(UUID userId, UUID orderId, CancelOrderRequest request) {
        log.info("Processing cancelBuyerOrder for userId={}, orderId={}", userId, orderId);

        validateBuyerAccount(userId);
        if (orderId == null || request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return buildBuyerOrderDetailResponse(order);
        }

        // Direct cancel is ONLY allowed when PENDING_CONFIRMATION (or PENDING_PAYMENT)
        if (order.getStatus() != OrderStatus.PENDING_CONFIRMATION && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Buyer cannot direct cancel order {} in status {}", orderId, order.getStatus());
            throw new BusinessException(ErrorCode.CANCELLATION_NOT_ALLOWED);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        order.setUpdatedAt(now);

        String cancelNote = "[Buyer Cancel]: " + request.reasonCode() +
                (request.reasonNote() != null && !request.reasonNote().isBlank() ? " - " + request.reasonNote().trim() : "");
        String currentNote = order.getNote();
        order.setNote((currentNote != null && !currentNote.isBlank()) ? currentNote + " | " + cancelNote : cancelNote);

        restoreOrderInventory(order);
        processCancellationPayment(order, now);
        releaseOrderVouchers(order, now);

        orderRepository.save(order);
        log.info("Order {} successfully direct cancelled by buyer {}", orderId, userId);
        return buildBuyerOrderDetailResponse(order);
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
            log.info("Created refund transaction for cancelled order {}: amount={}", order.getId(), refundAmt);
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
            log.info("Updated COD payment {} amount after order {} cancellation", payment.getId(), order.getId());
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

        if (order.getPayment() != null) {
            List<Order> siblings = orderRepository.findByPaymentId(order.getPayment().getId());
            boolean allCancelled = siblings.stream().allMatch(o -> o.getId().equals(order.getId()) || o.getStatus() == OrderStatus.CANCELLED);
            if (allCancelled) {
                List<VoucherRedemption> paymentRedemptions = voucherRedemptionRepository.findByPaymentId(order.getPayment().getId());
                for (VoucherRedemption vr : paymentRedemptions) {
                    if (vr.getOrderId() == null && (vr.getStatus() == VoucherRedemptionStatus.RESERVED || vr.getStatus() == VoucherRedemptionStatus.REDEEMED)) {
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
    }

    private CancellationRequestResponse mapToCancellationRequestResponse(CancellationRequest cr, String orderNumber) {
        String statusDescription = switch (cr.getStatus()) {
            case PENDING -> "Đang chờ Người bán phản hồi";
            case APPROVED -> "Đã chấp nhận yêu cầu hủy";
            case REJECTED -> "Người bán đã từ chối yêu cầu hủy";
            case AUTO_APPROVED -> "Hệ thống tự động chấp thuận hủy sau 72h";
        };

        LocalDateTime requestedAt = cr.getCreatedAt() != null ? cr.getCreatedAt() : LocalDateTime.now();

        return new CancellationRequestResponse(
                cr.getId(),
                cr.getOrder() != null ? cr.getOrder().getId() : null,
                orderNumber != null ? orderNumber : (cr.getOrder() != null ? cr.getOrder().getOrderNumber() : ""),
                cr.getStatus(),
                statusDescription,
                cr.getReason(),
                cr.getSellerResponse(),
                requestedAt,
                cr.getRespondedAt()
        );
    }

    private BuyerOrderDetailResponse buildBuyerOrderDetailResponse(Order order) {
        // 1. Fetch OrderItems with ProductVariant via EntityGraph (Zero N+1 Query)
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        // 2. Map Items to BuyerOrderItemResponse
        List<BuyerOrderItemResponse> itemResponses = items.stream()
                .map(item -> new BuyerOrderItemResponse(
                        item.getId(),
                        item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                        item.getProductNameSnapshot(),
                        item.getVariantNameSnapshot(),
                        item.getSkuSnapshot(),
                        item.getImageUrlSnapshot(),
                        item.getOriginalPrice(),
                        item.getQuantity(),
                        item.getItemDiscountAmount(),
                        item.getDiscountedUnitPrice(),
                        item.getFinalSubtotal()
                ))
                .toList();

        int totalItemsCount = items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        // 3. Parse Delivery Address Snapshot safely (Fault-Tolerant TC-03)
        AddressSnapshotResponse deliveryAddress = parseAddressSnapshot(order.getDeliveryAddressSnapshot());

        LocalDateTime deliveredAt = order.getDeliveredAt();
        LocalDateTime completedAt = order.getCompletedAt();
        LocalDateTime cancelledAt = order.getCancelledAt();

        List<OrderTimelineStepResponse> timeline = buildOrderTimeline(order, deliveredAt, completedAt, cancelledAt);

        // 5. Shop & Payment Metadata
        String shopName = order.getShop() != null ? order.getShop().getName() : "";
        UUID shopId = order.getShop() != null ? order.getShop().getId() : null;
        String shopSlug = (shopName != null && !shopName.isBlank()) ? SlugUtils.slugify(shopName) : "";
        String shopAvatarUrl = order.getShop() != null ? order.getShop().getAvatarUrl() : null;

        PaymentMethod paymentMethod = order.getPayment() != null ? order.getPayment().getPaymentMethod() : PaymentMethod.COD;
        String paymentMethodName = getPaymentMethodName(paymentMethod);
        PaymentStatus paymentStatus = order.getPayment() != null ? order.getPayment().getStatus() : PaymentStatus.PENDING;
        String paymentStatusDescription = getPaymentStatusDescription(paymentStatus, paymentMethod);

        // 6. Assemble BuyerOrderDetailResponse (BR-20: availableActions includes REVIEW and BUY_AGAIN when COMPLETED)
        return new BuyerOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                getStatusDescription(order.getStatus()),
                shopId,
                shopName,
                shopSlug,
                shopAvatarUrl,
                deliveryAddress,
                itemResponses,
                totalItemsCount,
                order.getTotalProductAmount(),
                order.getShippingFee(),
                order.getShopDiscountAmount(),
                order.getPlatformDiscountAmount(),
                order.getTotalAmount(),
                order.getNote(),
                paymentMethod,
                paymentMethodName,
                paymentStatus,
                paymentStatusDescription,
                order.getCreatedAt(),
                deliveredAt,
                completedAt,
                cancelledAt,
                timeline,
                evaluateAvailableActions(order.getStatus())
        );
    }

    private User validateBuyerAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        return user;
    }

    private AddressSnapshotResponse parseAddressSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return new AddressSnapshotResponse(null, "", "", "", "", "", "", "");
        }
        try {
            UUID id = parseUuidField(json, "id");
            String recipientName = parseStringField(json, "recipientName");
            if (recipientName.isBlank()) {
                recipientName = parseStringField(json, "receiverName");
            }
            String phoneNumber = parseStringField(json, "phoneNumber");
            if (phoneNumber.isBlank()) {
                phoneNumber = parseStringField(json, "phone");
            }
            String streetAddress = parseStringField(json, "streetAddress");
            String ward = parseStringField(json, "ward");
            String district = parseStringField(json, "district");
            String provinceCity = parseStringField(json, "provinceCity");
            if (provinceCity.isBlank()) {
                provinceCity = parseStringField(json, "province");
            }

            List<String> parts = new ArrayList<>();
            if (!streetAddress.isBlank()) parts.add(streetAddress);
            if (!ward.isBlank()) parts.add(ward);
            if (!district.isBlank()) parts.add(district);
            if (!provinceCity.isBlank()) parts.add(provinceCity);
            String fullAddress = String.join(", ", parts);
            if (fullAddress.isBlank()) {
                fullAddress = json;
            }

            return new AddressSnapshotResponse(id, recipientName, phoneNumber, streetAddress, ward, district, provinceCity, fullAddress);
        } catch (Exception e) {
            log.warn("Failed to parse deliveryAddressSnapshot JSON: {}", json, e);
            return new AddressSnapshotResponse(null, "", "", "", "", "", "", json);
        }
    }

    private String parseStringField(String json, String fieldName) {
        Pattern pattern = ADDRESS_FIELD_PATTERNS.get(fieldName);
        if (pattern == null) {
            pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        }
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return "";
    }

    private UUID parseUuidField(String json, String fieldName) {
        String val = parseStringField(json, fieldName);
        if (val.isBlank()) return null;
        try {
            return UUID.fromString(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private List<OrderTimelineStepResponse> buildOrderTimeline(
            Order order,
            LocalDateTime deliveredAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt
    ) {
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.CANCELLED) {
            return List.of(
                    new OrderTimelineStepResponse(
                            "ORDER_PLACED",
                            "Đặt hàng thành công",
                            "Đơn hàng đã được tạo thành công trên hệ thống TechHub",
                            true,
                            false,
                            order.getCreatedAt()
                    ),
                    new OrderTimelineStepResponse(
                            "CANCELLED",
                            "Đơn hàng đã bị hủy",
                            "Đơn hàng đã được hủy thành công",
                            true,
                            true,
                            cancelledAt != null ? cancelledAt : order.getUpdatedAt()
                    )
            );
        }

        boolean isPendingPayment = status == OrderStatus.PENDING_PAYMENT;
        boolean isConfirmedOrLater = status != OrderStatus.PENDING_PAYMENT;
        boolean isPreparingOrLater = isConfirmedOrLater && status != OrderStatus.CONFIRMED;
        boolean isShippingOrLater = isPreparingOrLater && status != OrderStatus.PREPARING && status != OrderStatus.WAITING_FOR_PICKUP;
        boolean isDeliveredOrLater = status == OrderStatus.DELIVERED || status == OrderStatus.COMPLETED;
        boolean isCompleted = status == OrderStatus.COMPLETED;

        List<OrderTimelineStepResponse> steps = new ArrayList<>();

        steps.add(new OrderTimelineStepResponse(
                "ORDER_PLACED",
                "Đặt hàng thành công",
                "Đơn hàng đã được tạo thành công trên hệ thống TechHub",
                true,
                isPendingPayment,
                order.getCreatedAt()
        ));

        steps.add(new OrderTimelineStepResponse(
                "CONFIRMED",
                "Đã xác nhận đơn hàng",
                "Đơn hàng đã được tiếp nhận và chuyển đến Người bán",
                isConfirmedOrLater,
                status == OrderStatus.CONFIRMED,
                isConfirmedOrLater ? order.getCreatedAt() : null
        ));

        steps.add(new OrderTimelineStepResponse(
                "PREPARING",
                "Người bán đang chuẩn bị hàng",
                "Shop đang kiểm tra sản phẩm và đóng gói kiện hàng",
                isPreparingOrLater,
                status == OrderStatus.PREPARING || status == OrderStatus.WAITING_FOR_PICKUP,
                isPreparingOrLater ? order.getUpdatedAt() : null
        ));

        steps.add(new OrderTimelineStepResponse(
                "SHIPPING",
                "Đang vận chuyển",
                "Bưu tá đã lấy kiện hàng và đang trên đường giao đến bạn",
                isShippingOrLater,
                status == OrderStatus.SHIPPING,
                isShippingOrLater ? order.getUpdatedAt() : null
        ));

        steps.add(new OrderTimelineStepResponse(
                "DELIVERED",
                "Đã giao hàng thành công",
                "Kiện hàng đã được giao đến tay bạn",
                isDeliveredOrLater,
                status == OrderStatus.DELIVERED,
                isDeliveredOrLater ? deliveredAt : null
        ));

        steps.add(new OrderTimelineStepResponse(
                "COMPLETED",
                "Hoàn tất đơn hàng",
                "Người mua xác nhận đã nhận hàng và hài lòng với sản phẩm",
                isCompleted,
                isCompleted,
                isCompleted ? completedAt : null
        ));

        return steps;
    }

    private String getPaymentMethodName(PaymentMethod method) {
        if (method == null) return "Chưa xác định";
        return switch (method) {
            case COD -> "Thanh toán khi nhận hàng (COD)";
            case VNPAY -> "Cổng thanh toán VNPAY (QR, Thẻ ATM, Visa/Master, Ví VNPAY)";
        };
    }

    private String getPaymentStatusDescription(PaymentStatus status, PaymentMethod method) {
        if (status == null) return "Chưa thanh toán";
        return switch (status) {
            case PENDING -> "Chờ thanh toán";
            case PAID -> (method == PaymentMethod.COD ? "Đã thanh toán tiền mặt cho bưu tá" : "Đã thanh toán thành công qua VNPAY");
            case FAILED -> "Thanh toán thất bại";
            case CANCELLED -> "Giao dịch thanh toán đã hủy";
        };
    }

    private List<String> evaluateAvailableActions(OrderStatus status) {
        if (status == null) {
            return Collections.emptyList();
        }
        return switch (status) {
            case PENDING_PAYMENT -> List.of("PAY_NOW", "CANCEL_ORDER");
            case PENDING_CONFIRMATION -> List.of("CANCEL_ORDER");
            case CONFIRMED, PREPARING -> List.of("REQUEST_CANCELLATION");
            case WAITING_FOR_PICKUP, SHIPPING -> List.of("TRACK_SHIPPING");
            case DELIVERED -> List.of("COMPLETE_ORDER", "REQUEST_RETURN");
            case COMPLETED -> List.of("REVIEW", "BUY_AGAIN");
            case CANCELLED -> List.of("BUY_AGAIN");
            case RETURN_REQUESTED -> List.of("CANCEL_RETURN_REQUEST", "VIEW_RETURN_DETAILS");
            case REFUNDED -> List.of("VIEW_REFUND_DETAILS", "BUY_AGAIN");
        };
    }

    private String getStatusDescription(OrderStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING_PAYMENT -> "Chờ thanh toán";
            case PENDING_CONFIRMATION -> "Chờ người bán xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case PREPARING -> "Người bán đang chuẩn bị hàng";
            case WAITING_FOR_PICKUP -> "Chờ đơn vị vận chuyển lấy hàng";
            case SHIPPING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao hàng thành công";
            case COMPLETED -> "Hoàn thành đơn hàng";
            case CANCELLED -> "Đơn hàng đã bị hủy";
            case RETURN_REQUESTED -> "Yêu cầu trả hàng / hoàn tiền";
            case REFUNDED -> "Đã hoàn tiền";
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReorderResponse reorder(UUID userId, UUID orderId) {
        log.info("Processing reorder request: userId={}, orderId={}", userId, orderId);

        // 1. Fail-fast validation
        if (userId == null || orderId == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. Gatekeeper: Validate Buyer Security & Account Status (BR-11D)
        User buyerUser = validateBuyerAccount(userId);

        // 3. Query Order & IDOR ownership check
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 4. Retrieve order items
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("Reorder failed: order {} contains no items", orderId);
            return new ReorderResponse(orderId, 0, 0, List.of(), List.of(), 0, 0);
        }

        // 5. Pessimistic Write Lock on user's Cart (BR-12)
        Cart cart = cartRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    log.info("Lazy initializing new cart for user {} during reorder", userId);
                    Cart newCart = Cart.builder()
                            .user(buyerUser)
                            .build();
                    try {
                        return cartRepository.save(newCart);
                    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                        return cartRepository.findByUserIdWithLock(userId)
                                .orElseThrow(() -> ex);
                    }
                });

        // 6. Pre-fetch existing Cart Items for user
        List<CartItem> existingCartItems = cartItemRepository.findByCartId(cart.getId());
        Map<UUID, CartItem> existingCartItemMap = new HashMap<>();
        if (existingCartItems != null) {
            for (CartItem ci : existingCartItems) {
                if (ci.getProductVariant() != null && ci.getProductVariant().getId() != null) {
                    existingCartItemMap.put(ci.getProductVariant().getId(), ci);
                }
            }
        }
        int currentItemTypesCount = existingCartItemMap.size();

        // 7. Batch fetch Variants with details (Zero N+1 Query)
        List<UUID> variantIds = orderItems.stream()
                .map(OrderItem::getProductVariant)
                .filter(Objects::nonNull)
                .map(ProductVariant::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, ProductVariant> variantMap = new HashMap<>();
        if (!variantIds.isEmpty()) {
            List<ProductVariant> variants = productVariantRepository.findAllByIdInWithDetails(variantIds);
            if (variants != null) {
                for (ProductVariant pv : variants) {
                    variantMap.put(pv.getId(), pv);
                }
            }
        }

        // 8. Iterate over OrderItems and evaluate 5-Tier Gatekeeper & Live Stock
        List<ReorderItemDto> addedItems = new ArrayList<>();
        List<ReorderUnavailableItemDto> unavailableItems = new ArrayList<>();
        List<CartItem> itemsToSave = new ArrayList<>();

        for (OrderItem item : orderItems) {
            UUID variantId = item.getProductVariant() != null ? item.getProductVariant().getId() : null;
            String productName = item.getProductNameSnapshot();
            String variantName = item.getVariantNameSnapshot();
            String sku = item.getSkuSnapshot();
            int requestedQty = item.getQuantity();

            if (variantId == null || !variantMap.containsKey(variantId)) {
                unavailableItems.add(new ReorderUnavailableItemDto(
                        variantId, productName, variantName, sku, requestedQty,
                        "VARIANT_UNAVAILABLE", "Biến thể sản phẩm không tồn tại hoặc đã ngừng kinh doanh"
                ));
                continue;
            }

            ProductVariant variant = variantMap.get(variantId);
            Product product = variant.getProduct();
            Category category = product != null ? product.getCategory() : null;
            Shop shop = product != null ? product.getShop() : null;
            Seller seller = shop != null ? shop.getSeller() : null;
            User sellerUser = seller != null ? seller.getUser() : null;

            // 8.1 Variant Check
            if (variant.isDeleted() || variant.getStatus() != ProductVariantStatus.ACTIVE) {
                unavailableItems.add(new ReorderUnavailableItemDto(
                        variantId, productName, variantName, sku, requestedQty,
                        "VARIANT_UNAVAILABLE", "Biến thể sản phẩm đã bị xóa hoặc tạm ngưng bán"
                ));
                continue;
            }

            // 8.2 Product Check
            if (product == null || product.isDeleted() || product.isSuspended()
                    || product.getVisibilityStatus() != ProductVisibilityStatus.PUBLISHED
                    || product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
                unavailableItems.add(new ReorderUnavailableItemDto(
                        variantId, productName, variantName, sku, requestedQty,
                        "PRODUCT_UNAVAILABLE", "Sản phẩm hiện không khả dụng để đặt mua"
                ));
                continue;
            }

            // 8.3 Category Check
            if (category == null || category.isDeleted() || category.getStatus() != CategoryStatus.ACTIVE) {
                unavailableItems.add(new ReorderUnavailableItemDto(
                        variantId, productName, variantName, sku, requestedQty,
                        "CATEGORY_INACTIVE", "Ngành hàng của sản phẩm hiện đang tạm khóa"
                ));
                continue;
            }

            // 8.4 Shop Check (BR-07)
            if (shop == null || shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED
                    || (sellerUser != null && (sellerUser.getStatus() == UserStatus.BANNED || sellerUser.getStatus() == UserStatus.DELETED))) {
                unavailableItems.add(new ReorderUnavailableItemDto(
                        variantId, productName, variantName, sku, requestedQty,
                        "SHOP_NOT_ACTIVE", "Gian hàng hiện đang tạm ngưng hoạt động hoặc bị cấm"
                ));
                continue;
            }

            if (shop.getStatus() == ShopStatus.PAUSED) {
                unavailableItems.add(new ReorderUnavailableItemDto(
                        variantId, productName, variantName, sku, requestedQty,
                        "SHOP_IS_PAUSED", "Gian hàng hiện đang tạm nghỉ"
                ));
                continue;
            }

            // 8.5 Anti-Self-Buying Guard (BR-11B)
            if (sellerUser != null && userId.equals(sellerUser.getId())) {
                unavailableItems.add(new ReorderUnavailableItemDto(
                        variantId, productName, variantName, sku, requestedQty,
                        "CANNOT_ADD_OWN_PRODUCT", "Không thể mua lại sản phẩm từ chính gian hàng của bạn"
                ));
                continue;
            }

            // 8.6 Stock Check
            int stock = variant.getStockQuantity();
            if (stock <= 0) {
                unavailableItems.add(new ReorderUnavailableItemDto(
                        variantId, productName, variantName, sku, requestedQty,
                        "OUT_OF_STOCK", "Sản phẩm hiện đã hết hàng trong kho"
                ));
                continue;
            }

            // 8.7 Quantity & Cart Capacity Resolution
            CartItem existingItem = existingCartItemMap.get(variantId);
            if (existingItem != null) {
                int existingQty = existingItem.getQuantity();
                if (existingQty >= stock) {
                    unavailableItems.add(new ReorderUnavailableItemDto(
                            variantId, productName, variantName, sku, requestedQty,
                            "INSUFFICIENT_STOCK", "Số lượng trong giỏ hàng đã đạt giới hạn tồn kho hiện có"
                    ));
                    continue;
                }

                int addableQty = Math.min(requestedQty, stock - existingQty);
                if (existingQty + addableQty > 999) {
                    addableQty = 999 - existingQty;
                }

                if (addableQty <= 0) {
                    unavailableItems.add(new ReorderUnavailableItemDto(
                            variantId, productName, variantName, sku, requestedQty,
                            "INVALID_QUANTITY", "Số lượng sản phẩm trong giỏ hàng đạt giới hạn tối đa (999)"
                    ));
                    continue;
                }

                existingItem.setQuantity(existingQty + addableQty);
                itemsToSave.add(existingItem);

                BigDecimal price = variant.getPrice() != null ? variant.getPrice() : item.getDiscountedUnitPrice();
                String imgUrl = (variant.getImageUrl() != null && !variant.getImageUrl().isBlank())
                        ? variant.getImageUrl()
                        : item.getImageUrlSnapshot();

                addedItems.add(new ReorderItemDto(
                        variantId, product.getId(), product.getName(), variant.getVariantName(),
                        variant.getSku(), imgUrl, addableQty, price
                ));
            } else {
                // New SKU check capacity (max 50 distinct items)
                if (currentItemTypesCount >= 50) {
                    unavailableItems.add(new ReorderUnavailableItemDto(
                            variantId, productName, variantName, sku, requestedQty,
                            "CART_CAPACITY_EXCEEDED", "Giỏ hàng đã đạt giới hạn tối đa 50 loại mặt hàng"
                    ));
                    continue;
                }

                int addableQty = Math.min(requestedQty, stock);
                if (addableQty > 999) {
                    addableQty = 999;
                }

                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .productVariant(variant)
                        .quantity(addableQty)
                        .build();

                itemsToSave.add(newItem);
                existingCartItemMap.put(variantId, newItem);
                currentItemTypesCount++;

                BigDecimal price = variant.getPrice() != null ? variant.getPrice() : item.getDiscountedUnitPrice();
                String imgUrl = (variant.getImageUrl() != null && !variant.getImageUrl().isBlank())
                        ? variant.getImageUrl()
                        : item.getImageUrlSnapshot();

                addedItems.add(new ReorderItemDto(
                        variantId, product.getId(), product.getName(), variant.getVariantName(),
                        variant.getSku(), imgUrl, addableQty, price
                ));
            }
        }

        // 9. Batch save cart items (deduplicated to guarantee safety)
        if (!itemsToSave.isEmpty()) {
            List<CartItem> distinctItemsToSave = new ArrayList<>(new LinkedHashSet<>(itemsToSave));
            cartItemRepository.saveAll(distinctItemsToSave);
            log.info("Reorder saved {} cart items for user {}", distinctItemsToSave.size(), userId);
        }

        // 10. Compute updated cart metrics in-memory
        int updatedItemTypes = existingCartItemMap.size();
        int updatedTotalQty = existingCartItemMap.values().stream().mapToInt(CartItem::getQuantity).sum();

        log.info("Reorder completed for user {}, order {}: requested={}, added={}, unavailable={}",
                userId, orderId, orderItems.size(), addedItems.size(), unavailableItems.size());

        return new ReorderResponse(
                orderId,
                orderItems.size(),
                addedItems.size(),
                addedItems,
                unavailableItems,
                updatedItemTypes,
                updatedTotalQty
        );
    }
}

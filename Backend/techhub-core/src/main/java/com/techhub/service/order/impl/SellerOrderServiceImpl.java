package com.techhub.service.order.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateSellerOrderStatusRequest;
import com.techhub.model.dto.response.*;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.order.SellerOrderService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderServiceImpl implements SellerOrderService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.05");

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

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CancellationRequestRepository cancellationRequestRepository;
    private final com.techhub.repository.ProductVariantRepository productVariantRepository;
    private final com.techhub.repository.PaymentRepository paymentRepository;
    private final com.techhub.repository.RefundTransactionRepository refundTransactionRepository;
    private final com.techhub.repository.VoucherRedemptionRepository voucherRedemptionRepository;
    private final com.techhub.repository.VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SellerOrderResponse> getSellerOrders(
            UUID userId,
            OrderStatus status,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.info("Processing getSellerOrders for userId={}, status={}, keyword='{}', startDate={}, endDate={}, page={}, size={}",
                userId, status, keyword, startDate, endDate, page, size);

        // 1. Resolve and Validate Seller & Shop (BR-06, BR-07, BR-11D, BR-11F)
        Shop shop = validateSellerAndShop(userId);

        // 2. Validate Date Range (TC-04)
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                log.warn("startDate {} is after endDate {}", startDate, endDate);
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
            }
            if (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) > 365) {
                log.warn("Date range exceeds 365 days limit: startDate={}, endDate={}", startDate, endDate);
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
            }
        }

        // 3. Clamp Pagination & Whitelist Sort (TC-05)
        int clampedPage = Math.max(0, page);
        int clampedSize = Math.clamp(size, 1, 50);

        String resolvedSortBy = switch (sortBy != null ? sortBy.toLowerCase() : "createdat") {
            case "totalamount", "total_amount" -> "totalAmount";
            case "totalsellerearnings", "total_seller_earnings", "earnings" -> "totalSellerEarnings";
            case "ordernumber", "order_number" -> "orderNumber";
            default -> "createdAt";
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(clampedPage, clampedSize, Sort.by(direction, resolvedSortBy));

        // 4. Phase 1: Query Orders using Specification (Staff Engineer 2-Phase Query)
        Specification<Order> spec = OrderSpecification.filterSellerOrders(
                shop.getId(), status, keyword, startDate, endDate
        );
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        if (orderPage.isEmpty()) {
            return PageResponse.from(orderPage, Collections.emptyList());
        }

        // 5. Phase 2: Batch Fetch OrderItems (Zero N+1 Query)
        List<UUID> orderIds = orderPage.getContent().stream().map(Order::getId).toList();
        List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
        Map<UUID, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        // 6. Phase 3: Assemble Response DTOs
        List<SellerOrderResponse> responseList = orderPage.getContent().stream()
                .map(order -> buildSellerOrderResponse(order, itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList())))
                .toList();

        return PageResponse.from(orderPage, responseList);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerOrderDetailResponse getSellerOrderDetail(UUID userId, UUID orderId) {
        log.info("Processing getSellerOrderDetail for userId={}, orderId={}", userId, orderId);

        // 1. Resolve and Validate Seller & Shop (BR-06, BR-07, BR-11D, BR-11F)
        Shop shop = validateSellerAndShop(userId);

        // 2. Fetch Order with IDOR protection (BR-11F) and To-One associations eagerly loaded
        Order order = orderRepository.findByIdAndShopId(orderId, shop.getId())
                .orElseThrow(() -> {
                    log.warn("Order {} not found for shop {}. Access denied or order non-existent.", orderId, shop.getId());
                    return new BusinessException(ErrorCode.ORDER_NOT_FOUND);
                });

        return buildSellerOrderDetailResponse(order, shop);
    }

    @Override
    @Transactional
    public SellerOrderDetailResponse updateSellerOrderStatus(UUID userId, UUID orderId, UpdateSellerOrderStatusRequest request) {
        log.info("Processing updateSellerOrderStatus for userId={}, orderId={}, targetStatus={}",
                userId, orderId, request != null ? request.status() : null);

        // 1. Resolve and Validate Seller & Shop (BR-06, BR-07, BR-11D, BR-11F)
        Shop shop = validateSellerAndShop(userId);

        // 2. Input validation
        if (orderId == null || request == null || request.status() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        OrderStatus targetStatus = request.status();

        // 3. Whitelist targetStatus for Seller (CONFIRMED, PREPARING, WAITING_FOR_PICKUP)
        if (targetStatus != OrderStatus.CONFIRMED && targetStatus != OrderStatus.PREPARING && targetStatus != OrderStatus.WAITING_FOR_PICKUP) {
            log.warn("Seller tried to update order {} to unauthorized target status {}", orderId, targetStatus);
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 4. Fetch Order with Pessimistic Write Lock (TC-01, BR-11F)
        Order order = orderRepository.findByIdAndShopIdForUpdate(orderId, shop.getId())
                .orElseThrow(() -> {
                    log.warn("Order {} not found for shop {} during status update. Access denied or order non-existent.", orderId, shop.getId());
                    return new BusinessException(ErrorCode.ORDER_NOT_FOUND);
                });

        // 5. Idempotency Check (EC-01)
        if (order.getStatus() == targetStatus) {
            log.info("Order {} is already in target status {}. Returning current state (Idempotent).", orderId, targetStatus);
            return buildSellerOrderDetailResponse(order, shop);
        }

        // 6. Strict Fulfillment Lock (Chặng 3 / Issue #12 / Scenarios C6 & C7)
        if (targetStatus == OrderStatus.PREPARING || targetStatus == OrderStatus.WAITING_FOR_PICKUP) {
            if (cancellationRequestRepository.existsByOrderIdAndStatus(order.getId(), CancellationRequestStatus.PENDING)) {
                log.warn("Order {} has pending cancellation request. Rejecting transition to {}", orderId, targetStatus);
                throw new BusinessException(ErrorCode.ORDER_HAS_PENDING_CANCELLATION);
            }
        }

        // 7. State Machine Transition Validation (BR-15)
        OrderStatus currentStatus = order.getStatus();
        boolean isValidTransition = switch (currentStatus) {
            case PENDING_CONFIRMATION -> targetStatus == OrderStatus.CONFIRMED;
            case CONFIRMED -> targetStatus == OrderStatus.PREPARING;
            case PREPARING -> targetStatus == OrderStatus.WAITING_FOR_PICKUP;
            default -> false; // PENDING_PAYMENT, WAITING_FOR_PICKUP, SHIPPING, DELIVERED, COMPLETED, CANCELLED
        };

        if (!isValidTransition) {
            log.warn("Invalid order status transition from {} to {} for order {}", currentStatus, targetStatus, orderId);
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 8. Atomic Mutation & Note Preservation (BR-18, EC-07)
        order.setStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());

        if (request.note() != null && !request.note().isBlank()) {
            String sellerNote = "[Shop]: " + request.note().trim();
            String currentNote = order.getNote();
            String combinedNote = (currentNote != null && !currentNote.isBlank())
                    ? currentNote + " | " + sellerNote
                    : sellerNote;

            // Defensive truncation to prevent database column overflow (orders.note length = 500)
            if (combinedNote.length() > 500) {
                combinedNote = combinedNote.substring(0, 500);
            }
            order.setNote(combinedNote);
        }

        orderRepository.save(order);
        log.info("Order {} status updated successfully from {} to {}", orderId, currentStatus, targetStatus);

        return buildSellerOrderDetailResponse(order, shop);
    }

    @Override
    @Transactional
    public SellerOrderDetailResponse cancelSellerOrder(UUID userId, UUID orderId, com.techhub.model.dto.request.CancelOrderRequest request) {
        log.info("Processing cancelSellerOrder for userId={}, orderId={}", userId, orderId);

        Shop shop = validateSellerAndShop(userId);
        if (orderId == null || request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        Order order = orderRepository.findByIdAndShopIdForUpdate(orderId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return buildSellerOrderDetailResponse(order, shop);
        }

        // Point of No Return: CANNOT cancel if WAITING_FOR_PICKUP or later
        if (order.getStatus() != OrderStatus.PENDING_CONFIRMATION &&
            order.getStatus() != OrderStatus.CONFIRMED &&
            order.getStatus() != OrderStatus.PREPARING) {
            log.warn("Cannot cancel order {} in status {}", orderId, order.getStatus());
            throw new BusinessException(ErrorCode.CANCELLATION_NOT_ALLOWED);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        order.setUpdatedAt(now);

        String cancelNote = "[Shop Cancel]: " + request.reasonCode() +
                (request.reasonNote() != null && !request.reasonNote().isBlank() ? " - " + request.reasonNote().trim() : "");
        String currentNote = order.getNote();
        order.setNote((currentNote != null && !currentNote.isBlank()) ? currentNote + " | " + cancelNote : cancelNote);

        // 1. Restore Inventory
        restoreOrderInventory(order);

        // 2. Refund / Adjust Payment
        processCancellationPayment(order, now);

        // 3. Release Vouchers
        releaseOrderVouchers(order, now);

        // 4. If order had a pending buyer cancellation request, approve it
        cancellationRequestRepository.findByOrderId(order.getId()).ifPresent(cr -> {
            if (cr.getStatus() == CancellationRequestStatus.PENDING) {
                cr.setStatus(CancellationRequestStatus.APPROVED);
                cr.setSellerResponse("Shop đã chủ động hủy đơn hàng");
                cr.setRespondedAt(now);
                cancellationRequestRepository.save(cr);
            }
        });

        orderRepository.save(order);
        log.info("Order {} successfully cancelled by seller {}", orderId, shop.getId());
        return buildSellerOrderDetailResponse(order, shop);
    }

    @Override
    @Transactional
    public CancellationRequestResponse processCancellationRequest(UUID userId, UUID requestId, com.techhub.model.dto.request.ProcessCancellationRequest request) {
        log.info("Processing processCancellationRequest for userId={}, requestId={}, decision={}",
                userId, requestId, request != null ? request.decision() : null);

        Shop shop = validateSellerAndShop(userId);
        if (requestId == null || request == null || request.decision() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        CancellationRequest cr = cancellationRequestRepository.findByIdAndShopIdForUpdate(requestId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CANCELLATION_REQUEST_NOT_FOUND));

        if (cr.getStatus() != CancellationRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.CANCELLATION_REQUEST_ALREADY_PROCESSED);
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = orderRepository.findByIdForUpdate(cr.getOrder().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (request.decision() == CancellationRequestStatus.APPROVED) {
            if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PREPARING) {
                log.warn("Cannot approve cancellation request {} because order {} is in invalid status: {}",
                        requestId, order.getId(), order.getStatus());
                throw new BusinessException(ErrorCode.CANCELLATION_NOT_ALLOWED);
            }

            cr.setStatus(CancellationRequestStatus.APPROVED);
            cr.setSellerResponse(request.sellerResponse() != null && !request.sellerResponse().isBlank()
                    ? request.sellerResponse().trim() : "Đồng ý hủy đơn");
            cr.setRespondedAt(now);
            cancellationRequestRepository.save(cr);

            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(now);
            order.setUpdatedAt(now);

            restoreOrderInventory(order);
            processCancellationPayment(order, now);
            releaseOrderVouchers(order, now);
            orderRepository.save(order);
        } else if (request.decision() == CancellationRequestStatus.REJECTED) {
            cr.setStatus(CancellationRequestStatus.REJECTED);
            cr.setSellerResponse(request.sellerResponse() != null && !request.sellerResponse().isBlank()
                    ? request.sellerResponse().trim() : "Từ chối hủy đơn");
            cr.setRespondedAt(now);
            cancellationRequestRepository.save(cr);
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        return new CancellationRequestResponse(
                cr.getId(),
                order.getId(),
                order.getOrderNumber(),
                cr.getStatus(),
                resolveCancellationStatusDescription(cr.getStatus()),
                cr.getReason(),
                cr.getSellerResponse(),
                cr.getCreatedAt(),
                cr.getRespondedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CancellationRequestResponse> getSellerCancellationRequests(UUID userId, int page, int size) {
        Shop shop = validateSellerAndShop(userId);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CancellationRequest> crPage = cancellationRequestRepository.findByShopId(shop.getId(), pageable);
        List<CancellationRequestResponse> responses = crPage.getContent().stream()
                .map(cr -> new CancellationRequestResponse(
                        cr.getId(),
                        cr.getOrder() != null ? cr.getOrder().getId() : null,
                        cr.getOrder() != null ? cr.getOrder().getOrderNumber() : "",
                        cr.getStatus(),
                        resolveCancellationStatusDescription(cr.getStatus()),
                        cr.getReason(),
                        cr.getSellerResponse(),
                        cr.getCreatedAt(),
                        cr.getRespondedAt()
                ))
                .toList();
        return PageResponse.from(crPage, responses);
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
            log.info("Restored inventory for cancelled order {}", order.getId());
        }
    }

    private void processCancellationPayment(Order order, LocalDateTime now) {
        Payment rawPayment = order.getPayment();
        if (rawPayment == null) return;

        Payment payment = paymentRepository.findByIdForUpdate(rawPayment.getId())
                .orElse(rawPayment);

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
            log.info("Created refund transaction for online paid cancelled order {}: amount={}", order.getId(), refundAmt);
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
        List<UUID> voucherIds = redemptions.stream()
                .map(vr -> vr.getVoucher() != null ? vr.getVoucher().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, Voucher> voucherMap = Collections.emptyMap();
        if (!voucherIds.isEmpty()) {
            List<Voucher> lockedVouchers = voucherRepository.findAllWithShopByIdInForUpdate(voucherIds);
            voucherMap = lockedVouchers.stream().collect(Collectors.toMap(Voucher::getId, v -> v));
        }

        for (VoucherRedemption vr : redemptions) {
            if (vr.getStatus() == VoucherRedemptionStatus.RESERVED || vr.getStatus() == VoucherRedemptionStatus.REDEEMED) {
                Voucher voucher = vr.getVoucher() != null ? voucherMap.get(vr.getVoucher().getId()) : null;
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

        // If payment exists and all sibling orders are now cancelled, also release any platform voucher attached to payment
        if (order.getPayment() != null) {
            List<Order> siblings = orderRepository.findByPaymentId(order.getPayment().getId());
            boolean allCancelled = siblings.stream().allMatch(o -> o.getId().equals(order.getId()) || o.getStatus() == OrderStatus.CANCELLED);
            if (allCancelled) {
                List<VoucherRedemption> paymentRedemptions = voucherRedemptionRepository.findByPaymentId(order.getPayment().getId());
                List<UUID> platVoucherIds = paymentRedemptions.stream()
                        .filter(vr -> vr.getOrderId() == null && (vr.getStatus() == VoucherRedemptionStatus.RESERVED || vr.getStatus() == VoucherRedemptionStatus.REDEEMED))
                        .map(vr -> vr.getVoucher() != null ? vr.getVoucher().getId() : null)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                Map<UUID, Voucher> platVoucherMap = Collections.emptyMap();
                if (!platVoucherIds.isEmpty()) {
                    List<Voucher> lockedPlatVouchers = voucherRepository.findAllWithShopByIdInForUpdate(platVoucherIds);
                    platVoucherMap = lockedPlatVouchers.stream().collect(Collectors.toMap(Voucher::getId, v -> v));
                }

                for (VoucherRedemption vr : paymentRedemptions) {
                    if (vr.getOrderId() == null && (vr.getStatus() == VoucherRedemptionStatus.RESERVED || vr.getStatus() == VoucherRedemptionStatus.REDEEMED)) {
                        Voucher voucher = vr.getVoucher() != null ? platVoucherMap.get(vr.getVoucher().getId()) : null;
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

    private String resolveCancellationStatusDescription(CancellationRequestStatus status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case PENDING -> "Đang chờ Người bán phản hồi";
            case APPROVED -> "Đã chấp nhận yêu cầu hủy";
            case REJECTED -> "Người bán đã từ chối yêu cầu hủy";
            case AUTO_APPROVED -> "Hệ thống tự động chấp thuận hủy sau 72h";
        };
    }

    private SellerOrderDetailResponse buildSellerOrderDetailResponse(Order order, Shop shop) {
        // 1. Batch fetch items with productVariant (Zero N+1 Query)
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<SellerOrderItemResponse> itemResponses = items.stream()
                .map(this::toSellerOrderItemResponse)
                .toList();

        int totalItemsCount = items.stream().mapToInt(OrderItem::getQuantity).sum();

        // 2. Parse Buyer Info & Delivery Address Snapshot (Single-pass parsing optimization)
        AddressSnapshotResponse deliveryAddress = parseAddressSnapshot(order.getDeliveryAddressSnapshot());
        SellerOrderBuyerResponse buyerInfo = parseBuyerInfo(order, deliveryAddress);

        // 3. Financial calculations (BR-16D)
        BigDecimal totalProductAmount = Objects.requireNonNullElse(order.getTotalProductAmount(), BigDecimal.ZERO);
        BigDecimal shippingFee = Objects.requireNonNullElse(order.getShippingFee(), BigDecimal.ZERO);
        BigDecimal shopDiscountAmount = Objects.requireNonNullElse(order.getShopDiscountAmount(), BigDecimal.ZERO);
        BigDecimal platformDiscountAmount = Objects.requireNonNullElse(order.getPlatformDiscountAmount(), BigDecimal.ZERO);
        BigDecimal buyerTotalAmount = Objects.requireNonNullElse(order.getTotalAmount(), BigDecimal.ZERO);

        BigDecimal totalCommission = order.getTotalCommission();
        BigDecimal totalSellerEarnings = order.getTotalSellerEarnings();
        if (totalCommission == null || totalSellerEarnings == null) {
            BigDecimal netProduct = totalProductAmount.subtract(shopDiscountAmount).max(BigDecimal.ZERO);
            totalCommission = netProduct.multiply(COMMISSION_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
            totalSellerEarnings = netProduct.subtract(totalCommission).max(BigDecimal.ZERO);
        }

        // 4. Payment info
        PaymentMethod paymentMethod = order.getPayment() != null ? order.getPayment().getPaymentMethod() : PaymentMethod.COD;
        String paymentMethodName = resolvePaymentMethodName(paymentMethod);
        PaymentStatus paymentStatus = order.getPayment() != null ? order.getPayment().getStatus() : PaymentStatus.PENDING;
        String paymentStatusDescription = resolvePaymentStatusDescription(paymentStatus, paymentMethod);

        // 5. Milestone timestamps & Order Timeline
        LocalDateTime deliveredAt = (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED)
                ? order.getUpdatedAt() : null;
        LocalDateTime completedAt = order.getStatus() == OrderStatus.COMPLETED ? order.getUpdatedAt() : null;
        LocalDateTime cancelledAt = order.getStatus() == OrderStatus.CANCELLED ? order.getUpdatedAt() : null;
        List<OrderTimelineStepResponse> timeline = buildOrderTimeline(order, deliveredAt, completedAt, cancelledAt);

        // 6. Available actions (BR-15)
        List<String> availableActions = evaluateSellerActions(order.getStatus());

        return new SellerOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                resolveOrderStatusDescription(order.getStatus()),
                shop.getId(),
                shop.getName(),
                buyerInfo,
                deliveryAddress,
                itemResponses,
                totalItemsCount,
                order.getNote(),
                totalProductAmount,
                shippingFee,
                shopDiscountAmount,
                platformDiscountAmount,
                totalCommission,
                totalSellerEarnings,
                buyerTotalAmount,
                paymentMethod,
                paymentMethodName,
                paymentStatus,
                paymentStatusDescription,
                order.getCreatedAt(),
                deliveredAt,
                completedAt,
                cancelledAt,
                timeline,
                availableActions
        );
    }

    private Shop validateSellerAndShop(UUID userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        // Step 1: Validate Seller profile (BR-06)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            log.warn("Seller {} is not approved (verificationStatus: {}). Access denied.",
                    seller.getId(), seller.getVerificationStatus());
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        // Step 2: Validate Account Status (BR-11D)
        User user = seller.getUser();
        if (user != null) {
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
        }

        // Step 3: Resolve Shop & Apply Tiered Penalty Model (BR-07, BR-11F)
        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} is BANNED. Rejecting seller orders access.", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        // Note: Shop SUSPENDED or PAUSED is allowed to view orders in Read-only Mode for fulfillment and settlement (BR-07)

        return shop;
    }

    private SellerOrderResponse buildSellerOrderResponse(Order order, List<OrderItem> items) {
        // Buyer Info & Address Snapshot
        SellerOrderBuyerResponse buyerInfo = parseBuyerInfo(order);

        // Order Items mapping
        List<SellerOrderItemResponse> itemResponses = items.stream()
                .map(item -> new SellerOrderItemResponse(
                        item.getId(),
                        item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                        item.getProductNameSnapshot(),
                        item.getVariantNameSnapshot(),
                        item.getSkuSnapshot(),
                        item.getImageUrlSnapshot(),
                        item.getOriginalPrice(),
                        item.getDiscountedUnitPrice(),
                        item.getQuantity(),
                        item.getFinalSubtotal()
                ))
                .toList();

        int totalItemsCount = items.stream().mapToInt(OrderItem::getQuantity).sum();

        // Financial amounts with null safety
        BigDecimal totalProductAmount = Objects.requireNonNullElse(order.getTotalProductAmount(), BigDecimal.ZERO);
        BigDecimal shippingFee = Objects.requireNonNullElse(order.getShippingFee(), BigDecimal.ZERO);
        BigDecimal shopDiscountAmount = Objects.requireNonNullElse(order.getShopDiscountAmount(), BigDecimal.ZERO);
        BigDecimal platformDiscountAmount = Objects.requireNonNullElse(order.getPlatformDiscountAmount(), BigDecimal.ZERO);
        BigDecimal totalCommission = Objects.requireNonNullElse(order.getTotalCommission(), BigDecimal.ZERO);
        BigDecimal totalSellerEarnings = Objects.requireNonNullElse(order.getTotalSellerEarnings(), BigDecimal.ZERO);
        BigDecimal buyerTotalAmount = Objects.requireNonNullElse(order.getTotalAmount(), BigDecimal.ZERO);

        // Payment info
        PaymentMethod paymentMethod = order.getPayment() != null ? order.getPayment().getPaymentMethod() : PaymentMethod.COD;
        String paymentMethodName = resolvePaymentMethodName(paymentMethod);
        PaymentStatus paymentStatus = order.getPayment() != null ? order.getPayment().getStatus() : PaymentStatus.PENDING;
        String paymentStatusDescription = resolvePaymentStatusDescription(paymentStatus, paymentMethod);

        return new SellerOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                resolveOrderStatusDescription(order.getStatus()),
                buyerInfo,
                totalItemsCount,
                itemResponses,
                totalProductAmount,
                shippingFee,
                shopDiscountAmount,
                platformDiscountAmount,
                totalCommission,
                totalSellerEarnings,
                buyerTotalAmount,
                paymentMethod,
                paymentMethodName,
                paymentStatus,
                paymentStatusDescription,
                order.getNote(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                evaluateSellerActions(order.getStatus())
        );
    }

    private SellerOrderBuyerResponse parseBuyerInfo(Order order) {
        AddressSnapshotResponse deliveryAddress = parseAddressSnapshot(order.getDeliveryAddressSnapshot());
        return parseBuyerInfo(order, deliveryAddress);
    }

    private SellerOrderBuyerResponse parseBuyerInfo(Order order, AddressSnapshotResponse deliveryAddress) {
        User buyerUser = order.getUser();
        UUID buyerId = buyerUser != null ? buyerUser.getId() : null;
        String buyerName = resolveBuyerName(buyerUser);

        String recipientName = (deliveryAddress != null && deliveryAddress.recipientName() != null && !deliveryAddress.recipientName().isBlank())
                ? deliveryAddress.recipientName()
                : (buyerUser != null ? buyerName : "");

        String recipientPhone = (deliveryAddress != null && deliveryAddress.phoneNumber() != null && !deliveryAddress.phoneNumber().isBlank())
                ? deliveryAddress.phoneNumber()
                : (buyerUser != null && buyerUser.getPhone() != null ? buyerUser.getPhone() : "");

        String provinceCity = (deliveryAddress != null && deliveryAddress.provinceCity() != null)
                ? deliveryAddress.provinceCity() : "";

        String fullAddress = (deliveryAddress != null && deliveryAddress.fullAddress() != null && !deliveryAddress.fullAddress().isBlank())
                ? deliveryAddress.fullAddress()
                : (order.getDeliveryAddressSnapshot() != null ? order.getDeliveryAddressSnapshot() : "");

        return new SellerOrderBuyerResponse(
                buyerId,
                buyerName,
                recipientName,
                recipientPhone,
                provinceCity,
                fullAddress
        );
    }

    private String resolveBuyerName(User user) {
        if (user == null) return "Khách mua";
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "")).trim();
        if (!fullName.isBlank()) return fullName;
        if (user.getEmail() != null && !user.getEmail().isBlank()) return user.getEmail();
        return "Khách mua";
    }

    private String parseStringField(String json, String fieldName) {
        if (json == null || json.isBlank()) return "";
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

    private List<String> evaluateSellerActions(OrderStatus status) {
        if (status == null) return Collections.emptyList();
        return switch (status) {
            case PENDING_PAYMENT -> Collections.emptyList();
            case PENDING_CONFIRMATION -> List.of("CONFIRM_ORDER", "CANCEL_ORDER");
            case CONFIRMED -> List.of("START_PREPARING", "CANCEL_ORDER");
            case PREPARING -> List.of("READY_FOR_PICKUP", "PRINT_SHIPPING_LABEL", "CANCEL_ORDER");
            case WAITING_FOR_PICKUP -> List.of("PRINT_SHIPPING_LABEL", "TRACK_SHIPPING");
            case SHIPPING -> List.of("TRACK_SHIPPING");
            case DELIVERED, COMPLETED -> List.of("VIEW_SETTLEMENT");
            case CANCELLED -> List.of("VIEW_REASON");
            case RETURN_REQUESTED -> List.of("REVIEW_RETURN_REQUEST");
            case REFUNDED -> List.of("VIEW_REFUND_DETAILS");
        };
    }

    private String resolveOrderStatusDescription(OrderStatus status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case PENDING_PAYMENT -> "Chờ người mua thanh toán";
            case PENDING_CONFIRMATION -> "Chờ người bán xác nhận";
            case CONFIRMED -> "Đã xác nhận (Chờ chuẩn bị hàng)";
            case PREPARING -> "Shop đang chuẩn bị hàng";
            case WAITING_FOR_PICKUP -> "Chờ ĐVVC lấy hàng";
            case SHIPPING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao hàng thành công";
            case COMPLETED -> "Đã hoàn thành";
            case CANCELLED -> "Đã hủy";
            case RETURN_REQUESTED -> "Yêu cầu trả hàng / hoàn tiền";
            case REFUNDED -> "Đã hoàn tiền";
        };
    }

    private String resolvePaymentMethodName(PaymentMethod method) {
        if (method == null) return "Thanh toán khi nhận hàng (COD)";
        return switch (method) {
            case COD -> "Thanh toán khi nhận hàng (COD)";
            case VNPAY -> "Cổng thanh toán VNPAY (QR, Thẻ ATM, Visa/Master, Ví VNPAY)";
        };
    }

    private String resolvePaymentStatusDescription(PaymentStatus status, PaymentMethod method) {
        if (status == null) return "Chờ thanh toán";
        return switch (status) {
            case PENDING -> method == PaymentMethod.COD ? "Chưa thanh toán (Thu tiền mặt khi giao)" : "Chờ thanh toán qua cổng";
            case PAID -> method == PaymentMethod.COD ? "Đã thu tiền mặt khi giao hàng" : "Đã thanh toán online thành công";
            case FAILED -> "Thanh toán thất bại";
            case CANCELLED -> "Giao dịch đã hủy";
        };
    }

    private SellerOrderItemResponse toSellerOrderItemResponse(OrderItem item) {
        return new SellerOrderItemResponse(
                item.getId(),
                item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                item.getProductNameSnapshot(),
                item.getVariantNameSnapshot(),
                item.getSkuSnapshot(),
                item.getImageUrlSnapshot(),
                item.getOriginalPrice(),
                item.getDiscountedUnitPrice(),
                item.getQuantity(),
                item.getFinalSubtotal()
        );
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

    private UUID parseUuidField(String json, String fieldName) {
        String val = parseStringField(json, fieldName);
        if (val.isBlank()) return null;
        try {
            return UUID.fromString(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
                            "Người mua đã hoàn tất đặt đơn hàng",
                            true,
                            false,
                            order.getCreatedAt()
                    ),
                    new OrderTimelineStepResponse(
                            "CANCELLED",
                            "Đơn hàng đã bị hủy",
                            "Đơn hàng đã được xử lý hủy trên hệ thống",
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
                "Người mua đã hoàn tất đặt đơn hàng",
                true,
                isPendingPayment,
                order.getCreatedAt()
        ));

        steps.add(new OrderTimelineStepResponse(
                "ORDER_CONFIRMED",
                "Đã xác nhận đơn hàng",
                "Đơn hàng đã được xác nhận, chờ Shop chuẩn bị hàng",
                isConfirmedOrLater,
                status == OrderStatus.CONFIRMED,
                isConfirmedOrLater ? order.getCreatedAt() : null
        ));

        steps.add(new OrderTimelineStepResponse(
                "PREPARING",
                "Đang chuẩn bị hàng",
                "Shop đang kiểm tra sản phẩm và đóng gói kiện hàng",
                isPreparingOrLater,
                status == OrderStatus.PREPARING || status == OrderStatus.WAITING_FOR_PICKUP,
                isPreparingOrLater ? order.getUpdatedAt() : null
        ));

        steps.add(new OrderTimelineStepResponse(
                "SHIPPING",
                "Đang vận chuyển",
                "Đơn vị vận chuyển đã nhận hàng và đang giao đến khách",
                isShippingOrLater,
                status == OrderStatus.SHIPPING,
                isShippingOrLater ? order.getUpdatedAt() : null
        ));

        steps.add(new OrderTimelineStepResponse(
                "DELIVERED",
                "Giao hàng thành công",
                "Kiện hàng đã được giao đến tay người mua",
                isDeliveredOrLater,
                status == OrderStatus.DELIVERED,
                isDeliveredOrLater ? deliveredAt : null
        ));

        steps.add(new OrderTimelineStepResponse(
                "COMPLETED",
                "Hoàn tất đơn hàng",
                "Đơn hàng đã được xác nhận đóng đơn và quyết toán doanh thu",
                isCompleted,
                isCompleted,
                isCompleted ? completedAt : null
        ));

        return steps;
    }
}

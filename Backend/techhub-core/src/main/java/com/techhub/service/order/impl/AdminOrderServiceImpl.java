package com.techhub.service.order.impl;

import com.techhub.common.PageResponse;
import com.techhub.common.SlugUtils;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CancelOrderRequest;
import com.techhub.model.dto.response.*;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.order.AdminOrderService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOrderServiceImpl implements AdminOrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentRepository paymentRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final VoucherRepository voucherRepository;

    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.05);
    private static final Set<String> SORT_BY_WHITELIST = Set.of(
            "createdAt", "totalAmount", "totalCommission", "totalSellerEarnings", "totalProductAmount", "orderNumber"
    );

    private static final Map<String, Pattern> ADDRESS_FIELD_PATTERNS = Map.of(
            "id", Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\""),
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponse> getAdminOrders(
            UUID currentUserId,
            UUID shopId,
            UUID buyerId,
            OrderStatus status,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.info("Processing getAdminOrders for adminUserId={}, shopId={}, buyerId={}, status={}, keyword='{}', startDate={}, endDate={}, page={}, size={}",
                currentUserId, shopId, buyerId, status, keyword, startDate, endDate, page, size);

        // 1. Gatekeeper: Validate Admin user account (BR-11D)
        validateAdminUser(currentUserId);

        // 2. Validate Date Range (TC-03)
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                log.warn("startDate {} is after endDate {}", startDate, endDate);
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
            }
            if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
                log.warn("Date range exceeds 365 days limit: startDate={}, endDate={}", startDate, endDate);
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
            }
        }

        // 3. Whitelist Sort By & Clamp Pagination (TC-04)
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        String safeSortBy = SORT_BY_WHITELIST.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));

        // 4. Phase 1: Fetch Page of Orders via DB (TC-01, Zero In-Memory Pagination)
        Specification<Order> adminSpec = OrderSpecification.filterAdminOrders(shopId, buyerId, status, keyword, startDate, endDate);
        Page<Order> orderPage = orderRepository.findAll(adminSpec, pageable);

        if (orderPage.isEmpty()) {
            return PageResponse.from(orderPage, Collections.emptyList());
        }

        // 5. Phase 2: Batch Fetch OrderItems (Zero N+1 Query)
        List<UUID> orderIds = orderPage.getContent().stream().map(Order::getId).toList();
        List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
        Map<UUID, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        // 6. Phase 3: Assemble Response DTOs
        List<AdminOrderResponse> responseList = orderPage.getContent().stream()
                .map(order -> buildAdminOrderResponse(order, itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList())))
                .toList();

        return PageResponse.from(orderPage, responseList);
    }

    private AdminOrderResponse buildAdminOrderResponse(Order order, List<OrderItem> items) {
        // Shop info
        Shop shop = order.getShop();
        UUID shopId = shop != null ? shop.getId() : null;
        String shopName = shop != null ? shop.getName() : "Không xác định";
        String shopSlug = shop != null ? SlugUtils.slugify(shop.getName()) : "";
        String sellerEmail = "";
        String sellerPhone = "";
        if (shop != null && shop.getSeller() != null && shop.getSeller().getUser() != null) {
            User sellerUser = shop.getSeller().getUser();
            sellerEmail = sellerUser.getEmail() != null ? sellerUser.getEmail() : "";
            sellerPhone = sellerUser.getPhone() != null ? sellerUser.getPhone() : "";
        }
        AdminOrderShopResponse shopResponse = new AdminOrderShopResponse(shopId, shopName, shopSlug, sellerEmail, sellerPhone);

        // Buyer Info & Address Snapshot (Single-pass parsing optimization)
        AddressSnapshotResponse deliveryAddress = parseAddressSnapshot(order.getDeliveryAddressSnapshot());
        AdminOrderBuyerResponse buyerResponse = parseBuyerInfo(order, deliveryAddress);

        // Order Items mapping
        List<AdminOrderItemResponse> itemResponses = items.stream()
                .map(item -> new AdminOrderItemResponse(
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

        // Financial amounts with null safety (BR-16D)
        BigDecimal totalProductAmount = Objects.requireNonNullElse(order.getTotalProductAmount(), BigDecimal.ZERO);
        BigDecimal shippingFee = Objects.requireNonNullElse(order.getShippingFee(), BigDecimal.ZERO);
        BigDecimal shopDiscountAmount = Objects.requireNonNullElse(order.getShopDiscountAmount(), BigDecimal.ZERO);
        BigDecimal platformDiscountAmount = Objects.requireNonNullElse(order.getPlatformDiscountAmount(), BigDecimal.ZERO);
        BigDecimal buyerTotalAmount = Objects.requireNonNullElse(order.getTotalAmount(), BigDecimal.ZERO);

        BigDecimal totalCommission = order.getTotalCommission();
        BigDecimal totalSellerEarnings = order.getTotalSellerEarnings();
        if (totalCommission == null || totalSellerEarnings == null) {
            BigDecimal netProduct = totalProductAmount.subtract(shopDiscountAmount).max(BigDecimal.ZERO);
            totalCommission = netProduct.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
            totalSellerEarnings = netProduct.subtract(totalCommission).max(BigDecimal.ZERO);
        }

        // Payment info
        PaymentMethod paymentMethod = order.getPayment() != null ? order.getPayment().getPaymentMethod() : PaymentMethod.COD;
        String paymentMethodName = resolvePaymentMethodName(paymentMethod);
        PaymentStatus paymentStatus = order.getPayment() != null ? order.getPayment().getStatus() : PaymentStatus.PENDING;
        String paymentStatusDescription = resolvePaymentStatusDescription(paymentStatus, paymentMethod);

        return new AdminOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                resolveOrderStatusDescription(order.getStatus()),
                shopResponse,
                buyerResponse,
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
                evaluateAdminActions(order.getStatus())
        );
    }

    private AdminOrderBuyerResponse parseBuyerInfo(Order order, AddressSnapshotResponse deliveryAddress) {
        User buyerUser = order.getUser();
        UUID buyerId = buyerUser != null ? buyerUser.getId() : null;
        String buyerName = resolveBuyerName(buyerUser);
        String buyerEmail = buyerUser != null && buyerUser.getEmail() != null ? buyerUser.getEmail() : "";

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

        return new AdminOrderBuyerResponse(
                buyerId,
                buyerName,
                buyerEmail,
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

    private List<String> evaluateAdminActions(OrderStatus status) {
        if (status == null) return List.of("VIEW_DETAILS");
        return switch (status) {
            case PENDING_PAYMENT, PENDING_CONFIRMATION, CONFIRMED, PREPARING, WAITING_FOR_PICKUP ->
                    List.of("VIEW_DETAILS", "FORCE_CANCEL");
            case SHIPPING, DELIVERED, COMPLETED, CANCELLED, RETURN_REQUESTED, REFUNDED ->
                    List.of("VIEW_DETAILS");
        };
    }

    private String resolveOrderStatusDescription(OrderStatus status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case PENDING_PAYMENT -> "Chờ người mua thanh toán";
            case PENDING_CONFIRMATION -> "Chờ người bán xác nhận";
            case CONFIRMED -> "Đã xác nhận (Shop đang chuẩn bị hàng)";
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

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getAdminOrderDetail(UUID currentUserId, UUID orderId) {
        log.info("Processing getAdminOrderDetail for adminUserId={}, orderId={}", currentUserId, orderId);

        // 1. Gatekeeper: Validate Admin user account (BR-11D)
        validateAdminUser(currentUserId);

        // 2. Validate input
        if (orderId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        // 3. Phase 1 Query: Fetch Order with To-One Associations (Zero N+1 Query)
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found. Access denied or order non-existent.", orderId);
                    return new BusinessException(ErrorCode.ORDER_NOT_FOUND);
                });

        // 4. Phase 2 Query: Fetch Order Items with productVariant
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<AdminOrderItemResponse> itemResponses = items.stream()
                .map(item -> new AdminOrderItemResponse(
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

        // 5. Shop info
        Shop shop = order.getShop();
        UUID shopId = shop != null ? shop.getId() : null;
        String shopName = shop != null ? shop.getName() : "Không xác định";
        String shopSlug = shop != null ? SlugUtils.slugify(shop.getName()) : "";
        String sellerEmail = "";
        String sellerPhone = "";
        if (shop != null && shop.getSeller() != null && shop.getSeller().getUser() != null) {
            User sellerUser = shop.getSeller().getUser();
            sellerEmail = sellerUser.getEmail() != null ? sellerUser.getEmail() : "";
            sellerPhone = sellerUser.getPhone() != null ? sellerUser.getPhone() : "";
        }
        AdminOrderShopResponse shopResponse = new AdminOrderShopResponse(shopId, shopName, shopSlug, sellerEmail, sellerPhone);

        // 6. Buyer Info & Address Snapshot (Single-pass parsing optimization)
        AddressSnapshotResponse deliveryAddress = parseAddressSnapshot(order.getDeliveryAddressSnapshot());
        AdminOrderBuyerResponse buyerResponse = parseBuyerInfo(order, deliveryAddress);

        // 7. Financial amounts with null safety (BR-16D)
        BigDecimal totalProductAmount = Objects.requireNonNullElse(order.getTotalProductAmount(), BigDecimal.ZERO);
        BigDecimal shippingFee = Objects.requireNonNullElse(order.getShippingFee(), BigDecimal.ZERO);
        BigDecimal shopDiscountAmount = Objects.requireNonNullElse(order.getShopDiscountAmount(), BigDecimal.ZERO);
        BigDecimal platformDiscountAmount = Objects.requireNonNullElse(order.getPlatformDiscountAmount(), BigDecimal.ZERO);
        BigDecimal buyerTotalAmount = Objects.requireNonNullElse(order.getTotalAmount(), BigDecimal.ZERO);

        BigDecimal totalCommission = order.getTotalCommission();
        BigDecimal totalSellerEarnings = order.getTotalSellerEarnings();
        if (totalCommission == null || totalSellerEarnings == null) {
            BigDecimal netProduct = totalProductAmount.subtract(shopDiscountAmount).max(BigDecimal.ZERO);
            totalCommission = netProduct.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
            totalSellerEarnings = netProduct.subtract(totalCommission).max(BigDecimal.ZERO);
        }

        // 8. Payment info
        Payment payment = order.getPayment();
        PaymentMethod paymentMethod = payment != null ? payment.getPaymentMethod() : PaymentMethod.COD;
        String paymentMethodName = resolvePaymentMethodName(paymentMethod);
        PaymentStatus paymentStatus = payment != null ? payment.getStatus() : PaymentStatus.PENDING;
        String paymentStatusDescription = resolvePaymentStatusDescription(paymentStatus, paymentMethod);
        UUID paymentId = payment != null ? payment.getId() : null;
        String transactionId = payment != null ? payment.getTransactionId() : null;
        LocalDateTime paidAt = (payment != null && payment.getStatus() == PaymentStatus.PAID) ? payment.getUpdatedAt() : null;

        AdminOrderPaymentResponse paymentInfo = new AdminOrderPaymentResponse(
                paymentId,
                paymentMethod,
                paymentMethodName,
                paymentStatus,
                paymentStatusDescription,
                transactionId,
                paidAt
        );

        // 9. Milestone timestamps & Order Timeline (BR-15)
        LocalDateTime deliveredAt = (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED)
                ? order.getUpdatedAt() : null;
        LocalDateTime completedAt = order.getStatus() == OrderStatus.COMPLETED ? order.getUpdatedAt() : null;
        LocalDateTime cancelledAt = order.getStatus() == OrderStatus.CANCELLED ? order.getUpdatedAt() : null;
        List<OrderTimelineStepResponse> timeline = buildOrderTimeline(order, deliveredAt, completedAt, cancelledAt);

        // 10. Available actions (BR-18)
        List<String> availableActions = evaluateAdminActions(order.getStatus());

        return new AdminOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                resolveOrderStatusDescription(order.getStatus()),
                shopResponse,
                buyerResponse,
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
                paymentInfo,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                deliveredAt,
                completedAt,
                cancelledAt,
                timeline,
                availableActions
        );
    }

    private User validateAdminUser(UUID currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        User adminUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (adminUser.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        if (adminUser.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (adminUser.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }
        if (!adminUser.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        return adminUser;
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

        LocalDateTime confirmedAt = order.getCreatedAt();
        if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.PAID && order.getPayment().getUpdatedAt() != null) {
            confirmedAt = order.getPayment().getUpdatedAt();
        }

        steps.add(new OrderTimelineStepResponse(
                "ORDER_CONFIRMED",
                "Đã xác nhận đơn hàng",
                "Đơn hàng đã được xác nhận, chờ Shop chuẩn bị hàng",
                isConfirmedOrLater,
                status == OrderStatus.CONFIRMED,
                isConfirmedOrLater ? confirmedAt : null
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

    @Override
    @Transactional
    public AdminOrderDetailResponse cancelAdminOrder(UUID currentUserId, UUID orderId, CancelOrderRequest request) {
        log.info("Processing cancelAdminOrder for adminUserId={}, orderId={}, reasonCode={}",
                currentUserId, orderId, request != null ? request.reasonCode() : null);

        // 1. Fail-fast input validation (avoid unnecessary DB queries/locks)
        if (orderId == null || request == null || request.reasonCode() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        // 2. Gatekeeper: Validate Admin user account (BR-11D)
        User adminUser = validateAdminUser(currentUserId);

        // 3. Acquire pessimistic lock on the Order
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found for cancellation", orderId);
                    return new BusinessException(ErrorCode.ORDER_NOT_FOUND);
                });

        // 4. Idempotency Check (BR-18.C)
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Order {} is already CANCELLED. Returning existing details idempotently.", orderId);
            return getAdminOrderDetail(currentUserId, orderId);
        }

        // 5. State Machine Validation (BR-18.A & BR-18.B)
        Set<OrderStatus> allowedStates = Set.of(
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PENDING_CONFIRMATION,
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.WAITING_FOR_PICKUP
        );
        if (!allowedStates.contains(order.getStatus())) {
            log.warn("Order {} status is {} which cannot be cancelled by admin", orderId, order.getStatus());
            throw new BusinessException(ErrorCode.CANCELLATION_NOT_ALLOWED);
        }

        LocalDateTime now = LocalDateTime.now();

        // 6. Step A: Restore inventory with deadlock prevention (BR-18.D.1)
        restoreOrderInventory(order);

        // 7. Step B: Process Payment compensation & refunds (BR-18.D.2)
        processCancellationPayment(order, now);

        // 8. Step C: Release and roll back vouchers (BR-18.D.3)
        releaseOrderVouchers(order, now);

        // 9. Step D: Update Order status, cancelledAt, and audit note
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        order.setUpdatedAt(now);

        String adminIdentifier = (adminUser.getEmail() != null && !adminUser.getEmail().isBlank())
                ? adminUser.getEmail() : adminUser.getId().toString();
        String cancelAudit = String.format("[Admin Cancel - %s]: %s%s",
                adminIdentifier,
                request.reasonCode().name(),
                (request.reasonNote() != null && !request.reasonNote().isBlank()) ? " - " + request.reasonNote().trim() : ""
        );
        String existingNote = order.getNote();
        if (existingNote != null && !existingNote.isBlank()) {
            String combined = existingNote + " | " + cancelAudit;
            order.setNote(combined.length() > 500 ? combined.substring(0, 500) : combined);
        } else {
            order.setNote(cancelAudit.length() > 500 ? cancelAudit.substring(0, 500) : cancelAudit);
        }
        orderRepository.save(order);

        log.info("Order {} successfully cancelled by admin {}", orderId, adminUser.getEmail());
        return getAdminOrderDetail(currentUserId, orderId);
    }

    private void restoreOrderInventory(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if (items == null || items.isEmpty()) {
            return;
        }

        // Sort variant IDs in ascending order to prevent deadlocks (TC-11)
        List<UUID> sortedVariantIds = items.stream()
                .map(OrderItem::getProductVariant)
                .filter(Objects::nonNull)
                .map(ProductVariant::getId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        if (sortedVariantIds.isEmpty()) {
            return;
        }

        List<ProductVariant> variants = productVariantRepository.findAllByIdInForUpdate(sortedVariantIds);
        Map<UUID, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        for (OrderItem item : items) {
            if (item.getProductVariant() != null && item.getProductVariant().getId() != null) {
                ProductVariant variant = variantMap.get(item.getProductVariant().getId());
                if (variant != null) {
                    variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                }
            }
        }
        productVariantRepository.saveAll(variants);
    }

    private void processCancellationPayment(Order order, LocalDateTime now) {
        if (order.getPayment() == null || order.getPayment().getId() == null) {
            return;
        }

        // Lock payment row to prevent race conditions on shared multi-order payments
        Payment payment = paymentRepository.findByIdForUpdate(order.getPayment().getId())
                .orElse(order.getPayment());

        BigDecimal orderAmount = Objects.requireNonNullElse(order.getTotalAmount(), BigDecimal.ZERO);

        if (payment.getStatus() == PaymentStatus.PAID) {
            BigDecimal currentRefunded = Objects.requireNonNullElse(payment.getRefundedAmount(), BigDecimal.ZERO);
            payment.setRefundedAmount(currentRefunded.add(orderAmount));
            payment.setUpdatedAt(now);
            paymentRepository.save(payment);

            RefundMethod refundMethod = payment.getPaymentMethod() == PaymentMethod.VNPAY
                    ? RefundMethod.GATEWAY_REFUND
                    : RefundMethod.BANK_TRANSFER_MANUAL;

            RefundTransaction refundTx = RefundTransaction.builder()
                    .payment(payment)
                    .order(order)
                    .amount(orderAmount)
                    .refundMethod(refundMethod)
                    .status(RefundStatus.SUCCESS)
                    .gatewayRefundId("ADMIN_REFUND_" + order.getOrderNumber() + "_" + System.currentTimeMillis())
                    .createdAt(now)
                    .build();
            refundTransactionRepository.save(refundTx);
        } else if (payment.getStatus() == PaymentStatus.PENDING) {
            if (payment.getPaymentMethod() == PaymentMethod.COD) {
                BigDecimal currentAmount = Objects.requireNonNullElse(payment.getAmount(), BigDecimal.ZERO);
                payment.setAmount(currentAmount.subtract(orderAmount).max(BigDecimal.ZERO));
            }
            payment.setUpdatedAt(now);

            List<Order> siblingOrders = orderRepository.findByPaymentId(payment.getId());
            boolean allCancelled = siblingOrders != null && siblingOrders.stream()
                    .allMatch(o -> Objects.equals(o.getId(), order.getId()) || o.getStatus() == OrderStatus.CANCELLED);
            if (allCancelled) {
                payment.setStatus(PaymentStatus.CANCELLED);
            }
            paymentRepository.save(payment);
        }
    }

    private void releaseOrderVouchers(Order order, LocalDateTime now) {
        List<VoucherRedemption> redemptions = voucherRedemptionRepository.findByOrderId(order.getId());
        if (redemptions == null || redemptions.isEmpty()) {
            return;
        }

        for (VoucherRedemption redemption : redemptions) {
            if (redemption.getStatus() == VoucherRedemptionStatus.REDEEMED) {
                Voucher voucher = redemption.getVoucher();
                if (voucher != null) {
                    int usedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
                    voucher.setUsedCount(Math.max(0, usedCount - 1));
                    voucher.setUpdatedAt(now);
                    voucherRepository.save(voucher);
                }
                redemption.setStatus(VoucherRedemptionStatus.RELEASED);
                redemption.setReleasedAt(now);
                redemption.setUpdatedAt(now);
                voucherRedemptionRepository.save(redemption);
            } else if (redemption.getStatus() == VoucherRedemptionStatus.RESERVED) {
                Voucher voucher = redemption.getVoucher();
                if (voucher != null) {
                    int reservedCount = voucher.getReservedCount() != null ? voucher.getReservedCount() : 0;
                    voucher.setReservedCount(Math.max(0, reservedCount - 1));
                    voucher.setUpdatedAt(now);
                    voucherRepository.save(voucher);
                }
                redemption.setStatus(VoucherRedemptionStatus.RELEASED);
                redemption.setReleasedAt(now);
                redemption.setUpdatedAt(now);
                voucherRedemptionRepository.save(redemption);
            }
        }
    }
}

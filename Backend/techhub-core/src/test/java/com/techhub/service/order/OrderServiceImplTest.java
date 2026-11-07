package com.techhub.service.order;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.BuyerOrderResponse;
import com.techhub.model.entity.Order;
import com.techhub.model.entity.OrderItem;
import com.techhub.model.entity.Payment;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PaymentMethod;
import com.techhub.model.enums.PaymentStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.model.dto.request.CreateCancellationRequest;
import com.techhub.model.dto.response.CancellationRequestResponse;
import com.techhub.model.dto.response.ReorderItemDto;
import com.techhub.model.dto.response.ReorderResponse;
import com.techhub.model.dto.response.ReorderUnavailableItemDto;
import com.techhub.model.entity.CancellationRequest;
import com.techhub.model.entity.Cart;
import com.techhub.model.entity.CartItem;
import com.techhub.model.entity.Category;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.ProductVariant;
import com.techhub.model.entity.Seller;
import com.techhub.model.enums.CategoryStatus;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVariantStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.CancellationRequestStatus;
import com.techhub.repository.CancellationRequestRepository;
import com.techhub.repository.OrderItemRepository;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.PaymentRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.order.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho OrderService (GET /api/buyer/orders & GET /api/buyer/orders/{id})")
class OrderServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CancellationRequestRepository cancellationRequestRepository;

    @Mock
    private com.techhub.repository.SellerRepository sellerRepository;

    @Mock
    private com.techhub.repository.SellerWalletTransactionRepository sellerWalletTransactionRepository;

    @Mock
    private com.techhub.repository.ProductVariantRepository productVariantRepository;

    @Mock
    private com.techhub.repository.VoucherRedemptionRepository voucherRedemptionRepository;

    @Mock
    private com.techhub.repository.VoucherRepository voucherRepository;

    @Mock
    private com.techhub.repository.RefundTransactionRepository refundTransactionRepository;

    @Mock
    private com.techhub.repository.CartRepository cartRepository;

    @Mock
    private com.techhub.repository.CartItemRepository cartItemRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = new User();
        activeUser.setId(userId);
        activeUser.setEmail("buyer@techhub.vn");
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setEmailVerified(true);
    }

    private Order createMockOrder(UUID orderId, String orderNumber, OrderStatus status, Shop shop, Payment payment) {
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNumber(orderNumber);
        order.setUser(activeUser);
        order.setShop(shop);
        order.setPayment(payment);
        order.setStatus(status);
        order.setDeliveryAddressSnapshot("{\"receiverName\":\"Nguyễn Văn A\",\"phone\":\"0912345678\"}");
        order.setTotalProductAmount(new BigDecimal("500000.00"));
        order.setShippingFee(new BigDecimal("30000.00"));
        order.setShopDiscountAmount(new BigDecimal("20000.00"));
        order.setPlatformDiscountAmount(new BigDecimal("10000.00"));
        order.setTotalAmount(new BigDecimal("500000.00"));
        order.setTotalCommission(new BigDecimal("23500.00"));
        order.setTotalSellerEarnings(new BigDecimal("446500.00"));
        order.setCreatedAt(LocalDateTime.now().minusHours(2));
        order.setUpdatedAt(LocalDateTime.now().minusHours(1));
        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(order.getUpdatedAt());
        } else if (status == OrderStatus.COMPLETED) {
            order.setDeliveredAt(order.getCreatedAt().plusHours(1));
            order.setCompletedAt(order.getUpdatedAt());
        } else if (status == OrderStatus.CANCELLED) {
            order.setCancelledAt(order.getUpdatedAt());
        }
        return order;
    }

    private Shop createMockShop(UUID shopId, String name) {
        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setName(name);
        return shop;
    }

    private Payment createMockPayment(UUID paymentId, PaymentMethod method, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("500000.00"));
        return payment;
    }

    private OrderItem createMockOrderItem(UUID itemId, Order order, String productName, BigDecimal price, int qty) {
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setOrder(order);
        item.setProductNameSnapshot(productName);
        item.setVariantNameSnapshot("Màu Đen");
        item.setSkuSnapshot("SKU-001");
        item.setImageUrlSnapshot("https://img.techhub.vn/p1.jpg");
        item.setOriginalPrice(price);
        item.setQuantity(qty);
        item.setItemDiscountAmount(new BigDecimal("30000.00"));
        item.setDiscountedUnitPrice(price.subtract(new BigDecimal("30000.00")));
        item.setFinalSubtotal(item.getDiscountedUnitPrice().multiply(BigDecimal.valueOf(qty)));
        item.setCommissionRateSnapshot(new BigDecimal("5.00"));
        item.setCommissionAmount(new BigDecimal("23500.00"));
        item.setSellerEarnings(new BigDecimal("446500.00"));
        item.setCreatedAt(LocalDateTime.now().minusHours(2));
        return item;
    }

    // =========================================================================
    // TC-01: Default All Status Sorted CreatedAt Desc
    // =========================================================================
    @Test
    @DisplayName("TC-01: getBuyerOrders_Success_DefaultAllStatus_SortedCreatedAtDesc")
    void tc01_getBuyerOrders_Success_DefaultAllStatus_SortedCreatedAtDesc() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Apple Official Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);

        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        UUID orderId3 = UUID.randomUUID();

        Order o1 = createMockOrder(orderId1, "ORD-20261015-001", OrderStatus.CONFIRMED, shop, payment);
        Order o2 = createMockOrder(orderId2, "ORD-20261015-002", OrderStatus.SHIPPING, shop, payment);
        Order o3 = createMockOrder(orderId3, "ORD-20261015-003", OrderStatus.DELIVERED, shop, payment);

        Page<Order> orderPage = new PageImpl<>(List.of(o1, o2, o3), PageRequest.of(0, 10), 3);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);

        OrderItem i1 = createMockOrderItem(UUID.randomUUID(), o1, "Củ sạc Apple 20W", new BigDecimal("500000.00"), 1);
        OrderItem i2 = createMockOrderItem(UUID.randomUUID(), o2, "Cáp Lightning 1m", new BigDecimal("300000.00"), 2);
        OrderItem i3 = createMockOrderItem(UUID.randomUUID(), o3, "Tai nghe EarPods", new BigDecimal("450000.00"), 1);

        when(orderItemRepository.findByOrderIdIn(eq(List.of(orderId1, orderId2, orderId3))))
                .thenReturn(List.of(i1, i2, i3));

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(3, response.content().size());
        assertEquals(3L, response.totalElements());
        assertEquals(0, response.pageNumber());
        assertEquals(10, response.pageSize());
        assertTrue(response.last());

        BuyerOrderResponse firstOrder = response.content().get(0);
        assertEquals("ORD-20261015-001", firstOrder.orderNumber());
        assertEquals("Apple Official Store", firstOrder.shopName());
        assertEquals("apple-official-store", firstOrder.shopSlug());
        assertEquals(OrderStatus.CONFIRMED, firstOrder.status());
        assertEquals("Đã xác nhận", firstOrder.statusDescription());
        assertEquals(1, firstOrder.items().size());
        assertEquals("Củ sạc Apple 20W", firstOrder.items().get(0).productName());
        assertEquals(1, firstOrder.totalItemsCount());
    }

    // =========================================================================
    // TC-02: Filter By Status Confirmed
    // =========================================================================
    @Test
    @DisplayName("TC-02: getBuyerOrders_Success_FilterByStatus_Confirmed")
    void tc02_getBuyerOrders_Success_FilterByStatus_Confirmed() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Tech Shop");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);

        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        Order o1 = createMockOrder(orderId1, "ORD-CONF-001", OrderStatus.CONFIRMED, shop, payment);
        Order o2 = createMockOrder(orderId2, "ORD-CONF-002", OrderStatus.CONFIRMED, shop, payment);

        Page<Order> orderPage = new PageImpl<>(List.of(o1, o2), PageRequest.of(0, 10), 2);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, OrderStatus.CONFIRMED, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertEquals(OrderStatus.CONFIRMED, response.content().get(0).status());
        assertEquals(OrderStatus.CONFIRMED, response.content().get(1).status());
    }

    // =========================================================================
    // TC-03: Filter By Status Pending Payment
    // =========================================================================
    @Test
    @DisplayName("TC-03: getBuyerOrders_Success_FilterByStatus_PendingPayment")
    void tc03_getBuyerOrders_Success_FilterByStatus_PendingPayment() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Zalo Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.VNPAY, PaymentStatus.PENDING);

        UUID orderId = UUID.randomUUID();
        Order o = createMockOrder(orderId, "ORD-ZP-001", OrderStatus.PENDING_PAYMENT, shop, payment);

        Page<Order> orderPage = new PageImpl<>(List.of(o), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, OrderStatus.PENDING_PAYMENT, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        BuyerOrderResponse order = response.content().get(0);
        assertEquals(OrderStatus.PENDING_PAYMENT, order.status());
        assertEquals("Chờ thanh toán", order.statusDescription());
        assertEquals(List.of("PAY_NOW", "CANCEL_ORDER"), order.availableActions());
    }

    // =========================================================================
    // TC-04: Filter By Status Delivered
    // =========================================================================
    @Test
    @DisplayName("TC-04: getBuyerOrders_Success_FilterByStatus_Delivered")
    void tc04_getBuyerOrders_Success_FilterByStatus_Delivered() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Logitech Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);

        UUID orderId = UUID.randomUUID();
        Order o = createMockOrder(orderId, "ORD-DEL-001", OrderStatus.DELIVERED, shop, payment);

        Page<Order> orderPage = new PageImpl<>(List.of(o), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, OrderStatus.DELIVERED, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        BuyerOrderResponse order = response.content().get(0);
        assertEquals(OrderStatus.DELIVERED, order.status());
        assertEquals("Đã giao hàng thành công", order.statusDescription());
        assertNotNull(order.deliveredAt());
        assertEquals(List.of("COMPLETE_ORDER", "REQUEST_RETURN"), order.availableActions());
    }

    // =========================================================================
    // TC-05: Filter By Status Completed
    // =========================================================================
    @Test
    @DisplayName("TC-05: getBuyerOrders_Success_FilterByStatus_Completed")
    void tc05_getBuyerOrders_Success_FilterByStatus_Completed() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Samsung Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PAID);

        UUID orderId = UUID.randomUUID();
        Order o = createMockOrder(orderId, "ORD-COMP-001", OrderStatus.COMPLETED, shop, payment);

        Page<Order> orderPage = new PageImpl<>(List.of(o), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, OrderStatus.COMPLETED, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        BuyerOrderResponse order = response.content().get(0);
        assertEquals(OrderStatus.COMPLETED, order.status());
        assertEquals("Hoàn thành đơn hàng", order.statusDescription());
        assertNotNull(order.completedAt());
        assertEquals(List.of("REVIEW", "BUY_AGAIN"), order.availableActions());
    }

    // =========================================================================
    // TC-06: Search By Order Number
    // =========================================================================
    @Test
    @DisplayName("TC-06: getBuyerOrders_Success_SearchByOrderNumber")
    void tc06_getBuyerOrders_Success_SearchByOrderNumber() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Apple Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);

        UUID orderId = UUID.randomUUID();
        Order o = createMockOrder(orderId, "ORD-20261015-8F3A", OrderStatus.CONFIRMED, shop, payment);

        Page<Order> orderPage = new PageImpl<>(List.of(o), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, null, "8F3A", 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("ORD-20261015-8F3A", response.content().get(0).orderNumber());
    }

    // =========================================================================
    // TC-07: Search By Product Name Snapshot
    // =========================================================================
    @Test
    @DisplayName("TC-07: getBuyerOrders_Success_SearchByProductNameSnapshot")
    void tc07_getBuyerOrders_Success_SearchByProductNameSnapshot() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Keychron Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);

        UUID orderId = UUID.randomUUID();
        Order o = createMockOrder(orderId, "ORD-KEY-001", OrderStatus.CONFIRMED, shop, payment);
        OrderItem item = createMockOrderItem(UUID.randomUUID(), o, "Bàn phím cơ Keychron K2", new BigDecimal("1800000.00"), 1);

        Page<Order> orderPage = new PageImpl<>(List.of(o), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(eq(List.of(orderId)))).thenReturn(List.of(item));

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, null, "Keychron", 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Bàn phím cơ Keychron K2", response.content().get(0).items().get(0).productName());
    }

    // =========================================================================
    // TC-08: Search By Shop Name
    // =========================================================================
    @Test
    @DisplayName("TC-08: getBuyerOrders_Success_SearchByShopName")
    void tc08_getBuyerOrders_Success_SearchByShopName() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Logitech Official");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);

        UUID orderId = UUID.randomUUID();
        Order o = createMockOrder(orderId, "ORD-LOGI-001", OrderStatus.CONFIRMED, shop, payment);

        Page<Order> orderPage = new PageImpl<>(List.of(o), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, null, "Logitech", 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Logitech Official", response.content().get(0).shopName());
    }

    // =========================================================================
    // TC-09: IDOR Protection Only Returns Own Orders
    // =========================================================================
    @Test
    @DisplayName("TC-09: getBuyerOrders_IDORProtection_OnlyReturnsOwnOrders")
    void tc09_getBuyerOrders_IDORProtection_OnlyReturnsOwnOrders() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);

        UUID myOrderId = UUID.randomUUID();
        Order myOrder = createMockOrder(myOrderId, "ORD-MY-001", OrderStatus.CONFIRMED, shop, payment);

        // Capture specification and verify it filters by activeUser.getId()
        ArgumentCaptor<Specification<Order>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        Page<Order> orderPage = new PageImpl<>(List.of(myOrder), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(specCaptor.capture(), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(myOrderId, response.content().get(0).orderId());
        assertNotNull(specCaptor.getValue());
    }

    // =========================================================================
    // TC-10: Empty List When No Orders
    // =========================================================================
    @Test
    @DisplayName("TC-10: getBuyerOrders_Success_EmptyList_WhenNoOrders")
    void tc09_getBuyerOrders_Success_EmptyList_WhenNoOrders() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0L, response.totalElements());
        assertEquals(0, response.totalPages());
        assertTrue(response.last());

        // Zero N+1: orderItemRepository must NOT be invoked when orders are empty
        verifyNoInteractions(orderItemRepository);
    }

    // =========================================================================
    // TC-11: Pagination Next Page
    // =========================================================================
    @Test
    @DisplayName("TC-11: getBuyerOrders_Success_Pagination_NextPage")
    void tc11_getBuyerOrders_Success_Pagination_NextPage() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Shop");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);

        List<Order> page2Orders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            page2Orders.add(createMockOrder(UUID.randomUUID(), "ORD-P2-" + i, OrderStatus.CONFIRMED, shop, payment));
        }

        Page<Order> orderPage = new PageImpl<>(page2Orders, PageRequest.of(1, 10), 15);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, null, null, 1, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(5, response.content().size());
        assertEquals(1, response.pageNumber());
        assertEquals(15L, response.totalElements());
        assertEquals(2, response.totalPages());
        assertTrue(response.last());
    }

    // =========================================================================
    // TC-12: Sanitize Page Size Exceed Max
    // =========================================================================
    @Test
    @DisplayName("TC-12: getBuyerOrders_Success_SanitizePageSize_ExceedMax")
    void tc12_getBuyerOrders_Success_SanitizePageSize_ExceedMax() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
        when(orderRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(emptyPage);

        orderService.getBuyerOrders(userId, null, null, 0, 500, "createdAt", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(50, capturedPageable.getPageSize(), "Kích thước trang phải được clamp về tối đa 50");
    }

    // =========================================================================
    // TC-13: Fallback SortBy Invalid Field
    // =========================================================================
    @Test
    @DisplayName("TC-13: getBuyerOrders_Success_FallbackSortBy_InvalidField")
    void tc13_getBuyerOrders_Success_FallbackSortBy_InvalidField() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(orderRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(emptyPage);

        orderService.getBuyerOrders(userId, null, null, 0, 10, "malicious_col; DROP TABLE orders;--", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        Sort.Order sortOrder = capturedPageable.getSort().iterator().next();
        assertEquals("createdAt", sortOrder.getProperty(), "Trường sort không hợp lệ phải fallback về createdAt");
        assertEquals(Sort.Direction.DESC, sortOrder.getDirection());
    }

    // =========================================================================
    // TC-14: Throw When User Banned
    // =========================================================================
    @Test
    @DisplayName("TC-14: getBuyerOrders_Throw_WhenUserBanned")
    void tc14_getBuyerOrders_Throw_WhenUserBanned() {
        activeUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrders(userId, null, null, 0, 10, "createdAt", "desc")
        );

        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    // =========================================================================
    // TC-15: Throw When Email Not Verified
    // =========================================================================
    @Test
    @DisplayName("TC-15: getBuyerOrders_Throw_WhenEmailNotVerified")
    void tc15_getBuyerOrders_Throw_WhenEmailNotVerified() {
        activeUser.setEmailVerified(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrders(userId, null, null, 0, 10, "createdAt", "desc")
        );

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    // =========================================================================
    // TC-16: Zero N+1 Query Batch Fetch Order Items
    // =========================================================================
    @Test
    @DisplayName("TC-16: getBuyerOrders_ZeroNPlusOneQuery_BatchFetchOrderItems")
    void tc16_getBuyerOrders_ZeroNPlusOneQuery_BatchFetchOrderItems() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        List<Order> orders = new ArrayList<>();
        List<UUID> orderIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID orderId = UUID.randomUUID();
            orderIds.add(orderId);
            Shop shop = createMockShop(UUID.randomUUID(), "Shop " + (i % 5));
            Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
            orders.add(createMockOrder(orderId, "ORD-BATCH-" + i, OrderStatus.CONFIRMED, shop, payment));
        }

        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 10);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);

        List<OrderItem> allItems = new ArrayList<>();
        for (Order o : orders) {
            allItems.add(createMockOrderItem(UUID.randomUUID(), o, "Item for " + o.getOrderNumber(), new BigDecimal("100000.00"), 1));
        }
        when(orderItemRepository.findByOrderIdIn(eq(orderIds))).thenReturn(allItems);

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(10, response.content().size());

        // STRICT VERIFICATION: Exactly 1 query for orders and exactly 1 query for items!
        verify(orderRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(orderItemRepository, times(1)).findByOrderIdIn(eq(orderIds));
    }

    // =========================================================================
    // Additional Edge Cases & Branch Coverage
    // =========================================================================
    @Test
    @DisplayName("Edge Case: getBuyerOrders ném USER_NOT_FOUND khi user không tồn tại")
    void getBuyerOrders_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrders(userId, null, null, 0, 10, "createdAt", "desc")
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Edge Case: getBuyerOrders ném ACCOUNT_SUSPENDED khi user bị SUSPENDED")
    void getBuyerOrders_UserSuspended_ThrowsException() {
        activeUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrders(userId, null, null, 0, 10, "createdAt", "desc")
        );

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Edge Case: getBuyerOrders ném ACCOUNT_DELETED khi user bị DELETED")
    void getBuyerOrders_UserDeleted_ThrowsException() {
        activeUser.setStatus(UserStatus.DELETED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrders(userId, null, null, 0, 10, "createdAt", "desc")
        );

        assertEquals(ErrorCode.ACCOUNT_DELETED, exception.getErrorCode());
    }


    @Test
    @DisplayName("Edge Case: getBuyerOrders với page âm và size < 1 được clamp chính xác")
    void getBuyerOrders_NegativePageAndZeroSize_Clamped() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1), 0);
        when(orderRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(emptyPage);

        orderService.getBuyerOrders(userId, null, null, -5, 0, "totalAmount", "asc");

        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber(), "Page âm phải được clamp về 0");
        assertEquals(1, captured.getPageSize(), "Size < 1 phải được clamp về 1");
        Sort.Order sortOrder = captured.getSort().iterator().next();
        assertEquals("totalAmount", sortOrder.getProperty());
        assertEquals(Sort.Direction.ASC, sortOrder.getDirection());
    }

    @Test
    @DisplayName("Edge Case: Available actions và status description cho PREPARING, WAITING_FOR_PICKUP, SHIPPING, CANCELLED")
    void getBuyerOrders_AllOrderStatuses_ActionsMappedCorrectly() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createMockShop(UUID.randomUUID(), "Multi Status Shop");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);

        Order oPreparing = createMockOrder(UUID.randomUUID(), "ORD-PREP", OrderStatus.PREPARING, shop, payment);
        Order oWaiting = createMockOrder(UUID.randomUUID(), "ORD-WAIT", OrderStatus.WAITING_FOR_PICKUP, shop, payment);
        Order oShipping = createMockOrder(UUID.randomUUID(), "ORD-SHIP", OrderStatus.SHIPPING, shop, payment);
        Order oCancelled = createMockOrder(UUID.randomUUID(), "ORD-CANC", OrderStatus.CANCELLED, shop, payment);

        Page<Order> orderPage = new PageImpl<>(List.of(oPreparing, oWaiting, oShipping, oCancelled), PageRequest.of(0, 10), 4);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        PageResponse<BuyerOrderResponse> response = orderService.getBuyerOrders(
                userId, null, null, 0, 10, "orderNumber", "desc"
        );

        assertNotNull(response);
        assertEquals(4, response.content().size());

        // PREPARING
        BuyerOrderResponse rPreparing = response.content().get(0);
        assertEquals("Người bán đang chuẩn bị hàng", rPreparing.statusDescription());
        assertEquals(List.of("REQUEST_CANCELLATION"), rPreparing.availableActions());

        // WAITING_FOR_PICKUP
        BuyerOrderResponse rWaiting = response.content().get(1);
        assertEquals("Chờ đơn vị vận chuyển lấy hàng", rWaiting.statusDescription());
        assertEquals(List.of("TRACK_SHIPPING"), rWaiting.availableActions());

        // SHIPPING
        BuyerOrderResponse rShipping = response.content().get(2);
        assertEquals("Đang giao hàng", rShipping.statusDescription());
        assertEquals(List.of("TRACK_SHIPPING"), rShipping.availableActions());

        // CANCELLED
        BuyerOrderResponse rCancelled = response.content().get(3);
        assertEquals("Đơn hàng đã bị hủy", rCancelled.statusDescription());
        assertNotNull(rCancelled.cancelledAt());
        assertEquals(List.of("BUY_AGAIN"), rCancelled.availableActions());
    }

    // =========================================================================
    // GET /api/buyer/orders/{id} - Tests
    // =========================================================================

    @Test
    @DisplayName("TC-01 (Detail): getBuyerOrderDetail_Success_DeliveredOrder_WithFullTimelineAndActions")
    void tc01_getBuyerOrderDetail_Success_DeliveredOrder() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Apple Official Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PAID);
        Order order = createMockOrder(orderId, "ORD-20261015-8F3A", OrderStatus.DELIVERED, shop, payment);
        order.setDeliveryAddressSnapshot("{\"id\":\"" + UUID.randomUUID() + "\",\"recipientName\":\"Nguyễn Văn A\",\"phoneNumber\":\"0987654321\",\"streetAddress\":\"123 Cầu Giấy\",\"ward\":\"Dịch Vọng\",\"district\":\"Cầu Giấy\",\"provinceCity\":\"Hà Nội\",\"isDefault\":true}");

        OrderItem item1 = createMockOrderItem(UUID.randomUUID(), order, "Củ sạc nhanh Apple 20W", new BigDecimal("500000.00"), 1);
        OrderItem item2 = createMockOrderItem(UUID.randomUUID(), order, "Cáp Type-C to Lightning", new BigDecimal("300000.00"), 2);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1, item2));

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.getBuyerOrderDetail(userId, orderId);

        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals("ORD-20261015-8F3A", response.orderNumber());
        assertEquals(OrderStatus.DELIVERED, response.status());
        assertEquals("Đã giao hàng thành công", response.statusDescription());
        assertEquals("Apple Official Store", response.shopName());
        assertEquals("apple-official-store", response.shopSlug());

        // Address check
        assertNotNull(response.deliveryAddress());
        assertEquals("Nguyễn Văn A", response.deliveryAddress().recipientName());
        assertEquals("0987654321", response.deliveryAddress().phoneNumber());
        assertEquals("123 Cầu Giấy, Dịch Vọng, Cầu Giấy, Hà Nội", response.deliveryAddress().fullAddress());

        // Items check
        assertEquals(2, response.items().size());
        assertEquals(3, response.totalItemsCount()); // 1 + 2

        // Financials check
        assertEquals(new BigDecimal("500000.00"), response.totalProductAmount());
        assertEquals(new BigDecimal("30000.00"), response.shippingFee());
        assertEquals(new BigDecimal("20000.00"), response.shopDiscountAmount());
        assertEquals(new BigDecimal("10000.00"), response.platformDiscountAmount());
        assertEquals(new BigDecimal("500000.00"), response.totalAmount());

        // Payment check
        assertEquals(PaymentMethod.COD, response.paymentMethod());
        assertEquals("Thanh toán khi nhận hàng (COD)", response.paymentMethodName());
        assertEquals(PaymentStatus.PAID, response.paymentStatus());
        assertEquals("Đã thanh toán tiền mặt cho bưu tá", response.paymentStatusDescription());

        // Actions & Dates check
        assertEquals(List.of("COMPLETE_ORDER", "REQUEST_RETURN"), response.availableActions());
        assertNotNull(response.deliveredAt());
        assertNull(response.completedAt());
        assertNull(response.cancelledAt());

        // Timeline check
        assertNotNull(response.timeline());
        assertEquals(6, response.timeline().size());
        assertTrue(response.timeline().stream().anyMatch(t -> t.step().equals("DELIVERED") && t.current()));
    }

    @Test
    @DisplayName("TC-02 (Detail): getBuyerOrderDetail_Success_PendingPaymentOrder_VNPay")
    void tc02_getBuyerOrderDetail_Success_PendingPaymentOrder_VNPay() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Logitech Flagship");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.VNPAY, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261015-VNPAY", OrderStatus.PENDING_PAYMENT, shop, payment);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.getBuyerOrderDetail(userId, orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING_PAYMENT, response.status());
        assertEquals(PaymentMethod.VNPAY, response.paymentMethod());
        assertEquals("Cổng thanh toán VNPAY (QR, Thẻ ATM, Visa/Master, Ví VNPAY)", response.paymentMethodName());
        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
        assertEquals("Chờ thanh toán", response.paymentStatusDescription());
        assertEquals(List.of("PAY_NOW", "CANCEL_ORDER"), response.availableActions());

        // Timeline: ORDER_PLACED is current
        assertTrue(response.timeline().stream().anyMatch(t -> t.step().equals("ORDER_PLACED") && t.current()));
    }

    @Test
    @DisplayName("TC-03 (Detail): getBuyerOrderDetail_Success_ConfirmedOrder_COD")
    void tc03_getBuyerOrderDetail_Success_ConfirmedOrder_COD() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Sony Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261015-CONF", OrderStatus.CONFIRMED, shop, payment);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.getBuyerOrderDetail(userId, orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(List.of("REQUEST_CANCELLATION"), response.availableActions());
        assertTrue(response.timeline().stream().anyMatch(t -> t.step().equals("CONFIRMED") && t.current()));
    }

    @Test
    @DisplayName("TC-04 (Detail): getBuyerOrderDetail_Success_CompletedOrder")
    void tc04_getBuyerOrderDetail_Success_CompletedOrder() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Samsung Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PAID);
        Order order = createMockOrder(orderId, "ORD-20261015-COMP", OrderStatus.COMPLETED, shop, payment);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.getBuyerOrderDetail(userId, orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertNotNull(response.completedAt());
        assertEquals(List.of("REVIEW", "BUY_AGAIN"), response.availableActions());
        assertTrue(response.timeline().stream().anyMatch(t -> t.step().equals("COMPLETED") && t.completed()));
    }

    @Test
    @DisplayName("TC-05 (Detail): getBuyerOrderDetail_Success_CancelledOrder")
    void tc05_getBuyerOrderDetail_Success_CancelledOrder() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Anker Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.CANCELLED);
        Order order = createMockOrder(orderId, "ORD-20261015-CANC", OrderStatus.CANCELLED, shop, payment);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.getBuyerOrderDetail(userId, orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.status());
        assertNotNull(response.cancelledAt());
        assertEquals(List.of("BUY_AGAIN"), response.availableActions());
        assertEquals(2, response.timeline().size());
        assertTrue(response.timeline().stream().anyMatch(t -> t.step().equals("CANCELLED") && t.current()));
    }

    @Test
    @DisplayName("TC-06 (Detail): getBuyerOrderDetail_Throw_NotFound_WhenOrderBelongsToAnotherBuyer")
    void tc06_getBuyerOrderDetail_Throw_NotFound_WhenOrderBelongsToAnotherBuyer() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID anotherUserOrderId = UUID.randomUUID();
        // findByIdAndUserId will return empty for another user's order
        when(orderRepository.findByIdAndUserId(anotherUserOrderId, userId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrderDetail(userId, anotherUserOrderId)
        );

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        verify(orderItemRepository, never()).findByOrderId(any());
    }

    @Test
    @DisplayName("TC-07 (Detail): getBuyerOrderDetail_Throw_NotFound_WhenOrderIdDoesNotExist")
    void tc07_getBuyerOrderDetail_Throw_NotFound_WhenOrderIdDoesNotExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID nonExistentId = UUID.randomUUID();
        when(orderRepository.findByIdAndUserId(nonExistentId, userId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrderDetail(userId, nonExistentId)
        );

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        verify(orderItemRepository, never()).findByOrderId(any());
    }

    @Test
    @DisplayName("TC-08 (Detail): getBuyerOrderDetail_Success_FallbackAddress_WhenSnapshotJsonCorrupted")
    void tc08_getBuyerOrderDetail_Success_FallbackAddress_WhenSnapshotJsonCorrupted() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Apple Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261015-CORR", OrderStatus.CONFIRMED, shop, payment);
        order.setDeliveryAddressSnapshot("INVALID_CORRUPTED_JSON_NOT_VALID");

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.getBuyerOrderDetail(userId, orderId);

        assertNotNull(response);
        assertNotNull(response.deliveryAddress());
        assertEquals("INVALID_CORRUPTED_JSON_NOT_VALID", response.deliveryAddress().fullAddress());
    }

    @Test
    @DisplayName("TC-09 (Detail): getBuyerOrderDetail_Throw_WhenUserBanned")
    void tc09_getBuyerOrderDetail_Throw_WhenUserBanned() {
        activeUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrderDetail(userId, orderId)
        );

        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("TC-10 (Detail): getBuyerOrderDetail_Throw_WhenUserSuspended")
    void tc10_getBuyerOrderDetail_Throw_WhenUserSuspended() {
        activeUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrderDetail(userId, orderId)
        );

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, exception.getErrorCode());
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("TC-11 (Detail): getBuyerOrderDetail_Throw_WhenEmailNotVerified")
    void tc11_getBuyerOrderDetail_Throw_WhenEmailNotVerified() {
        activeUser.setEmailVerified(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getBuyerOrderDetail(userId, orderId)
        );

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("TC-12 (Detail): getBuyerOrderDetail_ZeroNPlusOneQuery_VerifyTwoSqlQueries")
    void tc12_getBuyerOrderDetail_ZeroNPlusOneQuery_VerifyTwoSqlQueries() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Apple Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261015-SQL", OrderStatus.CONFIRMED, shop, payment);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        orderService.getBuyerOrderDetail(userId, orderId);

        // Verify exactly 1 query to orderRepository and 1 query to orderItemRepository
        verify(orderRepository, times(1)).findByIdAndUserId(orderId, userId);
        verify(orderItemRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("TC-13 (Detail): getBuyerOrderDetail_Success_SpecialCharactersInShopNameAndNote")
    void tc13_getBuyerOrderDetail_Success_SpecialCharactersInShopNameAndNote() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Cửa hàng Công Nghệ & Phụ Kiện Cao Cấp");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261015-SPEC", OrderStatus.PREPARING, shop, payment);
        order.setNote("Giao trước 17h chiều! Đồ dễ vỡ 📦⚡");

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.getBuyerOrderDetail(userId, orderId);

        assertNotNull(response);
        assertEquals("cua-hang-cong-nghe-phu-kien-cao-cap", response.shopSlug());
        assertEquals("Giao trước 17h chiều! Đồ dễ vỡ 📦⚡", response.note());
        assertEquals(OrderStatus.PREPARING, response.status());
        assertEquals("Người bán đang chuẩn bị hàng", response.statusDescription());
        assertEquals(List.of("REQUEST_CANCELLATION"), response.availableActions());
    }

    // =========================================================================
    // UNIT TESTS CHO completeOrder (PATCH /api/buyer/orders/{id}/complete)
    // =========================================================================

    @Test
    @DisplayName("TC-SVC-01: completeOrder từ DELIVERED thành công - Trạng thái chuyển COMPLETED, mở quyền REVIEW & BUY_AGAIN")
    void tc01_completeOrder_FromDelivered_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "TechHub Flagship");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.VNPAY, PaymentStatus.PAID);
        Order order = createMockOrder(orderId, "ORD-20261016-001", OrderStatus.DELIVERED, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.completeOrder(userId, orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertEquals("Hoàn thành đơn hàng", response.statusDescription());
        assertNotNull(response.completedAt());
        assertTrue(response.availableActions().contains("REVIEW"));
        assertTrue(response.availableActions().contains("BUY_AGAIN"));

        verify(orderRepository, times(1)).save(order);
        verify(paymentRepository, never()).save(any()); // VNPAY đã PAID không cần save lại
    }

    @Test
    @DisplayName("TC-SVC-02: completeOrder kiểm tra tính Idempotent - Đơn vốn đã COMPLETED thì trả về kết quả hiện tại ngay")
    void tc02_completeOrder_Idempotency_AlreadyCompleted() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "TechHub Flagship");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PAID);
        Order order = createMockOrder(orderId, "ORD-20261016-IDEM", OrderStatus.COMPLETED, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.completeOrder(userId, orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.status());
        verify(orderRepository, never()).save(any()); // Không lưu DB lần 2
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-03: completeOrder ném INVALID_STATUS_TRANSITION khi đơn đang SHIPPING")
    void tc03_completeOrder_FromShipping_ThrowsInvalidStatusTransition() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "TechHub Flagship");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-SHIP", OrderStatus.SHIPPING, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.completeOrder(userId, orderId)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-04: completeOrder ném INVALID_STATUS_TRANSITION khi đơn mới ở CONFIRMED")
    void tc04_completeOrder_FromConfirmed_ThrowsInvalidStatusTransition() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "TechHub Flagship");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-CONF", OrderStatus.CONFIRMED, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.completeOrder(userId, orderId)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-05: completeOrder ném INVALID_STATUS_TRANSITION khi đơn đã CANCELLED")
    void tc05_completeOrder_FromCancelled_ThrowsInvalidStatusTransition() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "TechHub Flagship");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-CANC", OrderStatus.CANCELLED, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.completeOrder(userId, orderId)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-06: completeOrder ném ORDER_NOT_FOUND khi đơn thuộc về Buyer khác (IDOR Protection)")
    void tc06_completeOrder_IDOR_OrderBelongsToOtherUser_ThrowsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.completeOrder(userId, orderId)
        );

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-07: completeOrder cho đơn COD lẻ (1 Shop) - Chuyển Payment sang PAID")
    void tc07_completeOrder_CodSingleOrder_PaymentUpdatedToPaid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "TechHub Flagship");
        Payment payment = createMockPayment(paymentId, PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-COD1", OrderStatus.DELIVERED, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(List.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.completeOrder(userId, orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertEquals(PaymentStatus.PAID, payment.getStatus(), "Payment COD phải chuyển thành PAID");
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    @DisplayName("TC-SVC-08: completeOrder đơn COD Đa Shop - 1 đơn COMPLETED, 1 đơn còn SHIPPING -> Payment giữ PENDING")
    void tc08_completeOrder_CodMultiShop_NotAllSettled_PaymentRemainsPending() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Shop shop1 = createMockShop(UUID.randomUUID(), "Shop 1");
        Shop shop2 = createMockShop(UUID.randomUUID(), "Shop 2");
        Payment payment = createMockPayment(paymentId, PaymentMethod.COD, PaymentStatus.PENDING);

        Order order1 = createMockOrder(orderId1, "ORD-20261016-COD-A", OrderStatus.DELIVERED, shop1, payment);
        Order order2StillShipping = createMockOrder(orderId2, "ORD-20261016-COD-B", OrderStatus.SHIPPING, shop2, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId1, userId)).thenReturn(Optional.of(order1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(List.of(order1, order2StillShipping));
        when(orderItemRepository.findByOrderId(orderId1)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.completeOrder(userId, orderId1);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertEquals(PaymentStatus.PENDING, payment.getStatus(), "Payment COD phải giữ PENDING vì còn đơn con đang SHIPPING");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-09: completeOrder đơn COD Đa Shop - 1 đơn COMPLETED, 1 đơn CANCELLED -> Payment chuyển PAID")
    void tc09_completeOrder_CodMultiShop_AllSettledWithCancelled_PaymentUpdatedToPaid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Shop shop1 = createMockShop(UUID.randomUUID(), "Shop 1");
        Shop shop2 = createMockShop(UUID.randomUUID(), "Shop 2");
        Payment payment = createMockPayment(paymentId, PaymentMethod.COD, PaymentStatus.PENDING);

        Order order1 = createMockOrder(orderId1, "ORD-20261016-COD-A", OrderStatus.DELIVERED, shop1, payment);
        Order order2Cancelled = createMockOrder(orderId2, "ORD-20261016-COD-B", OrderStatus.CANCELLED, shop2, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId1, userId)).thenReturn(Optional.of(order1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(List.of(order1, order2Cancelled));
        when(orderItemRepository.findByOrderId(orderId1)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.response.BuyerOrderDetailResponse response = orderService.completeOrder(userId, orderId1);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertEquals(PaymentStatus.PAID, payment.getStatus(), "Payment COD phải chuyển thành PAID khi các đơn còn lại đều đã kết thúc");
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    @DisplayName("TC-SVC-10: completeOrder ném INVALID_INPUT_DATA khi orderId = null")
    void tc10_completeOrder_NullOrderId_ThrowsInvalidInputData() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.completeOrder(userId, null)
        );

        assertEquals(ErrorCode.INVALID_INPUT_DATA, exception.getErrorCode());
        verifyNoInteractions(orderRepository);
    }

    // =========================================================================
    // Unit Tests cho requestOrderCancellation (POST /api/buyer/orders/{id}/cancellation-request)
    // =========================================================================

    @Test
    @DisplayName("TC-SVC-01: requestCancellation từ trạng thái CONFIRMED thành công -> Tạo CancellationRequest PENDING")
    void tc01_requestCancellation_FromConfirmed_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Laptop Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-CONF", OrderStatus.CONFIRMED, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));
        when(cancellationRequestRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        UUID requestId = UUID.randomUUID();
        when(cancellationRequestRepository.save(any(CancellationRequest.class))).thenAnswer(invocation -> {
            CancellationRequest cr = invocation.getArgument(0);
            cr.setId(requestId);
            return cr;
        });

        CreateCancellationRequest request = new CreateCancellationRequest("Đổi địa chỉ giao hàng");
        CancellationRequestResponse response = orderService.requestOrderCancellation(userId, orderId, request);

        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals("ORD-20261016-CONF", response.orderNumber());
        assertEquals(CancellationRequestStatus.PENDING, response.status());
        assertEquals("Đang chờ Người bán phản hồi", response.statusDescription());
        assertEquals("Đổi địa chỉ giao hàng", response.reason());
        assertNull(response.sellerResponse());
        verify(cancellationRequestRepository, times(1)).save(any(CancellationRequest.class));
    }

    @Test
    @DisplayName("TC-SVC-02: requestCancellation từ trạng thái PREPARING thành công -> Tạo CancellationRequest PENDING")
    void tc02_requestCancellation_FromPreparing_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Laptop Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-PREP", OrderStatus.PREPARING, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));
        when(cancellationRequestRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        when(cancellationRequestRepository.save(any(CancellationRequest.class))).thenAnswer(invocation -> {
            CancellationRequest cr = invocation.getArgument(0);
            cr.setId(UUID.randomUUID());
            return cr;
        });

        CreateCancellationRequest request = new CreateCancellationRequest("Tìm thấy giá rẻ hơn ở nơi khác");
        CancellationRequestResponse response = orderService.requestOrderCancellation(userId, orderId, request);

        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals(CancellationRequestStatus.PENDING, response.status());
        assertEquals("Tìm thấy giá rẻ hơn ở nơi khác", response.reason());
        verify(cancellationRequestRepository, times(1)).save(any(CancellationRequest.class));
    }

    @Test
    @DisplayName("TC-SVC-03: requestCancellation Idempotency khi đã có yêu cầu PENDING cùng lý do -> Trả về bản ghi hiện tại")
    void tc03_requestCancellation_Idempotent_DuplicatePendingRequest() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Laptop Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-DUP", OrderStatus.CONFIRMED, shop, payment);

        CancellationRequest existing = CancellationRequest.builder()
                .id(UUID.randomUUID())
                .order(order)
                .user(activeUser)
                .reason("Trùng lý do hủy đơn")
                .status(CancellationRequestStatus.PENDING)
                .build();

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));
        when(cancellationRequestRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        CreateCancellationRequest request = new CreateCancellationRequest("Trùng lý do hủy đơn");
        CancellationRequestResponse response = orderService.requestOrderCancellation(userId, orderId, request);

        assertNotNull(response);
        assertEquals(existing.getId(), response.id());
        assertEquals(CancellationRequestStatus.PENDING, response.status());
        assertEquals("Trùng lý do hủy đơn", response.reason());
        verify(cancellationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-04: requestCancellation ném CANCELLATION_REQUEST_ALREADY_EXISTS khi đã có PENDING khác lý do")
    void tc04_requestCancellation_Conflict_WhenPendingWithDifferentReason() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Laptop Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-DIFF", OrderStatus.CONFIRMED, shop, payment);

        CancellationRequest existing = CancellationRequest.builder()
                .id(UUID.randomUUID())
                .order(order)
                .user(activeUser)
                .reason("Lý do cũ A")
                .status(CancellationRequestStatus.PENDING)
                .build();

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));
        when(cancellationRequestRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        CreateCancellationRequest request = new CreateCancellationRequest("Lý do mới B");
        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.requestOrderCancellation(userId, orderId, request)
        );

        assertEquals(ErrorCode.CANCELLATION_REQUEST_ALREADY_EXISTS, exception.getErrorCode());
        verify(cancellationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-05: requestCancellation ném CANCELLATION_REQUEST_ALREADY_EXISTS khi đơn từng bị REJECTED")
    void tc05_requestCancellation_Throws_WhenPreviouslyRejected() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Laptop Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-REJ", OrderStatus.CONFIRMED, shop, payment);

        CancellationRequest existingRejected = CancellationRequest.builder()
                .id(UUID.randomUUID())
                .order(order)
                .user(activeUser)
                .reason("Muốn hủy đơn")
                .status(CancellationRequestStatus.REJECTED)
                .sellerResponse("Hàng đã đóng gói xong")
                .build();

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));
        when(cancellationRequestRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingRejected));

        CreateCancellationRequest request = new CreateCancellationRequest("Muốn hủy lại lần 2");
        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.requestOrderCancellation(userId, orderId, request)
        );

        assertEquals(ErrorCode.CANCELLATION_REQUEST_ALREADY_EXISTS, exception.getErrorCode());
        verify(cancellationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-SVC-06: requestCancellation từ PENDING_PAYMENT ném INVALID_STATUS_TRANSITION")
    void tc06_requestCancellation_FromPendingPayment_ThrowsInvalidStatusTransition() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Laptop Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.VNPAY, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-PEND", OrderStatus.PENDING_PAYMENT, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));

        CreateCancellationRequest request = new CreateCancellationRequest("Muốn hủy đơn chưa trả tiền");
        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.requestOrderCancellation(userId, orderId, request)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verifyNoInteractions(cancellationRequestRepository);
    }

    @Test
    @DisplayName("TC-SVC-07: requestCancellation từ SHIPPING ném INVALID_STATUS_TRANSITION")
    void tc07_requestCancellation_FromShipping_ThrowsInvalidStatusTransition() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Laptop Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = createMockOrder(orderId, "ORD-20261016-SHIP", OrderStatus.SHIPPING, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));

        CreateCancellationRequest request = new CreateCancellationRequest("Hàng đang giao nhưng muốn hủy");
        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.requestOrderCancellation(userId, orderId, request)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verifyNoInteractions(cancellationRequestRepository);
    }

    @Test
    @DisplayName("TC-SVC-08: requestCancellation từ DELIVERED hoặc COMPLETED ném INVALID_STATUS_TRANSITION")
    void tc08_requestCancellation_FromDeliveredOrCompleted_ThrowsInvalidStatusTransition() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        Shop shop = createMockShop(UUID.randomUUID(), "Laptop Store");
        Payment payment = createMockPayment(UUID.randomUUID(), PaymentMethod.COD, PaymentStatus.PAID);
        Order order = createMockOrder(orderId, "ORD-20261016-COMP", OrderStatus.COMPLETED, shop, payment);

        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.of(order));

        CreateCancellationRequest request = new CreateCancellationRequest("Đã nhận hàng xong muốn hủy");
        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.requestOrderCancellation(userId, orderId, request)
        );

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        verifyNoInteractions(cancellationRequestRepository);
    }

    @Test
    @DisplayName("TC-SVC-09: requestCancellation khi đơn hàng thuộc Buyer khác (IDOR) ném ORDER_NOT_FOUND")
    void tc09_requestCancellation_IDOR_BelongsToOtherUser_ThrowsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndUserIdForUpdate(orderId, userId)).thenReturn(Optional.empty());

        CreateCancellationRequest request = new CreateCancellationRequest("Đổi địa chỉ giao hàng");
        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.requestOrderCancellation(userId, orderId, request)
        );

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(cancellationRequestRepository);
    }

    @Test
    @DisplayName("TC-SVC-10: requestCancellation ném INVALID_INPUT_DATA khi orderId null hoặc reason không hợp lệ")
    void tc10_requestCancellation_NullOrderIdOrInvalidReason_ThrowsInvalidInputData() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        // Case A: orderId = null
        assertThrows(BusinessException.class, () ->
                orderService.requestOrderCancellation(userId, null, new CreateCancellationRequest("Lý do hợp lệ"))
        );

        // Case B: request = null
        assertThrows(BusinessException.class, () ->
                orderService.requestOrderCancellation(userId, UUID.randomUUID(), null)
        );

        // Case C: reason quá ngắn (< 5 ký tự)
        assertThrows(BusinessException.class, () ->
                orderService.requestOrderCancellation(userId, UUID.randomUUID(), new CreateCancellationRequest("Hủy"))
        );

        verifyNoInteractions(orderRepository);
    }

    // ==========================================
    // POST /api/buyer/orders/{id}/reorder (API 12)
    // ==========================================

    @Test
    @DisplayName("TC-01: reorder_Success_AllItemsAvailable - Mua lại toàn bộ sản phẩm thành công")
    void reorder_Success_AllItemsAvailable() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), shop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 10, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);

        UUID v2Id = UUID.randomUUID();
        ProductVariant v2 = createTestVariant(v2Id, product, 5, new BigDecimal("200000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(2).discountedUnitPrice(new BigDecimal("100000")).build();
        OrderItem item2 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v2).productNameSnapshot("SP 2").variantNameSnapshot("V2").skuSnapshot("SKU2").quantity(1).discountedUnitPrice(new BigDecimal("200000")).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1, item2));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>());
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1, v2));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals(2, response.totalItemsRequested());
        assertEquals(2, response.itemsAddedSuccessfully());
        assertEquals(2, response.addedItems().size());
        assertTrue(response.unavailableItems().isEmpty());
        assertEquals(2, response.cartTotalItemTypes());
        assertEquals(3, response.cartTotalQuantity());

        verify(cartItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-02: reorder_Success_PartialItemsAvailable_OutOfStock - Mua lại thành công 1 phần khi có sản phẩm hết hàng")
    void reorder_Success_PartialItemsAvailable_OutOfStock() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), shop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 10, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);
        UUID v2Id = UUID.randomUUID();
        ProductVariant v2 = createTestVariant(v2Id, product, 0, new BigDecimal("200000"), ProductVariantStatus.ACTIVE); // Hết hàng

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(1).build();
        OrderItem item2 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v2).productNameSnapshot("SP 2").variantNameSnapshot("V2").skuSnapshot("SKU2").quantity(2).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1, item2));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>());
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1, v2));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.totalItemsRequested());
        assertEquals(1, response.itemsAddedSuccessfully());
        assertEquals(1, response.addedItems().size());
        assertEquals(1, response.unavailableItems().size());
        assertEquals("OUT_OF_STOCK", response.unavailableItems().get(0).reason());
        assertEquals(v2Id, response.unavailableItems().get(0).productVariantId());
    }

    @Test
    @DisplayName("TC-03: reorder_Success_PartialItemsAvailable_InactiveShop - Chặn mặt hàng khi Shop tạm nghỉ hoặc tạm khóa")
    void reorder_Success_PartialItemsAvailable_InactiveShop() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop activeShop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Shop pausedShop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.PAUSED);

        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product p1 = createTestProduct(UUID.randomUUID(), activeShop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);
        Product p2 = createTestProduct(UUID.randomUUID(), pausedShop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, p1, 10, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);
        UUID v2Id = UUID.randomUUID();
        ProductVariant v2 = createTestVariant(v2Id, p2, 10, new BigDecimal("200000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(activeShop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(1).build();
        OrderItem item2 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v2).productNameSnapshot("SP 2").variantNameSnapshot("V2").skuSnapshot("SKU2").quantity(1).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1, item2));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>());
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1, v2));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertEquals(2, response.totalItemsRequested());
        assertEquals(1, response.itemsAddedSuccessfully());
        assertEquals(1, response.unavailableItems().size());
        assertEquals("SHOP_IS_PAUSED", response.unavailableItems().get(0).reason());
    }

    @Test
    @DisplayName("TC-04: reorder_Success_AllItemsUnavailable - Tất cả sản phẩm không khả dụng trả về HTTP 200 OK với added=0")
    void reorder_Success_AllItemsUnavailable() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), shop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 0, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(1).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>());
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertEquals(1, response.totalItemsRequested());
        assertEquals(0, response.itemsAddedSuccessfully());
        assertTrue(response.addedItems().isEmpty());
        assertEquals(1, response.unavailableItems().size());
        verify(cartItemRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-05: reorder_Success_ExistingCartItem_AccumulatesQuantity - Cộng dồn số lượng khi SKU đã có sẵn trong giỏ")
    void reorder_Success_ExistingCartItem_AccumulatesQuantity() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), shop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 20, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(2).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        CartItem existingCartItem = CartItem.builder().id(UUID.randomUUID()).cart(cart).product(product).productVariant(v1).quantity(3).build();

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>(List.of(existingCartItem)));
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertEquals(1, response.itemsAddedSuccessfully());
        assertEquals(5, existingCartItem.getQuantity()); // 3 + 2 = 5
        assertEquals(1, response.cartTotalItemTypes());
        assertEquals(5, response.cartTotalQuantity());
        verify(cartItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-06: reorder_Success_ExistingCartItem_StockCap - Cộng dồn tối đa đến mức tồn kho còn lại")
    void reorder_Success_ExistingCartItem_StockCap() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), shop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 5, new BigDecimal("100000"), ProductVariantStatus.ACTIVE); // Tồn kho chỉ còn 5

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(3).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        CartItem existingCartItem = CartItem.builder().id(UUID.randomUUID()).cart(cart).product(product).productVariant(v1).quantity(4).build(); // Giỏ đã có 4

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>(List.of(existingCartItem)));
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertEquals(1, response.itemsAddedSuccessfully());
        assertEquals(1, response.addedItems().get(0).quantityAdded()); // Chỉ thêm được 5 - 4 = 1
        assertEquals(5, existingCartItem.getQuantity()); // Đạt mức tối đa tồn kho 5
    }

    @Test
    @DisplayName("TC-07: reorder_Success_ExistingCartItem_AlreadyAtStockLimit - Chặn cộng dồn khi giỏ hàng đã bằng tồn kho")
    void reorder_Success_ExistingCartItem_AlreadyAtStockLimit() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), shop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 5, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(1).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        CartItem existingCartItem = CartItem.builder().id(UUID.randomUUID()).cart(cart).product(product).productVariant(v1).quantity(5).build(); // Đã đủ 5 cái

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>(List.of(existingCartItem)));
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertEquals(0, response.itemsAddedSuccessfully());
        assertEquals(1, response.unavailableItems().size());
        assertEquals("INSUFFICIENT_STOCK", response.unavailableItems().get(0).reason());
    }

    @Test
    @DisplayName("TC-08: reorder_Success_CartCapacityLimitEnforced - Chặn thêm SKU mới khi giỏ hàng đã đạt 50 loại mặt hàng")
    void reorder_Success_CartCapacityLimitEnforced() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), shop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 10, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(1).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        List<CartItem> fullCartItems = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            UUID dummyVarId = UUID.randomUUID();
            ProductVariant dummyVar = ProductVariant.builder().id(dummyVarId).build();
            fullCartItems.add(CartItem.builder().id(UUID.randomUUID()).cart(cart).productVariant(dummyVar).quantity(1).build());
        }

        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(fullCartItems);
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertEquals(0, response.itemsAddedSuccessfully());
        assertEquals(1, response.unavailableItems().size());
        assertEquals("CART_CAPACITY_EXCEEDED", response.unavailableItems().get(0).reason());
    }

    @Test
    @DisplayName("TC-09: reorder_Success_ProductUnpublishedOrSuspended_Blocked - Chặn mặt hàng khi sản phẩm bị gỡ hoặc khóa")
    void reorder_Success_ProductUnpublishedOrSuspended_Blocked() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product suspendedProduct = createTestProduct(UUID.randomUUID(), shop, category, ProductVisibilityStatus.UNPUBLISHED, ProductApprovalStatus.APPROVED, true);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, suspendedProduct, 10, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(1).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>());
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertEquals(0, response.itemsAddedSuccessfully());
        assertEquals(1, response.unavailableItems().size());
        assertEquals("PRODUCT_UNAVAILABLE", response.unavailableItems().get(0).reason());
    }

    @Test
    @DisplayName("TC-10: reorder_Success_CategoryInactiveOrDeleted_Blocked - Chặn mặt hàng khi ngành hàng bị khóa hoặc xóa mềm")
    void reorder_Success_CategoryInactiveOrDeleted_Blocked() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category inactiveCategory = createTestCategory(UUID.randomUUID(), CategoryStatus.INACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), shop, inactiveCategory, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 10, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(1).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>());
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertEquals(0, response.itemsAddedSuccessfully());
        assertEquals(1, response.unavailableItems().size());
        assertEquals("CATEGORY_INACTIVE", response.unavailableItems().get(0).reason());
    }

    @Test
    @DisplayName("TC-11: reorder_Success_AntiSelfBuying_Blocked - Chặn mua lại sản phẩm từ gian hàng của chính mình")
    void reorder_Success_AntiSelfBuying_Blocked() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        // Shop belongs to userId
        Shop selfShop = createTestShop(UUID.randomUUID(), userId, ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), selfShop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 10, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(selfShop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(1).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1));

        Cart cart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        when(cartRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(new ArrayList<>());
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertEquals(0, response.itemsAddedSuccessfully());
        assertEquals(1, response.unavailableItems().size());
        assertEquals("CANNOT_ADD_OWN_PRODUCT", response.unavailableItems().get(0).reason());
    }

    @Test
    @DisplayName("TC-12: reorder_Success_LazyCreateCartWhenNotExists - Tự động tạo giỏ hàng mới khi người dùng chưa có giỏ")
    void reorder_Success_LazyCreateCartWhenNotExists() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Shop shop = createTestShop(UUID.randomUUID(), UUID.randomUUID(), ShopStatus.ACTIVE);
        Category category = createTestCategory(UUID.randomUUID(), CategoryStatus.ACTIVE);
        Product product = createTestProduct(UUID.randomUUID(), shop, category, ProductVisibilityStatus.PUBLISHED, ProductApprovalStatus.APPROVED, false);

        UUID v1Id = UUID.randomUUID();
        ProductVariant v1 = createTestVariant(v1Id, product, 10, new BigDecimal("100000"), ProductVariantStatus.ACTIVE);

        Order order = Order.builder().id(orderId).user(activeUser).shop(shop).status(OrderStatus.COMPLETED).build();
        OrderItem item1 = OrderItem.builder().id(UUID.randomUUID()).order(order).productVariant(v1).productNameSnapshot("SP 1").variantNameSnapshot("V1").skuSnapshot("SKU1").quantity(1).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1));

        Cart newlyCreatedCart = Cart.builder().id(UUID.randomUUID()).user(activeUser).build();
        when(cartRepository.findByUserIdWithLock(userId))
                .thenReturn(Optional.empty()) // Lúc đầu chưa có
                .thenReturn(Optional.of(newlyCreatedCart)); // Sau khi save
        when(cartRepository.save(any(Cart.class))).thenReturn(newlyCreatedCart);
        when(cartItemRepository.findByCartId(newlyCreatedCart.getId())).thenReturn(new ArrayList<>());
        when(productVariantRepository.findAllByIdInWithDetails(anyCollection())).thenReturn(List.of(v1));

        // Act
        ReorderResponse response = orderService.reorder(userId, orderId);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.itemsAddedSuccessfully());
        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-13: reorder_Throws_OrderNotFound_WhenIdNotExists - Đơn hàng không tồn tại ném ORDER_NOT_FOUND (HTTP 404)")
    void reorder_Throws_OrderNotFound_WhenIdNotExists() {
        // Arrange
        UUID nonExistentOrderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderRepository.findByIdAndUserId(nonExistentOrderId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.reorder(userId, nonExistentOrderId));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(cartRepository);
    }

    @Test
    @DisplayName("TC-14: reorder_Throws_OrderNotFound_WhenBelongsToAnotherUser - Chặn IDOR khi truy cập đơn của người khác")
    void reorder_Throws_OrderNotFound_WhenBelongsToAnotherUser() {
        // Arrange
        UUID otherUserOrderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderRepository.findByIdAndUserId(otherUserOrderId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.reorder(userId, otherUserOrderId));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(cartRepository);
    }

    @Test
    @DisplayName("TC-15: reorder_Throws_WhenUserIdOrOrderIdNull - Đầu vào null ném ORDER_NOT_FOUND fail-fast")
    void reorder_Throws_WhenUserIdOrOrderIdNull() {
        // Case 1: userId null
        assertThrows(BusinessException.class, () -> orderService.reorder(null, UUID.randomUUID()));

        // Case 2: orderId null
        assertThrows(BusinessException.class, () -> orderService.reorder(userId, null));

        verifyNoInteractions(orderRepository);
    }

    // ==========================================
    // Helper Test Methods
    // ==========================================

    private Shop createTestShop(UUID shopId, UUID sellerUserId, ShopStatus status) {
        User sellerUser = new User();
        sellerUser.setId(sellerUserId);
        sellerUser.setStatus(UserStatus.ACTIVE);
        Seller seller = Seller.builder().id(UUID.randomUUID()).user(sellerUser).sellerName("Seller Test").build();
        return Shop.builder().id(shopId).name("Shop Test").seller(seller).status(status).build();
    }

    private Category createTestCategory(UUID categoryId, CategoryStatus status) {
        return Category.builder().id(categoryId).name("Category Test").status(status).isDeleted(false).build();
    }

    private Product createTestProduct(UUID productId, Shop shop, Category category, ProductVisibilityStatus visStatus, ProductApprovalStatus appStatus, boolean isSuspended) {
        return Product.builder()
                .id(productId)
                .name("Product Test")
                .shop(shop)
                .category(category)
                .visibilityStatus(visStatus)
                .approvalStatus(appStatus)
                .isSuspended(isSuspended)
                .isDeleted(false)
                .build();
    }

    private ProductVariant createTestVariant(UUID variantId, Product product, int stock, BigDecimal price, ProductVariantStatus status) {
        return ProductVariant.builder()
                .id(variantId)
                .product(product)
                .variantName("Variant Test")
                .sku("SKU-" + variantId.toString().substring(0, 8))
                .stockQuantity(stock)
                .price(price)
                .status(status)
                .isDeleted(false)
                .build();
    }
}

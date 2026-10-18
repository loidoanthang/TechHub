package com.techhub.service.order;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateSellerOrderStatusRequest;
import com.techhub.model.dto.response.*;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.OrderItemRepository;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.service.order.impl.SellerOrderServiceImpl;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerOrderService (GET /api/seller/orders)")
class SellerOrderServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private com.techhub.repository.CancellationRequestRepository cancellationRequestRepository;

    @Mock
    private com.techhub.repository.ProductVariantRepository productVariantRepository;

    @Mock
    private com.techhub.repository.PaymentRepository paymentRepository;

    @Mock
    private com.techhub.repository.RefundTransactionRepository refundTransactionRepository;

    @Mock
    private com.techhub.repository.VoucherRedemptionRepository voucherRedemptionRepository;

    @Mock
    private com.techhub.repository.VoucherRepository voucherRepository;

    @InjectMocks
    private SellerOrderServiceImpl sellerOrderService;

    private UUID userId;
    private UUID sellerId;
    private UUID shopId;
    private User mockUser;
    private Seller mockSeller;
    private Shop mockShop;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();

        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("seller@techhub.vn");
        mockUser.setFirstName("Van A");
        mockUser.setLastName("Nguyen");
        mockUser.setPhone("0987654321");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setEmailVerified(true);
        mockUser.setRoles(Set.of(Role.SELLER));

        mockSeller = new Seller();
        mockSeller.setId(sellerId);
        mockSeller.setUser(mockUser);
        mockSeller.setSellerName("apple-store");
        mockSeller.setVerificationStatus(SellerVerificationStatus.APPROVED);

        mockShop = new Shop();
        mockShop.setId(shopId);
        mockShop.setSeller(mockSeller);
        mockShop.setName("Apple Official Store");
        mockShop.setStatus(ShopStatus.ACTIVE);
    }

    private Order createMockOrder(UUID orderId, String orderNumber, OrderStatus status, BigDecimal productAmount, BigDecimal shippingFee, BigDecimal shopDiscount, BigDecimal platformDiscount) {
        User buyer = new User();
        buyer.setId(UUID.randomUUID());
        buyer.setFirstName("Thi B");
        buyer.setLastName("Tran");
        buyer.setEmail("buyer@gmail.com");
        buyer.setPhone("0912345678");

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .amount(productAmount.add(shippingFee).subtract(shopDiscount).subtract(platformDiscount))
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .build();

        String addressSnapshot = """
                {
                  "id": "%s",
                  "recipientName": "Tran Thi B",
                  "phoneNumber": "0912345678",
                  "streetAddress": "Số 123 Đường Cầu Giấy",
                  "ward": "Phường Dịch Vọng",
                  "district": "Quận Cầu Giấy",
                  "provinceCity": "Thành phố Hà Nội"
                }
                """.formatted(UUID.randomUUID());

        BigDecimal totalAmount = productAmount.add(shippingFee).subtract(shopDiscount).subtract(platformDiscount);
        BigDecimal commission = productAmount.subtract(shopDiscount).multiply(BigDecimal.valueOf(0.05));
        BigDecimal earnings = productAmount.subtract(shopDiscount).subtract(commission);

        return Order.builder()
                .id(orderId)
                .orderNumber(orderNumber)
                .user(buyer)
                .shop(mockShop)
                .payment(payment)
                .status(status)
                .deliveryAddressSnapshot(addressSnapshot)
                .totalProductAmount(productAmount)
                .shippingFee(shippingFee)
                .shopDiscountAmount(shopDiscount)
                .platformDiscountAmount(platformDiscount)
                .totalCommission(commission)
                .totalSellerEarnings(earnings)
                .totalAmount(totalAmount)
                .note("Giao giờ hành chính")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private OrderItem createMockOrderItem(Order order, String productName, String variantName, BigDecimal price, int qty) {
        ProductVariant variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .sku("SKU-" + UUID.randomUUID().toString().substring(0, 8))
                .build();

        return OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .productVariant(variant)
                .productNameSnapshot(productName)
                .variantNameSnapshot(variantName)
                .skuSnapshot(variant.getSku())
                .imageUrlSnapshot("https://img.techhub.vn/item.png")
                .originalPrice(price)
                .discountedUnitPrice(price)
                .quantity(qty)
                .finalSubtotal(price.multiply(BigDecimal.valueOf(qty)))
                .build();
    }

    @Test
    @DisplayName("TC-01: getSellerOrders_Success_DefaultPagination_ReturnAllShopOrders")
    void tc01_getSellerOrders_Success_DefaultPagination_ReturnAllShopOrders() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Order order1 = createMockOrder(UUID.randomUUID(), "ORD-001", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.valueOf(20000), BigDecimal.ZERO);
        Order order2 = createMockOrder(UUID.randomUUID(), "ORD-002", OrderStatus.PREPARING, BigDecimal.valueOf(1000000), BigDecimal.valueOf(40000), BigDecimal.valueOf(50000), BigDecimal.ZERO);
        Page<Order> orderPage = new PageImpl<>(List.of(order1, order2), PageRequest.of(0, 10), 2);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);

        OrderItem item1 = createMockOrderItem(order1, "Sạc 20W", "Trắng", BigDecimal.valueOf(500000), 1);
        OrderItem item2 = createMockOrderItem(order2, "Chuột MX", "Đen", BigDecimal.valueOf(1000000), 1);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(List.of(item1, item2));

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(2, result.content().size());
        assertEquals("ORD-001", result.content().get(0).orderNumber());
        assertEquals(OrderStatus.CONFIRMED, result.content().get(0).status());
        assertEquals(1, result.content().get(0).totalItemsCount());
        assertNotNull(result.content().get(0).buyerInfo());
        assertEquals("Tran Thi B", result.content().get(0).buyerInfo().recipientName());
        assertEquals("0912345678", result.content().get(0).buyerInfo().recipientPhone());

        verify(orderRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(orderItemRepository, times(1)).findByOrderIdIn(anyList());
    }

    @Test
    @DisplayName("TC-02: getSellerOrders_Success_FilterByStatus_Confirmed")
    void tc02_getSellerOrders_Success_FilterByStatus_Confirmed() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-CONFIRMED", OrderStatus.CONFIRMED, BigDecimal.valueOf(300000), BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.ZERO);
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);

        OrderItem item = createMockOrderItem(order, "Tai nghe", "Xanh", BigDecimal.valueOf(300000), 1);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(List.of(item));

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, OrderStatus.CONFIRMED, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(OrderStatus.CONFIRMED, result.content().get(0).status());
        assertEquals("Đã xác nhận (Chờ chuẩn bị hàng)", result.content().get(0).statusDescription());
        assertTrue(result.content().get(0).availableActions().contains("START_PREPARING"));
        assertTrue(result.content().get(0).availableActions().contains("CANCEL_ORDER"));
    }

    @Test
    @DisplayName("TC-03: getSellerOrders_Success_FilterByStatus_PendingPayment_EmptyActions")
    void tc03_getSellerOrders_Success_FilterByStatus_PendingPayment_EmptyActions() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-PENDING", OrderStatus.PENDING_PAYMENT, BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.ZERO);
        order.getPayment().setPaymentMethod(PaymentMethod.VNPAY);
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, OrderStatus.PENDING_PAYMENT, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(OrderStatus.PENDING_PAYMENT, result.content().get(0).status());
        assertEquals("Chờ người mua thanh toán", result.content().get(0).statusDescription());
        // BR-15: Khi chưa thanh toán, Seller không được thao tác bất kỳ nút fulfillment nào
        assertTrue(result.content().get(0).availableActions().isEmpty(),
                "Khi đơn hàng PENDING_PAYMENT, availableActions phải là danh sách rỗng");
    }

    @Test
    @DisplayName("TC-04: getSellerOrders_Success_FilterByStatus_Preparing")
    void tc04_getSellerOrders_Success_FilterByStatus_Preparing() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-PREP", OrderStatus.PREPARING, BigDecimal.valueOf(400000), BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO);
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, OrderStatus.PREPARING, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Shop đang chuẩn bị hàng", result.content().get(0).statusDescription());
        assertTrue(result.content().get(0).availableActions().contains("READY_FOR_PICKUP"));
        assertTrue(result.content().get(0).availableActions().contains("PRINT_SHIPPING_LABEL"));
        assertTrue(result.content().get(0).availableActions().contains("CANCEL_ORDER"));
    }

    @Test
    @DisplayName("TC-05: getSellerOrders_Success_SearchByOrderNumberKeyword")
    void tc05_getSellerOrders_Success_SearchByOrderNumberKeyword() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-20261015-8F3A", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, "8F3A", null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("ORD-20261015-8F3A", result.content().get(0).orderNumber());
    }

    @Test
    @DisplayName("TC-06: getSellerOrders_Success_SearchByProductNameSnapshotKeyword")
    void tc06_getSellerOrders_Success_SearchByProductNameSnapshotKeyword() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-006", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);

        OrderItem item = createMockOrderItem(order, "Củ sạc nhanh Apple 20W", "Trắng", BigDecimal.valueOf(500000), 1);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(List.of(item));

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, "Apple 20W", null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Củ sạc nhanh Apple 20W", result.content().get(0).items().get(0).productName());
    }

    @Test
    @DisplayName("TC-07: getSellerOrders_Success_FilterByDateRange_WithinRange")
    void tc07_getSellerOrders_Success_FilterByDateRange_WithinRange() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        LocalDate startDate = LocalDate.of(2026, 10, 10);
        LocalDate endDate = LocalDate.of(2026, 10, 16);

        Order order = createMockOrder(UUID.randomUUID(), "ORD-DATE", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, null, startDate, endDate, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
    }

    @Test
    @DisplayName("TC-08: getSellerOrders_Throw_WhenStartDateAfterEndDate")
    void tc08_getSellerOrders_Throw_WhenStartDateAfterEndDate() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        LocalDate startDate = LocalDate.of(2026, 10, 20);
        LocalDate endDate = LocalDate.of(2026, 10, 10);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrders(userId, null, null, startDate, endDate, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-08b: getSellerOrders_Throw_WhenDateRangeExceeds365Days")
    void tc08b_getSellerOrders_Throw_WhenDateRangeExceeds365Days() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 5); // > 365 days

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrders(userId, null, null, startDate, endDate, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-09: getSellerOrders_IDOR_Isolation_NeverReturnOtherShopOrders")
    void tc09_getSellerOrders_IDOR_Isolation_NeverReturnOtherShopOrders() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        // When queried, only orders matching shopId are fetched
        Order shop1Order = createMockOrder(UUID.randomUUID(), "ORD-SHOP1", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        Page<Order> orderPage = new PageImpl<>(List.of(shop1Order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("ORD-SHOP1", result.content().get(0).orderNumber());
    }

    @Test
    @DisplayName("TC-10: getSellerOrders_Success_ShopPaused_AllowedToViewOrders")
    void tc10_getSellerOrders_Success_ShopPaused_AllowedToViewOrders() {
        mockShop.setStatus(ShopStatus.PAUSED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-PAUSED", OrderStatus.CONFIRMED, BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.ZERO);
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        // BR-07: Shop PAUSED is allowed to view orders to fulfill pending orders
        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
    }

    @Test
    @DisplayName("TC-11: getSellerOrders_Success_ShopSuspended_AllowedReadOnly")
    void tc11_getSellerOrders_Success_ShopSuspended_AllowedReadOnly() {
        mockShop.setStatus(ShopStatus.SUSPENDED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-SUSPENDED", OrderStatus.CONFIRMED, BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.ZERO);
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        // BR-07: Shop SUSPENDED is allowed read-only access for settlement & disputes
        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
    }

    @Test
    @DisplayName("TC-12: getSellerOrders_Throw_WhenShopBanned")
    void tc12_getSellerOrders_Throw_WhenShopBanned() {
        mockShop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        // BR-07: Shop BANNED is locked out from Seller Center
        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrders(userId, null, null, null, null, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: getSellerOrders_Throw_WhenUserNotApprovedSeller")
    void tc13_getSellerOrders_Throw_WhenUserNotApprovedSeller() {
        mockSeller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));

        // BR-06: Only APPROVED sellers can access seller orders
        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrders(userId, null, null, null, null, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.NOT_A_SELLER, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: getSellerOrders_ZeroNPlusOneQuery_VerifyFixedSqlCount")
    void tc14_getSellerOrders_ZeroNPlusOneQuery_VerifyFixedSqlCount() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        List<Order> orders = new ArrayList<>();
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Order o = createMockOrder(UUID.randomUUID(), "ORD-" + i, OrderStatus.CONFIRMED, BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.ZERO);
            orders.add(o);
            items.add(createMockOrderItem(o, "Item 1", "Default", BigDecimal.valueOf(100000), 1));
            items.add(createMockOrderItem(o, "Item 2", "Default", BigDecimal.valueOf(100000), 1));
        }

        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 5);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(items);

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertEquals(5, result.content().size());
        // Verify: đúng 1 query findAll orders và đúng 1 query findByOrderIdIn items (Zero N+1)
        verify(orderRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(orderItemRepository, times(1)).findByOrderIdIn(anyList());
    }

    @Test
    @DisplayName("TC-15: getSellerOrders_SanitizeKeyword_SpecialCharacters")
    void tc15_getSellerOrders_SanitizeKeyword_SpecialCharacters() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, "%_special\\_", null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(result);
        assertTrue(result.content().isEmpty());
    }

    @Test
    @DisplayName("TC-16: getSellerOrders_ClampPagination_SizeMax50")
    void tc16_getSellerOrders_ClampPagination_SizeMax50() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(orderRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(emptyPage);

        sellerOrderService.getSellerOrders(
                userId, null, null, null, null, -5, 9999, "total_seller_earnings", "asc"
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber(), "Page âm phải tự động clamp về 0");
        assertEquals(50, capturedPageable.getPageSize(), "Size quá lớn phải tự động clamp về 50");
        assertTrue(capturedPageable.getSort().getOrderFor("totalSellerEarnings").isAscending());
    }

    @Test
    @DisplayName("getSellerOrders_Throw_WhenUserBanned")
    void getSellerOrders_Throw_WhenUserBanned() {
        mockUser.setStatus(UserStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrders(userId, null, null, null, null, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
    }

    @Test
    @DisplayName("getSellerOrders_Throw_WhenEmailNotVerified")
    void getSellerOrders_Throw_WhenEmailNotVerified() {
        mockUser.setEmailVerified(false);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrders(userId, null, null, null, null, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("getSellerOrders_Throw_WhenSellerNotFound")
    void getSellerOrders_Throw_WhenSellerNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrders(userId, null, null, null, null, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("getSellerOrders_Throw_WhenShopNotFound")
    void getSellerOrders_Throw_WhenShopNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrders(userId, null, null, null, null, 0, 10, "createdAt", "desc"));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("getSellerOrders_FinancialBreakdown_AndEarningsAccuracy")
    void getSellerOrders_FinancialBreakdown_AndEarningsAccuracy() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        BigDecimal productAmount = BigDecimal.valueOf(1000000.00);
        BigDecimal shippingFee = BigDecimal.valueOf(30000.00);
        BigDecimal shopDiscount = BigDecimal.valueOf(60000.00);
        BigDecimal platformDiscount = BigDecimal.valueOf(40000.00);

        Order order = createMockOrder(UUID.randomUUID(), "ORD-FIN", OrderStatus.CONFIRMED, productAmount, shippingFee, shopDiscount, platformDiscount);
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        SellerOrderResponse res = result.content().get(0);
        assertEquals(0, productAmount.compareTo(res.totalProductAmount()));
        assertEquals(0, shippingFee.compareTo(res.shippingFee()));
        assertEquals(0, shopDiscount.compareTo(res.shopDiscountAmount()));
        assertEquals(0, platformDiscount.compareTo(res.platformDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(47000).compareTo(res.totalCommission()));
        assertEquals(0, BigDecimal.valueOf(893000).compareTo(res.totalSellerEarnings()));
        assertEquals(0, BigDecimal.valueOf(930000).compareTo(res.buyerTotalAmount()));
    }

    @Test
    @DisplayName("getSellerOrders_AllStatusActions_Evaluation")
    void getSellerOrders_AllStatusActions_Evaluation() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        Order o1 = createMockOrder(UUID.randomUUID(), "O1", OrderStatus.WAITING_FOR_PICKUP, BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Order o2 = createMockOrder(UUID.randomUUID(), "O2", OrderStatus.SHIPPING, BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Order o3 = createMockOrder(UUID.randomUUID(), "O3", OrderStatus.DELIVERED, BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Order o4 = createMockOrder(UUID.randomUUID(), "O4", OrderStatus.COMPLETED, BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Order o5 = createMockOrder(UUID.randomUUID(), "O5", OrderStatus.CANCELLED, BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        Page<Order> orderPage = new PageImpl<>(List.of(o1, o2, o3, o4, o5), PageRequest.of(0, 10), 5);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerOrderResponse> result = sellerOrderService.getSellerOrders(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertEquals(List.of("PRINT_SHIPPING_LABEL", "TRACK_SHIPPING"), result.content().get(0).availableActions());
        assertEquals(List.of("TRACK_SHIPPING"), result.content().get(1).availableActions());
        assertEquals(List.of("VIEW_SETTLEMENT"), result.content().get(2).availableActions());
        assertEquals(List.of("VIEW_SETTLEMENT"), result.content().get(3).availableActions());
        assertEquals(List.of("VIEW_REASON"), result.content().get(4).availableActions());
    }

    // ==========================================
    // GET /api/seller/orders/{id} - DETAIL TESTS
    // ==========================================

    @Test
    @DisplayName("TC-01: getSellerOrderDetail_Success_ConfirmedOrder_FullBreakdown")
    void tc01_getSellerOrderDetail_Success_ConfirmedOrder_FullBreakdown() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-CONFIRMED", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.valueOf(30000), BigDecimal.ZERO);
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));

        OrderItem item1 = createMockOrderItem(order, "Sạc 20W", "Trắng", BigDecimal.valueOf(250000), 1);
        OrderItem item2 = createMockOrderItem(order, "Cáp Type C", "Trắng", BigDecimal.valueOf(250000), 1);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item1, item2));

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertEquals(orderId, res.orderId());
        assertEquals("ORD-CONFIRMED", res.orderNumber());
        assertEquals(OrderStatus.CONFIRMED, res.status());
        assertEquals("Đã xác nhận (Chờ chuẩn bị hàng)", res.statusDescription());
        assertEquals(2, res.items().size());
        assertEquals(2, res.totalItemsCount());
        assertEquals("Tran Thi B", res.buyerInfo().recipientName());
        assertEquals("0912345678", res.buyerInfo().recipientPhone());
        assertNotNull(res.deliveryAddress());
        assertEquals("Tran Thi B", res.deliveryAddress().recipientName());
        assertEquals("Thành phố Hà Nội", res.deliveryAddress().provinceCity());
        assertEquals(0, BigDecimal.valueOf(23500).compareTo(res.totalCommission()));
        assertEquals(0, BigDecimal.valueOf(446500).compareTo(res.totalSellerEarnings()));
        assertTrue(res.availableActions().contains("START_PREPARING"));
        assertTrue(res.availableActions().contains("CANCEL_ORDER"));
        assertFalse(res.timeline().isEmpty());
    }

    @Test
    @DisplayName("TC-02: getSellerOrderDetail_Success_PendingPayment_EmptyActions")
    void tc02_getSellerOrderDetail_Success_PendingPayment_EmptyActions() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PENDING", OrderStatus.PENDING_PAYMENT, BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.ZERO);
        order.getPayment().setPaymentMethod(PaymentMethod.VNPAY);
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertEquals(OrderStatus.PENDING_PAYMENT, res.status());
        assertEquals("Chờ người mua thanh toán", res.statusDescription());
        assertTrue(res.availableActions().isEmpty(), "Khi đơn hàng PENDING_PAYMENT, availableActions phải là rỗng để chặn fulfillment (BR-15)");
    }

    @Test
    @DisplayName("TC-03: getSellerOrderDetail_Success_PreparingOrder_FulfillmentActions")
    void tc03_getSellerOrderDetail_Success_PreparingOrder_FulfillmentActions() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PREP", OrderStatus.PREPARING, BigDecimal.valueOf(400000), BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO);
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertEquals("Shop đang chuẩn bị hàng", res.statusDescription());
        assertTrue(res.availableActions().contains("READY_FOR_PICKUP"));
        assertTrue(res.availableActions().contains("PRINT_SHIPPING_LABEL"));
        assertTrue(res.availableActions().contains("CANCEL_ORDER"));
    }

    @Test
    @DisplayName("TC-04: getSellerOrderDetail_Success_WaitingForPickup_ShippingActions")
    void tc04_getSellerOrderDetail_Success_WaitingForPickup_ShippingActions() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-WAIT", OrderStatus.WAITING_FOR_PICKUP, BigDecimal.valueOf(400000), BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO);
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertEquals(List.of("PRINT_SHIPPING_LABEL", "TRACK_SHIPPING"), res.availableActions());
    }

    @Test
    @DisplayName("TC-05: getSellerOrderDetail_Success_Shipping_TrackAction")
    void tc05_getSellerOrderDetail_Success_Shipping_TrackAction() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-SHIP", OrderStatus.SHIPPING, BigDecimal.valueOf(400000), BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO);
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertEquals(List.of("TRACK_SHIPPING"), res.availableActions());
    }

    @Test
    @DisplayName("TC-06: getSellerOrderDetail_Success_Delivered_ViewSettlementAction")
    void tc06_getSellerOrderDetail_Success_Delivered_ViewSettlementAction() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-DELIV", OrderStatus.DELIVERED, BigDecimal.valueOf(400000), BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO);
        order.setUpdatedAt(LocalDateTime.now());
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertNotNull(res.deliveredAt());
        assertEquals(List.of("VIEW_SETTLEMENT"), res.availableActions());
    }

    @Test
    @DisplayName("TC-07: getSellerOrderDetail_Success_Completed_ClosedLifecycle")
    void tc07_getSellerOrderDetail_Success_Completed_ClosedLifecycle() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-COMP", OrderStatus.COMPLETED, BigDecimal.valueOf(400000), BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO);
        order.setUpdatedAt(LocalDateTime.now());
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertNotNull(res.completedAt());
        assertEquals(List.of("VIEW_SETTLEMENT"), res.availableActions());
    }

    @Test
    @DisplayName("TC-08: getSellerOrderDetail_Success_Cancelled_ViewReasonAction")
    void tc08_getSellerOrderDetail_Success_Cancelled_ViewReasonAction() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-CANCEL", OrderStatus.CANCELLED, BigDecimal.valueOf(400000), BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO);
        order.setUpdatedAt(LocalDateTime.now());
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertNotNull(res.cancelledAt());
        assertEquals(List.of("VIEW_REASON"), res.availableActions());
        assertEquals(2, res.timeline().size());
        assertEquals("CANCELLED", res.timeline().get(1).step());
    }

    @Test
    @DisplayName("TC-09: getSellerOrderDetail_IDOR_Isolation_Throw_WhenOrderBelongsToOtherShop")
    void tc09_getSellerOrderDetail_IDOR_Isolation_Throw_WhenOrderBelongsToOtherShop() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        // Repository filters by orderId and shopId. If order belongs to Shop B, it returns empty
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrderDetail(userId, orderId));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: getSellerOrderDetail_Throw_WhenOrderNotFound")
    void tc10_getSellerOrderDetail_Throw_WhenOrderNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrderDetail(userId, orderId));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: getSellerOrderDetail_Success_ShopPaused_AllowedReadOnly")
    void tc11_getSellerOrderDetail_Success_ShopPaused_AllowedReadOnly() {
        mockShop.setStatus(ShopStatus.PAUSED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PAUSED", OrderStatus.CONFIRMED, BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.ZERO);
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertEquals(orderId, res.orderId());
    }

    @Test
    @DisplayName("TC-12: getSellerOrderDetail_Success_ShopSuspended_AllowedReadOnly")
    void tc12_getSellerOrderDetail_Success_ShopSuspended_AllowedReadOnly() {
        mockShop.setStatus(ShopStatus.SUSPENDED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-SUSPENDED", OrderStatus.CONFIRMED, BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.ZERO);
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertEquals(orderId, res.orderId());
    }

    @Test
    @DisplayName("TC-13: getSellerOrderDetail_Throw_WhenShopBanned")
    void tc13_getSellerOrderDetail_Throw_WhenShopBanned() {
        mockShop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrderDetail(userId, orderId));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: getSellerOrderDetail_Throw_WhenUserNotApprovedSeller")
    void tc14_getSellerOrderDetail_Throw_WhenUserNotApprovedSeller() {
        mockSeller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));

        UUID orderId = UUID.randomUUID();
        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrderDetail(userId, orderId));

        assertEquals(ErrorCode.NOT_A_SELLER, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: getSellerOrderDetail_Throw_WhenUserAccountBanned")
    void tc15_getSellerOrderDetail_Throw_WhenUserAccountBanned() {
        mockUser.setStatus(UserStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));

        UUID orderId = UUID.randomUUID();
        BusinessException exception = assertThrows(BusinessException.class, () ->
                sellerOrderService.getSellerOrderDetail(userId, orderId));

        assertEquals(ErrorCode.ACCOUNT_BANNED, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-16: getSellerOrderDetail_ZeroNPlusOneQuery_VerifyFixedSqlCount")
    void tc16_getSellerOrderDetail_ZeroNPlusOneQuery_VerifyFixedSqlCount() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-N1", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));

        OrderItem i1 = createMockOrderItem(order, "SP 1", "V1", BigDecimal.valueOf(100000), 1);
        OrderItem i2 = createMockOrderItem(order, "SP 2", "V2", BigDecimal.valueOf(200000), 1);
        OrderItem i3 = createMockOrderItem(order, "SP 3", "V3", BigDecimal.valueOf(200000), 1);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(i1, i2, i3));

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertEquals(3, res.items().size());
        verify(orderRepository, times(1)).findByIdAndShopId(orderId, shopId);
        verify(orderItemRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("TC-17: getSellerOrderDetail_FaultTolerant_CorruptedAddressSnapshot")
    void tc17_getSellerOrderDetail_FaultTolerant_CorruptedAddressSnapshot() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-CORRUPT", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        order.setDeliveryAddressSnapshot("CORRUPTED_NON_JSON_DATA");
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertNotNull(res.deliveryAddress());
        assertEquals("CORRUPTED_NON_JSON_DATA", res.deliveryAddress().fullAddress());
    }

    @Test
    @DisplayName("TC-18: getSellerOrderDetail_FinancialAccuracy_WithPlatformAndShopVouchers")
    void tc18_getSellerOrderDetail_FinancialAccuracy_WithPlatformAndShopVouchers() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        BigDecimal productAmount = BigDecimal.valueOf(1000000.00);
        BigDecimal shippingFee = BigDecimal.valueOf(30000.00);
        BigDecimal shopDiscount = BigDecimal.valueOf(60000.00);
        BigDecimal platformDiscount = BigDecimal.valueOf(40000.00);

        Order order = createMockOrder(orderId, "ORD-FIN-DETAIL", OrderStatus.CONFIRMED, productAmount, shippingFee, shopDiscount, platformDiscount);
        when(orderRepository.findByIdAndShopId(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        SellerOrderDetailResponse res = sellerOrderService.getSellerOrderDetail(userId, orderId);

        assertNotNull(res);
        assertEquals(0, productAmount.compareTo(res.totalProductAmount()));
        assertEquals(0, shippingFee.compareTo(res.shippingFee()));
        assertEquals(0, shopDiscount.compareTo(res.shopDiscountAmount()));
        assertEquals(0, platformDiscount.compareTo(res.platformDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(47000).compareTo(res.totalCommission()));
        assertEquals(0, BigDecimal.valueOf(893000).compareTo(res.totalSellerEarnings()));
        assertEquals(0, BigDecimal.valueOf(930000).compareTo(res.buyerTotalAmount()));
    }

    // =========================================================================
    // PATCH /api/seller/orders/{id}/status - Update Fulfillment Status Tests
    // =========================================================================

    @Test
    @DisplayName("TC-01: Cập nhật thành công từ CONFIRMED sang PREPARING")
    void tc01_updateSellerOrderStatus_Success_ConfirmedToPreparing() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-CONF-TO-PREP", OrderStatus.CONFIRMED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        order.setNote("Giao giờ hành chính");

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, "Đang nhặt hàng");
        SellerOrderDetailResponse response = sellerOrderService.updateSellerOrderStatus(userId, orderId, request);

        assertNotNull(response);
        assertEquals(OrderStatus.PREPARING, response.status());
        assertEquals("Giao giờ hành chính | [Shop]: Đang nhặt hàng", response.note());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("TC-02: Cập nhật thành công từ PREPARING sang WAITING_FOR_PICKUP")
    void tc02_updateSellerOrderStatus_Success_PreparingToWaitingForPickup() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PREP-TO-WAIT", OrderStatus.PREPARING,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        order.setNote("Giao giờ hành chính");

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.WAITING_FOR_PICKUP, null);
        SellerOrderDetailResponse response = sellerOrderService.updateSellerOrderStatus(userId, orderId, request);

        assertNotNull(response);
        assertEquals(OrderStatus.WAITING_FOR_PICKUP, response.status());
        assertEquals("Giao giờ hành chính", response.note());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("TC-03: Idempotent khi đơn hàng đã ở trạng thái PREPARING")
    void tc03_updateSellerOrderStatus_Idempotent_WhenAlreadyPreparing() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-IDEMP-PREP", OrderStatus.PREPARING,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, "Thử lại");
        SellerOrderDetailResponse response = sellerOrderService.updateSellerOrderStatus(userId, orderId, request);

        assertNotNull(response);
        assertEquals(OrderStatus.PREPARING, response.status());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("TC-04: Idempotent khi đơn hàng đã ở trạng thái WAITING_FOR_PICKUP")
    void tc04_updateSellerOrderStatus_Idempotent_WhenAlreadyWaitingForPickup() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-IDEMP-WAIT", OrderStatus.WAITING_FOR_PICKUP,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.WAITING_FOR_PICKUP, null);
        SellerOrderDetailResponse response = sellerOrderService.updateSellerOrderStatus(userId, orderId, request);

        assertNotNull(response);
        assertEquals(OrderStatus.WAITING_FOR_PICKUP, response.status());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("TC-05: Chặn nhảy cóc từ CONFIRMED sang WAITING_FOR_PICKUP")
    void tc05_updateSellerOrderStatus_Throw_WhenIllegalSkipConfirmedToWaitingForPickup() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-SKIP", OrderStatus.CONFIRMED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.WAITING_FOR_PICKUP, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, orderId, request));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("TC-06: Chặn cập nhật khi đơn hàng đang PENDING_PAYMENT")
    void tc06_updateSellerOrderStatus_Throw_WhenOrderPendingPayment() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-UNPAID", OrderStatus.PENDING_PAYMENT,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, orderId, request));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("TC-07: Chặn quay ngược từ WAITING_FOR_PICKUP về PREPARING")
    void tc07_updateSellerOrderStatus_Throw_WhenReverseTransitionWaitingToPreparing() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-REVERSE", OrderStatus.WAITING_FOR_PICKUP,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, orderId, request));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("TC-08: Chặn cập nhật khi đơn hàng đã SHIPPING")
    void tc08_updateSellerOrderStatus_Throw_WhenOrderAlreadyShipping() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-SHIPPING", OrderStatus.SHIPPING,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.WAITING_FOR_PICKUP, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, orderId, request));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-09: Chặn cập nhật khi đơn hàng đã DELIVERED")
    void tc09_updateSellerOrderStatus_Throw_WhenOrderAlreadyDelivered() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-DELIVERED", OrderStatus.DELIVERED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, orderId, request));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-10: Chặn cập nhật khi đơn hàng đã COMPLETED")
    void tc10_updateSellerOrderStatus_Throw_WhenOrderAlreadyCompleted() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-COMPLETED", OrderStatus.COMPLETED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, orderId, request));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: Chặn cập nhật khi đơn hàng đã CANCELLED")
    void tc11_updateSellerOrderStatus_Throw_WhenOrderAlreadyCancelled() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-CANCELLED", OrderStatus.CANCELLED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, orderId, request));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: Chặn seller gửi targetStatus không thuộc thẩm quyền (ví dụ SHIPPING)")
    void tc12_updateSellerOrderStatus_Throw_WhenTargetStatusUnauthorizedForSeller() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.SHIPPING, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, orderId, request));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(orderRepository, never()).findByIdAndShopIdForUpdate(any(), any());
    }

    @Test
    @DisplayName("TC-13: Chặn IDOR - Seller A cố tình cập nhật đơn của Shop B")
    void tc13_updateSellerOrderStatus_Throw_WhenOrderBelongsToAnotherShop_IDOR() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.empty());

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, orderId, request));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: Đơn hàng không tồn tại trong CSDL")
    void tc14_updateSellerOrderStatus_Throw_WhenOrderNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID randomOrderId = UUID.randomUUID();
        when(orderRepository.findByIdAndShopIdForUpdate(randomOrderId, shopId)).thenReturn(Optional.empty());

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, randomOrderId, request));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: Tài khoản Seller bị BANNED")
    void tc15_updateSellerOrderStatus_Throw_WhenSellerAccountBanned() {
        mockUser.setStatus(UserStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.updateSellerOrderStatus(userId, UUID.randomUUID(), request));
        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-16: Bảo toàn ghi chú của Buyer khi Seller gửi thêm ghi chú đóng gói")
    void tc16_updateSellerOrderStatus_PreserveBuyerNote_WhenSellerAppendsNote() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PRESERVE-NOTE", OrderStatus.CONFIRMED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        order.setNote("Giao giờ HC");

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, "Gói 2 lớp");
        SellerOrderDetailResponse response = sellerOrderService.updateSellerOrderStatus(userId, orderId, request);

        assertNotNull(response);
        assertEquals("Giao giờ HC | [Shop]: Gói 2 lớp", order.getNote());
        assertEquals("Giao giờ HC | [Shop]: Gói 2 lớp", response.note());
    }

    @Test
    @DisplayName("TC-17: Cắt ngắn an toàn tối đa 500 ký tự khi tổng ghi chú Buyer + Seller vượt quá 500 ký tự")
    void tc17_updateSellerOrderStatus_TruncateNote_WhenCombinedLengthExceeds500() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-OVERFLOW-NOTE", OrderStatus.CONFIRMED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        String longBuyerNote = "B".repeat(350);
        order.setNote(longBuyerNote);

        when(orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        String longSellerNote = "S".repeat(200);
        UpdateSellerOrderStatusRequest request = new UpdateSellerOrderStatusRequest(OrderStatus.PREPARING, longSellerNote);
        SellerOrderDetailResponse response = sellerOrderService.updateSellerOrderStatus(userId, orderId, request);

        assertNotNull(response);
        assertNotNull(order.getNote());
        assertEquals(500, order.getNote().length(), "Ghi chú không được vượt quá độ dài cột CSDL (500 ký tự)");
        assertEquals(500, response.note().length());
        assertTrue(order.getNote().startsWith(longBuyerNote + " | [Shop]:"));
    }

    // =========================================================================
    // TESTS FOR processCancellationRequest (PATCH /api/seller/orders/cancellation-requests/{id}/process)
    // =========================================================================

    @Test
    @DisplayName("TC-PCR-01: Seller duyệt hủy đơn thành công (APPROVED) với đơn COD")
    void tc_processCancellationRequest_Success_WhenApproved_COD() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID requestId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PCR-01", OrderStatus.CONFIRMED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.valueOf(530000))
                .build();
        order.setPayment(payment);

        CancellationRequest cr = CancellationRequest.builder()
                .id(requestId)
                .order(order)
                .status(CancellationRequestStatus.PENDING)
                .reason("Khách đổi ý")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .stockQuantity(10)
                .build();
        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .productVariant(variant)
                .quantity(2)
                .build();

        when(cancellationRequestRepository.findByIdAndShopIdForUpdate(requestId, shopId)).thenReturn(Optional.of(cr));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
        when(productVariantRepository.findAllByIdInForUpdate(List.of(variant.getId()))).thenReturn(List.of(variant));
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(orderRepository.findByPaymentId(payment.getId())).thenReturn(List.of(order));
        when(voucherRedemptionRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.request.ProcessCancellationRequest request =
                new com.techhub.model.dto.request.ProcessCancellationRequest(CancellationRequestStatus.APPROVED, "Shop đồng ý hủy");

        CancellationRequestResponse response = sellerOrderService.processCancellationRequest(userId, requestId, request);

        assertNotNull(response);
        assertEquals(CancellationRequestStatus.APPROVED, response.status());
        assertEquals("Shop đồng ý hủy", response.sellerResponse());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(12, variant.getStockQuantity());
        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        verify(cancellationRequestRepository).save(cr);
        verify(orderRepository).save(order);
        verify(productVariantRepository).saveAll(any());
    }

    @Test
    @DisplayName("TC-PCR-02: Seller duyệt hủy đơn thành công (APPROVED) với đơn Online Paid tạo RefundTransaction")
    void tc_processCancellationRequest_Success_WhenApproved_OnlinePaid() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID requestId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PCR-02", OrderStatus.PREPARING,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PAID)
                .amount(BigDecimal.valueOf(530000))
                .refundedAmount(BigDecimal.ZERO)
                .build();
        order.setPayment(payment);

        CancellationRequest cr = CancellationRequest.builder()
                .id(requestId)
                .order(order)
                .status(CancellationRequestStatus.PENDING)
                .reason("Muốn đổi sản phẩm khác")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(cancellationRequestRepository.findByIdAndShopIdForUpdate(requestId, shopId)).thenReturn(Optional.of(cr));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(voucherRedemptionRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        com.techhub.model.dto.request.ProcessCancellationRequest request =
                new com.techhub.model.dto.request.ProcessCancellationRequest(CancellationRequestStatus.APPROVED, null);

        CancellationRequestResponse response = sellerOrderService.processCancellationRequest(userId, requestId, request);

        assertNotNull(response);
        assertEquals(CancellationRequestStatus.APPROVED, response.status());
        assertEquals("Đồng ý hủy đơn", response.sellerResponse());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(BigDecimal.valueOf(530000), payment.getRefundedAmount());
        verify(refundTransactionRepository).save(any(RefundTransaction.class));
    }

    @Test
    @DisplayName("TC-PCR-03: Seller từ chối yêu cầu hủy đơn (REJECTED)")
    void tc_processCancellationRequest_Success_WhenRejected() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID requestId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PCR-03", OrderStatus.PREPARING,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        CancellationRequest cr = CancellationRequest.builder()
                .id(requestId)
                .order(order)
                .status(CancellationRequestStatus.PENDING)
                .reason("Hủy đơn")
                .build();

        when(cancellationRequestRepository.findByIdAndShopIdForUpdate(requestId, shopId)).thenReturn(Optional.of(cr));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        com.techhub.model.dto.request.ProcessCancellationRequest request =
                new com.techhub.model.dto.request.ProcessCancellationRequest(CancellationRequestStatus.REJECTED, "Đã đóng gói xong");

        CancellationRequestResponse response = sellerOrderService.processCancellationRequest(userId, requestId, request);

        assertNotNull(response);
        assertEquals(CancellationRequestStatus.REJECTED, response.status());
        assertEquals("Đã đóng gói xong", response.sellerResponse());
        assertEquals(OrderStatus.PREPARING, order.getStatus());
        verify(orderRepository, never()).save(order);
        verify(refundTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-PCR-04: Chặn duyệt hủy khi đơn hàng không ở trạng thái CONFIRMED hoặc PREPARING")
    void tc_processCancellationRequest_Throw_WhenOrderNotInAllowedStatus() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID requestId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PCR-04", OrderStatus.SHIPPING,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        CancellationRequest cr = CancellationRequest.builder()
                .id(requestId)
                .order(order)
                .status(CancellationRequestStatus.PENDING)
                .build();

        when(cancellationRequestRepository.findByIdAndShopIdForUpdate(requestId, shopId)).thenReturn(Optional.of(cr));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        com.techhub.model.dto.request.ProcessCancellationRequest request =
                new com.techhub.model.dto.request.ProcessCancellationRequest(CancellationRequestStatus.APPROVED, "Shop duyệt");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.processCancellationRequest(userId, requestId, request));
        assertEquals(ErrorCode.CANCELLATION_NOT_ALLOWED, ex.getErrorCode());
        verify(cancellationRequestRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-PCR-05: Chặn xử lý khi yêu cầu hủy đã được xử lý trước đó (không còn PENDING)")
    void tc_processCancellationRequest_Throw_WhenAlreadyProcessed() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID requestId = UUID.randomUUID();
        CancellationRequest cr = CancellationRequest.builder()
                .id(requestId)
                .status(CancellationRequestStatus.APPROVED)
                .build();

        when(cancellationRequestRepository.findByIdAndShopIdForUpdate(requestId, shopId)).thenReturn(Optional.of(cr));

        com.techhub.model.dto.request.ProcessCancellationRequest request =
                new com.techhub.model.dto.request.ProcessCancellationRequest(CancellationRequestStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.processCancellationRequest(userId, requestId, request));
        assertEquals(ErrorCode.CANCELLATION_REQUEST_ALREADY_PROCESSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-PCR-06: Chặn khi yêu cầu hủy không tồn tại hoặc không thuộc shop của seller")
    void tc_processCancellationRequest_Throw_WhenNotFoundOrShopMismatch() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID requestId = UUID.randomUUID();
        when(cancellationRequestRepository.findByIdAndShopIdForUpdate(requestId, shopId)).thenReturn(Optional.empty());

        com.techhub.model.dto.request.ProcessCancellationRequest request =
                new com.techhub.model.dto.request.ProcessCancellationRequest(CancellationRequestStatus.APPROVED, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerOrderService.processCancellationRequest(userId, requestId, request));
        assertEquals(ErrorCode.CANCELLATION_REQUEST_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-PCR-07: Giải phóng voucher và hoàn lượt dùng khi duyệt hủy đơn")
    void tc_processCancellationRequest_ReleaseVoucher_WhenApproved() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(mockSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(mockShop));

        UUID requestId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-PCR-07", OrderStatus.CONFIRMED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        Voucher voucher = Voucher.builder()
                .id(UUID.randomUUID())
                .code("DISCOUNT50")
                .usedCount(5)
                .reservedCount(0)
                .build();

        VoucherRedemption vr = VoucherRedemption.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .voucher(voucher)
                .status(VoucherRedemptionStatus.REDEEMED)
                .build();

        CancellationRequest cr = CancellationRequest.builder()
                .id(requestId)
                .order(order)
                .status(CancellationRequestStatus.PENDING)
                .build();

        when(cancellationRequestRepository.findByIdAndShopIdForUpdate(requestId, shopId)).thenReturn(Optional.of(cr));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        when(voucherRedemptionRepository.findByOrderId(orderId)).thenReturn(List.of(vr));
        when(voucherRepository.findAllWithShopByIdInForUpdate(List.of(voucher.getId()))).thenReturn(List.of(voucher));

        com.techhub.model.dto.request.ProcessCancellationRequest request =
                new com.techhub.model.dto.request.ProcessCancellationRequest(CancellationRequestStatus.APPROVED, "OK");

        CancellationRequestResponse response = sellerOrderService.processCancellationRequest(userId, requestId, request);

        assertNotNull(response);
        assertEquals(VoucherRedemptionStatus.RELEASED, vr.getStatus());
        assertEquals(4, voucher.getUsedCount());
        verify(voucherRepository).save(voucher);
        verify(voucherRedemptionRepository).save(vr);
    }
}

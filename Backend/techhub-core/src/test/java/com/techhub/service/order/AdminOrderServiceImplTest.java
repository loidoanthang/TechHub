package com.techhub.service.order;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.*;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.OrderItemRepository;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.order.impl.AdminOrderServiceImpl;
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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminOrderService (GET /api/admin/orders)")
class AdminOrderServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private AdminOrderServiceImpl adminOrderService;

    private UUID adminUserId;
    private User mockAdminUser;
    private Shop mockShop;
    private User mockBuyer;

    @BeforeEach
    void setUp() {
        adminUserId = UUID.randomUUID();

        mockAdminUser = new User();
        mockAdminUser.setId(adminUserId);
        mockAdminUser.setEmail("admin@techhub.vn");
        mockAdminUser.setFirstName("Admin");
        mockAdminUser.setLastName("TechHub");
        mockAdminUser.setStatus(UserStatus.ACTIVE);
        mockAdminUser.setEmailVerified(true);
        mockAdminUser.setRoles(Set.of(Role.ADMIN));

        User sellerUser = new User();
        sellerUser.setId(UUID.randomUUID());
        sellerUser.setEmail("seller.apple@techhub.vn");
        sellerUser.setPhone("0912345678");

        Seller seller = new Seller();
        seller.setId(UUID.randomUUID());
        seller.setUser(sellerUser);
        seller.setSellerName("apple-store");

        mockShop = new Shop();
        mockShop.setId(UUID.randomUUID());
        mockShop.setName("Apple Official Store");
        mockShop.setSeller(seller);
        mockShop.setStatus(ShopStatus.ACTIVE);

        mockBuyer = new User();
        mockBuyer.setId(UUID.randomUUID());
        mockBuyer.setFirstName("Nguyen");
        mockBuyer.setLastName("Van A");
        mockBuyer.setEmail("nguyenvana@gmail.com");
        mockBuyer.setPhone("0987654321");
    }

    private Order createMockOrder(UUID orderId, String orderNumber, OrderStatus status, BigDecimal productAmount, BigDecimal shippingFee, BigDecimal shopDiscount, BigDecimal platformDiscount) {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .amount(productAmount.add(shippingFee).subtract(shopDiscount).subtract(platformDiscount))
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .build();

        String addressSnapshot = """
                {
                  "id": "%s",
                  "recipientName": "Nguyen Van A",
                  "phoneNumber": "0987654321",
                  "streetAddress": "Số 123 Đường Cầu Giấy",
                  "ward": "Phường Dịch Vọng",
                  "district": "Quận Cầu Giấy",
                  "provinceCity": "Thành phố Hà Nội"
                }
                """.formatted(UUID.randomUUID());

        BigDecimal netProduct = productAmount.subtract(shopDiscount).max(BigDecimal.ZERO);
        BigDecimal commission = netProduct.multiply(BigDecimal.valueOf(0.05)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal earnings = netProduct.subtract(commission).max(BigDecimal.ZERO);

        return Order.builder()
                .id(orderId)
                .orderNumber(orderNumber)
                .user(mockBuyer)
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
                .totalAmount(productAmount.add(shippingFee).subtract(shopDiscount).subtract(platformDiscount))
                .note("Giao vào giờ hành chính giúp em ạ")
                .createdAt(LocalDateTime.of(2026, 10, 15, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 10, 15, 9, 30))
                .build();
    }

    private OrderItem createMockOrderItem(UUID itemId, Order order, String productName, BigDecimal price, int quantity) {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());

        return OrderItem.builder()
                .id(itemId)
                .order(order)
                .productVariant(variant)
                .productNameSnapshot(productName)
                .variantNameSnapshot("Màu Trắng")
                .skuSnapshot("APL-20W-WHT")
                .imageUrlSnapshot("https://img.techhub.vn/apple-20w.png")
                .originalPrice(price)
                .itemDiscountAmount(BigDecimal.ZERO)
                .discountedUnitPrice(price)
                .quantity(quantity)
                .finalSubtotal(price.multiply(BigDecimal.valueOf(quantity)))
                .build();
    }

    @Test
    @DisplayName("TC-01: getAdminOrders thành công với bộ lọc mặc định, phân trang đầy đủ")
    void tc01_getAdminOrders_Success_DefaultFilter_ReturnsPagedOrders() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-20261015-01", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.valueOf(30000), BigDecimal.ZERO);
        OrderItem item = createMockOrderItem(UUID.randomUUID(), order, "Củ sạc nhanh Apple 20W", BigDecimal.valueOf(500000), 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(List.of(orderId)))
                .thenReturn(List.of(item));

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        AdminOrderResponse dto = response.content().get(0);
        assertEquals(orderId, dto.orderId());
        assertEquals("ORD-20261015-01", dto.orderNumber());
        assertEquals("Apple Official Store", dto.shop().shopName());
        assertEquals("seller.apple@techhub.vn", dto.shop().sellerEmail());
        assertEquals("Nguyen Van A", dto.buyer().buyerName());
        assertEquals(1, dto.totalItemsCount());
        assertEquals(1, dto.items().size());
        assertEquals(new BigDecimal("23500.00"), dto.totalCommission());
        assertEquals(new BigDecimal("446500.00"), dto.totalSellerEarnings());
        assertTrue(dto.availableActions().contains("VIEW_DETAILS"));
        assertTrue(dto.availableActions().contains("FORCE_CANCEL"));
    }

    @Test
    @DisplayName("TC-02: getAdminOrders lọc theo shopId thành công")
    void tc02_getAdminOrders_Success_FilterByShopId() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID shopId = mockShop.getId();
        Order order = createMockOrder(UUID.randomUUID(), "ORD-20261015-02", OrderStatus.PREPARING, BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, shopId, null, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(shopId, response.content().get(0).shop().shopId());
    }

    @Test
    @DisplayName("TC-03: getAdminOrders lọc theo userId (buyerId) thành công")
    void tc03_getAdminOrders_Success_FilterByUserId() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID buyerId = mockBuyer.getId();
        Order order = createMockOrder(UUID.randomUUID(), "ORD-20261015-03", OrderStatus.WAITING_FOR_PICKUP, BigDecimal.valueOf(300000), BigDecimal.valueOf(15000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, buyerId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(buyerId, response.content().get(0).buyer().buyerId());
    }

    @Test
    @DisplayName("TC-04: getAdminOrders lọc theo status CONFIRMED thành công")
    void tc04_getAdminOrders_Success_FilterByStatus() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-20261015-04", OrderStatus.CONFIRMED, BigDecimal.valueOf(400000), BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, OrderStatus.CONFIRMED, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(OrderStatus.CONFIRMED, response.content().get(0).status());
    }

    @Test
    @DisplayName("TC-05: getAdminOrders lọc theo dải ngày hợp lệ thành công")
    void tc05_getAdminOrders_Success_FilterByDateRange_Valid() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 15);

        Order order = createMockOrder(UUID.randomUUID(), "ORD-20261015-05", OrderStatus.SHIPPING, BigDecimal.valueOf(1000000), BigDecimal.valueOf(50000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, null, startDate, endDate, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-06: getAdminOrders tìm kiếm theo keyword mã đơn hàng")
    void tc06_getAdminOrders_Success_SearchByOrderNumberKeyword() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-20261015-8F3A", OrderStatus.DELIVERED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, "8F3A", null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("ORD-20261015-8F3A", response.content().get(0).orderNumber());
    }

    @Test
    @DisplayName("TC-07: getAdminOrders tìm kiếm theo keyword tên shop")
    void tc07_getAdminOrders_Success_SearchByShopNameKeyword() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-20261015-07", OrderStatus.COMPLETED, BigDecimal.valueOf(800000), BigDecimal.valueOf(40000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, "Apple", null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Apple Official Store", response.content().get(0).shop().shopName());
    }

    @Test
    @DisplayName("TC-08: getAdminOrders tìm kiếm theo keyword thông tin buyer (email/phone)")
    void tc08_getAdminOrders_Success_SearchByBuyerInfoKeyword() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        Order order = createMockOrder(UUID.randomUUID(), "ORD-20261015-08", OrderStatus.CONFIRMED, BigDecimal.valueOf(600000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, "nguyenvana", null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("nguyenvana@gmail.com", response.content().get(0).buyer().buyerEmail());
    }

    @Test
    @DisplayName("TC-09: getAdminOrders tìm kiếm theo keyword tên sản phẩm snapshot")
    void tc09_getAdminOrders_Success_SearchByProductNameKeyword() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-20261015-09", OrderStatus.CONFIRMED, BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.ZERO);
        OrderItem item = createMockOrderItem(UUID.randomUUID(), order, "Củ sạc nhanh Apple 20W", BigDecimal.valueOf(500000), 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(List.of(orderId)))
                .thenReturn(List.of(item));

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, "sạc nhanh", null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Củ sạc nhanh Apple 20W", response.content().get(0).items().get(0).productName());
    }

    @Test
    @DisplayName("TC-10: getAdminOrders trả về PageResponse rỗng khi không có bản ghi khớp")
    void tc10_getAdminOrders_Success_EmptyPage_WhenNoOrdersMatch() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, "nonexistent", null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        verify(orderItemRepository, never()).findByOrderIdIn(anyList());
    }

    @Test
    @DisplayName("TC-11: Ném INVALID_DATE_RANGE khi startDate > endDate")
    void tc11_getAdminOrders_Throw_WhenStartDateAfterEndDate() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        LocalDate start = LocalDate.of(2026, 10, 20);
        LocalDate end = LocalDate.of(2026, 10, 10);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrders(adminUserId, null, null, null, null, start, end, 0, 10, "createdAt", "desc"));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: Ném INVALID_DATE_RANGE khi dải ngày vượt quá 365 ngày")
    void tc12_getAdminOrders_Throw_WhenDateRangeExceeds365Days() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 5);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrders(adminUserId, null, null, null, null, start, end, 0, 10, "createdAt", "desc"));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-13: Ném USER_NOT_FOUND khi tài khoản không tồn tại")
    void tc13_getAdminOrders_Throw_WhenUserNotFound() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrders(adminUserId, null, null, null, null, null, null, 0, 10, "createdAt", "desc"));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: Ném ACCOUNT_BANNED khi tài khoản Admin bị BANNED")
    void tc14_getAdminOrders_Throw_WhenAdminAccountBanned() {
        mockAdminUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrders(adminUserId, null, null, null, null, null, null, 0, 10, "createdAt", "desc"));
        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: Tự động clamp size phân trang khi vượt quá giới hạn [1, 50]")
    void tc15_getAdminOrders_PaginationClamping_SizeOutOfBounds() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0));

        adminOrderService.getAdminOrders(adminUserId, null, null, null, null, null, null, -5, 500, "invalidSort", "invalidDir");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(any(Specification.class), captor.capture());
        Pageable captured = captor.getValue();

        assertEquals(0, captured.getPageNumber());
        assertEquals(50, captured.getPageSize());
        assertEquals("createdAt: DESC", captured.getSort().toString());
    }

    @Test
    @DisplayName("TC-16: Sanitize keyword chứa ký tự đặc biệt %_special\\_ an toàn")
    void tc16_getAdminOrders_SanitizeKeyword_SpecialWildcards() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

        assertDoesNotThrow(() ->
                adminOrderService.getAdminOrders(adminUserId, null, null, null, "%_special\\_", null, null, 0, 10, "createdAt", "desc"));
    }

    @Test
    @DisplayName("TC-17: Xác minh Staff Engineer Zero N+1 query: Cố định 2 câu SQL cho toàn bộ trang")
    void tc17_getAdminOrders_ZeroNPlusOne_VerifyTwoSqlQueries() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        List<Order> orders = new ArrayList<>();
        List<UUID> orderIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UUID oid = UUID.randomUUID();
            orderIds.add(oid);
            orders.add(createMockOrder(oid, "ORD-" + i, OrderStatus.CONFIRMED, BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(orders, PageRequest.of(0, 10), 5));
        when(orderItemRepository.findByOrderIdIn(orderIds))
                .thenReturn(Collections.emptyList());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(5, response.content().size());
        verify(orderRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(orderItemRepository, times(1)).findByOrderIdIn(orderIds);
    }

    @Test
    @DisplayName("TC-18: Xác minh độ chính xác tài chính hoa hồng sàn 5% và tiền thực trả shop (BR-16D)")
    void tc18_getAdminOrders_FinancialCommissionAccuracy_BR16D() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        // Tiền hàng 1.000.000đ, voucher shop 100.000đ, voucher sàn 50.000đ, cước ship 30.000đ
        // netProduct = 900.000đ -> totalCommission = 45.000đ (5%) -> totalSellerEarnings = 855.000đ
        Order order = createMockOrder(orderId, "ORD-FINANCIAL", OrderStatus.COMPLETED,
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(30000), BigDecimal.valueOf(100000), BigDecimal.valueOf(50000));
        order.setTotalCommission(null); // Test fallback dynamic calculation
        order.setTotalSellerEarnings(null);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdIn(List.of(orderId)))
                .thenReturn(Collections.emptyList());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        AdminOrderResponse dto = response.content().get(0);
        assertEquals(new BigDecimal("45000.00"), dto.totalCommission());
        assertEquals(new BigDecimal("855000.00"), dto.totalSellerEarnings());
        assertEquals(0, new BigDecimal("880000").compareTo(dto.buyerTotalAmount())); // 1000k + 30k - 100k - 50k = 880k
    }

    @Test
    @DisplayName("TC-19: getAdminOrders với currentUserId = null ném UNAUTHENTICATED")
    void tc19_getAdminOrders_NullAdminUserId_ThrowsUnauthenticated() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrders(null, null, null, null, null, null, null, 0, 10, "createdAt", "desc")
        );
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-20: getAdminOrders sắp xếp theo totalSellerEarnings nằm trong whitelist")
    void tc20_getAdminOrders_SortByTotalSellerEarnings_Success() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        PageResponse<AdminOrderResponse> response = adminOrderService.getAdminOrders(
                adminUserId, null, null, null, null, null, null, 0, 10, "totalSellerEarnings", "asc"
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
    }

    // ==========================================
    // GET /api/admin/orders/{id} - Detail Tests
    // ==========================================

    @Test
    @DisplayName("TC-21: getAdminOrderDetail thành công, trả về đầy đủ 25 trường thông tin chi tiết")
    void tc21_getAdminOrderDetail_Success_ReturnsFullDetails() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-20261015-001", OrderStatus.CONFIRMED,
                BigDecimal.valueOf(500000), BigDecimal.valueOf(30000), BigDecimal.valueOf(50000), BigDecimal.valueOf(20000));
        OrderItem item = createMockOrderItem(UUID.randomUUID(), order, "Củ sạc Apple 20W USB-C", BigDecimal.valueOf(250000), 2);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals("ORD-20261015-001", response.orderNumber());
        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertNotNull(response.statusDescription());
        assertNotNull(response.shop());
        assertEquals("Apple Official Store", response.shop().shopName());
        assertNotNull(response.buyer());
        assertEquals("Nguyen Van A", response.buyer().buyerName());
        assertNotNull(response.deliveryAddress());
        assertEquals("Thành phố Hà Nội", response.deliveryAddress().provinceCity());
        assertEquals(1, response.items().size());
        assertEquals(2, response.totalItemsCount());
        assertEquals("Giao vào giờ hành chính giúp em ạ", response.note());
        assertEquals(BigDecimal.valueOf(500000), response.totalProductAmount());
        assertEquals(BigDecimal.valueOf(30000), response.shippingFee());
        assertEquals(BigDecimal.valueOf(50000), response.shopDiscountAmount());
        assertEquals(BigDecimal.valueOf(20000), response.platformDiscountAmount());
        assertNotNull(response.paymentInfo());
        assertEquals(PaymentMethod.COD, response.paymentInfo().paymentMethod());
        assertNotNull(response.timeline());
        assertTrue(response.availableActions().contains("FORCE_CANCEL"));

        verify(orderRepository).findWithDetailsById(orderId);
        verify(orderItemRepository).findByOrderId(orderId);
    }

    @Test
    @DisplayName("TC-22: Ném ORDER_NOT_FOUND khi mã đơn hàng không tồn tại trên hệ thống")
    void tc22_getAdminOrderDetail_OrderNotFound_ThrowsException() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));
        UUID nonExistentId = UUID.randomUUID();
        when(orderRepository.findWithDetailsById(nonExistentId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrderDetail(adminUserId, nonExistentId));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-23: Ném ACCOUNT_BANNED khi tài khoản Admin bị BANNED")
    void tc23_getAdminOrderDetail_AdminBanned_ThrowsAccountBanned() {
        mockAdminUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrderDetail(adminUserId, UUID.randomUUID()));
        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-24: Ném ACCOUNT_NOT_VERIFIED khi tài khoản Admin chưa xác thực email")
    void tc24_getAdminOrderDetail_AdminEmailNotVerified_ThrowsException() {
        mockAdminUser.setEmailVerified(false);
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrderDetail(adminUserId, UUID.randomUUID()));
        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-25: Ném USER_NOT_FOUND khi tài khoản Admin không tồn tại trong DB")
    void tc25_getAdminOrderDetail_AdminUserNotFound_ThrowsException() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrderDetail(adminUserId, UUID.randomUUID()));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-26: Ném UNAUTHENTICATED khi currentUserId = null")
    void tc26_getAdminOrderDetail_NullAdminUserId_ThrowsUnauthenticated() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrderDetail(null, UUID.randomUUID()));
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-27: Ném INVALID_INPUT_DATA khi orderId = null")
    void tc27_getAdminOrderDetail_NullOrderId_ThrowsInvalidInput() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminOrderService.getAdminOrderDetail(adminUserId, null));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-28: Tính toán dự phòng tài chính hoa hồng 5% và thực nhận Shop khi DB chưa lưu (BR-16D)")
    void tc28_getAdminOrderDetail_FinancialCommissionAccuracy_BR16D() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-COMMISSION", OrderStatus.COMPLETED,
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(30000), BigDecimal.valueOf(100000), BigDecimal.valueOf(50000));
        order.setTotalCommission(null);
        order.setTotalSellerEarnings(null);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertNotNull(response);
        assertEquals(new BigDecimal("45000.00"), response.totalCommission());
        assertEquals(new BigDecimal("855000.00"), response.totalSellerEarnings());
    }

    @Test
    @DisplayName("TC-29: Fallback an toàn khi deliveryAddressSnapshot bị hỏng JSON")
    void tc29_getAdminOrderDetail_DeliveryAddressCorruptedJson_FallbackSafe() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-CORRUPTED", OrderStatus.PREPARING,
                BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        order.setDeliveryAddressSnapshot("Malformed JSON {not_valid_json}");

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertNotNull(response);
        assertNotNull(response.deliveryAddress());
        assertEquals("Malformed JSON {not_valid_json}", response.deliveryAddress().fullAddress());
    }

    @Test
    @DisplayName("TC-30: Đơn hàng rỗng item vẫn trả về DTO an toàn không lỗi")
    void tc30_getAdminOrderDetail_EmptyItems_ReturnsEmptyList() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-EMPTY", OrderStatus.PENDING_PAYMENT,
                BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertNotNull(response);
        assertTrue(response.items().isEmpty());
        assertEquals(0, response.totalItemsCount());
    }

    @Test
    @DisplayName("TC-31: Đơn hàng CONFIRMED có quyền FORCE_CANCEL")
    void tc31_getAdminOrderDetail_ForceCancelAction_AvailableForConfirmed() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-CONFIRMED", OrderStatus.CONFIRMED,
                BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertTrue(response.availableActions().contains("FORCE_CANCEL"));
        assertTrue(response.availableActions().contains("VIEW_DETAILS"));
    }

    @Test
    @DisplayName("TC-32: Đơn hàng SHIPPING không có quyền FORCE_CANCEL")
    void tc32_getAdminOrderDetail_ForceCancelAction_NotAvailableForShipping() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-SHIPPING", OrderStatus.SHIPPING,
                BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertFalse(response.availableActions().contains("FORCE_CANCEL"));
        assertEquals(List.of("VIEW_DETAILS"), response.availableActions());
    }

    @Test
    @DisplayName("TC-33: Đơn hàng COMPLETED không có quyền FORCE_CANCEL")
    void tc33_getAdminOrderDetail_ForceCancelAction_NotAvailableForCompleted() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-COMPLETED", OrderStatus.COMPLETED,
                BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertFalse(response.availableActions().contains("FORCE_CANCEL"));
        assertEquals(List.of("VIEW_DETAILS"), response.availableActions());
    }

    @Test
    @DisplayName("TC-34: Đơn hàng thanh toán VNPAY đã trả thành công trả về TransactionId và PaidAt")
    void tc34_getAdminOrderDetail_VNPayPaymentInfo_ReturnsTransactionId() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-VNPAY", OrderStatus.CONFIRMED,
                BigDecimal.valueOf(200000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .amount(BigDecimal.valueOf(200000))
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PAID)
                .transactionId("VNP_TRANS_999888")
                .updatedAt(LocalDateTime.of(2026, 10, 15, 10, 0))
                .build();
        order.setPayment(payment);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertNotNull(response.paymentInfo());
        assertEquals(PaymentMethod.VNPAY, response.paymentInfo().paymentMethod());
        assertEquals(PaymentStatus.PAID, response.paymentInfo().paymentStatus());
        assertEquals("VNP_TRANS_999888", response.paymentInfo().transactionId());
        assertNotNull(response.paymentInfo().paidAt());
        assertEquals("Đã thanh toán online thành công", response.paymentInfo().paymentStatusDescription());
    }

    @Test
    @DisplayName("TC-35: Đơn hàng COD chưa thanh toán trả về trạng thái Pending chuẩn")
    void tc35_getAdminOrderDetail_CodPaymentInfo_ReturnsPending() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-COD", OrderStatus.PENDING_PAYMENT,
                BigDecimal.valueOf(150000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertNotNull(response.paymentInfo());
        assertEquals(PaymentMethod.COD, response.paymentInfo().paymentMethod());
        assertEquals(PaymentStatus.PENDING, response.paymentInfo().paymentStatus());
        assertEquals("Chưa thanh toán (Thu tiền mặt khi giao)", response.paymentInfo().paymentStatusDescription());
        assertNull(response.paymentInfo().paidAt());
    }

    @Test
    @DisplayName("TC-36: Timeline cho đơn hàng COMPLETED hiển thị 6 bước hợp lệ")
    void tc36_getAdminOrderDetail_TimelineForCompletedOrder_AllStepsValid() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-TIMELINE-COMPLETED", OrderStatus.COMPLETED,
                BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertNotNull(response.timeline());
        assertEquals(6, response.timeline().size());
        assertTrue(response.timeline().get(0).completed());
        assertTrue(response.timeline().get(5).completed());
        assertTrue(response.timeline().get(5).current());
        assertEquals("COMPLETED", response.timeline().get(5).step());
    }

    @Test
    @DisplayName("TC-37: Timeline cho đơn hàng CANCELLED hiển thị 2 bước Đặt hàng và Đã hủy")
    void tc37_getAdminOrderDetail_TimelineForCancelledOrder() {
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(mockAdminUser));

        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, "ORD-TIMELINE-CANCELLED", OrderStatus.CANCELLED,
                BigDecimal.valueOf(100000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        AdminOrderDetailResponse response = adminOrderService.getAdminOrderDetail(adminUserId, orderId);

        assertNotNull(response.timeline());
        assertEquals(2, response.timeline().size());
        assertEquals("ORDER_PLACED", response.timeline().get(0).step());
        assertEquals("CANCELLED", response.timeline().get(1).step());
        assertTrue(response.timeline().get(1).current());
    }
}

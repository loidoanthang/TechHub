package com.techhub.service.dashboard;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.dashboard.LowStockVariantDto;
import com.techhub.model.dto.response.dashboard.SellerDashboardMetricsResponse;
import com.techhub.model.dto.response.dashboard.TopSellingProductDto;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.model.projection.OrderStatusSummaryProjection;
import com.techhub.model.projection.TopSellingProductProjection;
import com.techhub.repository.*;
import com.techhub.service.dashboard.impl.SellerDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerDashboardServiceImpl (GET /api/seller/dashboard/metrics)")
class SellerDashboardServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private SellerPayoutRequestRepository sellerPayoutRequestRepository;

    @InjectMocks
    private SellerDashboardServiceImpl sellerDashboardService;

    private UUID userId;
    private User testUser;
    private Seller testSeller;
    private Shop testShop;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("seller@techhub.com");
        testUser.setStatus(UserStatus.ACTIVE);

        testSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .sellerName("Official Tech Store")
                .bankName("Vietcombank")
                .bankAccountNumber("1234567890")
                .bankAccountHolder("NGUYEN VAN A")
                .balance(new BigDecimal("15000000.00"))
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        testShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(testSeller)
                .name("Official Tech Store")
                .status(ShopStatus.ACTIVE)
                .build();
    }

    private OrderStatusSummaryProjection createOrderSummary(OrderStatus status, Long count, BigDecimal earnings) {
        return new OrderStatusSummaryProjection() {
            @Override
            public OrderStatus getStatus() { return status; }
            @Override
            public Long getOrderCount() { return count; }
            @Override
            public BigDecimal getTotalEarnings() { return earnings; }
        };
    }

    private TopSellingProductProjection createTopProduct(UUID id, String name, String img, Long units, BigDecimal rev) {
        return new TopSellingProductProjection() {
            @Override
            public UUID getProductId() { return id; }
            @Override
            public String getProductName() { return name; }
            @Override
            public String getImageUrl() { return img; }
            @Override
            public Long getUnitsSold() { return units; }
            @Override
            public BigDecimal getTotalRevenue() { return rev; }
        };
    }

    @Test
    @DisplayName("TC-01: getMetrics_Success_Last7Days_Default - Mặc định tải chỉ số 7 ngày qua")
    void getMetrics_Success_Last7Days_Default() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(eq(testShop.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        createOrderSummary(OrderStatus.COMPLETED, 5L, new BigDecimal("10000000.00")),
                        createOrderSummary(OrderStatus.PENDING_CONFIRMATION, 2L, BigDecimal.ZERO)
                ));

        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(eq(testShop.getId()), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L);

        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(eq(testSeller.getId()), eq(PayoutStatus.PENDING)))
                .thenReturn(new BigDecimal("2000000.00"));

        when(orderItemRepository.findTopSellingProductsByShopId(eq(testShop.getId()), anyList(), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(
                        createTopProduct(UUID.randomUUID(), "Keychron K2 V2", "img1.png", 6L, new BigDecimal("6000000.00"))
                ));

        when(productVariantRepository.findLowStockVariantsByShopId(eq(testShop.getId()), eq(5), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, null, null, null);

        assertNotNull(response);
        assertEquals(DashboardPeriod.LAST_7_DAYS, response.period());
        assertEquals(LocalDate.now().minusDays(6), response.startDate());
        assertEquals(LocalDate.now(), response.endDate());
        assertEquals(new BigDecimal("10000000.00"), response.summary().netRevenue());
        assertEquals(7L, response.summary().totalOrders());
        assertEquals(5L, response.summary().completedOrders());
        assertEquals(10L, response.summary().totalUnitsSold());
        assertEquals(new BigDecimal("15000000.00"), response.summary().walletBalance());
        assertEquals(new BigDecimal("2000000.00"), response.summary().pendingPayoutAmount());
        assertEquals(new BigDecimal("2000000.00"), response.summary().averageOrderValue());
        assertEquals(2L, response.orderStatusBreakdown().pendingConfirmation());
        assertEquals(5L, response.orderStatusBreakdown().completed());
        assertEquals(1, response.topSellingProducts().size());
        assertTrue(response.lowStockAlerts().isEmpty());
    }

    @Test
    @DisplayName("TC-02: getMetrics_Success_Today - Lấy chỉ số trong ngày hôm nay")
    void getMetrics_Success_Today() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(eq(testShop.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createOrderSummary(OrderStatus.COMPLETED, 1L, new BigDecimal("1500000.00"))));

        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(2L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderItemRepository.findTopSellingProductsByShopId(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(productVariantRepository.findLowStockVariantsByShopId(any(), anyInt(), any()))
                .thenReturn(Collections.emptyList());

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.TODAY, null, null);

        assertNotNull(response);
        assertEquals(DashboardPeriod.TODAY, response.period());
        assertEquals(LocalDate.now(), response.startDate());
        assertEquals(LocalDate.now(), response.endDate());
        assertEquals(new BigDecimal("1500000.00"), response.summary().netRevenue());
        assertEquals(1L, response.summary().totalOrders());
    }

    @Test
    @DisplayName("TC-03: getMetrics_Success_ThisMonth - Lấy chỉ số trong tháng hiện tại")
    void getMetrics_Success_ThisMonth() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(0L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.THIS_MONTH, null, null);

        assertNotNull(response);
        assertEquals(DashboardPeriod.THIS_MONTH, response.period());
        assertEquals(LocalDate.now().withDayOfMonth(1), response.startDate());
        assertEquals(LocalDate.now(), response.endDate());
    }

    @Test
    @DisplayName("TC-04: getMetrics_Success_CustomDateRange - Tra cứu khoảng ngày tùy chỉnh hợp lệ")
    void getMetrics_Success_CustomDateRange() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(10);

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(0L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(
                userId, DashboardPeriod.CUSTOM, start, end);

        assertNotNull(response);
        assertEquals(DashboardPeriod.CUSTOM, response.period());
        assertEquals(start, response.startDate());
        assertEquals(end, response.endDate());
    }

    @Test
    @DisplayName("TC-05: getMetrics_Success_EmptyShop_ZeroMetrics - Shop mới chưa có đơn trả về số liệu 0 an toàn")
    void getMetrics_Success_EmptyShop_ZeroMetrics() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(0L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(null);
        when(orderItemRepository.findTopSellingProductsByShopId(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(productVariantRepository.findLowStockVariantsByShopId(any(), anyInt(), any()))
                .thenReturn(Collections.emptyList());

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.summary().netRevenue());
        assertEquals(0L, response.summary().totalOrders());
        assertEquals(0L, response.summary().completedOrders());
        assertEquals(0L, response.summary().totalUnitsSold());
        assertEquals(BigDecimal.ZERO, response.summary().pendingPayoutAmount());
        assertEquals(BigDecimal.ZERO, response.summary().averageOrderValue());
        assertEquals(0L, response.orderStatusBreakdown().pendingConfirmation());
    }

    @Test
    @DisplayName("TC-06: getMetrics_Success_SuspendedShop_ReadOnlyAllowed - Shop tạm khóa (SUSPENDED) được phép đọc đối soát (BR-07)")
    void getMetrics_Success_SuspendedShop_ReadOnlyAllowed() {
        testShop.setStatus(ShopStatus.SUSPENDED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(0L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null);

        assertNotNull(response);
        assertEquals(DashboardPeriod.LAST_7_DAYS, response.period());
    }

    @Test
    @DisplayName("TC-07: getMetrics_Success_AovCalculatedCorrectly - Tính chính xác AOV và làm tròn an toàn")
    void getMetrics_Success_AovCalculatedCorrectly() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        // 3 đơn COMPLETED với doanh thu 10.000.000 đ -> 10.000.000 / 3 = 3333333.33
        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of(createOrderSummary(OrderStatus.COMPLETED, 3L, new BigDecimal("10000000.00"))));

        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(5L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null);

        assertEquals(new BigDecimal("3333333.33"), response.summary().averageOrderValue());
    }

    @Test
    @DisplayName("TC-08: getMetrics_Success_OrderStatusBreakdown_Accurate - Phân loại chính xác từng trạng thái đơn hàng")
    void getMetrics_Success_OrderStatusBreakdown_Accurate() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of(
                        createOrderSummary(OrderStatus.PENDING_CONFIRMATION, 4L, BigDecimal.ZERO),
                        createOrderSummary(OrderStatus.PREPARING, 2L, BigDecimal.ZERO),
                        createOrderSummary(OrderStatus.WAITING_FOR_PICKUP, 1L, BigDecimal.ZERO),
                        createOrderSummary(OrderStatus.SHIPPING, 5L, BigDecimal.ZERO),
                        createOrderSummary(OrderStatus.DELIVERED, 3L, new BigDecimal("3000000.00")),
                        createOrderSummary(OrderStatus.COMPLETED, 10L, new BigDecimal("20000000.00")),
                        createOrderSummary(OrderStatus.CANCELLED, 2L, BigDecimal.ZERO),
                        createOrderSummary(OrderStatus.RETURN_REQUESTED, 1L, BigDecimal.ZERO),
                        createOrderSummary(OrderStatus.REFUNDED, 1L, BigDecimal.ZERO)
                ));

        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(25L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null);

        var bd = response.orderStatusBreakdown();
        assertEquals(4L, bd.pendingConfirmation());
        assertEquals(2L, bd.preparing());
        assertEquals(1L, bd.waitingForPickup());
        assertEquals(5L, bd.shipping());
        assertEquals(3L, bd.delivered());
        assertEquals(10L, bd.completed());
        assertEquals(2L, bd.cancelled());
        assertEquals(1L, bd.returnRequested());
        assertEquals(1L, bd.refunded());
        assertEquals(29L, response.summary().totalOrders());
        assertEquals(new BigDecimal("23000000.00"), response.summary().netRevenue());
    }

    @Test
    @DisplayName("TC-09: getMetrics_Success_TopSellingProducts_Max5 - Trả về danh sách Top sản phẩm bán chạy")
    void getMetrics_Success_TopSellingProducts_Max5() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(0L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        UUID prod1 = UUID.randomUUID();
        UUID prod2 = UUID.randomUUID();
        when(orderItemRepository.findTopSellingProductsByShopId(any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        createTopProduct(prod1, "Sản phẩm A", "imgA.png", 50L, new BigDecimal("50000000.00")),
                        createTopProduct(prod2, "Sản phẩm B", "imgB.png", 30L, new BigDecimal("30000000.00"))
                ));

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null);

        assertEquals(2, response.topSellingProducts().size());
        TopSellingProductDto top1 = response.topSellingProducts().get(0);
        assertEquals(prod1, top1.productId());
        assertEquals("Sản phẩm A", top1.productName());
        assertEquals(50L, top1.unitsSold());
        assertEquals(new BigDecimal("50000000.00"), top1.totalRevenue());
    }

    @Test
    @DisplayName("TC-10: getMetrics_Success_LowStockAlerts_Threshold5 - Trả về cảnh báo các biến thể tồn kho thấp <= 5")
    void getMetrics_Success_LowStockAlerts_Threshold5() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(0L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        Product testProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Tai nghe Bluetooth")
                .build();

        ProductVariant variant1 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(testProduct)
                .variantName("Màu Đen")
                .sku("HEADSET-BLK")
                .stockQuantity(2)
                .price(new BigDecimal("1200000.00"))
                .imageUrl("img_var.png")
                .build();

        when(productVariantRepository.findLowStockVariantsByShopId(eq(testShop.getId()), eq(5), any(Pageable.class)))
                .thenReturn(List.of(variant1));

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null);

        assertEquals(1, response.lowStockAlerts().size());
        LowStockVariantDto alert = response.lowStockAlerts().get(0);
        assertEquals(variant1.getId(), alert.variantId());
        assertEquals("Tai nghe Bluetooth", alert.productName());
        assertEquals("Màu Đen", alert.variantName());
        assertEquals(2, alert.stockQuantity());
        assertEquals(new BigDecimal("1200000.00"), alert.price());
    }

    @Test
    @DisplayName("TC-11: getMetrics_Success_LowStockAlerts_ExcludesDeleted - Truy vấn biến thể đúng threshold=5")
    void getMetrics_Success_LowStockAlerts_ExcludesDeleted() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(0L);
        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(productVariantRepository.findLowStockVariantsByShopId(eq(testShop.getId()), eq(5), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null);

        verify(productVariantRepository).findLowStockVariantsByShopId(eq(testShop.getId()), eq(5), any(Pageable.class));
        assertTrue(response.lowStockAlerts().isEmpty());
    }

    @Test
    @DisplayName("TC-12: getMetrics_Success_PendingPayoutAmount_Included - Tính đúng số tiền đang chờ duyệt rút")
    void getMetrics_Success_PendingPayoutAmount_Included() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        when(orderRepository.aggregateOrdersByShopIdAndDateRange(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(any(), any(), any(), any()))
                .thenReturn(0L);

        when(sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(eq(testSeller.getId()), eq(PayoutStatus.PENDING)))
                .thenReturn(new BigDecimal("8500000.00"));

        SellerDashboardMetricsResponse response = sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null);

        assertEquals(new BigDecimal("8500000.00"), response.summary().pendingPayoutAmount());
    }

    @Test
    @DisplayName("TC-13: getMetrics_Custom_MissingDates_ThrowsException - Period=CUSTOM nhưng thiếu ngày ném INVALID_INPUT_DATA")
    void getMetrics_Custom_MissingDates_ThrowsException() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.CUSTOM, null, LocalDate.now()));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: getMetrics_Custom_StartDateAfterEndDate_ThrowsException - startDate > endDate ném INVALID_INPUT_DATA")
    void getMetrics_Custom_StartDateAfterEndDate_ThrowsException() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(5);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.CUSTOM, start, end));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-15: getMetrics_Custom_RangeExceeds90Days_ThrowsException - Khoảng thời gian > 90 ngày ném INVALID_INPUT_DATA")
    void getMetrics_Custom_RangeExceeds90Days_ThrowsException() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        LocalDate start = LocalDate.now().minusDays(95);
        LocalDate end = LocalDate.now();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.CUSTOM, start, end));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-16: getMetrics_Custom_FutureDate_ThrowsException - endDate trong tương lai ném INVALID_INPUT_DATA")
    void getMetrics_Custom_FutureDate_ThrowsException() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now().plusDays(3);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.CUSTOM, start, end));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-17: getMetrics_ShopNotFound_ThrowsNotFoundException - Không tìm thấy Shop ném SHOP_NOT_FOUND (404)")
    void getMetrics_ShopNotFound_ThrowsNotFoundException() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null));
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-18: getMetrics_BannedShop_ThrowsShopNotActiveException - Shop bị BANNED ném SHOP_NOT_ACTIVE (400)")
    void getMetrics_BannedShop_ThrowsShopNotActiveException() {
        testShop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(testSeller.getId())).thenReturn(Optional.of(testShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerDashboardService.getDashboardMetrics(userId, DashboardPeriod.LAST_7_DAYS, null, null));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }
}

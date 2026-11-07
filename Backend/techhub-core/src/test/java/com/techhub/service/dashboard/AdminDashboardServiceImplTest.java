package com.techhub.service.dashboard;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.dashboard.AdminDashboardMetricsResponse;
import com.techhub.model.dto.response.dashboard.TopPerformingShopDto;
import com.techhub.model.enums.DashboardPeriod;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.projection.PlatformOrderStatusSummaryProjection;
import com.techhub.model.projection.ShopStatusSummaryProjection;
import com.techhub.model.projection.TopPerformingShopProjection;
import com.techhub.repository.*;
import com.techhub.service.dashboard.impl.AdminDashboardServiceImpl;
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
@DisplayName("Unit Tests cho AdminDashboardServiceImpl (GET /api/admin/dashboard/metrics)")
class AdminDashboardServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerPayoutRequestRepository sellerPayoutRequestRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    @BeforeEach
    void setUp() {
    }

    private PlatformOrderStatusSummaryProjection createPlatformOrderSummary(
            OrderStatus status, Long count, BigDecimal totalAmount, BigDecimal totalCommission) {
        return new PlatformOrderStatusSummaryProjection() {
            @Override public OrderStatus getStatus() { return status; }
            @Override public Long getOrderCount() { return count; }
            @Override public BigDecimal getTotalAmount() { return totalAmount; }
            @Override public BigDecimal getTotalCommission() { return totalCommission; }
        };
    }

    private ShopStatusSummaryProjection createShopStatusSummary(ShopStatus status, Long count) {
        return new ShopStatusSummaryProjection() {
            @Override public ShopStatus getStatus() { return status; }
            @Override public Long getCount() { return count; }
        };
    }

    private TopPerformingShopProjection createTopShop(
            UUID shopId, String name, String logo, Long orders, BigDecimal gmv, BigDecimal comm) {
        return new TopPerformingShopProjection() {
            @Override public UUID getShopId() { return shopId; }
            @Override public String getShopName() { return name; }
            @Override public String getLogoUrl() { return logo; }
            @Override public Long getTotalOrders() { return orders; }
            @Override public BigDecimal getTotalGmv() { return gmv; }
            @Override public BigDecimal getCommissionContributed() { return comm; }
        };
    }

    @Test
    @DisplayName("TC-ADM-DASH-01: Period null -> Mặc định gán LAST_7_DAYS")
    void testGetMetrics_DefaultPeriod_ResolvesToLast7Days() {
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(10L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(2L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(null, null, null);

        assertNotNull(response);
        assertEquals(DashboardPeriod.LAST_7_DAYS, response.period());
        assertEquals(LocalDate.now().minusDays(6), response.startDate());
        assertEquals(LocalDate.now(), response.endDate());
    }

    @Test
    @DisplayName("TC-ADM-DASH-02: Period TODAY -> Tính đúng hôm nay")
    void testGetMetrics_PeriodToday_ResolvesCorrectly() {
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertNotNull(response);
        assertEquals(DashboardPeriod.TODAY, response.period());
        assertEquals(LocalDate.now(), response.startDate());
        assertEquals(LocalDate.now(), response.endDate());
    }

    @Test
    @DisplayName("TC-ADM-DASH-03: Period THIS_MONTH -> Tính từ ngày 1 đầu tháng")
    void testGetMetrics_PeriodThisMonth_ResolvesCorrectly() {
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.THIS_MONTH, null, null);

        assertNotNull(response);
        assertEquals(DashboardPeriod.THIS_MONTH, response.period());
        assertEquals(LocalDate.now().withDayOfMonth(1), response.startDate());
        assertEquals(LocalDate.now(), response.endDate());
    }

    @Test
    @DisplayName("TC-ADM-DASH-04: Period CUSTOM với dải ngày hợp lệ")
    void testGetMetrics_PeriodCustom_ValidRange() {
        LocalDate start = LocalDate.now().minusDays(15);
        LocalDate end = LocalDate.now().minusDays(1);

        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.CUSTOM, start, end);

        assertNotNull(response);
        assertEquals(DashboardPeriod.CUSTOM, response.period());
        assertEquals(start, response.startDate());
        assertEquals(end, response.endDate());
    }

    @Test
    @DisplayName("TC-ADM-DASH-05: Period CUSTOM thiếu startDate -> ném INVALID_INPUT_DATA")
    void testGetMetrics_Custom_MissingStartDate_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.CUSTOM, null, LocalDate.now()));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-DASH-06: Period CUSTOM thiếu endDate -> ném INVALID_INPUT_DATA")
    void testGetMetrics_Custom_MissingEndDate_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.CUSTOM, LocalDate.now().minusDays(5), null));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-DASH-07: Period CUSTOM có startDate > endDate -> ném INVALID_INPUT_DATA")
    void testGetMetrics_Custom_StartDateAfterEndDate_ThrowsException() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(2);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.CUSTOM, start, end));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-DASH-08: Period CUSTOM có endDate trong tương lai -> ném INVALID_INPUT_DATA")
    void testGetMetrics_Custom_FutureDate_ThrowsException() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now().plusDays(2);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.CUSTOM, start, end));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-DASH-09: Period CUSTOM có khoảng cách > 90 ngày -> ném INVALID_INPUT_DATA")
    void testGetMetrics_Custom_RangeExceeds90Days_ThrowsException() {
        LocalDate start = LocalDate.now().minusDays(95);
        LocalDate end = LocalDate.now();
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.CUSTOM, start, end));
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-DASH-10: Sàn không có đơn hàng nào -> Trả về metrics 0 an toàn không NullPointer")
    void testGetMetrics_NoOrders_ReturnsSafeZeros() {
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(null);
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(null);
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(null);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(null);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(null);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(null);

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.summary().totalGmv());
        assertEquals(BigDecimal.ZERO, response.summary().completedGmv());
        assertEquals(BigDecimal.ZERO, response.summary().platformCommissionRevenue());
        assertEquals(0L, response.summary().totalOrders());
        assertEquals(0L, response.summary().completedOrders());
        assertEquals(0L, response.summary().totalUnitsSold());
        assertEquals(BigDecimal.ZERO, response.summary().averageOrderValue());
        assertEquals(0.0, response.summary().cancellationRate());
        assertEquals(0.0, response.summary().disputeRate());
        assertEquals(BigDecimal.ZERO, response.summary().totalSellersWalletBalance());
        assertEquals(BigDecimal.ZERO, response.summary().pendingPayoutAmount());
        assertEquals(BigDecimal.ZERO, response.summary().completedPayoutAmount());
        assertTrue(response.topPerformingShops().isEmpty());
    }

    @Test
    @DisplayName("TC-ADM-DASH-11: completedOrders = 0 -> AOV = 0.00 không bị chia cho 0")
    void testGetMetrics_NoCompletedOrders_AovIsZero() {
        List<PlatformOrderStatusSummaryProjection> summaries = List.of(
                createPlatformOrderSummary(OrderStatus.PENDING_CONFIRMATION, 5L, new BigDecimal("5000000.00"), new BigDecimal("250000.00"))
        );
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(summaries);
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertEquals(0L, response.summary().completedOrders());
        assertEquals(BigDecimal.ZERO, response.summary().averageOrderValue());
        assertEquals(5L, response.summary().totalOrders());
    }

    @Test
    @DisplayName("TC-ADM-DASH-12: totalOrders = 0 -> cancellationRate và disputeRate = 0.0 an toàn")
    void testGetMetrics_TotalOrdersZero_RatesAreZero() {
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertEquals(0.0, response.summary().cancellationRate());
        assertEquals(0.0, response.summary().disputeRate());
    }

    @Test
    @DisplayName("TC-ADM-DASH-13: Tính chính xác Cancellation Rate và Dispute Rate")
    void testGetMetrics_CalculatesRatesAccurately() {
        List<PlatformOrderStatusSummaryProjection> summaries = List.of(
                createPlatformOrderSummary(OrderStatus.COMPLETED, 80L, new BigDecimal("80000000.00"), new BigDecimal("4000000.00")),
                createPlatformOrderSummary(OrderStatus.CANCELLED, 10L, new BigDecimal("10000000.00"), new BigDecimal("500000.00")),
                createPlatformOrderSummary(OrderStatus.RETURN_REQUESTED, 6L, new BigDecimal("6000000.00"), new BigDecimal("300000.00")),
                createPlatformOrderSummary(OrderStatus.REFUNDED, 4L, new BigDecimal("4000000.00"), new BigDecimal("200000.00"))
        );
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(summaries);
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(100L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertEquals(100L, response.summary().totalOrders());
        assertEquals(10.0, response.summary().cancellationRate()); // 10/100 = 10%
        assertEquals(10.0, response.summary().disputeRate()); // (6+4)/100 = 10%
    }

    @Test
    @DisplayName("TC-ADM-DASH-14: Platform commission chỉ tính trên đơn COMPLETED & DELIVERED (BR-16D)")
    void testGetMetrics_CommissionOnlyFromCompletedAndDelivered() {
        List<PlatformOrderStatusSummaryProjection> summaries = List.of(
                createPlatformOrderSummary(OrderStatus.COMPLETED, 2L, new BigDecimal("2000000.00"), new BigDecimal("100000.00")),
                createPlatformOrderSummary(OrderStatus.DELIVERED, 1L, new BigDecimal("1000000.00"), new BigDecimal("50000.00")),
                createPlatformOrderSummary(OrderStatus.CANCELLED, 1L, new BigDecimal("1000000.00"), new BigDecimal("50000.00")),
                createPlatformOrderSummary(OrderStatus.PENDING_CONFIRMATION, 1L, new BigDecimal("1000000.00"), new BigDecimal("50000.00"))
        );
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(summaries);
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(5L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertEquals(new BigDecimal("5000000.00"), response.summary().totalGmv());
        assertEquals(new BigDecimal("3000000.00"), response.summary().completedGmv());
        assertEquals(new BigDecimal("150000.00"), response.summary().platformCommissionRevenue()); // 100k + 50k
    }

    @Test
    @DisplayName("TC-ADM-DASH-15: Growth metrics - Đếm người dùng và shop mới")
    void testGetMetrics_GrowthMetrics_NewUsersAndShops() {
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(25L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(7L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertEquals(25L, response.growthMetrics().newUsersCount());
        assertEquals(7L, response.growthMetrics().newShopsCount());
    }

    @Test
    @DisplayName("TC-ADM-DASH-16: Phân bổ trạng thái gian hàng toàn sàn (BR-07)")
    void testGetMetrics_GrowthMetrics_ShopStatusBreakdown() {
        List<ShopStatusSummaryProjection> summaries = List.of(
                createShopStatusSummary(ShopStatus.ACTIVE, 50L),
                createShopStatusSummary(ShopStatus.SUSPENDED, 5L),
                createShopStatusSummary(ShopStatus.PAUSED, 3L),
                createShopStatusSummary(ShopStatus.BANNED, 2L)
        );
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(summaries);
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertEquals(50L, response.growthMetrics().totalActiveShops());
        assertEquals(5L, response.growthMetrics().totalSuspendedShops());
        assertEquals(3L, response.growthMetrics().totalPausedShops());
        assertEquals(2L, response.growthMetrics().totalBannedShops());
    }

    @Test
    @DisplayName("TC-ADM-DASH-17: Tổng hợp số dư ví Sellers và tiền rút PENDING/COMPLETED")
    void testGetMetrics_FinancialExposure() {
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(new BigDecimal("120000000.00"));
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(new BigDecimal("35000000.00"));
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(new BigDecimal("80000000.00"));
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertEquals(new BigDecimal("120000000.00"), response.summary().totalSellersWalletBalance());
        assertEquals(new BigDecimal("35000000.00"), response.summary().pendingPayoutAmount());
        assertEquals(new BigDecimal("80000000.00"), response.summary().completedPayoutAmount());
    }

    @Test
    @DisplayName("TC-ADM-DASH-18: Top 5 Gian hàng xuất sắc nhất được map chính xác")
    void testGetMetrics_TopPerformingShops() {
        UUID shop1 = UUID.randomUUID();
        UUID shop2 = UUID.randomUUID();
        List<TopPerformingShopProjection> topShops = List.of(
                createTopShop(shop1, "Apple Store", "apple.png", 50L, new BigDecimal("100000000.00"), new BigDecimal("5000000.00")),
                createTopShop(shop2, "Sony Store", "sony.png", 30L, new BigDecimal("60000000.00"), new BigDecimal("3000000.00"))
        );
        when(orderRepository.aggregatePlatformOrdersByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(any(), any(), any())).thenReturn(0L);
        when(userRepository.countNewUsersByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countNewShopsByDateRange(any(), any())).thenReturn(0L);
        when(shopRepository.countShopsByStatusGrouped()).thenReturn(Collections.emptyList());
        when(sellerRepository.sumTotalWalletBalance()).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING)).thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(eq(PayoutStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTopPerformingShopsByDateRange(any(), any(), any(), any())).thenReturn(topShops);

        AdminDashboardMetricsResponse response = adminDashboardService.getPlatformDashboardMetrics(DashboardPeriod.TODAY, null, null);

        assertEquals(2, response.topPerformingShops().size());
        TopPerformingShopDto dto1 = response.topPerformingShops().get(0);
        assertEquals(shop1, dto1.shopId());
        assertEquals("Apple Store", dto1.shopName());
        assertEquals("apple.png", dto1.logoUrl());
        assertEquals(50L, dto1.totalOrders());
        assertEquals(new BigDecimal("100000000.00"), dto1.totalGmv());
        assertEquals(new BigDecimal("5000000.00"), dto1.commissionContributed());
    }
}

package com.techhub.service.dashboard.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.dashboard.*;
import com.techhub.model.enums.DashboardPeriod;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.projection.PlatformOrderStatusSummaryProjection;
import com.techhub.model.projection.ShopStatusSummaryProjection;
import com.techhub.model.projection.TopPerformingShopProjection;
import com.techhub.repository.*;
import com.techhub.service.dashboard.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final List<OrderStatus> REVENUE_ORDER_STATUSES = List.of(
            OrderStatus.COMPLETED,
            OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final SellerRepository sellerRepository;
    private final SellerPayoutRequestRepository sellerPayoutRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardMetricsResponse getPlatformDashboardMetrics(
            DashboardPeriod period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.info("Fetching admin platform dashboard metrics: period={}, startDate={}, endDate={}",
                period, startDate, endDate);

        // 1. Resolve & Validate Date Range
        DashboardPeriod activePeriod = period != null ? period : DashboardPeriod.LAST_7_DAYS;
        LocalDate resolvedStartDate;
        LocalDate resolvedEndDate;

        LocalDate today = LocalDate.now();
        switch (activePeriod) {
            case TODAY -> {
                resolvedStartDate = today;
                resolvedEndDate = today;
            }
            case LAST_7_DAYS -> {
                resolvedStartDate = today.minusDays(6);
                resolvedEndDate = today;
            }
            case THIS_MONTH -> {
                resolvedStartDate = today.withDayOfMonth(1);
                resolvedEndDate = today;
            }
            case CUSTOM -> {
                if (startDate == null || endDate == null) {
                    log.warn("Custom period requires both startDate and endDate");
                    throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
                }
                if (startDate.isAfter(endDate)) {
                    log.warn("Invalid custom range: startDate {} is after endDate {}", startDate, endDate);
                    throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
                }
                if (endDate.isAfter(today)) {
                    log.warn("Invalid custom range: endDate {} is in the future", endDate);
                    throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
                }
                if (ChronoUnit.DAYS.between(startDate, endDate) > 90) {
                    log.warn("Custom range exceeds maximum allowed 90 days: {} days",
                            ChronoUnit.DAYS.between(startDate, endDate));
                    throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
                }
                resolvedStartDate = startDate;
                resolvedEndDate = endDate;
            }
            default -> {
                resolvedStartDate = today.minusDays(6);
                resolvedEndDate = today;
            }
        }

        LocalDateTime startDateTime = resolvedStartDate.atStartOfDay();
        LocalDateTime endDateTime = resolvedEndDate.atTime(LocalTime.MAX);

        // 2. Query Platform Aggregated Orders (Single GROUP BY Query)
        List<PlatformOrderStatusSummaryProjection> orderSummaries = orderRepository
                .aggregatePlatformOrdersByDateRange(startDateTime, endDateTime);

        Map<OrderStatus, Long> statusCountMap = new EnumMap<>(OrderStatus.class);
        long totalOrders = 0;
        BigDecimal totalGmv = BigDecimal.ZERO;
        BigDecimal completedGmv = BigDecimal.ZERO;
        BigDecimal platformCommission = BigDecimal.ZERO;

        if (orderSummaries != null) {
            for (PlatformOrderStatusSummaryProjection summary : orderSummaries) {
                OrderStatus status = summary.getStatus();
                long count = summary.getOrderCount() != null ? summary.getOrderCount() : 0L;
                BigDecimal amount = summary.getTotalAmount() != null ? summary.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal commission = summary.getTotalCommission() != null ? summary.getTotalCommission() : BigDecimal.ZERO;

                statusCountMap.put(status, count);
                totalOrders += count;
                totalGmv = totalGmv.add(amount);

                if (REVENUE_ORDER_STATUSES.contains(status)) {
                    completedGmv = completedGmv.add(amount);
                    platformCommission = platformCommission.add(commission);
                }
            }
        }

        long completedOrders = statusCountMap.getOrDefault(OrderStatus.COMPLETED, 0L);

        // Calculate Average Order Value (AOV) safely against Divide-by-Zero
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (completedOrders > 0) {
            averageOrderValue = completedGmv.divide(BigDecimal.valueOf(completedOrders), 2, RoundingMode.HALF_UP);
        }

        // Calculate Cancellation & Dispute Rates safely against Divide-by-Zero
        double cancellationRate = 0.0;
        double disputeRate = 0.0;
        if (totalOrders > 0) {
            long cancelledCount = statusCountMap.getOrDefault(OrderStatus.CANCELLED, 0L);
            cancellationRate = BigDecimal.valueOf(cancelledCount * 100.0)
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                    .doubleValue();

            long disputeCount = statusCountMap.getOrDefault(OrderStatus.RETURN_REQUESTED, 0L)
                    + statusCountMap.getOrDefault(OrderStatus.REFUNDED, 0L);
            disputeRate = BigDecimal.valueOf(disputeCount * 100.0)
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // 3. Query Total Units Sold in Date Range
        long totalUnitsSold = orderItemRepository.sumTotalUnitsSoldByStatusesAndDateRange(
                REVENUE_ORDER_STATUSES, startDateTime, endDateTime);

        // 4. Query Growth Metrics (New Users & New Shops)
        long newUsersCount = userRepository.countNewUsersByDateRange(startDateTime, endDateTime);
        long newShopsCount = shopRepository.countNewShopsByDateRange(startDateTime, endDateTime);

        List<ShopStatusSummaryProjection> shopStatusSummaries = shopRepository.countShopsByStatusGrouped();
        Map<ShopStatus, Long> shopStatusMap = new EnumMap<>(ShopStatus.class);
        if (shopStatusSummaries != null) {
            for (ShopStatusSummaryProjection s : shopStatusSummaries) {
                if (s.getStatus() != null) {
                    shopStatusMap.put(s.getStatus(), s.getCount() != null ? s.getCount() : 0L);
                }
            }
        }

        PlatformGrowthDto growthMetrics = new PlatformGrowthDto(
                newUsersCount,
                newShopsCount,
                shopStatusMap.getOrDefault(ShopStatus.ACTIVE, 0L),
                shopStatusMap.getOrDefault(ShopStatus.SUSPENDED, 0L),
                shopStatusMap.getOrDefault(ShopStatus.PAUSED, 0L),
                shopStatusMap.getOrDefault(ShopStatus.BANNED, 0L)
        );

        // 5. Query Financial Exposure (Wallets & Payouts)
        BigDecimal totalSellersWalletBalance = sellerRepository.sumTotalWalletBalance();
        BigDecimal pendingPayoutAmount = sellerPayoutRequestRepository.sumAmountByStatus(PayoutStatus.PENDING);
        BigDecimal completedPayoutAmount = sellerPayoutRequestRepository.sumAmountByStatusAndProcessedAtBetween(
                PayoutStatus.COMPLETED, startDateTime, endDateTime);

        if (totalSellersWalletBalance == null) totalSellersWalletBalance = BigDecimal.ZERO;
        if (pendingPayoutAmount == null) pendingPayoutAmount = BigDecimal.ZERO;
        if (completedPayoutAmount == null) completedPayoutAmount = BigDecimal.ZERO;

        // 6. Build Order Status Breakdown DTO
        AdminOrderStatusBreakdownDto breakdown = new AdminOrderStatusBreakdownDto(
                statusCountMap.getOrDefault(OrderStatus.PENDING_CONFIRMATION, 0L),
                statusCountMap.getOrDefault(OrderStatus.PREPARING, 0L),
                statusCountMap.getOrDefault(OrderStatus.WAITING_FOR_PICKUP, 0L),
                statusCountMap.getOrDefault(OrderStatus.SHIPPING, 0L),
                statusCountMap.getOrDefault(OrderStatus.DELIVERED, 0L),
                statusCountMap.getOrDefault(OrderStatus.COMPLETED, 0L),
                statusCountMap.getOrDefault(OrderStatus.CANCELLED, 0L),
                statusCountMap.getOrDefault(OrderStatus.RETURN_REQUESTED, 0L),
                statusCountMap.getOrDefault(OrderStatus.REFUNDED, 0L)
        );

        // 7. Query Top 5 Performing Shops in Date Range
        List<TopPerformingShopProjection> topShopsProjections = orderRepository.findTopPerformingShopsByDateRange(
                REVENUE_ORDER_STATUSES,
                startDateTime,
                endDateTime,
                PageRequest.of(0, 5)
        );

        List<TopPerformingShopDto> topPerformingShops = new ArrayList<>();
        if (topShopsProjections != null) {
            for (TopPerformingShopProjection p : topShopsProjections) {
                topPerformingShops.add(new TopPerformingShopDto(
                        p.getShopId(),
                        p.getShopName(),
                        p.getLogoUrl(),
                        p.getTotalOrders() != null ? p.getTotalOrders() : 0L,
                        p.getTotalGmv() != null ? p.getTotalGmv() : BigDecimal.ZERO,
                        p.getCommissionContributed() != null ? p.getCommissionContributed() : BigDecimal.ZERO
                ));
            }
        }

        // 8. Assemble Summary DTO
        AdminDashboardSummaryDto summary = new AdminDashboardSummaryDto(
                totalGmv,
                completedGmv,
                platformCommission,
                totalOrders,
                completedOrders,
                totalUnitsSold,
                averageOrderValue,
                cancellationRate,
                disputeRate,
                totalSellersWalletBalance,
                pendingPayoutAmount,
                completedPayoutAmount
        );

        log.info("Admin platform metrics computed: gmv={}, commission={}, totalOrders={}, completedOrders={}",
                totalGmv, platformCommission, totalOrders, completedOrders);

        return new AdminDashboardMetricsResponse(
                activePeriod,
                resolvedStartDate,
                resolvedEndDate,
                summary,
                breakdown,
                growthMetrics,
                topPerformingShops
        );
    }
}

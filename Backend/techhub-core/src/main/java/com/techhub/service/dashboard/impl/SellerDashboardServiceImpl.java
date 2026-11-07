package com.techhub.service.dashboard.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.dashboard.*;
import com.techhub.model.entity.ProductVariant;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.enums.DashboardPeriod;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.projection.OrderStatusSummaryProjection;
import com.techhub.model.projection.TopSellingProductProjection;
import com.techhub.repository.*;
import com.techhub.service.dashboard.SellerDashboardService;
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
public class SellerDashboardServiceImpl implements SellerDashboardService {

    private static final List<OrderStatus> REVENUE_ORDER_STATUSES = List.of(
            OrderStatus.COMPLETED,
            OrderStatus.DELIVERED
    );

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SellerPayoutRequestRepository sellerPayoutRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public SellerDashboardMetricsResponse getDashboardMetrics(
            UUID userId,
            DashboardPeriod period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.info("Fetching seller dashboard metrics: userId={}, period={}, startDate={}, endDate={}",
                userId, period, startDate, endDate);

        // 1. Fail-fast validation on User ID
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        // 2. Fetch Seller & Shop (Ownership Check / IDOR Protection)
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        // 3. Shop Status Governance Check (BR-07)
        if (shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop {} is BANNED, rejecting dashboard access", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 4. Resolve & Validate Date Range
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

        // 5. Query Aggregated Order Metrics (Zero N+1 Group By)
        List<OrderStatusSummaryProjection> orderSummaries = orderRepository
                .aggregateOrdersByShopIdAndDateRange(shop.getId(), startDateTime, endDateTime);

        Map<OrderStatus, Long> statusCountMap = new EnumMap<>(OrderStatus.class);
        Map<OrderStatus, BigDecimal> statusEarningsMap = new EnumMap<>(OrderStatus.class);

        long totalOrders = 0;
        BigDecimal netRevenue = BigDecimal.ZERO;

        if (orderSummaries != null) {
            for (OrderStatusSummaryProjection summary : orderSummaries) {
                OrderStatus status = summary.getStatus();
                long count = summary.getOrderCount() != null ? summary.getOrderCount() : 0L;
                BigDecimal earnings = summary.getTotalEarnings() != null ? summary.getTotalEarnings() : BigDecimal.ZERO;

                statusCountMap.put(status, count);
                statusEarningsMap.put(status, earnings);
                totalOrders += count;

                if (REVENUE_ORDER_STATUSES.contains(status)) {
                    netRevenue = netRevenue.add(earnings);
                }
            }
        }

        long completedOrders = statusCountMap.getOrDefault(OrderStatus.COMPLETED, 0L);

        // Calculate Average Order Value (AOV) safely against Divide-by-Zero
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (completedOrders > 0) {
            averageOrderValue = netRevenue.divide(BigDecimal.valueOf(completedOrders), 2, RoundingMode.HALF_UP);
        }

        // 6. Query Total Units Sold in Date Range
        long totalUnitsSold = orderItemRepository.sumUnitsSoldByShopIdAndStatusesAndDateRange(
                shop.getId(), REVENUE_ORDER_STATUSES, startDateTime, endDateTime);

        // 7. Query Wallet & Payout Metrics
        BigDecimal walletBalance = seller.getBalance() != null ? seller.getBalance() : BigDecimal.ZERO;
        BigDecimal pendingPayoutAmount = sellerPayoutRequestRepository.sumAmountBySellerIdAndStatus(
                seller.getId(), PayoutStatus.PENDING);
        if (pendingPayoutAmount == null) {
            pendingPayoutAmount = BigDecimal.ZERO;
        }

        // 8. Build Order Status Breakdown DTO
        SellerOrderStatusBreakdownDto breakdown = new SellerOrderStatusBreakdownDto(
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

        // 9. Query Top 5 Selling Products in Date Range
        List<TopSellingProductProjection> topProductProjections = orderItemRepository
                .findTopSellingProductsByShopId(
                        shop.getId(),
                        REVENUE_ORDER_STATUSES,
                        startDateTime,
                        endDateTime,
                        PageRequest.of(0, 5)
                );

        List<TopSellingProductDto> topSellingProducts = new ArrayList<>();
        if (topProductProjections != null) {
            for (TopSellingProductProjection p : topProductProjections) {
                topSellingProducts.add(new TopSellingProductDto(
                        p.getProductId(),
                        p.getProductName(),
                        p.getImageUrl(),
                        p.getUnitsSold() != null ? p.getUnitsSold() : 0L,
                        p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO
                ));
            }
        }

        // 10. Query Low Stock Variants (stock <= 5)
        List<ProductVariant> lowStockVariants = productVariantRepository
                .findLowStockVariantsByShopId(shop.getId(), 5, PageRequest.of(0, 10));

        List<LowStockVariantDto> lowStockAlerts = new ArrayList<>();
        if (lowStockVariants != null) {
            for (ProductVariant pv : lowStockVariants) {
                lowStockAlerts.add(new LowStockVariantDto(
                        pv.getId(),
                        pv.getProduct() != null ? pv.getProduct().getId() : null,
                        pv.getProduct() != null ? pv.getProduct().getName() : null,
                        pv.getVariantName(),
                        pv.getSku(),
                        pv.getImageUrl(),
                        pv.getStockQuantity(),
                        pv.getPrice()
                ));
            }
        }

        // 11. Assemble Final Response DTO
        SellerDashboardSummaryDto summary = new SellerDashboardSummaryDto(
                netRevenue,
                totalOrders,
                completedOrders,
                totalUnitsSold,
                walletBalance,
                pendingPayoutAmount,
                averageOrderValue
        );

        log.info("Seller dashboard metrics computed for shop {}: revenue={}, totalOrders={}, completedOrders={}",
                shop.getId(), netRevenue, totalOrders, completedOrders);

        return new SellerDashboardMetricsResponse(
                activePeriod,
                resolvedStartDate,
                resolvedEndDate,
                summary,
                breakdown,
                topSellingProducts,
                lowStockAlerts
        );
    }
}

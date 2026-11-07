package com.techhub.repository;

import com.techhub.model.entity.Order;
import com.techhub.model.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByPaymentId(UUID paymentId);

    List<Order> findByUserId(UUID userId);

    boolean existsByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"shop", "payment"})
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"shop", "payment", "user"})
    Optional<Order> findByIdAndShopId(UUID id, UUID shopId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.shop.id = :shopId")
    Optional<Order> findByIdAndShopIdForUpdate(@Param("id") UUID id, @Param("shopId") UUID shopId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.user.id = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"shop", "shop.seller", "shop.seller.user", "payment", "user"})
    Optional<Order> findWithDetailsById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"shop", "shop.seller", "shop.seller.user", "payment", "user"})
    Page<Order> findAll(@Nullable Specification<Order> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"payment"})
    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deliveredAt IS NOT NULL AND o.deliveredAt <= :deliveredBefore")
    List<Order> findByStatusAndDeliveredAtBefore(@Param("status") OrderStatus status, @Param("deliveredBefore") java.time.LocalDateTime deliveredBefore, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Order o SET o.status = com.techhub.model.enums.OrderStatus.COMPLETED, o.completedAt = :now, o.updatedAt = :now WHERE o.id = :id AND o.status = com.techhub.model.enums.OrderStatus.DELIVERED")
    int completeDeliveredOrderAtomic(@Param("id") UUID id, @Param("now") java.time.LocalDateTime now);

    @Query("""
        SELECT COUNT(o.id) FROM Order o 
        WHERE o.shop.id = :shopId 
          AND o.status = com.techhub.model.enums.OrderStatus.COMPLETED
    """)
    long countCompletedOrdersByShopId(@Param("shopId") UUID shopId);

    @Query("""
        SELECT COALESCE(SUM(o.totalSellerEarnings), 0) FROM Order o 
        WHERE o.shop.id = :shopId 
          AND o.status = com.techhub.model.enums.OrderStatus.COMPLETED
    """)
    java.math.BigDecimal calculateCumulativeRevenueByShopId(@Param("shopId") UUID shopId);

    @Query("""
        SELECT o.status AS status,
               COUNT(o.id) AS orderCount,
               COALESCE(SUM(o.totalSellerEarnings), 0) AS totalEarnings
        FROM Order o
        WHERE o.shop.id = :shopId
          AND o.createdAt >= :startDate AND o.createdAt <= :endDate
        GROUP BY o.status
    """)
    List<com.techhub.model.projection.OrderStatusSummaryProjection> aggregateOrdersByShopIdAndDateRange(
            @Param("shopId") UUID shopId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );

    @Query("""
        SELECT o.status AS status,
               COUNT(o.id) AS orderCount,
               COALESCE(SUM(o.totalAmount), 0) AS totalAmount,
               COALESCE(SUM(o.totalCommission), 0) AS totalCommission
        FROM Order o
        WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate
        GROUP BY o.status
    """)
    List<com.techhub.model.projection.PlatformOrderStatusSummaryProjection> aggregatePlatformOrdersByDateRange(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );

    @Query("""
        SELECT o.shop.id AS shopId,
               o.shop.name AS shopName,
               o.shop.avatarUrl AS logoUrl,
               COUNT(o.id) AS totalOrders,
               COALESCE(SUM(o.totalAmount), 0) AS totalGmv,
               COALESCE(SUM(o.totalCommission), 0) AS commissionContributed
        FROM Order o
        WHERE o.status IN :statuses
          AND o.createdAt >= :startDate AND o.createdAt <= :endDate
        GROUP BY o.shop.id, o.shop.name, o.shop.avatarUrl
        ORDER BY SUM(o.totalAmount) DESC
    """)
    List<com.techhub.model.projection.TopPerformingShopProjection> findTopPerformingShopsByDateRange(
            @Param("statuses") Collection<com.techhub.model.enums.OrderStatus> statuses,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            org.springframework.data.domain.Pageable pageable
    );
}

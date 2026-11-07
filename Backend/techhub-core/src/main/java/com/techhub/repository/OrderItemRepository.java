package com.techhub.repository;

import com.techhub.model.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @EntityGraph(attributePaths = {"productVariant"})
    List<OrderItem> findByOrderId(UUID orderId);

    @EntityGraph(attributePaths = {"productVariant"})
    List<OrderItem> findByOrderIdIn(Collection<UUID> orderIds);

    @EntityGraph(attributePaths = {"order", "order.user", "productVariant", "productVariant.product"})
    Optional<OrderItem> findWithOrderAndProductById(UUID id);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.shop.id = :shopId
          AND o.status IN :statuses
          AND o.createdAt >= :startDate AND o.createdAt <= :endDate
    """)
    long sumUnitsSoldByShopIdAndStatusesAndDateRange(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("statuses") Collection<com.techhub.model.enums.OrderStatus> statuses,
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT oi.productVariant.product.id AS productId,
               oi.productNameSnapshot AS productName,
               MAX(oi.productVariant.imageUrl) AS imageUrl,
               SUM(oi.quantity) AS unitsSold,
               SUM(oi.finalSubtotal) AS totalRevenue
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.shop.id = :shopId
          AND o.status IN :statuses
          AND o.createdAt >= :startDate AND o.createdAt <= :endDate
        GROUP BY oi.productVariant.product.id, oi.productNameSnapshot
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<com.techhub.model.projection.TopSellingProductProjection> findTopSellingProductsByShopId(
            @org.springframework.data.repository.query.Param("shopId") UUID shopId,
            @org.springframework.data.repository.query.Param("statuses") Collection<com.techhub.model.enums.OrderStatus> statuses,
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status IN :statuses
          AND o.createdAt >= :startDate AND o.createdAt <= :endDate
    """)
    long sumTotalUnitsSoldByStatusesAndDateRange(
            @org.springframework.data.repository.query.Param("statuses") Collection<com.techhub.model.enums.OrderStatus> statuses,
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate
    );
}

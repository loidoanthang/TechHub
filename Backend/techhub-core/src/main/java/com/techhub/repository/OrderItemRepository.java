package com.techhub.repository;

import com.techhub.model.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @EntityGraph(attributePaths = {"productVariant"})
    List<OrderItem> findByOrderId(UUID orderId);

    @EntityGraph(attributePaths = {"productVariant"})
    List<OrderItem> findByOrderIdIn(Collection<UUID> orderIds);
}

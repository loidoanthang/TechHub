package com.techhub.repository;

import com.techhub.model.entity.ReturnRequest;
import com.techhub.model.enums.ReturnRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    @EntityGraph(attributePaths = {"order", "user"})
    Optional<ReturnRequest> findByOrderId(UUID orderId);

    @EntityGraph(attributePaths = {"order", "user"})
    Optional<ReturnRequest> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT r FROM ReturnRequest r JOIN r.order o WHERE r.id = :id AND o.shop.id = :shopId")
    Optional<ReturnRequest> findByIdAndShopId(@Param("id") UUID id, @Param("shopId") UUID shopId);

    boolean existsByOrderIdAndStatus(UUID orderId, ReturnRequestStatus status);

    @Query("SELECT r FROM ReturnRequest r WHERE r.status = :status AND r.createdAt <= :threshold")
    List<ReturnRequest> findByStatusAndCreatedAtBefore(@Param("status") ReturnRequestStatus status, @Param("threshold") LocalDateTime threshold, Pageable pageable);

    @Query("SELECT r FROM ReturnRequest r JOIN r.order o WHERE r.order.id = :orderId AND r.user.id = :userId")
    Optional<ReturnRequest> findByOrderIdAndUserId(@Param("orderId") UUID orderId, @Param("userId") UUID userId);

    @Query("SELECT r FROM ReturnRequest r JOIN r.order o WHERE o.shop.id = :shopId")
    Page<ReturnRequest> findByShopId(@Param("shopId") UUID shopId, Pageable pageable);

    @Query("SELECT r FROM ReturnRequest r JOIN r.order o WHERE o.shop.id = :shopId AND r.status = :status")
    Page<ReturnRequest> findByShopIdAndStatus(@Param("shopId") UUID shopId, @Param("status") ReturnRequestStatus status, Pageable pageable);
}

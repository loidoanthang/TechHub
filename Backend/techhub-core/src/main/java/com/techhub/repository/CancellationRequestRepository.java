package com.techhub.repository;

import com.techhub.model.entity.CancellationRequest;
import com.techhub.model.enums.CancellationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CancellationRequestRepository extends JpaRepository<CancellationRequest, UUID> {
    Optional<CancellationRequest> findByOrderId(UUID orderId);
    boolean existsByOrderIdAndStatus(UUID orderId, CancellationRequestStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM CancellationRequest c WHERE c.status = :status AND c.createdAt <= :threshold")
    java.util.List<CancellationRequest> findByStatusAndCreatedAtBefore(@org.springframework.data.repository.query.Param("status") CancellationRequestStatus status, @org.springframework.data.repository.query.Param("threshold") java.time.LocalDateTime threshold, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM CancellationRequest c JOIN c.order o WHERE c.id = :id AND o.shop.id = :shopId")
    Optional<CancellationRequest> findByIdAndShopId(@org.springframework.data.repository.query.Param("id") UUID id, @org.springframework.data.repository.query.Param("shopId") UUID shopId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM CancellationRequest c JOIN c.order o WHERE c.id = :id AND o.shop.id = :shopId")
    Optional<CancellationRequest> findByIdAndShopIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id, @org.springframework.data.repository.query.Param("shopId") UUID shopId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM CancellationRequest c JOIN c.order o WHERE o.shop.id = :shopId")
    org.springframework.data.domain.Page<CancellationRequest> findByShopId(@org.springframework.data.repository.query.Param("shopId") UUID shopId, org.springframework.data.domain.Pageable pageable);
}

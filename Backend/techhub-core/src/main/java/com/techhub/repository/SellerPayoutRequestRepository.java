package com.techhub.repository;

import com.techhub.model.entity.SellerPayoutRequest;
import com.techhub.model.enums.PayoutStatus;
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
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerPayoutRequestRepository extends JpaRepository<SellerPayoutRequest, UUID>, JpaSpecificationExecutor<SellerPayoutRequest> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"seller", "processedBy"})
    @Query("SELECT p FROM SellerPayoutRequest p WHERE p.id = :id")
    Optional<SellerPayoutRequest> findForUpdateById(@Param("id") UUID id);

    @Override
    @EntityGraph(attributePaths = {"seller", "processedBy"})
    Page<SellerPayoutRequest> findAll(Specification<SellerPayoutRequest> spec, Pageable pageable);

    Page<SellerPayoutRequest> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM SellerPayoutRequest p
            WHERE p.seller.id = :sellerId
              AND p.createdAt >= :startOfDay
              AND p.createdAt < :startOfNextDay
              AND p.status IN :statuses
            """)
    BigDecimal sumDailyWithdrawalAmount(
            @Param("sellerId") UUID sellerId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay,
            @Param("statuses") Collection<PayoutStatus> statuses
    );

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM SellerPayoutRequest p WHERE p.seller.id = :sellerId AND p.status = :status")
    BigDecimal sumAmountBySellerIdAndStatus(@Param("sellerId") UUID sellerId, @Param("status") PayoutStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM SellerPayoutRequest p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PayoutStatus status);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM SellerPayoutRequest p
        WHERE p.status = :status
          AND p.processedAt >= :startDate AND p.processedAt <= :endDate
    """)
    BigDecimal sumAmountByStatusAndProcessedAtBetween(
            @Param("status") PayoutStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}

package com.techhub.modules.voucher.repository;

import com.techhub.modules.voucher.entity.Voucher;
import com.techhub.modules.voucher.enums.VoucherAppliesTo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.techhub.modules.voucher.entity.Voucher;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID>, JpaSpecificationExecutor<Voucher> {
    boolean existsByCode(String code);
    Optional<Voucher> findByCode(String code);

    @Override
    @EntityGraph(attributePaths = {"shop"})
    Page<Voucher> findAll(Specification<Voucher> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"shop"})
    Optional<Voucher> findWithShopById(UUID id);

    @EntityGraph(attributePaths = {"shop"})
    @Query("SELECT v FROM Voucher v WHERE v.id IN :ids")
    List<Voucher> findAllWithShopByIdIn(@Param("ids") Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"shop"})
    @Query("SELECT v FROM Voucher v WHERE v.id IN :ids")
    List<Voucher> findAllWithShopByIdInForUpdate(@Param("ids") Collection<UUID> ids);

    @Query("""
        SELECT v FROM Voucher v
        WHERE v.shop.id = :shopId
          AND v.type = com.techhub.modules.voucher.enums.VoucherType.SHOP
          AND v.isDeleted = false
          AND v.isActive = true
          AND v.startDate <= :now
          AND v.endDate >= :now
          AND (v.usageLimit IS NULL OR (COALESCE(v.usedCount, 0) + COALESCE(v.reservedCount, 0)) < v.usageLimit)
        ORDER BY v.createdAt DESC
    """)
    List<Voucher> findAvailableShopVouchers(@Param("shopId") UUID shopId, @Param("now") LocalDateTime now);

    @Query("""
        SELECT v FROM Voucher v
        WHERE v.type = com.techhub.modules.voucher.enums.VoucherType.PLATFORM
          AND v.shop IS NULL
          AND v.isDeleted = false
          AND v.isActive = true
          AND v.startDate <= :now
          AND v.endDate >= :now
          AND (v.usageLimit IS NULL OR (COALESCE(v.usedCount, 0) + COALESCE(v.reservedCount, 0)) < v.usageLimit)
          AND (:appliesTo IS NULL OR v.appliesTo = :appliesTo)
        ORDER BY v.createdAt DESC
    """)
    List<Voucher> findAvailablePlatformVouchers(
            @Param("appliesTo") VoucherAppliesTo appliesTo,
            @Param("now") LocalDateTime now
    );
}

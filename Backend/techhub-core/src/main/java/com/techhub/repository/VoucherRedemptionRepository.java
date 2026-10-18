package com.techhub.repository;

import com.techhub.model.entity.VoucherRedemption;
import com.techhub.model.enums.VoucherRedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, UUID> {

    @Query("""
        SELECT vr.voucher.id, COUNT(vr.id)
        FROM VoucherRedemption vr
        WHERE vr.user.id = :userId
          AND vr.voucher.id IN :voucherIds
          AND vr.status IN :statuses
        GROUP BY vr.voucher.id
    """)
    List<Object[]> countByUserIdAndVoucherIdInAndStatusIn(
            @Param("userId") UUID userId,
            @Param("voucherIds") Collection<UUID> voucherIds,
            @Param("statuses") Collection<VoucherRedemptionStatus> statuses
    );

    long countByUserIdAndVoucherIdAndStatusIn(
            UUID userId,
            UUID voucherId,
            Collection<VoucherRedemptionStatus> statuses
    );

    List<VoucherRedemption> findByOrderId(UUID orderId);

    List<VoucherRedemption> findByPaymentId(UUID paymentId);
}

package com.techhub.modules.user.repository;

import com.techhub.modules.payment.enums.PaymentInfoStatus;
import com.techhub.modules.user.entity.UserPaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPaymentInfoRepository extends JpaRepository<UserPaymentInfo, UUID>, JpaSpecificationExecutor<UserPaymentInfo> {

    long countByUserIdAndStatus(UUID userId, PaymentInfoStatus status);

    boolean existsByUserIdAndVnpTokenAndStatus(UUID userId, String vnpToken, PaymentInfoStatus status);

    Optional<UserPaymentInfo> findByVnpToken(String vnpToken);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserPaymentInfo upi SET upi.isDefault = false WHERE upi.user.id = :userId")
    int resetDefaultByUserId(@Param("userId") UUID userId);

    List<UserPaymentInfo> findByUserIdAndStatusOrderByIsDefaultDescCreatedAtDesc(UUID userId, PaymentInfoStatus status);

    Optional<UserPaymentInfo> findByIdAndUserId(UUID id, UUID userId);

    Optional<UserPaymentInfo> findByIdAndUserIdAndStatus(UUID id, UUID userId, PaymentInfoStatus status);

    Optional<UserPaymentInfo> findFirstByUserIdAndIdNotAndStatusOrderByCreatedAtDesc(UUID userId, UUID id, PaymentInfoStatus status);
}

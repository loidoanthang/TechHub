package com.techhub.repository;

import com.techhub.model.entity.UserPaymentInfo;
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

    long countByUserId(UUID userId);

    boolean existsByUserIdAndProviderIgnoreCaseAndAccountNumber(UUID userId, String provider, String accountNumber);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserPaymentInfo upi SET upi.isDefault = false WHERE upi.user.id = :userId")
    int resetDefaultByUserId(@Param("userId") UUID userId);

    List<UserPaymentInfo> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    Optional<UserPaymentInfo> findByIdAndUserId(UUID id, UUID userId);

    Optional<UserPaymentInfo> findFirstByUserIdAndIdNotOrderByCreatedAtDesc(UUID userId, UUID id);
}

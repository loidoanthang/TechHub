package com.techhub.repository;

import com.techhub.model.entity.TransactionPolicy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionPolicyRepository extends JpaRepository<TransactionPolicy, UUID> {

    Optional<TransactionPolicy> findByPolicyKey(String policyKey);

    boolean existsByPolicyKey(String policyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM TransactionPolicy p WHERE p.policyKey = :policyKey")
    Optional<TransactionPolicy> findForUpdateByPolicyKey(@Param("policyKey") String policyKey);

    @EntityGraph(attributePaths = {"updatedBy"})
    @Query("SELECT p FROM TransactionPolicy p ORDER BY p.policyKey ASC")
    List<TransactionPolicy> findAllWithUpdatedByOrderByPolicyKeyAsc();
}

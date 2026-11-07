package com.techhub.repository;

import com.techhub.model.entity.SellerWalletTransaction;
import com.techhub.model.enums.SellerWalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerWalletTransactionRepository extends JpaRepository<SellerWalletTransaction, UUID>, JpaSpecificationExecutor<SellerWalletTransaction> {

    Page<SellerWalletTransaction> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Optional<SellerWalletTransaction> findByReferenceIdAndType(UUID referenceId, SellerWalletTransactionType type);

    boolean existsByReferenceIdAndType(UUID referenceId, SellerWalletTransactionType type);
}

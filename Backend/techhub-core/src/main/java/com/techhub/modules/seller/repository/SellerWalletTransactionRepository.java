package com.techhub.modules.seller.repository;

import com.techhub.modules.seller.entity.SellerWalletTransaction;
import com.techhub.modules.seller.enums.SellerWalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import com.techhub.modules.seller.entity.SellerWalletTransaction;

@Repository
public interface SellerWalletTransactionRepository extends JpaRepository<SellerWalletTransaction, UUID>, JpaSpecificationExecutor<SellerWalletTransaction> {

    Page<SellerWalletTransaction> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Optional<SellerWalletTransaction> findByReferenceIdAndType(UUID referenceId, SellerWalletTransactionType type);

    boolean existsByReferenceIdAndType(UUID referenceId, SellerWalletTransactionType type);
}

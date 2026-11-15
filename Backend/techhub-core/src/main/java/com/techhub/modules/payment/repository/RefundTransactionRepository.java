package com.techhub.modules.payment.repository;

import com.techhub.modules.payment.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import com.techhub.modules.payment.entity.RefundTransaction;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, UUID> {

    List<RefundTransaction> findByOrderId(UUID orderId);

    List<RefundTransaction> findByPaymentId(UUID paymentId);
}

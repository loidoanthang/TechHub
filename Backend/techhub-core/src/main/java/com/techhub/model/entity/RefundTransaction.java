package com.techhub.model.entity;

import com.techhub.model.enums.RefundMethod;
import com.techhub.model.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "refund_transactions",
        indexes = {
                @Index(name = "idx_refund_payment_id", columnList = "payment_id"),
                @Index(name = "idx_refund_order_id", columnList = "order_id"),
                @Index(name = "idx_refund_return_request_id", columnList = "return_request_id"),
                @Index(name = "idx_refund_status", columnList = "status"),
                @Index(name = "idx_refund_created_at", columnList = "created_at")
        }
)
public class RefundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id")
    private ReturnRequest returnRequest;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false, length = 30)
    private RefundMethod refundMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "target_bank_name", length = 100)
    private String targetBankName;

    @Column(name = "target_bank_account", length = 50)
    private String targetBankAccount;

    @Column(name = "target_bank_holder", length = 150)
    private String targetBankHolder;

    @Column(name = "gateway_refund_id", length = 100)
    private String gatewayRefundId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

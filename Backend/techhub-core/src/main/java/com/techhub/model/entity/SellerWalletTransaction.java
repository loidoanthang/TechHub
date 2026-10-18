package com.techhub.model.entity;

import com.techhub.model.enums.SellerWalletTransactionType;
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
        name = "seller_wallet_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seller_wallet_ref_type", columnNames = {"reference_id", "type"})
        },
        indexes = {
                @Index(name = "idx_seller_wallet_seller_id", columnList = "seller_id"),
                @Index(name = "idx_seller_wallet_reference_id", columnList = "reference_id"),
                @Index(name = "idx_seller_wallet_created_at", columnList = "created_at")
        }
)
public class SellerWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private SellerWalletTransactionType type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

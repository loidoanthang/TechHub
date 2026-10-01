package com.techhub.model.entity;

import com.techhub.model.enums.SellerVerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "sellers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sellers_user_id", columnNames = {"user_id"}),
                @UniqueConstraint(name = "uk_sellers_seller_name", columnNames = {"seller_name"})
        },
        indexes = {
                @Index(name = "idx_sellers_user_id", columnList = "user_id"),
                @Index(name = "idx_sellers_seller_name", columnList = "seller_name")
        }
)
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "seller_name", nullable = false, unique = true, length = 100)
    private String sellerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private SellerVerificationStatus verificationStatus = SellerVerificationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "bank_account_number", nullable = false, length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_account_holder", nullable = false, length = 150)
    private String bankAccountHolder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

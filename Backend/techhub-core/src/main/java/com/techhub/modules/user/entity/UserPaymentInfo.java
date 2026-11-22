package com.techhub.modules.user.entity;

import com.techhub.modules.payment.enums.PaymentInfoStatus;
import com.techhub.modules.payment.enums.PaymentType;
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
        name = "user_payment_info",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_payment_info_token", columnNames = {"vnp_token"})
        },
        indexes = {
                @Index(name = "idx_user_payment_info_user_id", columnList = "user_id"),
                @Index(name = "idx_user_payment_info_token", columnList = "vnp_token"),
                @Index(name = "idx_user_payment_info_card_unique", columnList = "user_id, bank_code, card_mask")
        }
)
public class UserPaymentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    @Builder.Default
    private PaymentType paymentType = PaymentType.BANK_ACCOUNT;

    @Column(name = "vnp_token", length = 255)
    private String vnpToken;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "card_mask", length = 50)
    private String cardMask;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentInfoStatus status = PaymentInfoStatus.ACTIVE;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

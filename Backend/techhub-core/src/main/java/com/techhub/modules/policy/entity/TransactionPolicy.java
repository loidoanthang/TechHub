package com.techhub.modules.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import com.techhub.modules.user.entity.User;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "transaction_policies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transaction_policies_key", columnNames = {"policy_key"})
        },
        indexes = {
                @Index(name = "idx_transaction_policies_key", columnList = "policy_key")
        }
)
public class TransactionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "policy_key", nullable = false, unique = true, length = 100)
    private String policyKey;

    @Column(name = "policy_value", nullable = false, length = 255)
    private String policyValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

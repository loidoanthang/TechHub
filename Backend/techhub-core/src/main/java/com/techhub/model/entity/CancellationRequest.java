package com.techhub.model.entity;

import com.techhub.model.enums.CancellationRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@Table(
        name = "cancellation_requests",
        indexes = {
                @Index(name = "idx_cancellation_order_id", columnList = "order_id"),
                @Index(name = "idx_cancellation_user_id", columnList = "user_id"),
                @Index(name = "idx_cancellation_status", columnList = "status")
        }
)
public class CancellationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 50)
    private com.techhub.model.enums.CancellationReasonCode reasonCode;

    @Column(name = "reason_note", columnDefinition = "TEXT")
    private String reasonNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CancellationRequestStatus status;

    @Column(name = "seller_response", length = 500)
    private String sellerResponse;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

package com.techhub.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "recently_viewed_products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recently_viewed_user_product", columnNames = {"user_id", "product_id"})
        },
        indexes = {
                @Index(name = "idx_recently_viewed_user_id", columnList = "user_id"),
                @Index(name = "idx_recently_viewed_viewed_at", columnList = "viewed_at"),
                @Index(name = "idx_recently_viewed_user_viewed", columnList = "user_id, viewed_at DESC")
        }
)
public class RecentlyViewedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
}

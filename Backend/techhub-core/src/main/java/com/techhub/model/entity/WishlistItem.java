package com.techhub.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "wishlist_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wishlist_user_product", columnNames = {"user_id", "product_id"})
        },
        indexes = {
                @Index(name = "idx_wishlist_user_id", columnList = "user_id"),
                @Index(name = "idx_wishlist_product_id", columnList = "product_id")
        }
)
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

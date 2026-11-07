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
        name = "shop_followings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shop_followings_user_shop", columnNames = {"user_id", "shop_id"})
        },
        indexes = {
                @Index(name = "idx_shop_followings_user_id", columnList = "user_id"),
                @Index(name = "idx_shop_followings_shop_id", columnList = "shop_id")
        }
)
public class ShopFollowing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

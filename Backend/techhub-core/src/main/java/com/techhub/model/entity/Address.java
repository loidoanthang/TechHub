package com.techhub.model.entity;

import com.techhub.model.enums.AddressLabel;
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
        name = "addresses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_addresses_shop_id", columnNames = {"shop_id"})
        },
        indexes = {
                @Index(name = "idx_addresses_user_id", columnList = "user_id"),
                @Index(name = "idx_addresses_shop_id", columnList = "shop_id")
        }
)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "shop_id")
    private UUID shopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "label", length = 20)
    private AddressLabel label;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "province", nullable = false, length = 100)
    private String province;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "ward", nullable = false, length = 100)
    private String ward;

    @Column(name = "street_address", nullable = false, length = 500)
    private String streetAddress;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

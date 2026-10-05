package com.techhub.model.entity;

import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_slug", columnNames = {"slug"})
        },
        indexes = {
                @Index(name = "idx_products_shop_id", columnList = "shop_id"),
                @Index(name = "idx_products_category_id", columnList = "category_id"),
                @Index(name = "idx_products_gatekeeper", columnList = "approval_status, visibility_status, is_suspended, is_deleted"),
                @Index(name = "idx_products_brand_name", columnList = "brand_name")
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 350)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "json")
    private Map<String, Object> attributes;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ProductApprovalStatus approvalStatus = ProductApprovalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_status", nullable = false, length = 20)
    @Builder.Default
    private ProductVisibilityStatus visibilityStatus = ProductVisibilityStatus.UNPUBLISHED;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "is_suspended", nullable = false)
    @Builder.Default
    private boolean isSuspended = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

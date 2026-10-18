package com.techhub.model.entity;

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
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id"),
                @Index(name = "idx_order_items_variant_id", columnList = "product_variant_id")
        }
)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "variant_name_snapshot", length = 255)
    private String variantNameSnapshot;

    @Column(name = "sku_snapshot", length = 100)
    private String skuSnapshot;

    @Column(name = "image_url_snapshot", length = 500)
    private String imageUrlSnapshot;

    @Column(name = "original_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "item_discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal itemDiscountAmount;

    @Column(name = "discounted_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountedUnitPrice;

    @Column(name = "final_subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalSubtotal;

    @Column(name = "commission_rate_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRateSnapshot;

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "seller_earnings", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellerEarnings;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

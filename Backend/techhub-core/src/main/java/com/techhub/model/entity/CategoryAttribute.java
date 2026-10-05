package com.techhub.model.entity;

import com.techhub.model.enums.CategoryAttributeDataType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "category_attributes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_attributes_category_name",
                        columnNames = {"category_id", "name"}
                )
        },
        indexes = {
                @Index(name = "idx_category_attributes_category_id", columnList = "category_id")
        }
)
public class CategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 30)
    private CategoryAttributeDataType dataType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "json")
    private List<String> options;

    @Column(name = "sample_value", length = 255)
    private String sampleValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

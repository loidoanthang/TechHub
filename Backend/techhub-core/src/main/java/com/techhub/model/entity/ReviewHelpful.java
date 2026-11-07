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
        name = "review_helpfuls",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_helpfuls_review_user", columnNames = {"review_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_review_helpfuls_review_id", columnList = "review_id"),
                @Index(name = "idx_review_helpfuls_user_id", columnList = "user_id")
        }
)
public class ReviewHelpful {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

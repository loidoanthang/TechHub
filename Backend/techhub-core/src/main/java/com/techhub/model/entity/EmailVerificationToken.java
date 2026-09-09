package com.techhub.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_verification_token", indexes = {
        @Index(name = "idx_email_verification_token_user_id", columnList = "user_id"),
        @Index(name = "idx_email_verification_token_expires_at", columnList = "expires_at")
})
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

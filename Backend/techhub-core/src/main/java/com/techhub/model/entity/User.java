package com.techhub.model.entity;

import com.techhub.model.enums.Role;
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
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(nullable = false, unique = true, name = "email")
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "phone")
    private String phone;
    @Column(name = "avatar_url")
    private String avatarUrl;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.BUYER;
    @Column(name = "enabled", nullable = false)
    private boolean enabled;
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;
    @Column(name = "lockout_end_time")
    private LocalDateTime lockoutEndTime;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isAccountNonLocked() {
        if (lockoutEndTime == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lockoutEndTime);
    }
}

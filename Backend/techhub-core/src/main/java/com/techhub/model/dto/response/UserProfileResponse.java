package com.techhub.model.dto.response;

import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String avatarUrl,
        Set<Role> roles,
        UserStatus status,
        boolean emailVerified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRoles(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

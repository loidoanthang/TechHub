package com.techhub.model.dto.response;

import com.techhub.model.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean emailVerified,
        UserStatus status,
        String defaultAddress,
        String avatarUrl,
        LocalDateTime createdAt
) {}

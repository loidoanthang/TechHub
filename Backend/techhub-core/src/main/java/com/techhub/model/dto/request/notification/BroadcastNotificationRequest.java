package com.techhub.model.dto.request.notification;

import com.techhub.model.enums.NotificationType;
import com.techhub.model.enums.TargetAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BroadcastNotificationRequest(
        @NotNull(message = "Target audience is required")
        TargetAudience targetAudience,

        @NotBlank(message = "Title is required")
        @Size(max = 300, message = "Title must not exceed 300 characters")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        NotificationType type,

        @Size(max = 30, message = "Reference type must not exceed 30 characters")
        String referenceType,

        UUID referenceId
) {}

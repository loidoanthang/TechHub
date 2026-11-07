package com.techhub.model.dto.response.notification;

import com.techhub.model.enums.NotificationType;
import com.techhub.model.enums.TargetAudience;

import java.time.LocalDateTime;

public record BroadcastNotificationResponse(
        int totalTargetUsers,
        int totalSent,
        TargetAudience targetAudience,
        NotificationType type,
        String title,
        LocalDateTime broadcastAt
) {}

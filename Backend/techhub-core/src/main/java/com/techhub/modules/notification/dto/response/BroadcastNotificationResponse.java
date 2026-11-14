package com.techhub.modules.notification.dto.response;

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

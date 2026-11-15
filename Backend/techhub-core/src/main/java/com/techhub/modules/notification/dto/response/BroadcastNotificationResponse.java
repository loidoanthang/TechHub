package com.techhub.modules.notification.dto.response;

import com.techhub.modules.notification.enums.NotificationType;
import com.techhub.modules.notification.enums.TargetAudience;

import java.time.LocalDateTime;

public record BroadcastNotificationResponse(
        int totalTargetUsers,
        int totalSent,
        TargetAudience targetAudience,
        NotificationType type,
        String title,
        LocalDateTime broadcastAt
) {}

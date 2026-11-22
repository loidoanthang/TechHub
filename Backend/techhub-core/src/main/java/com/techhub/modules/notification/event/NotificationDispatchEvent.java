package com.techhub.modules.notification.event;

import com.techhub.modules.notification.enums.NotificationType;

import java.util.UUID;

public record NotificationDispatchEvent(
        UUID recipientUserId,
        NotificationType type,
        String title,
        String content,
        String referenceType,
        UUID referenceId
) {}

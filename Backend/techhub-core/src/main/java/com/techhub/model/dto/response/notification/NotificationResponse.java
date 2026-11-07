package com.techhub.model.dto.response.notification;

import com.techhub.model.entity.Notification;
import com.techhub.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String content,
        boolean isRead,
        String referenceType,
        UUID referenceId,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.isRead(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getCreatedAt()
        );
    }
}

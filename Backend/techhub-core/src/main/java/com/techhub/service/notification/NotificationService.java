package com.techhub.service.notification;

import com.techhub.model.dto.response.notification.NotificationListResponse;
import com.techhub.model.enums.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    NotificationListResponse getNotifications(UUID userId, Boolean isRead, NotificationType type, Pageable pageable);

    void markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);

    void deleteNotification(UUID userId, UUID notificationId);
}

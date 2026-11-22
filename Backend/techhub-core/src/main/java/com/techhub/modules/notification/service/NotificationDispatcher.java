package com.techhub.modules.notification.service;

import com.techhub.modules.notification.enums.NotificationType;

import java.util.UUID;

public interface NotificationDispatcher {

    /**
     * Dispatch an automatic notification to a specific user.
     * The notification is persisted to database and pushed via SSE after the current transaction commits.
     *
     * @param recipientUserId Target user ID
     * @param type            NotificationType (ORDER, REVIEW, PROMOTION, SYSTEM, SHOP)
     * @param title           Notification title
     * @param content         Notification message content
     * @param referenceType   Context entity type for frontend routing (e.g. "ORDER", "REVIEW", "SHOP")
     * @param referenceId     Context entity ID for frontend routing
     */
    void dispatch(UUID recipientUserId, NotificationType type, String title, String content, String referenceType, UUID referenceId);
}

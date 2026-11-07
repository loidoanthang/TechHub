package com.techhub.model.dto.response.notification;

import com.techhub.common.PageResponse;

public record NotificationListResponse(
        long unreadCount,
        PageResponse<NotificationResponse> notifications
) {
}

package com.techhub.modules.notification.dto.response;

import com.techhub.common.PageResponse;

public record NotificationListResponse(
        long unreadCount,
        PageResponse<NotificationResponse> notifications
) {
}

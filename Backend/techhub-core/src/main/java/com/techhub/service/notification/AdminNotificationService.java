package com.techhub.service.notification;

import com.techhub.modules.notification.dto.request.BroadcastNotificationRequest;
import com.techhub.modules.notification.dto.response.BroadcastNotificationResponse;

import java.util.UUID;

public interface AdminNotificationService {

    BroadcastNotificationResponse broadcast(BroadcastNotificationRequest request, UUID adminId);
}

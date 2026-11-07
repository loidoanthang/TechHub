package com.techhub.service.notification;

import com.techhub.model.dto.request.notification.BroadcastNotificationRequest;
import com.techhub.model.dto.response.notification.BroadcastNotificationResponse;

import java.util.UUID;

public interface AdminNotificationService {

    BroadcastNotificationResponse broadcast(BroadcastNotificationRequest request, UUID adminId);
}

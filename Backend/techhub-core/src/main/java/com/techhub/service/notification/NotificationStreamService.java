package com.techhub.service.notification;

import com.techhub.model.dto.response.notification.NotificationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface NotificationStreamService {

    SseEmitter subscribe(UUID userId);

    void sendNotification(UUID userId, NotificationResponse notification);

    void broadcastToAllActive(NotificationResponse notification);

    void sendHeartbeat();
}

package com.techhub.modules.notification.service.impl;

import com.techhub.modules.notification.enums.NotificationType;
import com.techhub.modules.notification.event.NotificationDispatchEvent;
import com.techhub.modules.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcherImpl implements NotificationDispatcher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void dispatch(UUID recipientUserId, NotificationType type, String title, String content, String referenceType, UUID referenceId) {
        if (recipientUserId == null) {
            log.warn("Cannot dispatch notification: recipientUserId is null");
            return;
        }

        log.debug("Dispatching notification event for userId={}, type={}, title='{}'", recipientUserId, type, title);
        eventPublisher.publishEvent(new NotificationDispatchEvent(recipientUserId, type, title, content, referenceType, referenceId));
    }
}

package com.techhub.modules.notification.event;

import com.techhub.modules.notification.dto.response.NotificationResponse;
import com.techhub.modules.notification.entity.Notification;
import com.techhub.modules.notification.repository.NotificationRepository;
import com.techhub.modules.notification.service.NotificationStreamService;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationStreamService notificationStreamService;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationDispatch(NotificationDispatchEvent event) {
        if (event == null || event.recipientUserId() == null) {
            return;
        }

        try {
            User recipient = userRepository.findById(event.recipientUserId()).orElse(null);
            if (recipient == null) {
                log.warn("Cannot deliver notification: recipient user not found for id={}", event.recipientUserId());
                return;
            }

            Notification notification = Notification.builder()
                    .user(recipient)
                    .type(event.type())
                    .title(event.title())
                    .content(event.content())
                    .isRead(false)
                    .referenceType(event.referenceType())
                    .referenceId(event.referenceId())
                    .build();

            Notification saved = notificationRepository.save(notification);
            log.info("Saved auto-trigger notification id={} for userId={}, type={}", saved.getId(), recipient.getId(), event.type());

            NotificationResponse response = NotificationResponse.from(saved);
            notificationStreamService.sendNotification(recipient.getId(), response);
        } catch (Exception e) {
            log.error("Failed to save and send auto-trigger notification for userId={}: {}", event.recipientUserId(), e.getMessage(), e);
        }
    }
}

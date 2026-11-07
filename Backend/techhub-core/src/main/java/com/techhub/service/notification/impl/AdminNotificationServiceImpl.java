package com.techhub.service.notification.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.notification.BroadcastNotificationRequest;
import com.techhub.model.dto.response.notification.BroadcastNotificationResponse;
import com.techhub.model.dto.response.notification.NotificationResponse;
import com.techhub.model.enums.NotificationType;
import com.techhub.model.enums.TargetAudience;
import com.techhub.repository.NotificationRepository;
import com.techhub.service.notification.AdminNotificationService;
import com.techhub.service.notification.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationStreamService notificationStreamService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BroadcastNotificationResponse broadcast(BroadcastNotificationRequest request, UUID adminId) {
        if (adminId == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (request == null || request.targetAudience() == null) {
            throw new IllegalArgumentException("Target audience is required");
        }

        NotificationType type = (request.type() != null) ? request.type() : NotificationType.SYSTEM;
        TargetAudience audience = request.targetAudience();

        log.info("Admin {} initiating broadcast to audience={}, type={}, title='{}'",
                adminId, audience, type, request.title());

        int totalSent;
        switch (audience) {
            case ALL_USERS -> totalSent = notificationRepository.broadcastToAllUsers(
                    type.name(),
                    request.title(),
                    request.content(),
                    request.referenceType(),
                    request.referenceId(),
                    adminId
            );
            case ALL_BUYERS -> totalSent = notificationRepository.broadcastToAllBuyers(
                    type.name(),
                    request.title(),
                    request.content(),
                    request.referenceType(),
                    request.referenceId(),
                    adminId
            );
            case ALL_SELLERS -> totalSent = notificationRepository.broadcastToAllSellers(
                    type.name(),
                    request.title(),
                    request.content(),
                    request.referenceType(),
                    request.referenceId(),
                    adminId
            );
            default -> throw new IllegalArgumentException("Unsupported target audience: " + audience);
        }

        log.info("Broadcast completed successfully by admin {}: {} notifications created for audience {}",
                adminId, totalSent, audience);

        // Push real-time toast to currently active online SSE users AFTER database commit
        NotificationResponse ssePayload = new NotificationResponse(
                null,
                type,
                request.title(),
                request.content(),
                false,
                request.referenceType(),
                request.referenceId(),
                LocalDateTime.now()
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notificationStreamService.broadcastToAllActive(ssePayload);
                    } catch (Exception e) {
                        log.warn("Failed to push real-time broadcast SSE notification after commit: {}", e.getMessage());
                    }
                }
            });
        } else {
            try {
                notificationStreamService.broadcastToAllActive(ssePayload);
            } catch (Exception e) {
                log.warn("Failed to push real-time broadcast SSE notification: {}", e.getMessage());
            }
        }

        return new BroadcastNotificationResponse(
                totalSent,
                totalSent,
                audience,
                type,
                request.title(),
                LocalDateTime.now()
        );
    }
}

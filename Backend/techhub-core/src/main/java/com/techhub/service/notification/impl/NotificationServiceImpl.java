package com.techhub.service.notification.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.notification.NotificationListResponse;
import com.techhub.model.dto.response.notification.NotificationResponse;
import com.techhub.model.entity.Notification;
import com.techhub.model.enums.NotificationType;
import com.techhub.repository.NotificationRepository;
import com.techhub.service.notification.NotificationService;
import com.techhub.specification.NotificationSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(UUID userId, Boolean isRead, NotificationType type, Pageable pageable) {
        log.info("Fetching notifications for userId={}, isRead={}, type={}, page={}, size={}",
                userId, isRead, type, pageable.getPageNumber(), pageable.getPageSize());

        // 1. Tính tổng số thông báo chưa đọc toàn cục của user (isRead = false AND deletedAt IS NULL)
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);

        // 2. Xây dựng dynamic specification lọc theo điều kiện
        Specification<Notification> spec = Specification.where(NotificationSpecification.belongsToUser(userId))
                .and(NotificationSpecification.notDeleted())
                .and(NotificationSpecification.hasReadStatus(isRead))
                .and(NotificationSpecification.hasType(type));

        // 3. Đảm bảo sắp xếp giảm dần theo thời gian tạo, kèm tie-breaker ID giảm dần (Deterministic Sorting)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        // 4. Thực thi truy vấn phân trang
        Page<Notification> notificationPage = notificationRepository.findAll(spec, sortedPageable);

        // 5. Chuyển đổi sang DTO
        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        PageResponse<NotificationResponse> pageResponse = PageResponse.from(notificationPage, content);

        return new NotificationListResponse(unreadCount, pageResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(UUID userId, UUID notificationId) {
        log.info("Marking notification as read: userId={}, notificationId={}", userId, notificationId);

        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> {
                    log.warn("Notification not found for markAsRead: userId={}, notificationId={}", userId, notificationId);
                    return new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
                });

        if (notification.isRead()) {
            log.info("Notification {} is already marked as read, skipping update", notificationId);
            return;
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        log.info("Notification {} successfully marked as read for userId={}", notificationId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(UUID userId) {
        log.info("Marking all notifications as read for userId={}", userId);
        int updatedCount = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read for userId={}", updatedCount, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(UUID userId, UUID notificationId) {
        log.info("Deleting notification: userId={}, notificationId={}", userId, notificationId);

        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> {
                    log.warn("Notification not found for deletion: userId={}, notificationId={}", userId, notificationId);
                    return new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
                });

        notification.setDeletedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        log.info("Notification {} successfully soft-deleted for userId={}", notificationId, userId);
    }
}

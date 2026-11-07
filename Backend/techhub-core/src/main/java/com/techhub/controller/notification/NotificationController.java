package com.techhub.controller.notification;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.notification.NotificationListResponse;
import com.techhub.model.enums.NotificationType;
import com.techhub.security.SecurityUtils;
import com.techhub.service.notification.NotificationService;
import com.techhub.service.notification.NotificationStreamService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationListResponse> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must not be negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size must not exceed 100") int size
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        NotificationListResponse response = notificationService.getNotifications(userId, isRead, type, pageable);
        return ApiResponse.success(response, "Notifications retrieved successfully", null);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAsRead(@PathVariable("id") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(userId, id);
        return ApiResponse.success(null, "Notification marked as read successfully", null);
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllAsRead() {
        UUID userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.success(null, "All notifications marked as read successfully", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteNotification(@PathVariable("id") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        notificationService.deleteNotification(userId, id);
        return ApiResponse.success(null, "Notification deleted successfully", null);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter stream() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return notificationStreamService.subscribe(userId);
    }
}

package com.techhub.controller.notification;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.notification.BroadcastNotificationRequest;
import com.techhub.model.dto.response.notification.BroadcastNotificationResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.notification.AdminNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @PostMapping("/broadcast")
    public ApiResponse<BroadcastNotificationResponse> broadcast(@Valid @RequestBody BroadcastNotificationRequest request) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        BroadcastNotificationResponse response = adminNotificationService.broadcast(request, adminId);
        return ApiResponse.success(response, "Broadcast notification sent successfully", null);
    }
}

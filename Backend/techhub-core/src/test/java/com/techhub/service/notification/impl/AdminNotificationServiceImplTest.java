package com.techhub.service.notification.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.notification.BroadcastNotificationRequest;
import com.techhub.model.dto.response.notification.BroadcastNotificationResponse;
import com.techhub.model.dto.response.notification.NotificationResponse;
import com.techhub.model.enums.NotificationType;
import com.techhub.model.enums.TargetAudience;
import com.techhub.repository.NotificationRepository;
import com.techhub.service.notification.NotificationStreamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminNotificationServiceImpl (POST /api/admin/notifications/broadcast)")
class AdminNotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationStreamService notificationStreamService;

    @InjectMocks
    private AdminNotificationServiceImpl adminNotificationService;

    private UUID adminId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    @DisplayName("TC-69: broadcast_Success_HappyPath_AllUsers - Broadcast thành công tới toàn bộ người dùng active")
    void broadcast_Success_HappyPath_AllUsers() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS,
                "Bảo trì hệ thống",
                "Hệ thống bảo trì từ 1h đến 3h sáng",
                NotificationType.SYSTEM,
                null,
                null
        );

        when(notificationRepository.broadcastToAllUsers(
                eq("SYSTEM"), eq("Bảo trì hệ thống"), eq("Hệ thống bảo trì từ 1h đến 3h sáng"),
                isNull(), isNull(), eq(adminId)
        )).thenReturn(100);

        BroadcastNotificationResponse response = adminNotificationService.broadcast(request, adminId);

        assertNotNull(response);
        assertEquals(100, response.totalSent());
        assertEquals(100, response.totalTargetUsers());
        assertEquals(TargetAudience.ALL_USERS, response.targetAudience());
        assertEquals(NotificationType.SYSTEM, response.type());
        assertEquals("Bảo trì hệ thống", response.title());
        assertNotNull(response.broadcastAt());

        verify(notificationRepository, times(1)).broadcastToAllUsers(any(), any(), any(), any(), any(), eq(adminId));
        verify(notificationStreamService, times(1)).broadcastToAllActive(any(NotificationResponse.class));
    }

    @Test
    @DisplayName("TC-70: broadcast_Success_HappyPath_AllBuyers - Broadcast thành công tới toàn bộ Người mua")
    void broadcast_Success_HappyPath_AllBuyers() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_BUYERS,
                "Mega Sale 11.11",
                "Ưu đãi giảm giá 50% cho người mua",
                NotificationType.PROMOTION,
                null,
                null
        );

        when(notificationRepository.broadcastToAllBuyers(
                eq("PROMOTION"), eq("Mega Sale 11.11"), eq("Ưu đãi giảm giá 50% cho người mua"),
                isNull(), isNull(), eq(adminId)
        )).thenReturn(75);

        BroadcastNotificationResponse response = adminNotificationService.broadcast(request, adminId);

        assertNotNull(response);
        assertEquals(75, response.totalSent());
        assertEquals(TargetAudience.ALL_BUYERS, response.targetAudience());
        assertEquals(NotificationType.PROMOTION, response.type());

        verify(notificationRepository, times(1)).broadcastToAllBuyers(any(), any(), any(), any(), any(), eq(adminId));
        verify(notificationStreamService, times(1)).broadcastToAllActive(any(NotificationResponse.class));
    }

    @Test
    @DisplayName("TC-71: broadcast_Success_HappyPath_AllSellers - Broadcast thành công tới toàn bộ Người bán")
    void broadcast_Success_HappyPath_AllSellers() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_SELLERS,
                "Cập nhật chính sách phí sàn",
                "Biểu phí mới áp dụng từ tháng 12",
                NotificationType.SYSTEM,
                null,
                null
        );

        when(notificationRepository.broadcastToAllSellers(
                eq("SYSTEM"), eq("Cập nhật chính sách phí sàn"), eq("Biểu phí mới áp dụng từ tháng 12"),
                isNull(), isNull(), eq(adminId)
        )).thenReturn(25);

        BroadcastNotificationResponse response = adminNotificationService.broadcast(request, adminId);

        assertNotNull(response);
        assertEquals(25, response.totalSent());
        assertEquals(TargetAudience.ALL_SELLERS, response.targetAudience());

        verify(notificationRepository, times(1)).broadcastToAllSellers(any(), any(), any(), any(), any(), eq(adminId));
        verify(notificationStreamService, times(1)).broadcastToAllActive(any(NotificationResponse.class));
    }

    @Test
    @DisplayName("TC-72: broadcast_Success_WithOptionalReferenceData - Broadcast kèm thông tin tham chiếu điều hướng")
    void broadcast_Success_WithOptionalReferenceData() {
        UUID referenceId = UUID.randomUUID();
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_BUYERS,
                "Voucher mới cho bạn",
                "Nhận ngay voucher freeship",
                NotificationType.PROMOTION,
                "VOUCHER",
                referenceId
        );

        when(notificationRepository.broadcastToAllBuyers(
                eq("PROMOTION"), eq("Voucher mới cho bạn"), eq("Nhận ngay voucher freeship"),
                eq("VOUCHER"), eq(referenceId), eq(adminId)
        )).thenReturn(50);

        BroadcastNotificationResponse response = adminNotificationService.broadcast(request, adminId);

        assertNotNull(response);
        assertEquals(50, response.totalSent());

        verify(notificationRepository, times(1)).broadcastToAllBuyers(
                eq("PROMOTION"), any(), any(), eq("VOUCHER"), eq(referenceId), eq(adminId)
        );
    }

    @Test
    @DisplayName("TC-73: broadcast_Success_ZeroActiveUsers_ReturnsZeroSent - Xử lý êm dịu khi nhóm đối tượng không có user nào")
    void broadcast_Success_ZeroActiveUsers_ReturnsZeroSent() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_SELLERS,
                "Thông báo người bán",
                "Nội dung kiểm tra",
                NotificationType.SYSTEM,
                null,
                null
        );

        when(notificationRepository.broadcastToAllSellers(any(), any(), any(), any(), any(), eq(adminId)))
                .thenReturn(0);

        BroadcastNotificationResponse response = adminNotificationService.broadcast(request, adminId);

        assertNotNull(response);
        assertEquals(0, response.totalSent());
        assertEquals(0, response.totalTargetUsers());
    }

    @Test
    @DisplayName("TC-83: broadcast_Success_DefaultNotificationTypeIsSystem - Mặc định gán NotificationType.SYSTEM khi type null")
    void broadcast_Success_DefaultNotificationTypeIsSystem() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS,
                "Thông báo quan trọng",
                "Nội dung quan trọng",
                null, // type null
                null,
                null
        );

        when(notificationRepository.broadcastToAllUsers(
                eq("SYSTEM"), any(), any(), isNull(), isNull(), eq(adminId)
        )).thenReturn(10);

        BroadcastNotificationResponse response = adminNotificationService.broadcast(request, adminId);

        assertNotNull(response);
        assertEquals(NotificationType.SYSTEM, response.type());
        verify(notificationRepository).broadcastToAllUsers(eq("SYSTEM"), any(), any(), any(), any(), eq(adminId));
    }

    @Test
    @DisplayName("TC-84: broadcast_Failed_NullAdminId_ThrowsUnauthenticated - Ném exception UNAUTHENTICATED khi adminId null")
    void broadcast_Failed_NullAdminId_ThrowsUnauthenticated() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS,
                "Tiêu đề",
                "Nội dung",
                NotificationType.SYSTEM,
                null,
                null
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminNotificationService.broadcast(request, null));
        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("TC-85: broadcast_Failed_NullRequestOrAudience_ThrowsIllegalArgument - Ném exception khi request hoặc audience null")
    void broadcast_Failed_NullRequestOrAudience_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> adminNotificationService.broadcast(null, adminId));

        BroadcastNotificationRequest invalidRequest = new BroadcastNotificationRequest(
                null, "Tiêu đề", "Nội dung", NotificationType.SYSTEM, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> adminNotificationService.broadcast(invalidRequest, adminId));
    }

    @Test
    @DisplayName("TC-86: broadcast_SseFailure_DoesNotThrowAndCompletesSuccessfully - Lỗi đẩy SSE không làm rollback việc tạo thông báo")
    void broadcast_SseFailure_DoesNotThrowAndCompletesSuccessfully() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS,
                "Tiêu đề",
                "Nội dung",
                NotificationType.SYSTEM,
                null,
                null
        );

        when(notificationRepository.broadcastToAllUsers(any(), any(), any(), any(), any(), eq(adminId))).thenReturn(15);
        doThrow(new RuntimeException("SSE network error")).when(notificationStreamService).broadcastToAllActive(any());

        assertDoesNotThrow(() -> {
            BroadcastNotificationResponse response = adminNotificationService.broadcast(request, adminId);
            assertEquals(15, response.totalSent());
        });
    }

    @Test
    @DisplayName("TC-93: broadcast_Success_WithActiveTransactionSynchronization - Đăng ký afterCommit và kích hoạt broadcast SSE sau commit")
    void broadcast_Success_WithActiveTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                    TargetAudience.ALL_USERS,
                    "Tiêu đề giao dịch",
                    "Nội dung sau commit",
                    NotificationType.SYSTEM,
                    null,
                    null
            );

            when(notificationRepository.broadcastToAllUsers(any(), any(), any(), any(), any(), eq(adminId))).thenReturn(20);

            BroadcastNotificationResponse response = adminNotificationService.broadcast(request, adminId);
            assertNotNull(response);

            // Trước khi commit, notificationStreamService chưa được kích hoạt
            verify(notificationStreamService, never()).broadcastToAllActive(any());

            // Giả lập Spring commit transaction
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            // Sau khi commit, SSE broadcast mới được kích hoạt
            verify(notificationStreamService, times(1)).broadcastToAllActive(any());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}

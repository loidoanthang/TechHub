package com.techhub.modules.notification.event;

import com.techhub.modules.notification.dto.response.NotificationResponse;
import com.techhub.modules.notification.entity.Notification;
import com.techhub.modules.notification.enums.NotificationType;
import com.techhub.modules.notification.repository.NotificationRepository;
import com.techhub.modules.notification.service.NotificationStreamService;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho NotificationEventListener (Auto-Trigger Listener)")
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationStreamService notificationStreamService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationEventListener listener;

    private UUID userId;
    private User recipient;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        recipient = new User();
        recipient.setId(userId);
        recipient.setEmail("buyer@example.com");
        recipient.setFirstName("Nguyen");
        recipient.setLastName("An");
    }

    @Test
    @DisplayName("handleNotificationDispatch - Lưu DB và gửi SSE thành công")
    void handleNotificationDispatch_Success() {
        UUID refId = UUID.randomUUID();
        NotificationDispatchEvent event = new NotificationDispatchEvent(
                userId,
                NotificationType.ORDER,
                "Đơn hàng mới",
                "Đơn hàng của bạn đã được xác nhận",
                "ORDER",
                refId
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(recipient));

        Notification savedNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(recipient)
                .type(NotificationType.ORDER)
                .title(event.title())
                .content(event.content())
                .isRead(false)
                .referenceType(event.referenceType())
                .referenceId(event.referenceId())
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        listener.handleNotificationDispatch(event);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification captured = notifCaptor.getValue();
        assertEquals(recipient, captured.getUser());
        assertEquals(NotificationType.ORDER, captured.getType());
        assertEquals("Đơn hàng mới", captured.getTitle());
        assertEquals("Đơn hàng của bạn đã được xác nhận", captured.getContent());
        assertFalse(captured.isRead());
        assertEquals("ORDER", captured.getReferenceType());
        assertEquals(refId, captured.getReferenceId());

        verify(notificationStreamService).sendNotification(eq(userId), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("handleNotificationDispatch - Không gửi khi người dùng không tồn tại")
    void handleNotificationDispatch_UserNotFound() {
        NotificationDispatchEvent event = new NotificationDispatchEvent(
                userId,
                NotificationType.ORDER,
                "Đơn hàng mới",
                "Nội dung",
                "ORDER",
                UUID.randomUUID()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        listener.handleNotificationDispatch(event);

        verify(notificationRepository, never()).save(any());
        verify(notificationStreamService, never()).sendNotification(any(), any());
    }

    @Test
    @DisplayName("handleNotificationDispatch - Bỏ qua khi event hoặc recipientId là null")
    void handleNotificationDispatch_NullEventOrRecipient() {
        listener.handleNotificationDispatch(null);
        listener.handleNotificationDispatch(new NotificationDispatchEvent(null, NotificationType.ORDER, "T", "C", "ORDER", null));

        verifyNoInteractions(userRepository, notificationRepository, notificationStreamService);
    }

    @Test
    @DisplayName("handleNotificationDispatch - Không làm đứt đoạn khi Stream SSE xảy ra ngoại lệ")
    void handleNotificationDispatch_StreamExceptionHandledGracefully() {
        NotificationDispatchEvent event = new NotificationDispatchEvent(
                userId,
                NotificationType.SHOP,
                "Shop status",
                "Content",
                "SHOP",
                UUID.randomUUID()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(recipient));

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .user(recipient)
                .type(NotificationType.SHOP)
                .title(event.title())
                .content(event.content())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        doThrow(new RuntimeException("SSE connection lost")).when(notificationStreamService).sendNotification(any(), any());

        assertDoesNotThrow(() -> listener.handleNotificationDispatch(event));
        verify(notificationRepository).save(any(Notification.class));
    }
}

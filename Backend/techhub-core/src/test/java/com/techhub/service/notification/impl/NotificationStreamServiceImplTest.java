package com.techhub.service.notification.impl;

import com.techhub.model.dto.response.notification.NotificationResponse;
import com.techhub.model.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho NotificationStreamServiceImpl (GET /api/notifications/stream)")
class NotificationStreamServiceImplTest {

    private NotificationStreamServiceImpl streamService;
    private UUID userId;
    private NotificationResponse sampleNotification;

    @BeforeEach
    void setUp() {
        streamService = new NotificationStreamServiceImpl();
        userId = UUID.randomUUID();
        sampleNotification = new NotificationResponse(
                UUID.randomUUID(),
                NotificationType.ORDER,
                "Đơn hàng mới",
                "Đơn hàng đã được xác nhận",
                false,
                "ORDER",
                UUID.randomUUID(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("TC-52: stream_Success_HappyPath_CreatesEmitter - Tạo emitter thành công và lưu vào bộ nhớ")
    void stream_Success_HappyPath_CreatesEmitter() {
        SseEmitter emitter = streamService.subscribe(userId);

        assertNotNull(emitter);
        assertEquals(1, streamService.getActiveEmitterCount(userId));
    }

    @Test
    @DisplayName("TC-53: stream_Success_MultiTabSupport - Hỗ trợ cùng một user kết nối đồng thời từ nhiều tab")
    void stream_Success_MultiTabSupport() {
        SseEmitter emitter1 = streamService.subscribe(userId);
        SseEmitter emitter2 = streamService.subscribe(userId);

        assertNotNull(emitter1);
        assertNotNull(emitter2);
        assertEquals(2, streamService.getActiveEmitterCount(userId));
    }

    @Test
    @DisplayName("TC-54: stream_Success_OnCompletionCallbackRemovesEmitter - Tự động xóa Emitter khi hoàn thành phiên")
    void stream_Success_OnCompletionCallbackRemovesEmitter() {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        NotificationStreamServiceImpl customService = new NotificationStreamServiceImpl() {
            @Override
            protected SseEmitter createEmitter(Long timeout) {
                return mockEmitter;
            }
        };

        customService.subscribe(userId);
        assertEquals(1, customService.getActiveEmitterCount(userId));

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockEmitter).onCompletion(captor.capture());

        // Kích hoạt callback onCompletion
        captor.getValue().run();

        assertEquals(0, customService.getActiveEmitterCount(userId));
    }

    @Test
    @DisplayName("TC-55: stream_Success_OnTimeoutCallbackRemovesEmitter - Tự động dọn dẹp Emitter khi hết hạn timeout")
    void stream_Success_OnTimeoutCallbackRemovesEmitter() {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        NotificationStreamServiceImpl customService = new NotificationStreamServiceImpl() {
            @Override
            protected SseEmitter createEmitter(Long timeout) {
                return mockEmitter;
            }
        };

        customService.subscribe(userId);
        assertEquals(1, customService.getActiveEmitterCount(userId));

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockEmitter).onTimeout(captor.capture());

        // Kích hoạt callback onTimeout
        captor.getValue().run();

        verify(mockEmitter).complete();
        assertEquals(0, customService.getActiveEmitterCount(userId));
    }

    @Test
    @DisplayName("TC-56: stream_Success_OnErrorCallbackRemovesEmitter - Tự động dọn dẹp Emitter khi gặp sự cố mạng")
    void stream_Success_OnErrorCallbackRemovesEmitter() {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        NotificationStreamServiceImpl customService = new NotificationStreamServiceImpl() {
            @Override
            protected SseEmitter createEmitter(Long timeout) {
                return mockEmitter;
            }
        };

        customService.subscribe(userId);
        assertEquals(1, customService.getActiveEmitterCount(userId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Throwable>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(mockEmitter).onError(captor.capture());

        // Kích hoạt callback onError
        captor.getValue().accept(new IOException("Connection reset by peer"));

        assertEquals(0, customService.getActiveEmitterCount(userId));
    }

    @Test
    @DisplayName("TC-57: sendNotification_Success_BroadcastToUserTabs - Phát thông báo tới toàn bộ các tab của user")
    void sendNotification_Success_BroadcastToUserTabs() throws IOException {
        SseEmitter mockEmitter1 = mock(SseEmitter.class);
        SseEmitter mockEmitter2 = mock(SseEmitter.class);

        NotificationStreamServiceImpl customService = new NotificationStreamServiceImpl() {
            private int count = 0;
            @Override
            protected SseEmitter createEmitter(Long timeout) {
                return (count++ == 0) ? mockEmitter1 : mockEmitter2;
            }
        };

        customService.subscribe(userId);
        customService.subscribe(userId);

        customService.sendNotification(userId, sampleNotification);

        // Mỗi emitter được gọi 2 lần: 1 lần cho CONNECT event, 1 lần cho NOTIFICATION event
        verify(mockEmitter1, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter2, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("TC-58: sendNotification_Success_IgnoresDisconnectedTabAndContinues - Bỏ qua tab bị lỗi và tiếp tục gửi cho tab còn lại")
    void sendNotification_Success_IgnoresDisconnectedTabAndContinues() throws IOException {
        SseEmitter deadEmitter = mock(SseEmitter.class);
        SseEmitter aliveEmitter = mock(SseEmitter.class);

        // deadEmitter ném IOException khi gửi thông báo (sau khi đã send CONNECT thành công)
        doNothing().doThrow(new IOException("Broken pipe")).when(deadEmitter).send(any(SseEmitter.SseEventBuilder.class));
        doNothing().when(aliveEmitter).send(any(SseEmitter.SseEventBuilder.class));

        NotificationStreamServiceImpl customService = new NotificationStreamServiceImpl() {
            private int count = 0;
            @Override
            protected SseEmitter createEmitter(Long timeout) {
                return (count++ == 0) ? deadEmitter : aliveEmitter;
            }
        };

        customService.subscribe(userId);
        customService.subscribe(userId);
        assertEquals(2, customService.getActiveEmitterCount(userId));

        customService.sendNotification(userId, sampleNotification);

        // deadEmitter bị loại bỏ, aliveEmitter vẫn giữ
        assertEquals(1, customService.getActiveEmitterCount(userId));
    }

    @Test
    @DisplayName("TC-59: sendHeartbeat_Success_SendsPingToAllActiveEmitters - Gửi ping giữ kết nối tới tất cả emitter đang hoạt động")
    void sendHeartbeat_Success_SendsPingToAllActiveEmitters() throws IOException {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doNothing().when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        NotificationStreamServiceImpl customService = new NotificationStreamServiceImpl() {
            @Override
            protected SseEmitter createEmitter(Long timeout) {
                return mockEmitter;
            }
        };

        customService.subscribe(userId);

        assertDoesNotThrow(customService::sendHeartbeat);

        verify(mockEmitter, atLeast(2)).send(any(SseEmitter.SseEventBuilder.class)); // CONNECT + Heartbeat
    }

    @Test
    @DisplayName("TC-64: sendNotification_NoActiveEmitters_ReturnsSilently - Trả về êm dịu khi user không có kết nối SSE nào mở")
    void sendNotification_NoActiveEmitters_ReturnsSilently() {
        assertDoesNotThrow(() -> streamService.sendNotification(userId, sampleNotification));
    }

    @Test
    @DisplayName("TC-65: subscribe_NullUserId_ThrowsIllegalArgumentException - Ném exception khi userId null")
    void subscribe_NullUserId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> streamService.subscribe(null));
    }

    @Test
    @DisplayName("TC-66: sendNotification_NullPayloadOrUserId_ReturnsSilently - Trả về êm dịu khi tham số null")
    void sendNotification_NullPayloadOrUserId_ReturnsSilently() {
        assertDoesNotThrow(() -> streamService.sendNotification(null, sampleNotification));
        assertDoesNotThrow(() -> streamService.sendNotification(userId, null));
    }

    @Test
    @DisplayName("TC-67: sendNotification_HandlesIllegalStateExceptionGracefully - Bắt và xử lý IllegalStateException khi Emitter đã hoàn thành")
    void sendNotification_HandlesIllegalStateExceptionGracefully() throws IOException {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doNothing().when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        NotificationStreamServiceImpl customService = new NotificationStreamServiceImpl() {
            @Override
            protected SseEmitter createEmitter(Long timeout) {
                return mockEmitter;
            }
        };

        customService.subscribe(userId);
        assertEquals(1, customService.getActiveEmitterCount(userId));

        // Khi gửi thông báo thì emitter quăng IllegalStateException (do đã complete ở tầng servlet)
        doThrow(new IllegalStateException("ResponseBodyEmitter has already completed"))
                .when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        assertDoesNotThrow(() -> customService.sendNotification(userId, sampleNotification));

        // Emitter lỗi phải được tự động gỡ khỏi service
        assertEquals(0, customService.getActiveEmitterCount(userId));
    }

    @Test
    @DisplayName("TC-68: sendHeartbeat_HandlesIllegalStateExceptionGracefully - Bắt và xử lý IllegalStateException trong vòng lặp Heartbeat")
    void sendHeartbeat_HandlesIllegalStateExceptionGracefully() throws IOException {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doNothing().when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        NotificationStreamServiceImpl customService = new NotificationStreamServiceImpl() {
            @Override
            protected SseEmitter createEmitter(Long timeout) {
                return mockEmitter;
            }
        };

        customService.subscribe(userId);
        assertEquals(1, customService.getActiveEmitterCount(userId));

        // Khi gửi heartbeat thì emitter quăng IllegalStateException
        doThrow(new IllegalStateException("ResponseBodyEmitter has already completed"))
                .when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        assertDoesNotThrow(customService::sendHeartbeat);

        // Emitter lỗi phải được gỡ bỏ, vòng lặp không bị crash
        assertEquals(0, customService.getActiveEmitterCount(userId));
    }
}

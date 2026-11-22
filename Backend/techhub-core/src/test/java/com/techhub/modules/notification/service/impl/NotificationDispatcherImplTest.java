package com.techhub.modules.notification.service.impl;

import com.techhub.modules.notification.enums.NotificationType;
import com.techhub.modules.notification.event.NotificationDispatchEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho NotificationDispatcherImpl")
class NotificationDispatcherImplTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationDispatcherImpl dispatcher;

    @Test
    @DisplayName("dispatch - Phát sinh event hợp lệ thành công")
    void dispatch_Success() {
        UUID recipientId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();

        dispatcher.dispatch(
                recipientId,
                NotificationType.ORDER,
                "Đơn hàng mới",
                "Bạn có một đơn hàng mới",
                "ORDER",
                refId
        );

        ArgumentCaptor<NotificationDispatchEvent> captor = ArgumentCaptor.forClass(NotificationDispatchEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationDispatchEvent event = captor.getValue();
        assertEquals(recipientId, event.recipientUserId());
        assertEquals(NotificationType.ORDER, event.type());
        assertEquals("Đơn hàng mới", event.title());
        assertEquals("Bạn có một đơn hàng mới", event.content());
        assertEquals("ORDER", event.referenceType());
        assertEquals(refId, event.referenceId());
    }

    @Test
    @DisplayName("dispatch - Bỏ qua khi recipientUserId là null")
    void dispatch_NullRecipient_NoOp() {
        dispatcher.dispatch(
                null,
                NotificationType.ORDER,
                "Title",
                "Content",
                "ORDER",
                UUID.randomUUID()
        );

        verifyNoInteractions(eventPublisher);
    }
}

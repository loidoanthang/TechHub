package com.techhub.service.conversation.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.conversation.SendChatMessageRequest;
import com.techhub.model.dto.response.conversation.MessageResponse;
import com.techhub.model.entity.Conversation;
import com.techhub.model.entity.ConversationParticipant;
import com.techhub.model.entity.Message;
import com.techhub.model.entity.User;
import com.techhub.model.enums.ConversationType;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.ConversationParticipantRepository;
import com.techhub.repository.ConversationRepository;
import com.techhub.repository.MessageRepository;
import com.techhub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for ChatMessagingServiceImpl (WS STOMP /app/chat.send)")
class ChatMessagingServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private ChatMessagingServiceImpl chatMessagingService;

    private UUID senderId;
    private UUID opponentId;
    private UUID conversationId;
    private User sender;
    private User opponent;
    private Conversation conversation;
    private ConversationParticipant senderParticipant;
    private ConversationParticipant opponentParticipant;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        opponentId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        sender = new User();
        sender.setId(senderId);
        sender.setEmail("buyer@techhub.com");
        sender.setFirstName("John");
        sender.setLastName("Doe");
        sender.setAvatarUrl("https://cdn.techhub.com/john.png");
        sender.setStatus(UserStatus.ACTIVE);
        sender.setEmailVerified(true);
        sender.setRoles(new HashSet<>(Set.of(Role.BUYER)));

        opponent = new User();
        opponent.setId(opponentId);
        opponent.setEmail("seller@techhub.com");
        opponent.setFirstName("TechZone");
        opponent.setLastName("Store");
        opponent.setAvatarUrl("https://cdn.techhub.com/shop.png");
        opponent.setStatus(UserStatus.ACTIVE);
        opponent.setEmailVerified(true);
        opponent.setRoles(new HashSet<>(Set.of(Role.SELLER)));

        conversation = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.BUYER_SELLER)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        senderParticipant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .user(sender)
                .isDeleted(false)
                .lastReadAt(LocalDateTime.now().minusHours(2))
                .build();

        opponentParticipant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .user(opponent)
                .isDeleted(false)
                .lastReadAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("TC-CHAT-01: Send Message Successfully (Happy Path)")
    void testSendMessageSuccessfully() {
        TransactionSynchronizationManager.initSynchronization();

        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Shop còn hàng không ạ?");
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(conversationParticipantRepository.findByConversationIdAndUserId(conversationId, senderId))
                .thenReturn(Optional.of(senderParticipant));
        when(conversationParticipantRepository.findByConversationId(conversationId))
                .thenReturn(List.of(senderParticipant, opponentParticipant));

        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .sender(sender)
                .content("Shop còn hàng không ạ?")
                .createdAt(LocalDateTime.now())
                .build();
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        MessageResponse response = chatMessagingService.sendChatMessage(senderId, request);

        assertNotNull(response);
        assertEquals(savedMessage.getId(), response.id());
        assertEquals(conversationId, response.conversationId());
        assertEquals(senderId, response.senderId());
        assertEquals("John Doe", response.senderName());
        assertEquals("https://cdn.techhub.com/john.png", response.senderAvatar());
        assertEquals("Shop còn hàng không ạ?", response.content());
        assertFalse(response.isMine());

        // Verify conversation updatedAt updated
        verify(conversationRepository).save(conversation);
        assertEquals(savedMessage.getCreatedAt(), conversation.getUpdatedAt());

        // Verify sender lastReadAt updated
        verify(conversationParticipantRepository).save(senderParticipant);
        assertEquals(savedMessage.getCreatedAt(), senderParticipant.getLastReadAt());

        // Verify broadcast triggered after commit
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(simpMessagingTemplate, times(1)).convertAndSend(
                eq("/topic/conversation." + conversationId),
                eq(response)
        );
    }

    @Test
    @DisplayName("TC-CHAT-02: Auto-Reactivate Soft-Deleted Opponent")
    void testAutoReactivateSoftDeletedOpponent() {
        opponentParticipant.setDeleted(true);
        opponentParticipant.setDeletedAt(LocalDateTime.now().minusHours(1));

        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Tin nhắn mới sau khi xóa");
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(conversationParticipantRepository.findByConversationIdAndUserId(conversationId, senderId))
                .thenReturn(Optional.of(senderParticipant));
        when(conversationParticipantRepository.findByConversationId(conversationId))
                .thenReturn(List.of(senderParticipant, opponentParticipant));

        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .sender(sender)
                .content("Tin nhắn mới sau khi xóa")
                .createdAt(LocalDateTime.now())
                .build();
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        MessageResponse response = chatMessagingService.sendChatMessage(senderId, request);

        assertNotNull(response);
        assertFalse(opponentParticipant.isDeleted());
        assertNull(opponentParticipant.getDeletedAt());
        verify(conversationParticipantRepository).save(opponentParticipant);
    }

    @Test
    @DisplayName("TC-CHAT-03: Update Sender's lastReadAt on Message Send")
    void testUpdateSenderLastReadAtOnSend() {
        LocalDateTime oldLastRead = senderParticipant.getLastReadAt();
        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Tin nhắn kiểm tra lastReadAt");
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(conversationParticipantRepository.findByConversationIdAndUserId(conversationId, senderId))
                .thenReturn(Optional.of(senderParticipant));
        when(conversationParticipantRepository.findByConversationId(conversationId))
                .thenReturn(List.of(senderParticipant, opponentParticipant));

        LocalDateTime now = LocalDateTime.now();
        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .sender(sender)
                .content("Tin nhắn kiểm tra lastReadAt")
                .createdAt(now)
                .build();
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        chatMessagingService.sendChatMessage(senderId, request);

        assertNotEquals(oldLastRead, senderParticipant.getLastReadAt());
        assertEquals(now, senderParticipant.getLastReadAt());
        verify(conversationParticipantRepository).save(senderParticipant);
    }

    @Test
    @DisplayName("TC-CHAT-04: Reject Unauthorized Sender (Anti-IDOR Protection)")
    void testRejectUnauthorizedSenderIdor() {
        UUID strangerId = UUID.randomUUID();
        User stranger = new User();
        stranger.setId(strangerId);
        stranger.setStatus(UserStatus.ACTIVE);
        stranger.setRoles(new HashSet<>(Set.of(Role.BUYER)));

        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Tin nhắn tấn công IDOR");

        when(userRepository.findById(strangerId)).thenReturn(Optional.of(stranger));
        when(conversationParticipantRepository.findByConversationIdAndUserId(conversationId, strangerId))
                .thenReturn(Optional.empty());
        when(conversationRepository.existsById(conversationId)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                chatMessagingService.sendChatMessage(strangerId, request)
        );
        assertEquals(ErrorCode.NOT_CONVERSATION_PARTICIPANT, ex.getErrorCode());
        verify(messageRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("TC-CHAT-05: Reject Inactive / Banned Sender")
    void testRejectInactiveSender() {
        sender.setStatus(UserStatus.BANNED);
        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Tin nhắn spam từ user bị ban");
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                chatMessagingService.sendChatMessage(senderId, request)
        );
        assertEquals(ErrorCode.USER_NOT_ACTIVE, ex.getErrorCode());
        verify(messageRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("TC-CHAT-09: Prevent Ghost Message on Database Transaction Rollback")
    void testPreventGhostMessageOnRollback() {
        TransactionSynchronizationManager.initSynchronization();

        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Tin nhắn bị lỗi DB");
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(conversationParticipantRepository.findByConversationIdAndUserId(conversationId, senderId))
                .thenReturn(Optional.of(senderParticipant));
        when(messageRepository.save(any(Message.class))).thenThrow(new RuntimeException("PostgreSQL Connection Terminated"));

        assertThrows(RuntimeException.class, () ->
                chatMessagingService.sendChatMessage(senderId, request)
        );

        // Transaction failed, afterCommit is NEVER triggered
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("TC-CHAT-11: Fallback Sender Name to Email when Name Empty")
    void testFallbackSenderNameToEmailWhenNameEmpty() {
        sender.setFirstName("");
        sender.setLastName(null);

        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Tin nhắn khi không có họ tên");
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(conversationParticipantRepository.findByConversationIdAndUserId(conversationId, senderId))
                .thenReturn(Optional.of(senderParticipant));
        when(conversationParticipantRepository.findByConversationId(conversationId))
                .thenReturn(List.of(senderParticipant, opponentParticipant));

        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .sender(sender)
                .content("Tin nhắn khi không có họ tên")
                .createdAt(LocalDateTime.now())
                .build();
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        MessageResponse response = chatMessagingService.sendChatMessage(senderId, request);

        assertEquals("buyer@techhub.com", response.senderName());
    }

    @Test
    @DisplayName("TC-CHAT-12: Verify @Transactional Annotation on Service method")
    void testVerifyTransactionalAnnotation() throws NoSuchMethodException {
        Method method = ChatMessagingServiceImpl.class.getMethod("sendChatMessage", UUID.class, SendChatMessageRequest.class);
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertNotNull(annotation, "sendChatMessage method must be annotated with @Transactional");
        assertEquals(Exception.class, annotation.rollbackFor()[0]);
    }
}

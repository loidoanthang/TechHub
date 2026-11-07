package com.techhub.controller.conversation;

import com.techhub.common.ApiResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.conversation.SendChatMessageRequest;
import com.techhub.model.dto.response.conversation.MessageResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.conversation.ChatMessagingService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.lang.reflect.Method;
import java.security.Principal;
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
@DisplayName("Unit Tests for ChatController (WS STOMP /app/chat.send)")
class ChatControllerTest {

    @Mock
    private ChatMessagingService chatMessagingService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatController chatController;

    private Validator validator;
    private UUID currentUserId;
    private UUID conversationId;
    private User currentUser;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        currentUserId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("buyer@techhub.com");
        currentUser.setFirstName("John");
        currentUser.setLastName("Doe");
        currentUser.setStatus(UserStatus.ACTIVE);
        currentUser.setEmailVerified(true);
        currentUser.setRoles(new HashSet<>(Set.of(Role.BUYER)));
    }

    @Test
    @DisplayName("TC-CHAT-06: Reject Blank Content ('   ')")
    void testRejectBlankContent() {
        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "   ");
        Set<ConstraintViolation<SendChatMessageRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Blank content should trigger validation constraint violation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be blank")));
    }

    @Test
    @DisplayName("TC-CHAT-07: Reject Content Exceeding 2000 Characters")
    void testRejectContentExceeding2000Characters() {
        String longContent = "A".repeat(2001);
        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, longContent);
        Set<ConstraintViolation<SendChatMessageRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Content > 2000 chars should trigger constraint violation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 2000 characters")));
    }

    @Test
    @DisplayName("TC-CHAT-08: Reject Null Conversation ID")
    void testRejectNullConversationId() {
        SendChatMessageRequest request = new SendChatMessageRequest(null, "Tin nhắn hợp lệ");
        Set<ConstraintViolation<SendChatMessageRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Null conversationId should trigger constraint violation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be null")));
    }

    @Test
    @DisplayName("TC-CHAT-10: Controller @MessageMapping Delegation & Principal Extraction (CustomUserDetails)")
    void testControllerDelegationWithCustomUserDetails() {
        CustomUserDetails userDetails = new CustomUserDetails(currentUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Shop còn hàng không ạ?");
        MessageResponse mockResponse = new MessageResponse(
                UUID.randomUUID(), conversationId, currentUserId, "John Doe", null, "Shop còn hàng không ạ?", LocalDateTime.now(), false
        );
        when(chatMessagingService.sendChatMessage(eq(currentUserId), eq(request))).thenReturn(mockResponse);

        assertDoesNotThrow(() -> chatController.sendChatMessage(authentication, request));
        verify(chatMessagingService, times(1)).sendChatMessage(eq(currentUserId), eq(request));
    }

    @Test
    @DisplayName("TC-CHAT-10b: Controller Extraction with UUID String Principal")
    void testControllerDelegationWithUuidStringPrincipal() {
        Principal mockPrincipal = () -> currentUserId.toString();

        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Tin nhắn với UUID principal");
        MessageResponse mockResponse = new MessageResponse(
                UUID.randomUUID(), conversationId, currentUserId, "John Doe", null, "Tin nhắn với UUID principal", LocalDateTime.now(), false
        );
        when(chatMessagingService.sendChatMessage(eq(currentUserId), eq(request))).thenReturn(mockResponse);

        assertDoesNotThrow(() -> chatController.sendChatMessage(mockPrincipal, request));
        verify(chatMessagingService, times(1)).sendChatMessage(eq(currentUserId), eq(request));
    }

    @Test
    @DisplayName("TC-CHAT-10c: Controller Extraction with Email Principal Fallback")
    void testControllerDelegationWithEmailPrincipalFallback() {
        Principal mockPrincipal = () -> "buyer@techhub.com";
        when(userRepository.findByEmail("buyer@techhub.com")).thenReturn(Optional.of(currentUser));

        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Tin nhắn với Email principal");
        MessageResponse mockResponse = new MessageResponse(
                UUID.randomUUID(), conversationId, currentUserId, "John Doe", null, "Tin nhắn với Email principal", LocalDateTime.now(), false
        );
        when(chatMessagingService.sendChatMessage(eq(currentUserId), eq(request))).thenReturn(mockResponse);

        assertDoesNotThrow(() -> chatController.sendChatMessage(mockPrincipal, request));
        verify(chatMessagingService, times(1)).sendChatMessage(eq(currentUserId), eq(request));
    }

    @Test
    @DisplayName("TC-CHAT-10d: Reject Null Principal")
    void testRejectNullPrincipal() {
        SendChatMessageRequest request = new SendChatMessageRequest(conversationId, "Tin nhắn không có auth");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                chatController.sendChatMessage(null, request)
        );
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verify(chatMessagingService, never()).sendChatMessage(any(), any());
    }

    @Test
    @DisplayName("TC-CHAT-10e: Reject Null Request Payload")
    void testRejectNullRequest() {
        Principal mockPrincipal = () -> currentUserId.toString();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                chatController.sendChatMessage(mockPrincipal, null)
        );
        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
        verify(chatMessagingService, never()).sendChatMessage(any(), any());
    }

    @Test
    @DisplayName("TC-CHAT-13: Verify @MessageMapping Annotation on sendChatMessage method")
    void testVerifyMessageMappingAnnotation() throws NoSuchMethodException {
        Method method = ChatController.class.getMethod("sendChatMessage", Principal.class, SendChatMessageRequest.class);
        MessageMapping annotation = method.getAnnotation(MessageMapping.class);

        assertNotNull(annotation, "sendChatMessage method must be annotated with @MessageMapping");
        assertArrayEquals(new String[]{"/chat.send"}, annotation.value());
    }

    @Test
    @DisplayName("TC-CHAT-17: Handle BusinessException via @MessageExceptionHandler")
    void testHandleBusinessException() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_CONVERSATION_PARTICIPANT);
        ApiResponse<?> response = chatController.handleBusinessException(ex);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("You are not a participant in this conversation", response.getMessage());
    }

    @Test
    @DisplayName("TC-CHAT-18: Handle MethodArgumentNotValidException via @MessageExceptionHandler")
    void testHandleValidationException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(
                new FieldError("sendChatMessageRequest", "content", "Message content must not be blank")
        ));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ApiResponse<?> response = chatController.handleValidationException(ex);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Message content must not be blank", response.getMessage());
    }
}

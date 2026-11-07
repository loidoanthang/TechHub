package com.techhub.security.websocket;

import com.techhub.model.entity.Conversation;
import com.techhub.model.entity.ConversationParticipant;
import com.techhub.model.entity.User;
import com.techhub.model.enums.ConversationType;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.ConversationParticipantRepository;
import com.techhub.repository.ConversationRepository;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.security.CustomUserDetailsService;
import com.techhub.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for AuthChannelInterceptor (CONNECT & SUBSCRIBE /topic/conversation.{id})")
class AuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private AuthChannelInterceptor interceptor;

    private User user;
    private CustomUserDetails userDetails;
    private UsernamePasswordAuthenticationToken authentication;
    private UUID conversationId;
    private Conversation conversation;
    private ConversationParticipant participant;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("buyer@techhub.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setRoles(new HashSet<>(Set.of(Role.BUYER)));

        userDetails = new CustomUserDetails(user);
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        conversationId = UUID.randomUUID();
        conversation = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.BUYER_SELLER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        participant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .user(user)
                .isDeleted(false)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    // ==================== CONNECT FRAME TESTS ====================

    @Test
    @DisplayName("STOMP CONNECT with valid Bearer token sets Authentication in accessor")
    void testConnectWithValidBearerToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("Authorization", "Bearer valid.jwt.token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.extractEmail("valid.jwt.token")).thenReturn("buyer@techhub.com");
        when(userDetailsService.loadUserByUsername("buyer@techhub.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.jwt.token", userDetails)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertTrue(resultAccessor.getUser() instanceof Authentication);
        assertEquals("buyer@techhub.com", resultAccessor.getUser().getName());
    }

    @Test
    @DisplayName("STOMP CONNECT with lowercase authorization header sets Authentication")
    void testConnectWithLowercaseAuthorizationHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("authorization", "Bearer lowercase.token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.extractEmail("lowercase.token")).thenReturn("buyer@techhub.com");
        when(userDetailsService.loadUserByUsername("buyer@techhub.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("lowercase.token", userDetails)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
    }

    @Test
    @DisplayName("STOMP CONNECT with passcode header sets Authentication")
    void testConnectWithPasscodeHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("passcode", "valid.jwt.token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.extractEmail("valid.jwt.token")).thenReturn("buyer@techhub.com");
        when(userDetailsService.loadUserByUsername("buyer@techhub.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.jwt.token", userDetails)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
    }

    @Test
    @DisplayName("STOMP non-CONNECT frame is passed through without processing")
    void testNonConnectFramePassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // ==================== SUBSCRIBE FRAME TESTS (/topic/conversation.{id}) ====================

    @Test
    @DisplayName("TC-SUB-01: Subscribe to Own Conversation Successfully (Happy Path)")
    void testSubscribeToOwnConversationSuccessfully() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/conversation." + conversationId);
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(conversationParticipantRepository.findByConversationIdAndUserId(conversationId, user.getId()))
                .thenReturn(Optional.of(participant));

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        verify(conversationParticipantRepository).findByConversationIdAndUserId(conversationId, user.getId());
    }

    @Test
    @DisplayName("TC-SUB-02: Reject Subscription from Unauthorized User (Anti-Eavesdropping IDOR Guard)")
    void testRejectSubscriptionFromUnauthorizedUser() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/conversation." + conversationId);
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(conversationParticipantRepository.findByConversationIdAndUserId(conversationId, user.getId()))
                .thenReturn(Optional.empty());
        when(conversationRepository.existsById(conversationId)).thenReturn(true);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertEquals("Access denied: You are not a participant in this conversation", ex.getMessage());
    }

    @Test
    @DisplayName("TC-SUB-03: Reject Subscription to Non-Existent Conversation ID")
    void testRejectSubscriptionToNonExistentConversationId() {
        UUID nonExistentConvId = UUID.randomUUID();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/conversation." + nonExistentConvId);
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(conversationParticipantRepository.findByConversationIdAndUserId(nonExistentConvId, user.getId()))
                .thenReturn(Optional.empty());
        when(conversationRepository.existsById(nonExistentConvId)).thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertEquals("Conversation not found", ex.getMessage());
    }

    @Test
    @DisplayName("TC-SUB-04: Reject Subscription with Malformed UUID in Destination")
    void testRejectSubscriptionWithMalformedUuid() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/conversation.invalid-uuid-abc");
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertEquals("Invalid conversation ID format in destination", ex.getMessage());
        verifyNoInteractions(conversationParticipantRepository);
    }

    @Test
    @DisplayName("TC-SUB-05: Reject Unauthenticated Subscription Frame (Null Principal)")
    void testRejectUnauthenticatedSubscription() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/conversation." + conversationId);
        accessor.setUser(null);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertEquals("Unauthenticated STOMP subscription", ex.getMessage());
        verifyNoInteractions(conversationParticipantRepository);
    }

    @Test
    @DisplayName("TC-SUB-06: Reject Inactive / Banned User from Subscribing")
    void testRejectInactiveUserFromSubscribing() {
        user.setStatus(UserStatus.BANNED);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/conversation." + conversationId);
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertEquals("User account is not active", ex.getMessage());
        verifyNoInteractions(conversationParticipantRepository);
    }

    @Test
    @DisplayName("TC-SUB-07: Allow Re-subscription for Soft-Deleted Conversation")
    void testAllowResubscriptionForSoftDeletedConversation() {
        participant.setDeleted(true);
        participant.setDeletedAt(LocalDateTime.now().minusHours(1));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/conversation." + conversationId);
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(conversationParticipantRepository.findByConversationIdAndUserId(conversationId, user.getId()))
                .thenReturn(Optional.of(participant));

        assertDoesNotThrow(() -> interceptor.preSend(message, messageChannel));
        verify(conversationParticipantRepository).findByConversationIdAndUserId(conversationId, user.getId());
    }

    @Test
    @DisplayName("TC-SUB-08: Non-Conversation Topics Pass-Through Unchecked")
    void testNonConversationTopicsPassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/public-announcements");
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        verifyNoInteractions(conversationParticipantRepository, conversationRepository);
    }

    @Test
    @DisplayName("TC-SUB-09: Unsubscribe Frame Handling Cleanly")
    void testUnsubscribeFrameHandling() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("id", "sub-0");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        verifyNoInteractions(conversationParticipantRepository, conversationRepository);
    }

    @Test
    @DisplayName("TC-SUB-10: Reject Subscription with Slash Destination Pattern (/topic/conversation/...)")
    void testRejectSubscriptionWithSlashDestinationPattern() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/conversation/" + conversationId);
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertEquals("Invalid conversation destination format: must follow '/topic/conversation.{id}'", ex.getMessage());
        verifyNoInteractions(conversationParticipantRepository);
    }

    @Test
    @DisplayName("TC-SEND-01: Reject Direct Client SEND to Broker Topic (/topic/conversation.{id})")
    void testRejectDirectClientSendToBrokerTopic() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/topic/conversation." + conversationId);
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        assertEquals("Direct publishing to broker destinations is not permitted", ex.getMessage());
        verifyNoInteractions(conversationParticipantRepository);
    }

    @Test
    @DisplayName("TC-SEND-02: Allow Client SEND to Application Destination (/app/chat.send)")
    void testAllowClientSendToApplicationDestination() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/app/chat.send");
        accessor.setUser(authentication);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        verifyNoInteractions(conversationParticipantRepository, conversationRepository);
    }
}

package com.techhub.security.websocket;

import com.techhub.model.entity.User;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.ConversationParticipantRepository;
import com.techhub.repository.ConversationRepository;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.security.CustomUserDetailsService;
import com.techhub.service.auth.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                handleConnect(accessor, message);
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                validateSubscription(accessor);
            } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                validateSendDestination(accessor);
            }
        }
        return message;
    }

    private void validateSendDestination(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination != null && (destination.startsWith("/topic/") || destination.startsWith("/queue/"))) {
            log.warn("Blocked direct client SEND to broker destination: [{}]", destination);
            throw new AccessDeniedException("Direct publishing to broker destinations is not permitted");
        }
    }

    private void handleConnect(StompHeaderAccessor accessor, Message<?> message) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            authHeader = accessor.getFirstNativeHeader("authorization");
        }
        if (authHeader == null || authHeader.isBlank()) {
            authHeader = accessor.getFirstNativeHeader("passcode");
        }

        if (authHeader != null && !authHeader.isBlank()) {
            String jwt = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            try {
                String email = jwtService.extractEmail(jwt);
                if (email != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        if (accessor.isMutable()) {
                            accessor.setUser(authentication);
                        } else {
                            StompHeaderAccessor mutableAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
                            mutableAccessor.copyHeaders(accessor.toMap());
                            mutableAccessor.setUser(authentication);
                        }
                        log.debug("WebSocket STOMP authenticated user [{}]", email);
                    }
                }
            } catch (JwtException ex) {
                log.warn("Invalid JWT token on STOMP CONNECT: {}", ex.getMessage());
            } catch (Exception ex) {
                log.error("Failed to authenticate user on STOMP CONNECT", ex);
            }
        }
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        if (destination.startsWith("/topic/conversation") && !destination.startsWith("/topic/conversation.")) {
            log.warn("Invalid conversation destination pattern: [{}]", destination);
            throw new AccessDeniedException("Invalid conversation destination format: must follow '/topic/conversation.{id}'");
        }

        if (!destination.startsWith("/topic/conversation.")) {
            // Non-conversation topic, pass-through without interception
            return;
        }

        String convIdStr = destination.substring("/topic/conversation.".length());
        UUID conversationId;
        try {
            conversationId = UUID.fromString(convIdStr);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid conversation ID format in destination: [{}]", destination);
            throw new AccessDeniedException("Invalid conversation ID format in destination");
        }

        Principal principal = accessor.getUser();
        if (principal == null) {
            log.warn("Unauthenticated STOMP subscription attempt to destination: [{}]", destination);
            throw new AccessDeniedException("Unauthenticated STOMP subscription");
        }

        UUID currentUserId = extractUserId(principal);

        // Kiểm tra quyền thành viên trong CSDL (Anti-IDOR Guard)
        boolean isParticipant = conversationParticipantRepository
                .findByConversationIdAndUserId(conversationId, currentUserId)
                .isPresent();

        if (!isParticipant) {
            if (conversationRepository.existsById(conversationId)) {
                log.warn("Subscription IDOR attempt: User [{}] is not a participant in conversation [{}]",
                        currentUserId, conversationId);
                throw new AccessDeniedException("Access denied: You are not a participant in this conversation");
            }
            throw new AccessDeniedException("Conversation not found");
        }

        log.debug("User [{}] successfully authorized to subscribe to conversation [{}]", currentUserId, conversationId);
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails cud) {
            if (cud.user().getStatus() != UserStatus.ACTIVE) {
                throw new AccessDeniedException("User account is not active");
            }
            return cud.user().getId();
        }

        UUID userId;
        try {
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            userId = userRepository.findByEmail(principal.getName())
                    .map(User::getId)
                    .orElseThrow(() -> new AccessDeniedException("User not found"));
        }

        userRepository.findById(userId).ifPresent(u -> {
            if (u.getStatus() != UserStatus.ACTIVE) {
                throw new AccessDeniedException("User account is not active");
            }
        });

        return userId;
    }
}

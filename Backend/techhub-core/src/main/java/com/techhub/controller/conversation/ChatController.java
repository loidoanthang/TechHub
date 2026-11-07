package com.techhub.controller.conversation;

import com.techhub.common.ApiResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.conversation.SendChatMessageRequest;
import com.techhub.model.entity.User;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.conversation.ChatMessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatMessagingService chatMessagingService;
    private final UserRepository userRepository;

    @MessageMapping("/chat.send")
    public void sendChatMessage(Principal principal, @Valid @Payload SendChatMessageRequest request) {
        if (request == null) {
            log.warn("Received null payload on /chat.send from principal [{}]",
                    principal != null ? principal.getName() : "null");
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        log.info("Received STOMP chat.send frame from principal [{}] for conversation [{}]",
                principal != null ? principal.getName() : "null",
                request.conversationId());

        UUID senderId = extractSenderId(principal);
        chatMessagingService.sendChatMessage(senderId, request);
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public ApiResponse<?> handleBusinessException(BusinessException ex) {
        log.warn("WebSocket STOMP BusinessException: {}", ex.getMessage());
        return ApiResponse.error(ex.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        log.warn("WebSocket STOMP validation error: {}", errorMsg);
        return ApiResponse.error(errorMsg);
    }

    private UUID extractSenderId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.user().getId();
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            return userRepository.findByEmail(principal.getName())
                    .map(User::getId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        }
    }
}

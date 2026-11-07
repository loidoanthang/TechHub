package com.techhub.service.conversation.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.conversation.SendChatMessageRequest;
import com.techhub.model.dto.response.conversation.MessageResponse;
import com.techhub.model.entity.Conversation;
import com.techhub.model.entity.ConversationParticipant;
import com.techhub.model.entity.Message;
import com.techhub.model.entity.User;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.ConversationParticipantRepository;
import com.techhub.repository.ConversationRepository;
import com.techhub.repository.MessageRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.conversation.ChatMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessagingServiceImpl implements ChatMessagingService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse sendChatMessage(UUID senderId, SendChatMessageRequest request) {
        if (request == null || request.conversationId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        log.info("sendChatMessage requested by user [{}] for conversation [{}]", senderId, request.conversationId());

        // 1. Kiểm tra tài khoản người gửi (Active Guard)
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        if (sender.getStatus() != UserStatus.ACTIVE) {
            log.warn("User [{}] attempted to send chat message with status [{}]", senderId, sender.getStatus());
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }

        // 2. Kiểm tra tư cách thành viên phòng chat (Anti-IDOR Guard)
        ConversationParticipant senderParticipant = conversationParticipantRepository
                .findByConversationIdAndUserId(request.conversationId(), senderId)
                .orElseThrow(() -> {
                    if (conversationRepository.existsById(request.conversationId())) {
                        log.warn("IDOR attempt: User [{}] is not a participant in conversation [{}]", senderId, request.conversationId());
                        return new BusinessException(ErrorCode.NOT_CONVERSATION_PARTICIPANT);
                    }
                    return new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
                });

        Conversation conversation = senderParticipant.getConversation();

        // 3. Lưu tin nhắn vào CSDL
        String sanitizedContent = request.content() != null ? request.content().trim() : "";
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(sanitizedContent)
                .build();
        message = messageRepository.save(message);

        LocalDateTime now = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();

        // 4. Cập nhật thời gian hội thoại
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);

        // 5. Cập nhật lastReadAt và trạng thái active của người gửi
        senderParticipant.setLastReadAt(now);
        if (senderParticipant.isDeleted()) {
            senderParticipant.setDeleted(false);
            senderParticipant.setDeletedAt(null);
        }
        conversationParticipantRepository.save(senderParticipant);

        // 6. Tự động Hồi sinh đối phương nếu từng xóa mềm (Auto-Reactivation per BR-21.2 & BR-21.4)
        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversationId(request.conversationId());
        for (ConversationParticipant p : participants) {
            if (!p.getUser().getId().equals(senderId) && p.isDeleted()) {
                p.setDeleted(false);
                p.setDeletedAt(null);
                conversationParticipantRepository.save(p);
                log.info("Auto-reactivated opponent [{}] in conversation [{}]", p.getUser().getId(), request.conversationId());
            }
        }

        // 7. Tạo MessageResponse DTO
        String senderName = ((sender.getFirstName() != null ? sender.getFirstName() : "") + " "
                + (sender.getLastName() != null ? sender.getLastName() : "")).trim();
        if (senderName.isEmpty()) {
            senderName = sender.getEmail();
        }

        MessageResponse response = new MessageResponse(
                message.getId(),
                request.conversationId(),
                sender.getId(),
                senderName,
                sender.getAvatarUrl(),
                message.getContent(),
                now,
                false
        );

        // 8. Phát sóng WebSocket SAU KHI Transaction Commit (Ghost Message Prevention per TC-21.2)
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        simpMessagingTemplate.convertAndSend("/topic/conversation." + request.conversationId(), response);
                        log.info("Broadcasted chat message [{}] to destination /topic/conversation.{}",
                                response.id(), request.conversationId());
                    } catch (Exception ex) {
                        log.error("Failed to broadcast chat message [{}] to destination /topic/conversation.{}",
                                response.id(), request.conversationId(), ex);
                    }
                }
            });
        } else {
            try {
                simpMessagingTemplate.convertAndSend("/topic/conversation." + request.conversationId(), response);
                log.info("Broadcasted chat message [{}] directly (no active tx)", response.id());
            } catch (Exception ex) {
                log.error("Failed to broadcast chat message [{}] directly", response.id(), ex);
            }
        }

        return response;
    }
}

package com.techhub.service.conversation.impl;

import com.techhub.common.CursorPageResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.conversation.CreateConversationRequest;
import com.techhub.model.dto.response.conversation.ConversationResponse;
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
import com.techhub.service.conversation.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    private static final int MAX_SNIPPET_LENGTH = 100;

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public ConversationResponse createOrGetConversation(UUID currentUserId, CreateConversationRequest request) {
        log.info("createOrGetConversation requested by user [{}] with target [{}] and type [{}]",
                currentUserId, request.targetUserId(), request.type());

        // 1. Chặn tự chat với chính mình (Self-Chat Prevention)
        if (currentUserId.equals(request.targetUserId())) {
            throw new BusinessException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        // 2. Kiểm tra tài khoản người gọi và đối phương
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }

        User targetUser = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }

        // 3. Kiểm tra tính tương thích giữa vai trò và loại hình hội thoại
        validateConversationTypeRoles(currentUser, targetUser, request.type());

        // 4. Đồng bộ hóa theo cặp User (Canonical Lock Key) để bảo toàn tuyệt đối BR-21.1 Single Conversation Invariant
        String lockKey = getCanonicalLockKey(currentUserId, targetUser.getId());
        synchronized (lockKey) {
            return transactionTemplate.execute(status -> executeGetOrCreate(currentUserId, currentUser, targetUser, request));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getUserConversations(UUID currentUserId, ConversationType type, Pageable pageable) {
        log.info("getUserConversations requested by user [{}] with type [{}] and pageable [{}]", currentUserId, type, pageable);

        Page<Conversation> convPage = conversationRepository.findUserConversations(currentUserId, type, pageable);

        if (convPage.isEmpty()) {
            return PageResponse.from(convPage, Collections.emptyList());
        }

        List<UUID> convIds = convPage.getContent().stream().map(Conversation::getId).toList();

        // 1. Batch fetch opponents (Prevent N+1)
        List<ConversationParticipant> opponents = conversationParticipantRepository.findOpponentsInConversations(convIds, currentUserId);
        Map<UUID, User> opponentMap = opponents.stream()
                .filter(p -> p.getConversation() != null && p.getUser() != null)
                .collect(Collectors.toMap(
                        p -> p.getConversation().getId(),
                        ConversationParticipant::getUser,
                        (u1, u2) -> u1
                ));

        // 2. Batch fetch latest messages (Prevent N+1)
        List<Message> latestMessages = messageRepository.findLatestMessagesByConversationIds(convIds);
        Map<UUID, Message> latestMessageMap = latestMessages.stream()
                .filter(m -> m.getConversation() != null)
                .collect(Collectors.toMap(
                        m -> m.getConversation().getId(),
                        m -> m,
                        (m1, m2) -> m1
                ));

        // 3. Batch fetch unread counts (Prevent N+1)
        List<Object[]> unreadCounts = messageRepository.countUnreadMessagesByConversationIds(convIds, currentUserId);
        Map<UUID, Long> unreadMap = unreadCounts.stream()
                .filter(r -> r.length >= 2 && r[0] != null && r[1] != null)
                .collect(Collectors.toMap(
                        r -> (UUID) r[0],
                        r -> ((Number) r[1]).longValue(),
                        (c1, c2) -> c1
                ));

        // 4. Map DTO List
        List<ConversationResponse> dtoList = convPage.getContent().stream().map(conv -> {
            User opponent = opponentMap.get(conv.getId());
            if (opponent == null) {
                opponent = new User();
                opponent.setId(UUID.randomUUID());
                opponent.setFirstName("Người dùng");
                opponent.setLastName("TechHub");
                opponent.setEmail("user@techhub.com");
                opponent.setRoles(Set.of(Role.BUYER));
            }

            Message latestMsg = latestMessageMap.get(conv.getId());
            String snippet = latestMsg != null ? truncateSnippet(latestMsg.getContent()) : null;
            LocalDateTime messageTime = latestMsg != null ? latestMsg.getCreatedAt() : null;
            long unread = unreadMap.getOrDefault(conv.getId(), 0L);

            return buildConversationResponse(conv, opponent, snippet, messageTime, unread);
        }).toList();

        return PageResponse.from(convPage, dtoList);
    }

    @Override
    @Transactional
    public CursorPageResponse<MessageResponse> getMessageHistory(
            UUID currentUserId,
            UUID conversationId,
            LocalDateTime beforeTime,
            int limit
    ) {
        log.info("getMessageHistory requested by user [{}] for conversation [{}] with beforeTime [{}] and limit [{}]",
                currentUserId, conversationId, beforeTime, limit);

        // 1. Kiểm tra tư cách thành viên và tồn tại hội thoại (Anti-IDOR Guard)
        ConversationParticipant participant = conversationParticipantRepository
                .findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> {
                    if (!conversationRepository.existsById(conversationId)) {
                        return new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
                    }
                    return new BusinessException(ErrorCode.NOT_CONVERSATION_PARTICIPANT);
                });

        // 2. Tự động đánh dấu đã đọc (Auto Mark as Read) khi mở phòng chat lần đầu (beforeTime == null)
        if (beforeTime == null) {
            participant.setLastReadAt(LocalDateTime.now());
            conversationParticipantRepository.save(participant);
            log.info("Updated lastReadAt for user [{}] in conversation [{}]", currentUserId, conversationId);
        }

        // 3. Truy vấn tin nhắn theo Cursor (lấy limit + 1 để xác định hasMore)
        List<Message> messages = messageRepository.findMessagesBefore(
                conversationId,
                beforeTime,
                PageRequest.of(0, limit + 1)
        );

        boolean hasMore = messages.size() > limit;
        List<Message> contentList = hasMore ? messages.subList(0, limit) : messages;

        LocalDateTime nextCursor = contentList.isEmpty()
                ? null
                : contentList.get(contentList.size() - 1).getCreatedAt();

        // 4. Ánh xạ sang List<MessageResponse>
        List<MessageResponse> dtoList = contentList.stream().map(m -> {
            User sender = m.getSender();
            String senderName = sender != null
                    ? ((sender.getFirstName() != null ? sender.getFirstName() : "") + " "
                    + (sender.getLastName() != null ? sender.getLastName() : "")).trim()
                    : "";
            if (senderName.isEmpty() && sender != null) {
                senderName = sender.getEmail();
            }
            String senderAvatar = sender != null ? sender.getAvatarUrl() : null;
            boolean isMine = sender != null && currentUserId.equals(sender.getId());

            return new MessageResponse(
                    m.getId(),
                    conversationId,
                    sender != null ? sender.getId() : null,
                    senderName,
                    senderAvatar,
                    m.getContent(),
                    m.getCreatedAt(),
                    isMine
            );
        }).toList();

        return new CursorPageResponse<>(dtoList, nextCursor, hasMore, limit);
    }

    @Override
    @Transactional
    public void deleteConversation(UUID currentUserId, UUID conversationId) {
        log.info("deleteConversation requested by user [{}] for conversation [{}]", currentUserId, conversationId);

        // 1. Kiểm tra tư cách thành viên và tồn tại cuộc trò chuyện (Anti-IDOR Guard)
        ConversationParticipant participant = conversationParticipantRepository
                .findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> {
                    if (!conversationRepository.existsById(conversationId)) {
                        return new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
                    }
                    return new BusinessException(ErrorCode.NOT_CONVERSATION_PARTICIPANT);
                });

        // 2. Tính lũy thừa (Idempotency): nếu đã xóa mềm trước đó thì bỏ qua
        if (participant.isDeleted()) {
            log.info("Conversation [{}] was already soft-deleted for user [{}]", conversationId, currentUserId);
            return;
        }

        // 3. Thực hiện Soft Delete per-participant
        participant.setDeleted(true);
        participant.setDeletedAt(LocalDateTime.now());
        conversationParticipantRepository.save(participant);

        log.info("Successfully soft-deleted conversation [{}] for user [{}]", conversationId, currentUserId);
    }

    private ConversationResponse executeGetOrCreate(
            UUID currentUserId,
            User currentUser,
            User targetUser,
            CreateConversationRequest request
    ) {
        // Tìm kiếm cuộc trò chuyện đã có sẵn giữa 2 người dùng (Atomic Find)
        Optional<Conversation> existingConvOpt = conversationRepository.findConversationBetweenUsers(currentUserId, targetUser.getId());

        if (existingConvOpt.isPresent()) {
            Conversation existingConv = existingConvOpt.get();
            log.info("Existing conversation [{}] found between user [{}] and target [{}]",
                    existingConv.getId(), currentUserId, targetUser.getId());

            // Cơ chế hồi sinh tự động (Reactivation): reset is_deleted = false nếu người gọi từng xóa mềm
            conversationParticipantRepository.findByConversationIdAndUserId(existingConv.getId(), currentUserId)
                    .ifPresent(p -> {
                        if (p.isDeleted()) {
                            p.setDeleted(false);
                            p.setDeletedAt(null);
                            conversationParticipantRepository.save(p);
                            log.info("Reactivated soft-deleted conversation [{}] for user [{}]", existingConv.getId(), currentUserId);
                        }
                    });

            Optional<Message> latestMessage = messageRepository.findLatestMessage(existingConv.getId());
            String snippet = latestMessage.map(m -> truncateSnippet(m.getContent())).orElse(null);
            LocalDateTime messageTime = latestMessage.map(Message::getCreatedAt).orElse(null);

            return buildConversationResponse(existingConv, targetUser, snippet, messageTime, 0L);
        }

        // Tạo mới phòng hội thoại và 2 thành viên trong transaction
        Conversation newConversation = Conversation.builder()
                .type(request.type())
                .build();
        newConversation = conversationRepository.save(newConversation);

        ConversationParticipant participantCurrent = ConversationParticipant.builder()
                .conversation(newConversation)
                .user(currentUser)
                .isDeleted(false)
                .build();

        ConversationParticipant participantTarget = ConversationParticipant.builder()
                .conversation(newConversation)
                .user(targetUser)
                .isDeleted(false)
                .build();

        conversationParticipantRepository.saveAll(List.of(participantCurrent, participantTarget));

        log.info("Successfully created new conversation [{}] between user [{}] and target [{}]",
                newConversation.getId(), currentUserId, targetUser.getId());

        return buildConversationResponse(newConversation, targetUser, null, null, 0L);
    }

    private String getCanonicalLockKey(UUID u1, UUID u2) {
        return (u1.compareTo(u2) < 0
                ? "conv_lock_" + u1 + "_" + u2
                : "conv_lock_" + u2 + "_" + u1).intern();
    }

    private String truncateSnippet(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= MAX_SNIPPET_LENGTH) {
            return content;
        }
        int cutIndex = MAX_SNIPPET_LENGTH - 3;
        if (Character.isHighSurrogate(content.charAt(cutIndex - 1))) {
            cutIndex--;
        }
        return content.substring(0, cutIndex) + "...";
    }

    private void validateConversationTypeRoles(User currentUser, User targetUser, ConversationType type) {
        if (type == ConversationType.BUYER_SELLER) {
            boolean hasSeller = currentUser.getRoles().contains(Role.SELLER) || targetUser.getRoles().contains(Role.SELLER);
            if (!hasSeller) {
                throw new BusinessException(ErrorCode.INVALID_CONVERSATION_TYPE);
            }
        } else if (type == ConversationType.SELLER_ADMIN) {
            boolean hasAdmin = currentUser.getRoles().contains(Role.ADMIN) || targetUser.getRoles().contains(Role.ADMIN);
            boolean hasSeller = currentUser.getRoles().contains(Role.SELLER) || targetUser.getRoles().contains(Role.SELLER);
            if (!hasAdmin || !hasSeller) {
                throw new BusinessException(ErrorCode.INVALID_CONVERSATION_TYPE);
            }
        }
    }

    private ConversationResponse buildConversationResponse(
            Conversation conversation,
            User opponent,
            String lastMessageSnippet,
            LocalDateTime lastMessageTime,
            long unreadCount
    ) {
        String opponentName = ((opponent.getFirstName() != null ? opponent.getFirstName() : "") + " "
                + (opponent.getLastName() != null ? opponent.getLastName() : "")).trim();
        if (opponentName.isEmpty()) {
            opponentName = opponent.getEmail();
        }

        String opponentRole = opponent.getRoles().stream()
                .filter(r -> r == Role.ADMIN || r == Role.SELLER)
                .findFirst()
                .map(Enum::name)
                .orElse(opponent.getRoles().stream().findFirst().map(Enum::name).orElse("BUYER"));

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                opponent.getId(),
                opponentName,
                opponent.getAvatarUrl(),
                opponentRole,
                lastMessageSnippet,
                lastMessageTime,
                unreadCount,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}

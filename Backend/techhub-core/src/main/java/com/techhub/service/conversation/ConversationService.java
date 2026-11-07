package com.techhub.service.conversation;

import com.techhub.common.CursorPageResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.conversation.CreateConversationRequest;
import com.techhub.model.dto.response.conversation.ConversationResponse;
import com.techhub.model.dto.response.conversation.MessageResponse;
import com.techhub.model.enums.ConversationType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ConversationService {

    ConversationResponse createOrGetConversation(UUID currentUserId, CreateConversationRequest request);

    PageResponse<ConversationResponse> getUserConversations(UUID currentUserId, ConversationType type, Pageable pageable);

    CursorPageResponse<MessageResponse> getMessageHistory(
            UUID currentUserId,
            UUID conversationId,
            LocalDateTime beforeTime,
            int limit
    );

    void deleteConversation(UUID currentUserId, UUID conversationId);
}

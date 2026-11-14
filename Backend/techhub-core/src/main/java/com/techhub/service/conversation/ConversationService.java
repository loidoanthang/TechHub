package com.techhub.service.conversation;

import com.techhub.common.CursorPageResponse;
import com.techhub.common.PageResponse;
import com.techhub.modules.conversation.dto.request.CreateConversationRequest;
import com.techhub.modules.conversation.dto.response.ConversationResponse;
import com.techhub.modules.conversation.dto.response.MessageResponse;
import com.techhub.modules.conversation.enums.ConversationType;
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

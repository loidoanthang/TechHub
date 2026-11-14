package com.techhub.modules.conversation.dto.response;

import com.techhub.modules.conversation.enums.ConversationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        ConversationType type,
        UUID opponentId,
        String opponentName,
        String opponentAvatar,
        String opponentRole,
        String lastMessageSnippet,
        LocalDateTime lastMessageTime,
        long unreadCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

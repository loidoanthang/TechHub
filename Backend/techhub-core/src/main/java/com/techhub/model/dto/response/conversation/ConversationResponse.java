package com.techhub.model.dto.response.conversation;

import com.techhub.model.enums.ConversationType;

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

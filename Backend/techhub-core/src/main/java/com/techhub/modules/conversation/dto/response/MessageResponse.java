package com.techhub.modules.conversation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String senderAvatar,
        String content,
        LocalDateTime createdAt,
        boolean isMine
) {
}

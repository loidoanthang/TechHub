package com.techhub.model.dto.response.conversation;

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

package com.techhub.model.dto.request.conversation;

import com.techhub.model.enums.ConversationType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateConversationRequest(
        @NotNull(message = "Target user ID must not be null")
        UUID targetUserId,

        @NotNull(message = "Conversation type must not be null")
        ConversationType type
) {}

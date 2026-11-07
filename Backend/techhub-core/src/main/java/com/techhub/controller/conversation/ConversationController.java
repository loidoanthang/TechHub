package com.techhub.controller.conversation;

import com.techhub.common.ApiResponse;
import com.techhub.common.CursorPageResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.conversation.CreateConversationRequest;
import com.techhub.model.dto.response.conversation.ConversationResponse;
import com.techhub.model.dto.response.conversation.MessageResponse;
import com.techhub.model.enums.ConversationType;
import com.techhub.security.SecurityUtils;
import com.techhub.service.conversation.ConversationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Validated
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ConversationResponse> createOrGetConversation(
            @Valid @RequestBody CreateConversationRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ConversationResponse response = conversationService.createOrGetConversation(currentUserId, request);
        return ApiResponse.success(response, "Conversation retrieved or created successfully", null);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<ConversationResponse>> getUserConversations(
            @RequestParam(required = false) ConversationType type,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must not be negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size must not exceed 100") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ConversationResponse> response = conversationService.getUserConversations(currentUserId, type, pageable);
        return ApiResponse.success(response, "User conversations retrieved successfully", null);
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CursorPageResponse<MessageResponse>> getMessageHistory(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeTime,
            @RequestParam(defaultValue = "30") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit must not exceed 100") int limit
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        CursorPageResponse<MessageResponse> response = conversationService.getMessageHistory(currentUserId, id, beforeTime, limit);
        return ApiResponse.success(response, "Message history retrieved successfully", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteConversation(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        conversationService.deleteConversation(currentUserId, id);
        return ApiResponse.success(null, "Conversation deleted successfully", null);
    }
}

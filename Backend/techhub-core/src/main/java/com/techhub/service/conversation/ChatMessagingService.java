package com.techhub.service.conversation;

import com.techhub.model.dto.request.conversation.SendChatMessageRequest;
import com.techhub.model.dto.response.conversation.MessageResponse;

import java.util.UUID;

public interface ChatMessagingService {

    /**
     * Gửi tin nhắn trò chuyện vào phòng chat qua WebSocket STOMP,
     * lưu trữ vào PostgreSQL và phát sóng tới topic của cuộc trò chuyện.
     *
     * @param senderId ID người gửi
     * @param request  Dữ liệu yêu cầu gửi tin nhắn
     * @return MessageResponse thông tin tin nhắn đã tạo
     */
    MessageResponse sendChatMessage(UUID senderId, SendChatMessageRequest request);
}

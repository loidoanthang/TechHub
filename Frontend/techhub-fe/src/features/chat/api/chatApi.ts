import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  ConversationResponse,
  ConversationType,
  CreateConversationRequest,
  CursorPageResponse,
  MessageResponse,
} from "../types/chat";

/** Format a Date as ISO LocalDateTime (no millis / no zone) for @DateTimeFormat(iso = DATE_TIME). */
export function toLocalDateTimeCursor(date: Date | string): string {
  const d = typeof date === "string" ? new Date(date) : date;
  return d.toISOString().slice(0, 19);
}

export interface ConversationsQueryParams {
  type?: ConversationType;
  page?: number;
  size?: number;
}

export interface MessageHistoryParams {
  beforeTime?: string;
  limit?: number;
}

export const chatApi = {
  createOrGetConversation(
    req: CreateConversationRequest
  ): Promise<ApiResponse<ConversationResponse>> {
    return apiClient.post(API_ENDPOINTS.CONVERSATIONS.BASE, req);
  },

  getUserConversations(
    params?: ConversationsQueryParams
  ): Promise<ApiResponse<PageResponse<ConversationResponse>>> {
    return apiClient.get(API_ENDPOINTS.CONVERSATIONS.BASE, {
      params: {
        type: params?.type,
        page: params?.page ?? 0,
        size: params?.size ?? 20,
      },
    });
  },

  getMessageHistory(
    conversationId: string,
    params?: MessageHistoryParams
  ): Promise<ApiResponse<CursorPageResponse<MessageResponse>>> {
    return apiClient.get(
      API_ENDPOINTS.CONVERSATIONS.MESSAGES(conversationId),
      {
        params: {
          beforeTime: params?.beforeTime,
          limit: params?.limit ?? 30,
        },
      }
    );
  },

  deleteConversation(
    conversationId: string
  ): Promise<ApiResponse<void>> {
    return apiClient.delete(API_ENDPOINTS.CONVERSATIONS.DELETE(conversationId));
  },
};

/**
 * Chat DTOs — Backend Mapping
 *  - ConversationResponse / MessageResponse (records)
 *  - CreateConversationRequest / SendChatMessageRequest
 *  - CursorPageResponse (common/CursorPageResponse.java)
 */

export type ConversationType = "BUYER_SELLER" | "SELLER_ADMIN";

export interface ConversationResponse {
  id: string;
  type: ConversationType;
  opponentId: string;
  opponentName: string;
  opponentAvatar: string | null;
  opponentRole: string | null;
  lastMessageSnippet: string | null;
  lastMessageTime: string | null;
  unreadCount: number;
  createdAt: string;
  updatedAt: string;
}

/** Local UI state merged on top of MessageResponse for optimistic sending. */
export type MessageDeliveryStatus = "sending" | "sent" | "failed";

export interface MessageResponse {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  senderAvatar: string | null;
  content: string;
  createdAt: string;
  isMine: boolean;
  /** Client-only. Absent on server payloads. */
  status?: MessageDeliveryStatus;
  /** Client-only. Temp id used to reconcile optimistic sends with STOMP echoes. */
  _clientId?: string;
}

/** Mirrors common/CursorPageResponse.java (beforeTime cursor pagination). */
export interface CursorPageResponse<T> {
  content: T[];
  /** ISO LocalDateTime; null when no more pages. */
  nextCursor: string | null;
  hasMore: boolean;
  limit: number;
}

export interface CreateConversationRequest {
  targetUserId: string;
  type: ConversationType;
}

export interface SendChatMessageRequest {
  conversationId: string;
  content: string;
}

/** Product pinned to a conversation as advice context. */
export interface ChatProductContext {
  id: string;
  title: string;
  price: number;
  imageUrl: string | null;
  slug: string;
}

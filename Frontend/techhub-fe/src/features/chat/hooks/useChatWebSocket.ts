"use client";

import { useEffect, useMemo, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { stompChat, type ChatConnectionState } from "../services/stompClient";
import type {
  ConversationResponse,
  MessageResponse,
} from "../types/chat";
import { annotateIsMine, playChatChime } from "../utils/chatUtils";
import {
  CHAT_KEYS,
  applyIncomingMessageToCache,
  useConversationsQuery,
} from "./useChatQueries";
import type { PageResponse } from "@/types/api";

/** Maintain the STOMP connection while the user is authenticated. */
export function useChatConnection(enabled: boolean): ChatConnectionState {
  const stateRef = useRef(stompChat.getState());

  useEffect(() => {
    if (!enabled) {
      stompChat.disconnect();
      return;
    }
    stompChat.connect();
    const off = stompChat.onStateChange((state) => {
      stateRef.current = state;
    });
    return () => {
      off();
      // Leave the socket alive across route changes; the provider unmounts it on logout.
    };
  }, [enabled]);

  return stateRef.current;
}

/**
 * Realtime pipe for a conversation topic.
 * - Always wires the STOMP subscription while mounted.
 * - `isActive` controls side-effects: unread badges and chime only fire
 *   when the conversation is NOT the one on screen.
 */
export function useConversationSubscription(
  conversationId: string | null,
  options: {
    enabled?: boolean;
    isActive?: boolean;
    currentUserId?: string | null;
  } = {}
) {
  const queryClient = useQueryClient();
  const { enabled = true, isActive = false, currentUserId = null } = options;

  // Hold latest values to avoid re-subscribing on focus changes.
  const isActiveRef = useRef(isActive);
  isActiveRef.current = isActive;
  const userIdRef = useRef(currentUserId);
  userIdRef.current = currentUserId;

  useEffect(() => {
    if (!enabled || !conversationId) return;

    const off = stompChat.onConversationMessage(
      conversationId,
      (frame: MessageResponse) => {
        const incoming = annotateIsMine(frame, userIdRef.current);
        applyIncomingMessageToCache(queryClient, incoming);

        if (incoming.isMine) return;

        if (isActiveRef.current) return;

        // Background conversation: bump unread + update snippet + chime.
        queryClient.setQueriesData<PageResponse<ConversationResponse>>(
          { queryKey: [...CHAT_KEYS.all, "conversations"] },
          (data) => updateConversationRow(data, incoming, true)
        );
        playChatChime();
      }
    );
    return off;
  }, [enabled, conversationId, queryClient]);
}

/**
 * Global storefront socket: subscribes to every conversation in the cache
 * so unread badges & chime stay live even when no chat window is open.
 */
export function useAllConversationsSubscription(
  conversationIds: string[],
  options: {
    enabled?: boolean;
    activeConversationId?: string | null;
    currentUserId?: string | null;
  } = {}
) {
  const queryClient = useQueryClient();
  const {
    enabled = true,
    activeConversationId = null,
    currentUserId = null,
  } = options;
  const activeRef = useRef(activeConversationId);
  activeRef.current = activeConversationId;
  const userIdRef = useRef(currentUserId);
  userIdRef.current = currentUserId;

  const idsKey = [...conversationIds].sort().join(",");

  useEffect(() => {
    if (!enabled) return;
    const offs = conversationIds.map((id) =>
      stompChat.onConversationMessage(id, (frame) => {
        const incoming = annotateIsMine(frame, userIdRef.current);
        if (incoming.isMine) return;
        const isActive = id === activeRef.current;
        if (isActive) return; // active hook handles the append & no-badge case
        applyIncomingMessageToCache(queryClient, incoming);
        queryClient.setQueriesData<PageResponse<ConversationResponse>>(
          { queryKey: [...CHAT_KEYS.all, "conversations"] },
          (data) => updateConversationRow(data, incoming, true)
        );
        playChatChime();
      })
    );
    return () => offs.forEach((off) => off());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idsKey, enabled, queryClient]);
}

/** Refresh the conversation-list caches whenever realtime data changes them. */
export function updateConversationRow(
  data: PageResponse<ConversationResponse> | undefined,
  incoming: MessageResponse,
  bumpUnread: boolean
) {
  if (!data?.content) return data;
  const row = data.content.find((c) => c.id === incoming.conversationId);
  if (!row) return data;
  const next: ConversationResponse = {
    ...row,
    lastMessageSnippet: incoming.content,
    lastMessageTime: incoming.createdAt,
    unreadCount: bumpUnread ? row.unreadCount + 1 : row.unreadCount,
  };
  return {
    ...data,
    content: [
      next,
      ...data.content.filter((c) => c.id !== incoming.conversationId),
    ],
  };
}

/**
 * Live total unread count across all conversations (menu badges).
 * Owns its STOMP connection + per-conversation subscriptions, so it can be
 * mounted standalone (SellerLayout, AccountSidebar) without ChatProvider.
 */
export function useChatUnreadCount(
  enabled: boolean,
  activeConversationId?: string | null,
  currentUserId?: string | null
): number {
  useChatConnection(enabled);
  const { data } = useConversationsQuery({
    page: 0,
    size: 50,
    enabled,
  });
  const ids = useMemo(() => (data?.content ?? []).map((c) => c.id), [data]);
  useAllConversationsSubscription(ids, {
    enabled,
    activeConversationId: activeConversationId ?? null,
    currentUserId: currentUserId ?? null,
  });
  return (data?.content ?? []).reduce(
    (sum, c) => sum + (c.unreadCount ?? 0),
    0
  );
}

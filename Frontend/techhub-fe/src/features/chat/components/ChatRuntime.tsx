"use client";

import { useEffect, useMemo } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/providers/AuthProvider";
import { useChat } from "../context/ChatContext";
import { CHAT_KEYS, useConversationsQuery } from "../hooks/useChatQueries";
import {
  useAllConversationsSubscription,
  useChatConnection,
  useConversationSubscription,
} from "../hooks/useChatWebSocket";
import { FloatingChatWidget } from "./FloatingChatWidget";

/**
 * Client-side chat runtime: STOMP lifecycle, realtime subscriptions,
 * conversation cache and the floating widget. Rendered inside Suspense so
 * static prerender never blocks on its data fetching.
 */
export function ChatRuntime() {
  const { isAuthenticated, user } = useAuth();
  const { activeConversationId, isOpen } = useChat();
  const queryClient = useQueryClient();
  const currentUserId = user?.id ?? null;

  const { data: conversationsPage } = useConversationsQuery({
    type: "BUYER_SELLER",
    page: 0,
    size: 20,
    enabled: isAuthenticated,
  });

  // STOMP lifecycle for the whole storefront.
  useChatConnection(isAuthenticated);

  // Active conversation: realtime append, no unread bump, no chime.
  useConversationSubscription(activeConversationId, {
    enabled: isAuthenticated,
    isActive: true,
    currentUserId,
  });

  // All other conversations: live unread badges + chime while closed.
  useAllConversationsSubscription(
    useMemo(() => (conversationsPage?.content ?? []).map((c) => c.id), [conversationsPage]),
    { enabled: isAuthenticated, activeConversationId, currentUserId }
  );

  // Opening a conversation marks read on the backend (initial history
  // fetch); zero the badge locally to match.
  useEffect(() => {
    if (!isAuthenticated || !isOpen || !activeConversationId) return;
    queryClient.setQueriesData(
      { queryKey: [...CHAT_KEYS.all, "conversations"] },
      (data: unknown) => {
        const page = data as {
          content?: Array<{ id: string; unreadCount: number }>;
        };
        if (!page?.content) return data;
        return {
          ...page,
          content: page.content.map((c) =>
            c.id === activeConversationId ? { ...c, unreadCount: 0 } : c
          ),
        };
      }
    );
  }, [isAuthenticated, isOpen, activeConversationId, queryClient]);

  if (!isAuthenticated) return null;

  return <FloatingChatWidget conversations={conversationsPage?.content ?? []} />;
}

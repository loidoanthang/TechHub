"use client";

import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type InfiniteData,
} from "@tanstack/react-query";
import { chatApi, toLocalDateTimeCursor } from "../api/chatApi";
import type {
  ConversationType,
  CreateConversationRequest,
  MessageResponse,
} from "../types/chat";
import { reconcileIncomingMessage } from "../utils/chatUtils";

export const CHAT_KEYS = {
  all: ["chat"] as const,
  conversations: (params?: {
    type?: ConversationType;
    page?: number;
    size?: number;
  }) => [...CHAT_KEYS.all, "conversations", params ?? {}] as const,
  messages: (conversationId: string) =>
    [...CHAT_KEYS.all, "messages", conversationId] as const,
};

export { flattenHistoryPages } from "../utils/chatUtils";

export function useConversationsQuery(params?: {
  type?: ConversationType;
  page?: number;
  size?: number;
  enabled?: boolean;
}) {
  return useQuery({
    queryKey: CHAT_KEYS.conversations(params),
    queryFn: async () => {
      const res = await chatApi.getUserConversations(params);
      return res.data;
    },
    enabled: params?.enabled ?? true,
    staleTime: 30 * 1000,
  });
}

/**
 * Cursor-paginated message history. Page 0 (beforeTime=null) fetches the
 * newest N messages and auto-marks the conversation as read (backend).
 * Pages are DESC newest-first; use flattenHistoryPages() to render.
 */
export function useMessageHistoryQuery(conversationId: string | null) {
  return useInfiniteQuery({
    queryKey: CHAT_KEYS.messages(conversationId ?? "__none__"),
    enabled: Boolean(conversationId),
    queryFn: async ({ pageParam }) => {
      const res = await chatApi.getMessageHistory(conversationId!, {
        beforeTime: pageParam
          ? toLocalDateTimeCursor(pageParam as string)
          : undefined,
        limit: 30,
      });
      return res.data;
    },
    initialPageParam: undefined as string | undefined,
    // History only grows backwards; forward pagination is not used.
    getNextPageParam: () => undefined,
    getPreviousPageParam: (lastPage) =>
      lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined,
    staleTime: 0,
  });
}

export function useCreateConversationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (req: CreateConversationRequest) => {
      const res = await chatApi.createOrGetConversation(req);
      return res.data;
    },
    onSuccess: (conversation) => {
      // Refresh conversation lists so the new room appears.
      void queryClient.invalidateQueries({
        queryKey: [...CHAT_KEYS.all, "conversations"],
      });
      void conversation;
    },
  });
}

export function useDeleteConversationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (conversationId: string) => {
      await chatApi.deleteConversation(conversationId);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: [...CHAT_KEYS.all, "conversations"],
      });
    },
  });
}

/**
 * Total unread count read purely from the conversations cache (no socket).
 * Use inside ChatProvider trees (storefront) where ChatProvider already
 * keeps the cache live; avoids duplicate chimes from double subscriptions.
 */
export function useChatUnreadCountFromCache(
  enabled: boolean
): number {
  const { data } = useConversationsQuery({
    page: 0,
    size: 50,
    enabled,
  });
  return (data?.content ?? []).reduce(
    (sum, c) => sum + (c.unreadCount ?? 0),
    0
  );
}

/**
 * Insert a realtime/optimistic message into the cached history of a
 * conversation (reconciled by id/content). Exported for use by
 * useChatWebSocket. Pages are stored newest-first.
 */
export function applyIncomingMessageToCache(
  queryClient: ReturnType<typeof useQueryClient>,
  incoming: MessageResponse
) {
  const key = CHAT_KEYS.messages(incoming.conversationId);
  queryClient.setQueryData<
    InfiniteData<{ content: MessageResponse[]; [k: string]: unknown }>
  >(key, (data) => {
    if (!data) {
      // History not loaded yet: seed the structure so the realtime message
      // is never lost. flattenHistoryPages/reconcile dedupe by id when the
      // real history resolves.
      return { pages: [{ content: [incoming] }], pageParams: [undefined] };
    }
    const pages = data.pages.map((page, i) =>
      i === data.pages.length - 1
        ? {
            ...page,
            content: reconcileIncomingMessage(
              // newest-first page: reconcile works position-agnostically
              [...page.content].reverse(),
              incoming
            ).reverse(),
          }
        : page
    );
    return { ...data, pages };
  });
}

/** Prepend a just-sent optimistic message (DESC: newest first page 0 slot). */
export function applyOptimisticMessageToCache(
  queryClient: ReturnType<typeof useQueryClient>,
  optimistic: MessageResponse
) {
  const key = CHAT_KEYS.messages(optimistic.conversationId);
  queryClient.setQueryData<
    InfiniteData<{ content: MessageResponse[]; [k: string]: unknown }>
  >(key, (data) => {
    if (!data) {
      // History not fetched yet — seed a minimal structure so the
      // optimistic bubble renders; the real fetch will merge over it.
      return {
        pages: [{ content: [optimistic] }],
        pageParams: [undefined],
      };
    }
    const pages = data.pages.map((page, i) =>
      i === 0 ? { ...page, content: [optimistic, ...page.content] } : page
    );
    return { ...data, pages };
  });
}

/** Flip an optimistic message's status (sending → sent/failed) by clientId. */
export function setOptimisticStatus(
  queryClient: ReturnType<typeof useQueryClient>,
  conversationId: string,
  clientId: string,
  status: "sent" | "failed" | "sending"
) {
  const key = CHAT_KEYS.messages(conversationId);
  queryClient.setQueryData<
    InfiniteData<{ content: MessageResponse[]; [k: string]: unknown }>
  >(key, (data) => {
    if (!data) return data;
    const pages = data.pages.map((page) => ({
      ...page,
      content: page.content.map((m) =>
        m._clientId === clientId ? { ...m, status } : m
      ),
    }));
    return { ...data, pages };
  });
}

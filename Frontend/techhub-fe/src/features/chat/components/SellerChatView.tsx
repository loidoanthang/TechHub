"use client";

import { useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Search } from "lucide-react";
import {
  flattenHistoryPages,
  useMessageHistoryQuery,
  useConversationsQuery,
} from "../hooks/useChatQueries";
import {
  useAllConversationsSubscription,
  useChatConnection,
  useConversationSubscription,
} from "../hooks/useChatWebSocket";
import { useChatRetrier, useChatSender } from "../hooks/useChatSend";
import { stompChat } from "../services/stompClient";
import type { MessageResponse } from "../types/chat";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { ChatMessageList } from "./ChatMessageList";
import { ChatInputBar } from "./ChatInputBar";
import { cn } from "@/lib/utils";

const QUICK_REPLIES = [
  "Chào bạn, shop có thể hỗ trợ gì cho bạn?",
  "Sản phẩm này còn hàng ạ.",
  "Dự kiến giao trong 2-3 ngày làm việc.",
  "Cảm ơn bạn đã quan tâm shop ạ!",
];

const SELLER_CHAT_KEYS = { type: "BUYER_SELLER" as const, page: 0, size: 50 };

/**
 * Seller Chat Dashboard (/seller/chat): two-column layout —
 * customer list on the left, active thread + quick replies on the right.
 */
export function SellerChatView() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const currentUserId = user?.id ?? null;
  const { data: conversationsPage, isLoading } = useConversationsQuery(
    SELLER_CHAT_KEYS
  );
  const conversations = conversationsPage?.content ?? [];
  const [activeId, setActiveId] = useState<string | null>(null);
  const [search, setSearch] = useState("");

  // Seller portal mounts outside ChatProvider — own its STOMP lifecycle.
  useChatConnection(true);
  useAllConversationsSubscription(
    useMemo(() => conversations.map((c) => c.id), [conversations]),
    { enabled: true, activeConversationId: activeId, currentUserId }
  );

  // Active thread: live append, no badge bump.
  useConversationSubscription(activeId, {
    isActive: true,
    currentUserId,
  });

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return conversations;
    return conversations.filter((c) =>
      (c.opponentName ?? "").toLowerCase().includes(q)
    );
  }, [conversations, search]);

  const totalUnread = useMemo(
    () => conversations.reduce((sum, c) => sum + (c.unreadCount ?? 0), 0),
    [conversations]
  );

  const activeConversation = conversations.find((c) => c.id === activeId);

  function openConversation(id: string) {
    setActiveId(id);
    queryClient.setQueriesData(
      { queryKey: ["chat", "conversations"] },
      (data: unknown) => resetUnreadFor(data, id)
    );
  }

  return (
    <div className="flex flex-col lg:flex-row gap-4 lg:gap-6 min-h-[70vh]">
      {/* Left: customer list */}
      <aside className="w-full lg:w-80 xl:w-96 shrink-0 flex flex-col rounded-2xl border border-slate-100 bg-white shadow-sm overflow-hidden">
        <header className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
          <h2 className="text-sm font-bold text-slate-800">
            Khách hàng{" "}
            {totalUnread > 0 ? (
              <span className="ml-1 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1.5 text-[10px] font-bold text-white">
                {totalUnread > 99 ? "99+" : totalUnread}
              </span>
            ) : null}
          </h2>
        </header>
        <div className="border-b border-slate-100 px-3 py-2">
          <div className="flex items-center gap-2 rounded-xl bg-slate-50 px-3 py-2">
            <Search className="h-3.5 w-3.5 text-slate-400" />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm khách hàng..."
              className="w-full min-w-0 bg-transparent text-xs text-slate-700 outline-none placeholder:text-slate-400"
            />
          </div>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto divide-y divide-slate-50">
          {isLoading ? (
            <div className="flex justify-center py-8">
              <div className="h-5 w-5 animate-spin rounded-full border-2 border-slate-200 border-t-[#0045cc]" />
            </div>
          ) : null}
          {!isLoading && filtered.length === 0 ? (
            <p className="px-4 py-8 text-center text-xs text-slate-400">
              Chưa có khách hàng nào nhắn tin.
            </p>
          ) : null}
          {filtered.map((c) => (
            <button
              key={c.id}
              type="button"
              onClick={() => openConversation(c.id)}
              className={cn(
                "flex w-full items-center gap-3 px-4 py-3 text-left transition-colors",
                activeId === c.id ? "bg-[#eff4ff]" : "hover:bg-slate-50"
              )}
            >
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-100 text-xs font-bold text-slate-500">
                {(c.opponentName ?? "?").charAt(0).toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <p className="truncate text-[13px] font-semibold text-slate-800">
                    {c.opponentName}
                  </p>
                  {c.lastMessageTime ? (
                    <span className="shrink-0 text-[10px] text-slate-400">
                      {new Date(c.lastMessageTime).toLocaleTimeString("vi-VN", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </span>
                  ) : null}
                </div>
                <div className="flex items-center justify-between gap-2">
                  <p className="truncate text-xs text-slate-500">
                    {c.lastMessageSnippet ?? "Chưa có tin nhắn"}
                  </p>
                  {(c.unreadCount ?? 0) > 0 ? (
                    <span className="shrink-0 rounded-full bg-[#0045cc] px-1.5 py-0.5 text-[10px] font-bold text-white">
                      {c.unreadCount > 99 ? "99+" : c.unreadCount}
                    </span>
                  ) : null}
                </div>
              </div>
            </button>
          ))}
        </div>
      </aside>

      {/* Right: active thread */}
      <section className="flex min-h-[60vh] flex-1 flex-col rounded-2xl border border-slate-100 bg-white shadow-sm overflow-hidden">
        {activeId && activeConversation ? (
          <SellerChatThread
            key={activeId}
            conversationId={activeId}
            opponentName={activeConversation.opponentName}
            onSent={() => {
              // Keep the customer list snippet fresh after sending.
              void queryClient.invalidateQueries({
                queryKey: ["chat", "conversations"],
              });
            }}
          />
        ) : (
          <div className="flex flex-1 items-center justify-center px-6 text-center">
            <p className="text-xs text-slate-400">
              Chọn một khách hàng ở danh sách bên trái để bắt đầu trả lời.
            </p>
          </div>
        )}
      </section>
    </div>
  );
}

function SellerChatThread({
  conversationId,
  opponentName,
  onSent,
}: {
  conversationId: string;
  opponentName: string;
  onSent: () => void;
}) {
  const sendMessage = useChatSender();
  const retryMessage = useChatRetrier();
  const {
    data: history,
    isLoading,
    fetchPreviousPage,
    hasPreviousPage,
    isFetchingPreviousPage,
  } = useMessageHistoryQuery(conversationId);
  const messages = useMemo(
    () => flattenHistoryPages(history?.pages),
    [history]
  );

  function sendContent(content: string): void {
    sendMessage(conversationId, content);
    onSent();
  }

  function retry(message: MessageResponse): void {
    retryMessage(message);
  }

  return (
    <>
      <header className="flex shrink-0 items-center justify-between border-b border-slate-100 bg-[#0045cc] px-4 py-3 text-white">
        <p className="truncate text-sm font-bold">{opponentName}</p>
        <p className="text-[11px] text-white/70">
          {stompChat.isConnected() ? "Đang hoạt động" : "Mất kết nối..."}
        </p>
      </header>

      {isLoading ? (
        <div className="flex flex-1 items-center justify-center">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-slate-200 border-t-[#0045cc]" />
        </div>
      ) : (
        <>
          {hasPreviousPage ? (
            <button
              type="button"
              onClick={() => void fetchPreviousPage()}
              disabled={isFetchingPreviousPage}
              className="mx-auto mt-2 shrink-0 rounded-full bg-slate-50 px-3 py-1 text-[11px] font-semibold text-[#0045cc] disabled:opacity-50"
            >
              {isFetchingPreviousPage ? "Đang tải..." : "Tải tin nhắn cũ hơn"}
            </button>
          ) : null}
          <ChatMessageList
            messages={messages}
            opponentName={opponentName}
            onRetry={retry}
          />
        </>
      )}

      {/* Quick reply chips */}
      <div className="flex shrink-0 gap-2 overflow-x-auto border-t border-slate-100 px-3 py-2">
        {QUICK_REPLIES.map((reply) => (
          <button
            key={reply}
            type="button"
            onClick={() => sendContent(reply)}
            className="shrink-0 whitespace-nowrap rounded-full border border-[#0045cc]/20 bg-[#eff4ff] px-3 py-1.5 text-[11px] font-semibold text-[#0045cc] transition-colors hover:bg-[#0045cc] hover:text-white"
          >
            {reply}
          </button>
        ))}
      </div>

      <ChatInputBar onSend={sendContent} />
    </>
  );
}

function resetUnreadFor(data: unknown, conversationId: string): unknown {
  const page = data as { content?: Array<{ id: string; unreadCount: number }> };
  if (!page?.content) return data;
  return {
    ...page,
    content: page.content.map((c) =>
      c.id === conversationId ? { ...c, unreadCount: 0 } : c
    ),
  };
}

"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useQueryClient } from "@tanstack/react-query";
import {
  flattenHistoryPages,
  useConversationsQuery,
  useDeleteConversationMutation,
  useMessageHistoryQuery,
} from "../hooks/useChatQueries";
import {
  useAllConversationsSubscription,
  useChatConnection,
  useConversationSubscription,
} from "../hooks/useChatWebSocket";
import { useChatRetrier, useChatSender } from "../hooks/useChatSend";
import { useAuth } from "@/features/auth/hooks/useAuth";
import type { MessageResponse } from "../types/chat";
import { ChatMessageList } from "./ChatMessageList";
import { ChatInputBar } from "./ChatInputBar";
import { cn } from "@/lib/utils";

/**
 * Full-screen buyer messages page (/account/messages):
 * conversation list + selected thread, reusing the widget's building blocks.
 */
export function AccountMessagesPageView() {
  useChatConnection(true);
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const currentUserId = user?.id ?? null;
  const { data: conversationsPage, isLoading } = useConversationsQuery({
    type: "BUYER_SELLER",
    page: 0,
    size: 50,
  });
  const conversations = conversationsPage?.content ?? [];
  const [activeId, setActiveId] = useState<string | null>(null);
  const deleteMutation = useDeleteConversationMutation();

  useConversationSubscription(activeId, {
    isActive: true,
    currentUserId,
  });
  useAllConversationsSubscription(
    useMemo(() => conversations.map((c) => c.id), [conversations]),
    { enabled: true, activeConversationId: activeId, currentUserId }
  );

  const activeConversation = conversations.find((c) => c.id === activeId);

  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-6">
      <h1 className="mb-4 text-lg font-bold text-slate-900">Tin nhắn</h1>

      <div className="flex flex-col gap-4 lg:flex-row">
        <aside className="w-full shrink-0 overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm lg:w-80">
          {isLoading ? (
            <div className="flex justify-center py-10">
              <div className="h-5 w-5 animate-spin rounded-full border-2 border-slate-200 border-t-[#0045cc]" />
            </div>
          ) : null}
          {!isLoading && conversations.length === 0 ? (
            <p className="px-4 py-10 text-center text-xs text-slate-400">
              Bạn chưa có hội thoại nào. Bấm “Nhắn tin” trên trang sản phẩm để
              bắt đầu.
            </p>
          ) : null}
          <div className="divide-y divide-slate-50">
            {conversations.map((c) => (
              <div key={c.id} className="group flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => {
                    setActiveId(c.id);
                    queryClient.setQueriesData(
                      { queryKey: ["chat", "conversations"] },
                      (data: unknown) => {
                        const page = data as {
                          content?: Array<{ id: string; unreadCount: number }>;
                        };
                        if (!page?.content) return data;
                        return {
                          ...page,
                          content: page.content.map((row) =>
                            row.id === c.id ? { ...row, unreadCount: 0 } : row
                          ),
                        };
                      }
                    );
                  }}
                  className={cn(
                    "flex min-w-0 flex-1 items-center gap-3 px-4 py-3 text-left transition-colors",
                    activeId === c.id ? "bg-[#eff4ff]" : "hover:bg-slate-50"
                  )}
                >
                  <div className="relative h-9 w-9 shrink-0 overflow-hidden rounded-full bg-slate-100">
                    {c.opponentAvatar ? (
                      <Image
                        src={c.opponentAvatar}
                        alt={c.opponentName}
                        fill
                        sizes="36px"
                        className="object-cover"
                        unoptimized
                      />
                    ) : (
                      <span className="flex h-full w-full items-center justify-center text-xs font-bold text-slate-500">
                        {(c.opponentName ?? "?").charAt(0).toUpperCase()}
                      </span>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[13px] font-semibold text-slate-800">
                      {c.opponentName}
                    </p>
                    <p className="truncate text-xs text-slate-500">
                      {c.lastMessageSnippet ?? "Chưa có tin nhắn"}
                    </p>
                  </div>
                  {(c.unreadCount ?? 0) > 0 ? (
                    <span className="shrink-0 rounded-full bg-[#0045cc] px-1.5 py-0.5 text-[10px] font-bold text-white">
                      {c.unreadCount > 99 ? "99+" : c.unreadCount}
                    </span>
                  ) : null}
                </button>
                <button
                  type="button"
                  aria-label="Xóa hội thoại"
                  disabled={deleteMutation.isPending}
                  onClick={() => {
                    if (window.confirm("Xóa hội thoại này?")) {
                      deleteMutation.mutate(c.id);
                    }
                  }}
                  className="mr-2 shrink-0 rounded-full p-1.5 text-slate-300 opacity-0 transition hover:bg-red-50 hover:text-red-500 group-hover:opacity-100"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        </aside>

        <section className="flex min-h-[60vh] flex-1 flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm">
          {activeId && activeConversation ? (
            <AccountThread
              key={activeId}
              conversationId={activeId}
              opponentName={activeConversation.opponentName}
              opponentAvatar={activeConversation.opponentAvatar}
            />
          ) : (
            <div className="flex flex-1 items-center justify-center px-6 text-center">
              <p className="text-xs text-slate-400">
                Chọn một hội thoại để xem tin nhắn.
              </p>
            </div>
          )}
        </section>
      </div>

      <p className="mt-4 text-center text-[11px] text-slate-400">
        <Link href="/" className="text-[#0045cc] hover:underline">
          Tiếp tục mua sắm
        </Link>
      </p>
    </div>
  );
}

function AccountThread({
  conversationId,
  opponentName,
  opponentAvatar,
}: {
  conversationId: string;
  opponentName: string;
  opponentAvatar: string | null;
}) {
  const {
    data: history,
    isLoading,
    fetchPreviousPage,
    hasPreviousPage,
    isFetchingPreviousPage,
  } = useMessageHistoryQuery(conversationId);
  const sendMessage = useChatSender();
  const retryMessage = useChatRetrier();
  const messages = useMemo(
    () => flattenHistoryPages(history?.pages),
    [history]
  );

  function sendContent(content: string): void {
    sendMessage(conversationId, content);
  }

  function retry(message: MessageResponse): void {
    retryMessage(message);
  }

  return (
    <>
      <header className="flex shrink-0 items-center gap-2 border-b border-slate-100 bg-[#0045cc] px-4 py-3 text-white">
        <div className="relative h-8 w-8 shrink-0 overflow-hidden rounded-full bg-white/20">
          {opponentAvatar ? (
            <Image
              src={opponentAvatar}
              alt={opponentName}
              fill
              sizes="32px"
              className="object-cover"
              unoptimized
            />
          ) : (
            <span className="flex h-full w-full items-center justify-center text-xs font-bold">
              {opponentName.charAt(0).toUpperCase()}
            </span>
          )}
        </div>
        <p className="truncate text-sm font-bold">{opponentName}</p>
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
            opponentAvatar={opponentAvatar}
            onRetry={retry}
          />
        </>
      )}

      <ChatInputBar onSend={sendContent} />
    </>
  );
}

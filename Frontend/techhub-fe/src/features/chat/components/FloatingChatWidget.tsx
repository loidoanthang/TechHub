"use client";

import { useMemo, useState } from "react";
import Image from "next/image";
import {
  ArrowLeft,
  MessageCircle,
  Minus,
  ShoppingCart,
  Trash2,
  X,
} from "lucide-react";
import { useChat } from "../context/ChatContext";
import {
  flattenHistoryPages,
  useDeleteConversationMutation,
  useMessageHistoryQuery,
} from "../hooks/useChatQueries";
import { useChatRetrier, useChatSender } from "../hooks/useChatSend";
import { stompChat } from "../services/stompClient";
import type { ConversationResponse, MessageResponse } from "../types/chat";
import { ChatMessageList } from "./ChatMessageList";
import { ChatInputBar } from "./ChatInputBar";
import { ProductContextCard } from "./ProductContextCard";
import { formatPrice } from "@/lib/utils";

interface FloatingChatWidgetProps {
  conversations: ConversationResponse[];
}

/**
 * Buyer floating chat dock:
 *  - Collapsed: floating bubble bottom-right with unread badge.
 *  - Expanded: 380x520 dock on desktop, full-screen bottom sheet on mobile.
 *  - Two panes: conversation list ↔ active chat thread.
 */
export function FloatingChatWidget({ conversations }: FloatingChatWidgetProps) {
  const {
    isOpen,
    isMinimized,
    activeConversationId,
    openChat,
    closeChat,
    minimizeChat,
  } = useChat();

  const totalUnread = useMemo(
    () => conversations.reduce((sum, c) => sum + (c.unreadCount ?? 0), 0),
    [conversations]
  );

  /**
   * Bubble click behavior:
   *  - Minimized → restore the previously open thread.
   *  - Closed    → reopen the active thread (if it still exists), else the
   *                most recent conversation, else the empty list pane.
   */
  function handleBubbleOpen() {
    if (isMinimized) {
      minimizeChat(false);
      return;
    }
    const target =
      activeConversationId &&
      conversations.some((c) => c.id === activeConversationId)
        ? activeConversationId
        : (conversations[0]?.id ?? "");
    openChat(target);
  }

  if (!isOpen || isMinimized) {
    return (
      <FloatingBubble
        unread={totalUnread}
        minimized={isMinimized}
        onOpen={handleBubbleOpen}
        onClose={closeChat}
      />
    );
  }

  return (
    <div
      className="fixed inset-0 z-[70] flex flex-col bg-white shadow-2xl sm:inset-auto sm:bottom-6 sm:right-6 sm:h-[520px] sm:w-[380px] sm:rounded-2xl sm:border sm:border-slate-100"
      role="dialog"
      aria-label="Hộp thư trò chuyện"
    >
      {activeConversationId ? (
        <ChatThreadPane
          key={activeConversationId}
          conversationId={activeConversationId}
          conversations={conversations}
          onBack={() => openChat("")}
        />
      ) : (
        <ConversationListPane
          conversations={conversations}
          onOpen={openChat}
          onMinimize={() => minimizeChat(true)}
        />
      )}
    </div>
  );
}

/* ------------------------------ Bubble ------------------------------ */

function FloatingBubble({
  unread,
  minimized,
  onOpen,
  onClose,
}: {
  unread: number;
  minimized: boolean;
  onOpen: () => void;
  onClose: () => void;
}) {
  return (
    <div className="fixed bottom-5 right-5 z-[70] flex items-center gap-2">
      {minimized ? (
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng chat"
          className="flex h-9 w-9 items-center justify-center rounded-full bg-white text-slate-500 shadow-md border border-slate-100 hover:text-slate-700"
        >
          <X className="h-4 w-4" />
        </button>
      ) : null}
      <button
        type="button"
        onClick={onOpen}
        aria-label="Mở hộp thư chat"
        className="relative flex h-14 w-14 items-center justify-center rounded-full bg-[#0045cc] text-white shadow-lg transition-transform hover:scale-105"
      >
        <MessageCircle className="h-6 w-6" />
        {unread > 0 ? (
          <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold">
            {unread > 99 ? "99+" : unread}
          </span>
        ) : null}
      </button>
    </div>
  );
}

/* ------------------------ Conversation list pane ------------------------ */

function ConversationListPane({
  conversations,
  onOpen,
  onMinimize,
}: {
  conversations: ConversationResponse[];
  onOpen: (id: string) => void;
  onMinimize: () => void;
}) {
  const deleteMutation = useDeleteConversationMutation();

  return (
    <>
      <header className="flex shrink-0 items-center justify-between rounded-t-2xl border-b border-slate-100 bg-[#0045cc] px-4 py-3 text-white">
        <div className="min-w-0">
          <h2 className="truncate text-sm font-bold">Tin nhắn</h2>
          <p className="text-[11px] text-white/70">Người bán của bạn</p>
        </div>
        <button
          type="button"
          onClick={onMinimize}
          aria-label="Thu nhỏ"
          className="rounded-full p-1.5 hover:bg-white/15"
        >
          <Minus className="h-4 w-4" />
        </button>
      </header>

      <div className="min-h-0 flex-1 divide-y divide-slate-50 overflow-y-auto">
        {conversations.length === 0 ? (
          <div className="flex h-full flex-col items-center justify-center gap-2 px-6 text-center">
            <ShoppingCart className="h-8 w-8 text-slate-200" />
            <p className="text-xs text-slate-400">
              Chưa có hội thoại nào. Bấm “Nhắn tin” trên trang sản phẩm để bắt
              đầu tư vấn với shop.
            </p>
          </div>
        ) : null}

        {conversations.map((conversation) => (
          <div
            key={conversation.id}
            className="group flex w-full items-center gap-3 px-4 py-3 hover:bg-slate-50"
          >
            <button
              type="button"
              onClick={() => onOpen(conversation.id)}
              className="flex min-w-0 flex-1 items-center gap-3 text-left"
            >
              <Avatar name={conversation.opponentName} src={conversation.opponentAvatar} />
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <p className="truncate text-[13px] font-semibold text-slate-800">
                    {conversation.opponentName}
                  </p>
                  {conversation.lastMessageTime ? (
                    <span className="shrink-0 text-[10px] text-slate-400">
                      {new Date(conversation.lastMessageTime).toLocaleDateString(
                        "vi-VN",
                        { day: "2-digit", month: "2-digit" }
                      )}
                    </span>
                  ) : null}
                </div>
                <div className="flex items-center justify-between gap-2">
                  <p className="truncate text-xs text-slate-500">
                    {conversation.lastMessageSnippet ?? "Chưa có tin nhắn"}
                  </p>
                  {(conversation.unreadCount ?? 0) > 0 ? (
                    <span className="shrink-0 rounded-full bg-[#0045cc] px-1.5 py-0.5 text-[10px] font-bold text-white">
                      {conversation.unreadCount > 99
                        ? "99+"
                        : conversation.unreadCount}
                    </span>
                  ) : null}
                </div>
              </div>
            </button>
            <button
              type="button"
              aria-label="Xóa hội thoại"
              disabled={deleteMutation.isPending}
              onClick={() => {
                if (window.confirm("Xóa hội thoại này?")) {
                  deleteMutation.mutate(conversation.id);
                }
              }}
              className="shrink-0 rounded-full p-1.5 text-slate-300 opacity-0 transition hover:bg-red-50 hover:text-red-500 group-hover:opacity-100"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          </div>
        ))}
      </div>
    </>
  );
}

/* ----------------------------- Chat thread ----------------------------- */

function ChatThreadPane({
  conversationId,
  conversations,
  onBack,
}: {
  conversationId: string;
  conversations: ConversationResponse[];
  onBack: () => void;
}) {
  const {
    isOpen,
    productContext,
    closeChat,
    minimizeChat,
    setActiveProductContext,
  } = useChat();

  const [isSendingProduct, setIsSendingProduct] = useState(false);
  const sendMessage = useChatSender();
  const retryMessage = useChatRetrier();

  const conversation = conversations.find((c) => c.id === conversationId);
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
  }

  function retry(message: MessageResponse): void {
    retryMessage(message);
  }

  async function sendProductContext() {
    if (!productContext) return;
    setIsSendingProduct(true);
    const link = `${window.location.origin}/products/${
      productContext.slug || productContext.id
    }`;
    sendContent(
      `🛒 ${productContext.title}\nGiá: ${formatPrice(productContext.price)}\n${link}`
    );
    setActiveProductContext(null);
    setIsSendingProduct(false);
  }

  return (
    <>
      <header className="flex shrink-0 items-center gap-2 rounded-t-2xl border-b border-slate-100 bg-[#0045cc] px-3 py-2.5 text-white">
        <button
          type="button"
          onClick={onBack}
          aria-label="Danh sách hội thoại"
          className="rounded-full p-1.5 hover:bg-white/15"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <Avatar
          name={conversation?.opponentName ?? "Shop"}
          src={conversation?.opponentAvatar ?? null}
        />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-bold">
            {conversation?.opponentName ?? "Đang trò chuyện"}
          </p>
          <p className="text-[11px] text-white/70">
            {stompChat.isConnected() ? "Đang hoạt động" : "Mất kết nối..."}
          </p>
        </div>
        <button
          type="button"
          onClick={() => minimizeChat(true)}
          aria-label="Thu nhỏ"
          className="rounded-full p-1.5 hover:bg-white/15"
        >
          <Minus className="h-4 w-4" />
        </button>
        <button
          type="button"
          onClick={closeChat}
          aria-label="Đóng"
          className="rounded-full p-1.5 hover:bg-white/15"
        >
          <X className="h-4 w-4" />
        </button>
      </header>

      <ProductContextCard
        product={productContext}
        isSending={isSendingProduct}
        onSend={sendProductContext}
        onRemove={() => setActiveProductContext(null)}
      />

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
              className="mx-auto mt-1 shrink-0 rounded-full bg-slate-50 px-3 py-1 text-[11px] font-semibold text-[#0045cc] disabled:opacity-50"
            >
              {isFetchingPreviousPage ? "Đang tải..." : "Tải tin nhắn cũ hơn"}
            </button>
          ) : null}

          <ChatMessageList
            messages={messages}
            opponentName={conversation?.opponentName ?? "Shop"}
            opponentAvatar={conversation?.opponentAvatar ?? null}
            onRetry={retry}
          />
        </>
      )}

      <ChatInputBar onSend={sendContent} />
      <div className="h-[env(safe-area-inset-bottom)] shrink-0" />
    </>
  );
}

/* ------------------------------- Avatar -------------------------------- */

function Avatar({ name, src }: { name: string; src: string | null }) {
  const initial = name.charAt(0).toUpperCase() || "?";
  return (
    <div className="relative h-9 w-9 shrink-0 overflow-hidden rounded-full bg-slate-100">
      {src ? (
        <Image
          src={src}
          alt={name}
          fill
          sizes="36px"
          className="object-cover"
          unoptimized
        />
      ) : (
        <span className="flex h-full w-full items-center justify-center text-xs font-bold text-slate-500">
          {initial}
        </span>
      )}
    </div>
  );
}

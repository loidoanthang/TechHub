"use client";

import {
  createContext,
  Suspense,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import type { ChatProductContext } from "../types/chat";
import { ChatRuntime } from "../components/ChatRuntime";

export interface ChatContextValue {
  isOpen: boolean;
  isMinimized: boolean;
  activeConversationId: string | null;
  productContext: ChatProductContext | null;
  openChat: (conversationId: string) => void;
  closeChat: () => void;
  minimizeChat: (minimized: boolean) => void;
  setActiveProductContext: (product: ChatProductContext | null) => void;
  /**
   * Create-or-get the conversation with a seller, open the widget and
   * seed the Product Context Card. Resolves with the conversation id.
   * The backend dedupes per buyer↔seller pair, so no local lookup needed.
   */
  openChatWithSeller: (
    sellerUserId: string,
    productContext?: ChatProductContext
  ) => Promise<string | null>;
}

const ChatContext = createContext<ChatContextValue | null>(null);

export function useChat(): ChatContextValue {
  const ctx = useContext(ChatContext);
  if (!ctx) {
    throw new Error("useChat must be used within a ChatProvider");
  }
  return ctx;
}

const STORAGE_KEY = "techhub.chat.ui";

interface PersistedChatUiState {
  isOpen: boolean;
  isMinimized: boolean;
  activeConversationId: string | null;
  productContext: ChatProductContext | null;
}

function readPersistedState(): PersistedChatUiState {
  if (typeof window === "undefined") {
    return { isOpen: false, isMinimized: false, activeConversationId: null, productContext: null };
  }
  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY);
    if (!raw) throw new Error("empty");
    const parsed = JSON.parse(raw) as Partial<PersistedChatUiState>;
    return {
      isOpen: Boolean(parsed.isOpen),
      isMinimized: Boolean(parsed.isMinimized),
      activeConversationId:
        typeof parsed.activeConversationId === "string"
          ? parsed.activeConversationId
          : null,
      productContext: parsed.productContext ?? null,
    };
  } catch {
    return { isOpen: false, isMinimized: false, activeConversationId: null, productContext: null };
  }
}

function writePersistedState(state: PersistedChatUiState): void {
  if (typeof window === "undefined") return;
  try {
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // Storage full/unavailable — non-critical, ignore.
  }
}

/**
 * Pure-state provider: safe for static prerender (no data fetching here).
 * All realtime/data work lives in <ChatRuntime /> which streams in via
 * Suspense so prerendered routes keep their static shell.
 * UI state (open thread) survives page refreshes via sessionStorage;
 * messages themselves are server-persisted and refetched.
 */
export function ChatProvider({ children }: { children: React.ReactNode }) {
  const [initial] = useState(readPersistedState);
  const [isOpen, setIsOpen] = useState(initial.isOpen);
  const [isMinimized, setIsMinimized] = useState(initial.isMinimized);
  const [activeConversationId, setActiveConversationId] = useState<
    string | null
  >(initial.activeConversationId);
  const [productContext, setProductContext] =
    useState<ChatProductContext | null>(initial.productContext);

  // Persist the widget UI state so a refresh reopens the same thread.
  useEffect(() => {
    writePersistedState({ isOpen, isMinimized, activeConversationId, productContext });
  }, [isOpen, isMinimized, activeConversationId, productContext]);

  const openChat = useCallback((conversationId: string) => {
    setActiveConversationId(conversationId || null);
    setIsMinimized(false);
    setIsOpen(true);
  }, []);

  const closeChat = useCallback(() => {
    setIsOpen(false);
    setIsMinimized(false);
  }, []);

  const minimizeChat = useCallback((minimized: boolean) => {
    setIsMinimized(minimized);
  }, []);

  const openChatWithSeller = useCallback(
    async (sellerUserId: string, product?: ChatProductContext) => {
      try {
        const { chatApi } = await import("../api/chatApi");
        const res = await chatApi.createOrGetConversation({
          targetUserId: sellerUserId,
          type: "BUYER_SELLER",
        });
        const conversationId = res.data?.id ?? null;
        if (conversationId) {
          setProductContext(product ?? null);
          openChat(conversationId);
        }
        return conversationId;
      } catch (err) {
        console.error("[chat] Failed to create conversation:", err);
        return null;
      }
    },
    [openChat]
  );

  const value = useMemo<ChatContextValue>(
    () => ({
      isOpen,
      isMinimized,
      activeConversationId,
      productContext,
      openChat,
      closeChat,
      minimizeChat,
      setActiveProductContext: setProductContext,
      openChatWithSeller,
    }),
    [
      isOpen,
      isMinimized,
      activeConversationId,
      productContext,
      openChat,
      closeChat,
      minimizeChat,
      openChatWithSeller,
    ]
  );

  return (
    <ChatContext.Provider value={value}>
      {children}
      {/* Widget + sockets fetch data: stream in so prerender never blocks. */}
      <Suspense fallback={null}>
        <ChatRuntime />
      </Suspense>
    </ChatContext.Provider>
  );
}

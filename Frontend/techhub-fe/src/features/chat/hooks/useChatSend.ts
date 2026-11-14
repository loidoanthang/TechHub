"use client";

import { useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { stompChat } from "../services/stompClient";
import type { MessageResponse } from "../types/chat";
import {
  CHAT_KEYS,
  applyOptimisticMessageToCache,
  setOptimisticStatus,
} from "./useChatQueries";

function newClientId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `tmp-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

/**
 * Shared optimistic send flow used by the buyer widget, /account/messages
 * and the seller dashboard.
 *
 * 1. Insert a `sending` placeholder into the history cache (seeding the
 *    structure when history hasn't loaded yet).
 * 2. Publish via STOMP /app/chat.send. `false` → mark failed immediately.
 * 3. Self-heal: the backend broadcasts the echo, but if that frame is lost
 *    or races with the history fetch, refetch history (~1.2s later — the
 *    server has persisted the message by then) so the thread converges.
 * 4. Watchdog: if no echo reconciled the placeholder in 10s, mark failed.
 */
export function useChatSender() {
  const queryClient = useQueryClient();

  return useCallback(
    (conversationId: string, content: string): boolean => {
      const trimmed = content.trim();
      if (!trimmed) return false;

      const clientId = newClientId();
      const optimistic: MessageResponse = {
        id: "",
        conversationId,
        senderId: "",
        senderName: "Bạn",
        senderAvatar: null,
        content: trimmed,
        createdAt: new Date().toISOString(),
        isMine: true,
        status: "sending",
        _clientId: clientId,
      };
      applyOptimisticMessageToCache(queryClient, optimistic);

      const ok = stompChat.sendMessage({ conversationId, content: trimmed });
      if (!ok) {
        setOptimisticStatus(queryClient, conversationId, clientId, "failed");
        return false;
      }

      // Self-heal refetch: converge cache with the server-persisted message
      // even when the STOMP echo is lost or raced with the history fetch.
      setTimeout(() => {
        void queryClient.invalidateQueries({
          queryKey: CHAT_KEYS.messages(conversationId),
        });
        void queryClient.invalidateQueries({
          queryKey: [...CHAT_KEYS.all, "conversations"],
        });
      }, 1200);

      // Watchdog: flip to failed if still unreconciled.
      setTimeout(() => {
        setOptimisticStatus(queryClient, conversationId, clientId, "failed");
      }, 10_000);

      return true;
    },
    [queryClient]
  );
}

/** Retry a failed message through the same STOMP path. */
export function useChatRetrier() {
  const queryClient = useQueryClient();

  return useCallback(
    (message: MessageResponse): boolean => {
      const clientId = message._clientId ?? newClientId();
      setOptimisticStatus(queryClient, message.conversationId, clientId, "sending");
      const ok = stompChat.sendMessage({
        conversationId: message.conversationId,
        content: message.content,
      });
      if (!ok) {
        setOptimisticStatus(
          queryClient,
          message.conversationId,
          clientId,
          "failed"
        );
        return false;
      }
      setTimeout(() => {
        void queryClient.invalidateQueries({
          queryKey: CHAT_KEYS.messages(message.conversationId),
        });
      }, 1200);
      setTimeout(() => {
        setOptimisticStatus(
          queryClient,
          message.conversationId,
          clientId,
          "failed"
        );
      }, 10_000);
      return true;
    },
    [queryClient]
  );
}

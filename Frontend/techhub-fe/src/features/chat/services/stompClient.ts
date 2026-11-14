"use client";

/**
 * Singleton STOMP client for the live chat (techhub-core `/ws-chat`).
 *
 * - Raw WebSocket (brokerURL), no SockJS needed.
 * - CONNECT sends `Authorization: Bearer <token>`; token is re-read (and
 *   silently refreshed when expired) on every reconnect attempt.
 * - One STOMP subscription per conversation topic, fanned out to N
 *   local subscribers (widget + seller dashboard can share it).
 * - Errors raised by backend @MessageExceptionHandler land on
 *   `/user/queue/errors`.
 */

import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { WS_CHAT_URL } from "@/lib/constants";
import { getAccessToken } from "@/lib/api/token";
import { refreshAuthTokens } from "@/lib/api/client";
import type { MessageResponse, SendChatMessageRequest } from "../types/chat";

export const CHAT_DESTINATIONS = {
  SEND: "/app/chat.send",
  conversationTopic: (conversationId: string) =>
    `/topic/conversation.${conversationId}`,
  ERRORS: "/user/queue/errors",
} as const;

export type ChatConnectionState = "idle" | "connecting" | "connected" | "closed";

type ConversationListener = (message: MessageResponse) => void;
type ErrorListener = (error: { message: string; errorCode?: string }) => void;
type StateListener = (state: ChatConnectionState) => void;

class StompChatService {
  private client: Client | null = null;
  private conversationListeners = new Map<string, Set<ConversationListener>>();
  private conversationSubs = new Map<string, StompSubscription>();
  private errorListeners = new Set<ErrorListener>();
  private stateListeners = new Set<StateListener>();
  private errorSub: StompSubscription | null = null;
  private state: ChatConnectionState = "idle";

  getState(): ChatConnectionState {
    return this.state;
  }

  onStateChange(listener: StateListener): () => void {
    this.stateListeners.add(listener);
    return () => this.stateListeners.delete(listener);
  }

  onConversationMessage(
    conversationId: string,
    listener: ConversationListener
  ): () => void {
    let listeners = this.conversationListeners.get(conversationId);
    if (!listeners) {
      listeners = new Set();
      this.conversationListeners.set(conversationId, listeners);
    }
    listeners.add(listener);

    // Ensure the broker subscription exists for late joiners.
    if (this.isConnected()) {
      this.ensureConversationSub(conversationId);
    }

    return () => {
      listeners.delete(listener);
      if (listeners.size === 0) {
        this.conversationListeners.delete(conversationId);
        this.teardownConversationSub(conversationId);
      }
    };
  }

  onError(listener: ErrorListener): () => void {
    this.errorListeners.add(listener);
    if (this.isConnected() && !this.errorSub) {
      this.errorSub = this.subscribeErrorQueue();
    }
    return () => {
      this.errorListeners.delete(listener);
      if (this.errorListeners.size === 0 && this.errorSub) {
        this.errorSub.unsubscribe();
        this.errorSub = null;
      }
    };
  }

  isConnected(): boolean {
    return this.client?.connected === true;
  }

  /** Connect (or no-op if already connected/connecting). Safe to call repeatedly. */
  connect(): void {
    if (typeof window === "undefined") return;
    if (this.client && (this.state === "connecting" || this.isConnected())) {
      return;
    }

    this.setState("connecting");
    this.client = new Client({
      brokerURL: WS_CHAT_URL,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      // Fresh token on every (re)connect — access token lives 15 min.
      beforeConnect: async () => {
        let token = getAccessToken();
        if (!token) {
          // Access cookie expired: try silent refresh once.
          try {
            token = (await refreshAuthTokens()) ?? getAccessToken();
          } catch {
            token = null;
          }
        }
        if (this.client) {
          this.client.connectHeaders = token
            ? { Authorization: `Bearer ${token}` }
            : {};
        }
      },

      onConnect: () => {
        this.setState("connected");
        // (Re)establish all subscriptions after connect / reconnect.
        for (const conversationId of this.conversationListeners.keys()) {
          this.ensureConversationSub(conversationId);
        }
        if (this.errorListeners.size > 0 && !this.errorSub) {
          this.errorSub = this.subscribeErrorQueue();
        }
      },

      onWebSocketClose: () => {
        if (this.state === "connected") {
          this.setState("connecting");
        }
      },

      onStompError: (frame) => {
        console.error("[chat] STOMP broker error:", frame.headers["message"]);
        this.setState("closed");
      },

      onWebSocketError: (event) => {
        console.error("[chat] WebSocket error:", event);
      },

      onDisconnect: () => {
        this.conversationSubs.clear();
        this.errorSub = null;
        this.setState("closed");
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.conversationSubs.clear();
    this.errorSub = null;
    this.setState("idle");
  }

  /** Publish a chat message via /app/chat.send. Returns false when offline. */
  sendMessage(req: SendChatMessageRequest): boolean {
    if (!this.isConnected()) return false;
    this.client!.publish({
      destination: CHAT_DESTINATIONS.SEND,
      body: JSON.stringify(req),
    });
    return true;
  }

  private ensureConversationSub(conversationId: string): void {
    if (this.conversationSubs.has(conversationId) || !this.isConnected()) return;
    const sub = this.client!.subscribe(
      CHAT_DESTINATIONS.conversationTopic(conversationId),
      (frame: IMessage) => {
        try {
          const message = JSON.parse(frame.body) as MessageResponse;
          this.conversationListeners.get(conversationId)?.forEach((fn) => fn(message));
        } catch (err) {
          console.error("[chat] Failed to parse message frame:", err);
        }
      }
    );
    this.conversationSubs.set(conversationId, sub);
  }

  private teardownConversationSub(conversationId: string): void {
    this.conversationSubs.get(conversationId)?.unsubscribe();
    this.conversationSubs.delete(conversationId);
  }

  private subscribeErrorQueue(): StompSubscription {
    return this.client!.subscribe(CHAT_DESTINATIONS.ERRORS, (frame: IMessage) => {
      try {
        const payload = JSON.parse(frame.body) as {
          message?: string;
          data?: unknown;
        };
        const errorCode =
          (payload.data as { errorCode?: string } | undefined)?.errorCode;
        this.errorListeners.forEach((fn) =>
          fn({ message: payload.message ?? "Chat error", errorCode })
        );
      } catch {
        this.errorListeners.forEach((fn) =>
          fn({ message: frame.body || "Chat error" })
        );
      }
    });
  }

  private setState(state: ChatConnectionState): void {
    this.state = state;
    this.stateListeners.forEach((fn) => fn(state));
  }
}

export const stompChat = new StompChatService();

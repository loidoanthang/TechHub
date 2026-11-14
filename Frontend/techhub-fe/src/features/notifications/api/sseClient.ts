import { API_BASE_URL, API_ENDPOINTS } from "@/lib/constants";
import { getAccessToken } from "@/lib/api/token";
import { refreshAuthTokens } from "@/lib/api/client";
import { normalizeNotification } from "../utils/notificationHelpers";
import type { NotificationResponse } from "../types";

export interface SSECallbacks {
  onNotification: (notification: NotificationResponse) => void;
  onConnect?: () => void;
  onError?: (error: unknown) => void;
}

export interface SSEConnection {
  disconnect: () => void;
}

const BACKOFF_DELAYS = [1000, 2000, 5000, 10000];
const SUSPEND_DELAY_MS = 15 * 60 * 1000; // 15 minutes

export function createNotificationStream(callbacks: SSECallbacks): SSEConnection {
  let isStopped = false;
  let isSuspendedByVisibility = false;
  let abortController: AbortController | null = null;
  let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  let visibilitySuspendTimeout: ReturnType<typeof setTimeout> | null = null;
  let retryCount = 0;

  const cleanupTimers = () => {
    if (reconnectTimeout) {
      clearTimeout(reconnectTimeout);
      reconnectTimeout = null;
    }
  };

  const scheduleReconnect = (immediate = false) => {
    cleanupTimers();
    if (isStopped || isSuspendedByVisibility) return;

    const delay = immediate
      ? 0
      : BACKOFF_DELAYS[Math.min(retryCount, BACKOFF_DELAYS.length - 1)];
    retryCount++;

    reconnectTimeout = setTimeout(() => {
      connect();
    }, delay);
  };

  const connect = async () => {
    cleanupTimers();
    if (isStopped || isSuspendedByVisibility) return;

    let token = getAccessToken();
    if (!token) {
      try {
        token = await refreshAuthTokens();
      } catch {
        return;
      }
      if (!token) {
        return;
      }
    }

    abortController = new AbortController();

    try {
      const streamUrl = `${API_BASE_URL}${API_ENDPOINTS.NOTIFICATIONS.STREAM}`;
      const response = await fetch(streamUrl, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "text/event-stream",
          "Cache-Control": "no-cache",
        },
        signal: abortController.signal,
      });

      if (!response.ok) {
        if (response.status === 401) {
          try {
            const newToken = await refreshAuthTokens();
            if (newToken) {
              scheduleReconnect(true);
              return;
            }
          } catch {
            return;
          }
        }
        throw new Error(`SSE stream failed with status ${response.status}`);
      }

      if (!response.body) {
        throw new Error("No response body available for SSE stream");
      }

      // Connection opened successfully, reset retry count
      retryCount = 0;
      callbacks.onConnect?.();

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";
      let currentEvent = "";
      let currentData = "";

      while (!isStopped && !isSuspendedByVisibility) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        // Keep the last partial line in buffer
        buffer = lines.pop() || "";

        for (const line of lines) {
          const trimmed = line.trim();

          // Empty line marks the end of an SSE message
          if (trimmed === "") {
            if (currentEvent === "NOTIFICATION" && currentData) {
              try {
                const raw = JSON.parse(currentData) as Record<string, unknown>;
                const parsed = normalizeNotification(raw);
                callbacks.onNotification(parsed);
              } catch (e) {
                console.warn("[SSE] Failed to parse notification data", e, currentData);
              }
            } else if (currentEvent === "CONNECT") {
              callbacks.onConnect?.();
            }

            currentEvent = "";
            currentData = "";
            continue;
          }

          // Heartbeat comment
          if (trimmed.startsWith(":")) {
            continue;
          }

          if (trimmed.startsWith("event:")) {
            currentEvent = trimmed.slice(6).trim();
          } else if (trimmed.startsWith("data:")) {
            const dataPart = trimmed.slice(5).trim();
            currentData = currentData ? `${currentData}\n${dataPart}` : dataPart;
          }
        }
      }

      // If stream ended normally and not explicitly stopped/suspended -> reconnect
      scheduleReconnect();
    } catch (err: unknown) {
      if (
        isStopped ||
        isSuspendedByVisibility ||
        (err instanceof DOMException && err.name === "AbortError")
      ) {
        return;
      }
      callbacks.onError?.(err);
      scheduleReconnect();
    }
  };

  // Visibility change handling: Suspend after 15 minutes hidden, reconnect immediately on return
  const handleVisibilityChange = () => {
    if (typeof document === "undefined" || isStopped) return;

    if (document.visibilityState === "hidden") {
      if (visibilitySuspendTimeout) {
        clearTimeout(visibilitySuspendTimeout);
      }
      visibilitySuspendTimeout = setTimeout(() => {
        if (document.visibilityState === "hidden" && !isStopped) {
          isSuspendedByVisibility = true;
          if (abortController) {
            abortController.abort();
            abortController = null;
          }
        }
      }, SUSPEND_DELAY_MS);
    } else if (document.visibilityState === "visible") {
      if (visibilitySuspendTimeout) {
        clearTimeout(visibilitySuspendTimeout);
        visibilitySuspendTimeout = null;
      }
      if (isSuspendedByVisibility) {
        isSuspendedByVisibility = false;
        connect();
      }
    }
  };

  if (typeof document !== "undefined") {
    document.addEventListener("visibilitychange", handleVisibilityChange);
  }

  // Start initial connection
  connect();

  return {
    disconnect: () => {
      isStopped = true;
      cleanupTimers();
      if (visibilitySuspendTimeout) {
        clearTimeout(visibilitySuspendTimeout);
        visibilitySuspendTimeout = null;
      }
      if (typeof document !== "undefined") {
        document.removeEventListener("visibilitychange", handleVisibilityChange);
      }
      if (abortController) {
        abortController.abort();
        abortController = null;
      }
    },
  };
}

// Global Singleton Subscriber Manager: ensures exactly ONE active SSE connection
let sharedConnection: SSEConnection | null = null;
const subscribers = new Set<SSECallbacks>();
let isSharedConnected = false;

export function getIsStreamConnected(): boolean {
  return isSharedConnected;
}

export function subscribeNotificationStream(callbacks: SSECallbacks): () => void {
  subscribers.add(callbacks);
  if (isSharedConnected) {
    callbacks.onConnect?.();
  }

  if (!sharedConnection) {
    sharedConnection = createNotificationStream({
      onNotification: (notif) => {
        subscribers.forEach((cb) => {
          try {
            cb.onNotification(notif);
          } catch (e) {
            console.error("[SSE Subscriber Error]", e);
          }
        });
      },
      onConnect: () => {
        isSharedConnected = true;
        subscribers.forEach((cb) => cb.onConnect?.());
      },
      onError: (err) => {
        isSharedConnected = false;
        subscribers.forEach((cb) => cb.onError?.(err));
      },
    });
  }

  return () => {
    subscribers.delete(callbacks);
    if (subscribers.size === 0 && sharedConnection) {
      sharedConnection.disconnect();
      sharedConnection = null;
      isSharedConnected = false;
    }
  };
}

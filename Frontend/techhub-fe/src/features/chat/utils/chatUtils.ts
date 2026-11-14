import type { MessageResponse } from "../types/chat";

/** Compare by server id, or by clientId for optimistic messages. */
export function isSameMessage(a: MessageResponse, b: MessageResponse): boolean {
  if (a.id && b.id && a.id === b.id) return true;
  if (a._clientId && b._clientId && a._clientId === b._clientId) return true;
  return false;
}

/**
 * Reconcile an incoming realtime message into a local message list.
 * Handles:
 *  1. Optimistic echo — replaces a pending `sending` message that matches
 *     the same sender + content (server doesn't echo back a client id).
 *  2. Duplicate frames (reconnect / re-subscribe replaying the same id).
 *  3. Normal append for genuinely new messages.
 * Input list order: oldest → newest (render order).
 */
export function reconcileIncomingMessage(
  list: MessageResponse[],
  incoming: MessageResponse
): MessageResponse[] {
  const duplicateIdx = list.findIndex((m) => isSameMessage(m, incoming));
  if (duplicateIdx !== -1) {
    const next = [...list];
    next[duplicateIdx] = { ...incoming, status: "sent" };
    return next;
  }

  if (incoming.isMine) {
    // Our own echo: replace the pending placeholder with same content.
    // (Any `sending` placeholder is by definition this user's own send.)
    const pendingIdx = list.findIndex(
      (m) => m.status === "sending" && m.content === incoming.content
    );
    if (pendingIdx !== -1) {
      const next = [...list];
      next[pendingIdx] = { ...incoming, status: "sent" };
      return next;
    }
  }

  return [...list, { ...incoming, status: "sent" }];
}

/** Mark every `sending` message older than timeoutMs as failed. */
export function failStalePendingMessages(
  list: MessageResponse[],
  timeoutMs = 10_000
): MessageResponse[] {
  let changed = false;
  const now = Date.now();
  const next = list.map((m) => {
    if (m.status !== "sending") return m;
    const sentAt = new Date(m.createdAt).getTime();
    if (!Number.isNaN(sentAt) && now - sentAt > timeoutMs) {
      changed = true;
      return { ...m, status: "failed" as const };
    }
    return m;
  });
  return changed ? next : list;
}

/** Stable sort oldest → newest by createdAt (string ISO compare works). */
export function sortMessagesAsc(list: MessageResponse[]): MessageResponse[] {
  return [...list].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

/**
 * Flatten infinite history pages (each page is DESC newest-first) into a
 * single oldest → newest list, deduped by message id / clientId.
 */
export function flattenHistoryPages(
  pages: Array<{ content: MessageResponse[] }> | undefined
): MessageResponse[] {
  if (!pages) return [];
  const seen = new Set<string>();
  const result: MessageResponse[] = [];
  for (const page of pages) {
    for (const msg of page.content) {
      const key = msg.id || msg._clientId || `${msg.content}-${msg.createdAt}`;
      if (seen.has(key)) continue;
      seen.add(key);
      result.push(msg);
    }
  }
  return sortMessagesAsc(result);
}

/**
 * The STOMP broadcast is a single shared topic frame with isMine hardcoded
 * to false (backend can't personalize per-recipient), so each client must
 * re-derive ownership from senderId vs its own user id.
 */
export function annotateIsMine<T extends MessageResponse>(
  message: T,
  currentUserId: string | null | undefined
): T {
  if (!currentUserId || !message.senderId) return message;
  return { ...message, isMine: message.senderId === currentUserId };
}

/** True when two timestamps belong to the same 5-minute visual cluster. */
export function isWithinFiveMinutes(
  a: string | null,
  b: string | null
): boolean {
  if (!a || !b) return false;
  const ta = new Date(a).getTime();
  const tb = new Date(b).getTime();
  if (Number.isNaN(ta) || Number.isNaN(tb)) return false;
  return Math.abs(tb - ta) < 5 * 60 * 1000;
}

let audioCtx: AudioContext | null = null;

/** Tiny two-tone chime via WebAudio (no audio asset needed). */
export function playChatChime(): void {
  if (typeof window === "undefined") return;
  try {
    const Ctx =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext?: typeof AudioContext })
        .webkitAudioContext;
    if (!Ctx) return;
    audioCtx = audioCtx ?? new Ctx();
    const ctx = audioCtx;
    if (ctx.state === "suspended") void ctx.resume();

    const now = ctx.currentTime;
    [880, 1320].forEach((freq, i) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = "sine";
      osc.frequency.value = freq;
      gain.gain.setValueAtTime(0.0001, now + i * 0.12);
      gain.gain.exponentialRampToValueAtTime(0.08, now + i * 0.12 + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + i * 0.12 + 0.18);
      osc.connect(gain).connect(ctx.destination);
      osc.start(now + i * 0.12);
      osc.stop(now + i * 0.12 + 0.2);
    });
  } catch {
    // Audio not available (autoplay policy) — silently ignore.
  }
}

const URL_REGEX = /(https?:\/\/[^\s]+)/g;

/** Render-safe splitting for auto-linkification. */
export function splitMessageByLink(content: string): Array<
  { type: "text"; value: string } | { type: "link"; value: string }
> {
  const parts: Array<
    { type: "text"; value: string } | { type: "link"; value: string }
  > = [];
  let lastIndex = 0;
  for (const match of content.matchAll(URL_REGEX)) {
    const idx = match.index ?? 0;
    if (idx > lastIndex) {
      parts.push({ type: "text", value: content.slice(lastIndex, idx) });
    }
    parts.push({ type: "link", value: match[0] });
    lastIndex = idx + match[0].length;
  }
  if (lastIndex < content.length) {
    parts.push({ type: "text", value: content.slice(lastIndex) });
  }
  return parts;
}

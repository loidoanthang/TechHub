"use client";

import { useEffect, useRef } from "react";
import { AlertCircle, Clock, RotateCcw } from "lucide-react";
import type { ConversationResponse, MessageResponse } from "../types/chat";
import { isWithinFiveMinutes, splitMessageByLink } from "../utils/chatUtils";
import { cn } from "@/lib/utils";

interface ChatMessageListProps {
  /** Oldest → newest */
  messages: MessageResponse[];
  opponentName: string;
  opponentAvatar?: string | null;
  conversation?: ConversationResponse | null;
  onRetry?: (message: MessageResponse) => void;
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  return d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
}

function formatDayLabel(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  const today = new Date();
  const isToday = d.toDateString() === today.toDateString();
  if (isToday) return "Hôm nay";
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  if (d.toDateString() === yesterday.toDateString()) return "Hôm qua";
  return d.toLocaleDateString("vi-VN");
}

/**
 * Scrollable bubble list. Buyer/other users on the left (#f1f5f9),
 * own messages on the right (#0045cc). Groups consecutive messages
 * within a 5-minute window; pending/failed states show clock/alert icons.
 */
export function ChatMessageList({
  messages,
  opponentName,
  opponentAvatar,
  onRetry,
}: ChatMessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const prevCountRef = useRef(0);

  useEffect(() => {
    if (messages.length !== prevCountRef.current) {
      prevCountRef.current = messages.length;
      bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
    }
  }, [messages]);

  return (
    <div
      ref={scrollRef}
      className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden px-3 py-3"
    >
      {messages.length === 0 ? (
        <div className="flex h-full min-h-[120px] items-center justify-center px-4 text-center">
          <p className="text-xs text-slate-400">
            Bắt đầu trò chuyện với {opponentName}...
          </p>
        </div>
      ) : null}

      {messages.map((message, index) => {
        const prev = messages[index - 1];
        const showDay =
          !prev ||
          new Date(prev.createdAt).toDateString() !==
            new Date(message.createdAt).toDateString();
        const clusterWithPrev =
          !showDay &&
          prev.isMine === message.isMine &&
          isWithinFiveMinutes(prev.createdAt, message.createdAt);

        return (
          <div key={message.id || message._clientId || index}>
            {showDay ? (
              <div className="my-3 flex justify-center">
                <span className="rounded-full bg-slate-100 px-3 py-0.5 text-[10px] font-semibold text-slate-500">
                  {formatDayLabel(message.createdAt)}
                </span>
              </div>
            ) : null}

            <div
              className={cn(
                "flex w-full gap-2",
                message.isMine ? "justify-end" : "justify-start",
                clusterWithPrev ? "mt-0.5" : "mt-2"
              )}
            >
              {!message.isMine && !clusterWithPrev ? (
                <div className="relative mt-1 h-6 w-6 shrink-0 overflow-hidden rounded-full bg-slate-200">
                  {opponentAvatar ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={opponentAvatar}
                      alt={opponentName}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <span className="flex h-full w-full items-center justify-center text-[10px] font-bold text-slate-500">
                      {opponentName.charAt(0).toUpperCase() || "?"}
                    </span>
                  )}
                </div>
              ) : null}

              <div
                className={cn(
                  "max-w-[80%] min-w-0 rounded-2xl px-3 py-2",
                  message.isMine
                    ? "rounded-br-md bg-[#0045cc] text-white"
                    : "rounded-bl-md bg-[#f1f5f9] text-slate-800",
                  message.status === "sending" && "opacity-60",
                  message.status === "failed" && "opacity-90"
                )}
              >
                <p className="break-words text-[13px] leading-snug">
                  {splitMessageByLink(message.content).map((part, i) =>
                    part.type === "link" ? (
                      <a
                        key={i}
                        href={part.value}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={cn(
                          "underline break-all",
                          message.isMine
                            ? "text-white underline-white/60"
                            : "text-[#0045cc]"
                        )}
                      >
                        {part.value}
                      </a>
                    ) : (
                      <span key={i}>{part.value}</span>
                    )
                  )}
                </p>

                <div
                  className={cn(
                    "mt-0.5 flex items-center gap-1",
                    message.isMine ? "justify-end" : "justify-start"
                  )}
                >
                  <span
                    className={cn(
                      "text-[10px]",
                      message.isMine ? "text-white/70" : "text-slate-400"
                    )}
                  >
                    {formatTime(message.createdAt)}
                  </span>
                  {message.status === "sending" ? (
                    <Clock
                      className={cn(
                        "h-3 w-3 animate-pulse",
                        message.isMine ? "text-white/70" : "text-slate-400"
                      )}
                      aria-label="Đang gửi"
                    />
                  ) : null}
                  {message.status === "failed" ? (
                    <button
                      type="button"
                      onClick={() => onRetry?.(message)}
                      className={cn(
                        "flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-[10px] font-semibold transition-colors",
                        message.isMine
                          ? "bg-white/20 text-white hover:bg-white/30"
                          : "bg-red-50 text-red-500 hover:bg-red-100"
                      )}
                    >
                      <AlertCircle className="h-3 w-3" />
                      Thất bại
                      <RotateCcw className="h-2.5 w-2.5" />
                      Thử lại
                    </button>
                  ) : null}
                </div>
              </div>
            </div>
          </div>
        );
      })}

      <div ref={bottomRef} />
    </div>
  );
}

"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { SendHorizontal } from "lucide-react";
import {
  chatMessageSchema,
  type ChatMessageFormValues,
} from "../schemas/chatSchema";

interface ChatInputBarProps {
  onSend: (content: string) => void;
  disabled?: boolean;
  placeholder?: string;
}

/**
 * Composer: Enter to send, Shift+Enter for a new line.
 * RHF + Zod per project rules (1–2000 chars, no blank sends).
 */
export function ChatInputBar({
  onSend,
  disabled,
  placeholder = "Nhập tin nhắn...",
}: ChatInputBarProps) {
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ChatMessageFormValues>({
    resolver: zodResolver(chatMessageSchema),
    defaultValues: { content: "" },
  });

  const content = watch("content");

  const submit = handleSubmit((values) => {
    onSend(values.content);
    reset({ content: "" });
  });

  const handleKeyDown = (
    event: React.KeyboardEvent<HTMLTextAreaElement>
  ) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void submit();
    }
  };

  return (
    <div className="w-full shrink-0 border-t border-slate-100 bg-white px-3 py-2.5">
      {errors.content ? (
        <p className="mb-1 text-[11px] text-red-500">{errors.content.message}</p>
      ) : null}
      <form onSubmit={submit} className="flex items-end gap-2">
        <textarea
          {...register("content")}
          rows={1}
          maxLength={2000}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          className="max-h-24 min-h-[40px] w-full min-w-0 flex-1 resize-none rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-[13px] text-slate-800 outline-none transition-colors placeholder:text-slate-400 focus:border-[#0045cc] focus:bg-white disabled:opacity-60"
        />
        <button
          type="submit"
          disabled={disabled || isSubmitting || !content?.trim()}
          aria-label="Gửi tin nhắn"
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#0045cc] text-white transition-colors hover:bg-[#0D5BFF] disabled:cursor-not-allowed disabled:opacity-40"
        >
          <SendHorizontal className="h-4 w-4" />
        </button>
      </form>
    </div>
  );
}

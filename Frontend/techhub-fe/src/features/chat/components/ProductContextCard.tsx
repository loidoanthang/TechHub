"use client";

import Image from "next/image";
import { Send, X } from "lucide-react";
import type { ChatProductContext } from "../types/chat";
import { formatPrice } from "@/lib/utils";

interface ProductContextCardProps {
  product: ChatProductContext | null;
  onRemove: () => void;
  onSend: (product: ChatProductContext) => void;
  disabled?: boolean;
  isSending?: boolean;
}

/**
 * Pinned product card above the composer: "[Ảnh + Tên SP + Giá]" with a
 * one-tap "Gửi thông tin sản phẩm" action.
 */
export function ProductContextCard({
  product,
  onRemove,
  onSend,
  disabled,
  isSending,
}: ProductContextCardProps) {
  if (!product) return null;

  return (
    <div className="flex items-center gap-2 border-b border-slate-100 bg-slate-50 px-3 py-2">
      <div className="relative h-10 w-10 shrink-0 overflow-hidden rounded-lg bg-slate-100">
        {product.imageUrl ? (
          <Image
            src={product.imageUrl}
            alt={product.title}
            fill
            sizes="40px"
            className="object-cover"
            unoptimized
          />
        ) : null}
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate text-xs font-semibold text-slate-800">
          {product.title}
        </p>
        <p className="text-xs font-bold text-[#0045cc]">
          {formatPrice(product.price)}
        </p>
      </div>

      <button
        type="button"
        onClick={() => onSend(product)}
        disabled={disabled || isSending}
        className="flex shrink-0 items-center gap-1 rounded-lg bg-[#0045cc] px-2.5 py-1.5 text-[11px] font-semibold text-white transition-colors hover:bg-[#0D5BFF] disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Send className="h-3 w-3" />
        <span className="hidden sm:inline">Gửi thông tin</span>
        <span className="sm:hidden">Gửi</span>
      </button>

      <button
        type="button"
        onClick={onRemove}
        aria-label="Bỏ ghim sản phẩm"
        className="shrink-0 rounded-full p-1 text-slate-400 transition-colors hover:bg-slate-200 hover:text-slate-600"
      >
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

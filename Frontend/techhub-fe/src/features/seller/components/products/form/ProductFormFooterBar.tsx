"use client";

import React from "react";
import Link from "next/link";
import { ROUTES } from "@/lib/constants";
import {
  Bookmark,
  Send,
  Loader2,
  Clock,
  CheckCircle2,
  Image as ImageIcon,
  Layers,
} from "lucide-react";

interface ProductFormFooterBarProps {
  imageCount: number;
  variantCount: number;
  isSubmitting: boolean;
  submittingAction: "DRAFT" | "SUBMIT" | null;
  onSaveDraft: () => void;
  onSubmitApproval: () => void;
}

export function ProductFormFooterBar({
  imageCount,
  variantCount,
  isSubmitting,
  submittingAction,
  onSaveDraft,
  onSubmitApproval,
}: ProductFormFooterBarProps) {
  return (
    <div className="fixed bottom-0 right-0 left-0 lg:left-56 z-40 bg-white/95 backdrop-blur-md px-6 py-3 border-t border-slate-200 shadow-xl flex items-center justify-between transition-all">
      {/* Left: Summary Indicators */}
      <div className="hidden sm:flex items-center gap-4 text-xs text-slate-500">
        <div className="flex items-center gap-1.5">
          <ImageIcon className="w-3.5 h-3.5 text-[#0045cc]" />
          <span>
            Ảnh:{" "}
            <strong className={imageCount >= 1 ? "text-emerald-600 font-bold" : "text-amber-600"}>
              {imageCount}/6
            </strong>
          </span>
        </div>

        <div className="w-px h-3.5 bg-slate-200" />

        <div className="flex items-center gap-1.5">
          <Layers className="w-3.5 h-3.5 text-[#0045cc]" />
          <span>
            Biến thể SKU:{" "}
            <strong className="text-slate-900 font-bold">{variantCount}</strong>
          </span>
        </div>
      </div>

      {/* Right: Action Buttons */}
      <div className="flex items-center gap-2.5 ml-auto">
        {/* Cancel Button */}
        <Link
          href={ROUTES.SELLER.PRODUCTS}
          className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors"
        >
          Hủy bỏ
        </Link>

        {/* Save as Draft Button */}
        <button
          type="button"
          disabled={isSubmitting}
          onClick={onSaveDraft}
          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold transition-all disabled:opacity-50"
        >
          {isSubmitting && submittingAction === "DRAFT" ? (
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
          ) : (
            <Bookmark className="w-3.5 h-3.5 text-slate-500" />
          )}
          <span>Lưu Bản Nháp</span>
        </button>

        {/* Submit for Approval Button with SLA Tooltip */}
        <div className="relative group/submit">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={onSubmitApproval}
            className="inline-flex items-center gap-1.5 px-5 py-2 rounded-xl bg-[#0045cc] hover:bg-[#0038a8] text-white text-xs font-semibold shadow-sm transition-all disabled:opacity-50 hover:shadow-md"
          >
            {isSubmitting && submittingAction === "SUBMIT" ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Send className="w-3.5 h-3.5" />
            )}
            <span>Đăng Bán & Gửi Duyệt</span>
          </button>

          {/* Hover SLA Tooltip */}
          <div className="pointer-events-none absolute bottom-full right-0 mb-2.5 hidden group-hover/submit:flex flex-col w-72 p-3 bg-slate-900 text-white rounded-xl shadow-2xl text-[11px] leading-snug border border-slate-700 animate-in fade-in duration-150">
            <div className="flex items-center gap-1.5 text-blue-400 font-bold mb-1">
              <Clock className="w-3.5 h-3.5" />
              <span>Thời gian xét duyệt dự kiến</span>
            </div>
            <p className="text-slate-300">
              Sản phẩm sẽ tự động chuyển sang trạng thái <strong>CHỜ KIỂM DUYỆT</strong>. Đội ngũ
              chuyên viên TechHub sẽ kiểm tra tiêu chuẩn linh kiện trong vòng <strong>2–4 giờ</strong>.
            </p>
            <div className="absolute right-6 -bottom-1 w-2.5 h-2.5 bg-slate-900 rotate-45 border-r border-b border-slate-700" />
          </div>
        </div>
      </div>
    </div>
  );
}

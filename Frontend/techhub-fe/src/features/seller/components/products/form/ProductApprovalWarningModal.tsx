"use client";

import React from "react";
import { AlertTriangle, Loader2, ArrowRight } from "lucide-react";

interface ProductApprovalWarningModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isLoading?: boolean;
}

export function ProductApprovalWarningModal({
  isOpen,
  onClose,
  onConfirm,
  isLoading = false,
}: ProductApprovalWarningModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full relative border border-slate-200 shadow-2xl flex flex-col items-center text-center animate-in zoom-in-95 duration-200">
        {/* Warning Icon Badge */}
        <div className="w-14 h-14 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mb-4 ring-8 ring-amber-50/50">
          <AlertTriangle className="w-7 h-7" />
        </div>

        {/* Title */}
        <h3 className="font-headline font-bold text-lg text-slate-900 tracking-tight mb-2">
          Xác nhận thay đổi thông tin quan trọng
        </h3>

        {/* Body Copy */}
        <p className="text-xs text-slate-600 leading-relaxed mb-6">
          Bạn đang chỉnh sửa thông tin cốt lõi (Tên sản phẩm, Ảnh hoặc Thông số kỹ thuật) của sản phẩm đã được phê duyệt.
          Theo quy định sàn, sản phẩm sẽ tạm thời chuyển về trạng thái <strong>Chờ duyệt</strong> và tạm ẩn khỏi marketplace để Ban quản trị thẩm định lại (khoảng 2–4 giờ).
        </p>

        {/* Action Buttons */}
        <div className="flex items-center gap-3 w-full">
          <button
            type="button"
            disabled={isLoading}
            onClick={onClose}
            className="flex-1 py-2.5 px-4 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-xs font-semibold text-slate-700 transition-colors disabled:opacity-50"
          >
            Hủy bỏ
          </button>

          <button
            type="button"
            disabled={isLoading}
            onClick={onConfirm}
            className="flex-1 py-2.5 px-4 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold transition-all shadow-sm hover:shadow-md flex items-center justify-center gap-1.5 disabled:opacity-50"
          >
            {isLoading ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <>
                <span>Lưu & Gửi Duyệt</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

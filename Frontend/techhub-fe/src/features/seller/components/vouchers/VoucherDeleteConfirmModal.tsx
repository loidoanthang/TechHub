"use client";

import React from "react";
import { AlertTriangle, Loader2, Trash2 } from "lucide-react";

interface VoucherDeleteConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  voucherCode?: string;
  voucherName?: string;
  isLoading?: boolean;
}

export function VoucherDeleteConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  voucherCode,
  voucherName,
  isLoading = false,
}: VoucherDeleteConfirmModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full relative border border-slate-200 shadow-2xl flex flex-col items-center text-center animate-in zoom-in-95 duration-200">
        {/* Warning Icon Badge */}
        <div className="w-14 h-14 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mb-4 ring-8 ring-rose-50/50">
          <Trash2 className="w-7 h-7" />
        </div>

        {/* Title */}
        <h3 className="font-headline font-bold text-lg text-slate-900 tracking-tight mb-2">
          Xác nhận xóa mã giảm giá?
        </h3>

        {/* Body Copy */}
        <p className="text-xs text-slate-600 leading-relaxed mb-4">
          Bạn có chắc chắn muốn xóa mã giảm giá{" "}
          <span className="font-mono font-bold text-slate-900 bg-slate-100 px-1.5 py-0.5 rounded border border-slate-200">
            {voucherCode}
          </span>
          {voucherName ? ` (${voucherName})` : ""}? Thao tác này không thể hoàn tác và khách hàng sẽ không thể áp dụng mã này nữa.
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
            className="flex-1 py-2.5 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition-all shadow-sm hover:shadow-md flex items-center justify-center gap-1.5 disabled:opacity-50"
          >
            {isLoading ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <>
                <Trash2 className="w-3.5 h-3.5" />
                <span>Xóa Voucher</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

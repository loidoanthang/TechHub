"use client";

import React from "react";
import { AlertTriangle, Send, Trash2, X, Loader2 } from "lucide-react";

interface SubmitApprovalModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  productName: string;
  isLoading: boolean;
}

export function SubmitApprovalModal({
  isOpen,
  onClose,
  onConfirm,
  productName,
  isLoading,
}: SubmitApprovalModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-100 overflow-hidden">
        <div className="p-5 flex items-start gap-4">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-[#0045cc] flex items-center justify-center shrink-0">
            <Send className="w-5 h-5" />
          </div>
          <div className="flex-1">
            <h3 className="font-headline font-bold text-base text-slate-900">
              Gửi Duyệt Sản Phẩm
            </h3>
            <p className="text-xs text-slate-600 mt-1">
              Bạn có muốn gửi sản phẩm <strong className="text-slate-900 font-semibold">{productName}</strong> để Ban Quản Trị TechHub kiểm duyệt?
            </p>
            <div className="mt-3 p-3 bg-blue-50/60 rounded-xl border border-blue-100/80 text-[11px] text-blue-800">
              ⚡ Thời gian xét duyệt tiêu chuẩn từ 2 - 24 giờ làm việc. Sau khi được duyệt, sản phẩm sẽ tự động sẵn sàng để mở bán.
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-2.5">
          <button
            type="button"
            disabled={isLoading}
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200 transition-colors"
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            disabled={isLoading}
            onClick={onConfirm}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold bg-[#0045cc] text-white hover:bg-[#0038a8] disabled:opacity-50 transition-all shadow-sm"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Đang gửi...
              </>
            ) : (
              <>
                <Send className="w-3.5 h-3.5" />
                Xác nhận Gửi Duyệt
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

interface DeleteProductModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  productName: string;
  isLoading: boolean;
}

export function DeleteProductModal({
  isOpen,
  onClose,
  onConfirm,
  productName,
  isLoading,
}: DeleteProductModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-100 overflow-hidden">
        <div className="p-5 flex items-start gap-4">
          <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center shrink-0">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div className="flex-1">
            <h3 className="font-headline font-bold text-base text-slate-900">
              Xóa Sản Phẩm
            </h3>
            <p className="text-xs text-slate-600 mt-1">
              Bạn có chắc chắn muốn xóa sản phẩm <strong className="text-slate-900 font-semibold">{productName}</strong>?
            </p>
            <div className="mt-3 p-3 bg-rose-50/60 rounded-xl border border-rose-100/80 text-[11px] text-rose-700">
              ⚠️ Sản phẩm sẽ được đưa vào mục đã xóa và gỡ bỏ khỏi sàn TechHub. Bạn có thể liên hệ quản trị viên nếu cần khôi phục lại.
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-2.5">
          <button
            type="button"
            disabled={isLoading}
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200 transition-colors"
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            disabled={isLoading}
            onClick={onConfirm}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold bg-rose-600 text-white hover:bg-rose-700 disabled:opacity-50 transition-all shadow-sm"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Đang xóa...
              </>
            ) : (
              <>
                <Trash2 className="w-3.5 h-3.5" />
                Xác nhận Xóa
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

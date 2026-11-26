"use client";

import React, { useState } from "react";
import { AlertTriangle, Loader2, ShieldAlert, X } from "lucide-react";
import type { AdminReviewResponse } from "@/types/review";
import { useDeleteReviewMutation } from "../../hooks/useAdminModeration";
import { SelectDropdown } from "@/components/shared/SelectDropdown";

interface ReviewDeleteConfirmModalProps {
  isOpen: boolean;
  review: AdminReviewResponse | null;
  onClose: () => void;
  onSuccess?: () => void;
}

const VIOLATION_REASONS = [
  { value: "SPAM", label: "Spam / Đánh giá ảo / Nội dung vô nghĩa" },
  { value: "TOXIC_LANGUAGE", label: "Ngôn từ thô tục, xúc phạm, kích động bạo lực" },
  { value: "MISINFORMATION", label: "Thông tin sai lệch sự thật, vu khống gian hàng" },
  { value: "EXTERNAL_ADS", label: "Quảng cáo, chứa link độc hại / cờ bạc lừa đảo" },
  { value: "OTHER", label: "Vi phạm tiêu chuẩn cộng đồng khác" },
];

export function ReviewDeleteConfirmModal({
  isOpen,
  review,
  onClose,
  onSuccess,
}: ReviewDeleteConfirmModalProps) {
  const [selectedReason, setSelectedReason] = useState(VIOLATION_REASONS[0].value);
  const [adminNote, setAdminNote] = useState("");

  const deleteMutation = useDeleteReviewMutation();

  if (!isOpen || !review) return null;

  const handleConfirm = async () => {
    deleteMutation.mutate(
      {
        reviewId: review.id,
        productId: review.productId,
      },
      {
        onSuccess: () => {
          onClose();
          onSuccess?.();
        },
      }
    );
  };

  const isPending = deleteMutation.isPending;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className="w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[90vh]"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-rose-50/50">
          <div className="flex items-center gap-2.5 text-rose-600">
            <div className="p-2 bg-rose-100 rounded-xl text-rose-600">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <h3 id="modal-title" className="text-base font-bold text-slate-900">
                Xác nhận gỡ bỏ đánh giá vi phạm
              </h3>
              <p className="text-xs text-slate-500">
                Hành động sẽ xóa vĩnh viễn đánh giá khỏi hệ thống
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isPending}
            className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 space-y-4 overflow-y-auto">
          {/* Warning box */}
          <div className="flex items-start gap-3 p-3.5 bg-amber-50 border border-amber-200/70 rounded-xl text-xs text-amber-900 leading-relaxed">
            <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-amber-950">Lưu ý quan trọng:</p>
              <p>
                Sau khi gỡ bỏ, điểm đánh giá trung bình của sản phẩm{" "}
                <span className="font-medium text-amber-950">
                  &ldquo;{review.productName}&rdquo;
                </span>{" "}
                sẽ được tự động tính toán lại.
              </p>
            </div>
          </div>

          {/* Review Info Card */}
          <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/80 space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-500">
              <span className="font-semibold text-slate-700">
                Người mua: {review.reviewerName}
              </span>
              <span className="text-amber-500 font-bold">
                {"★".repeat(review.rating)}
                {"☆".repeat(5 - review.rating)}
              </span>
            </div>
            <p className="text-sm text-slate-800 italic bg-white p-2.5 rounded-lg border border-slate-200 line-clamp-3">
              &ldquo;{review.comment || "Không có bình luận chữ"}&rdquo;
            </p>
            <p className="text-xs text-slate-500 truncate">
              Sản phẩm: <span className="font-medium text-slate-700">{review.productName}</span>
            </p>
          </div>

          {/* Reason Selection */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Lý do gỡ bỏ đánh giá <span className="text-rose-500">*</span>
            </label>
            <SelectDropdown
              value={selectedReason}
              onChange={setSelectedReason}
              disabled={isPending}
              ariaLabel="Lý do gỡ bỏ đánh giá"
              buttonClassName="h-10 px-3.5 bg-white border border-slate-300 rounded-xl text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500"
              options={VIOLATION_REASONS}
            />
          </div>

          {/* Admin Note */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Ghi chú kiểm duyệt nội bộ (Tùy chọn)
            </label>
            <textarea
              value={adminNote}
              onChange={(e) => setAdminNote(e.target.value)}
              disabled={isPending}
              placeholder="Nhập ghi chú hoặc biên bản kiểm duyệt nếu cần..."
              rows={2}
              className="w-full text-sm px-3.5 py-2 bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 resize-none"
            />
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={isPending}
            className="px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-200/70 rounded-xl transition-colors disabled:opacity-50"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={isPending}
            className="inline-flex items-center gap-2 px-5 py-2 text-sm font-semibold text-white bg-rose-600 hover:bg-rose-700 rounded-xl shadow-sm shadow-rose-600/30 transition-all disabled:opacity-50"
          >
            {isPending ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Đang xử lý...</span>
              </>
            ) : (
              <span>Xác nhận gỡ bỏ</span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

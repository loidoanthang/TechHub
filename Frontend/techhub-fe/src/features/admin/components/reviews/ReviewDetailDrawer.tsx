"use client";

import React, { useState } from "react";
import Image from "next/image";
import {
  X,
  Star,
  ShieldAlert,
  Store,
  ShoppingBag,
  User,
  Clock,
  MessageSquare,
  Trash2,
  Maximize2,
  ExternalLink,
} from "lucide-react";
import type { AdminReviewResponse } from "@/types/review";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import { formatDate } from "@/lib/utils";

interface ReviewDetailDrawerProps {
  review: AdminReviewResponse | null;
  isOpen: boolean;
  onClose: () => void;
  onDeleteClick: (review: AdminReviewResponse) => void;
}

export function ReviewDetailDrawer({
  review,
  isOpen,
  onClose,
  onDeleteClick,
}: ReviewDetailDrawerProps) {
  useAdminDialogA11y(isOpen, onClose);
  const [activeMediaUrl, setActiveMediaUrl] = useState<string | null>(null);

  if (!isOpen || !review) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="review-detail-drawer-title"
      className="fixed inset-0 z-50 flex justify-end animate-in fade-in duration-200"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-950/50 backdrop-blur-xs transition-opacity"
        onClick={onClose}
      />

      {/* Drawer Panel */}
      <div className="relative z-10 w-full max-w-xl bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-200">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/80 shrink-0">
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h2
                id="review-detail-drawer-title"
                className="text-base font-bold text-slate-900 truncate"
              >
                Hồ sơ Kiểm duyệt Đánh giá
              </h2>
              {review.isReported && (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-rose-100 text-rose-700 shrink-0">
                  Bị báo cáo ({review.reportCount || 1})
                </span>
              )}
            </div>
            <p className="text-xs text-slate-500 font-mono truncate mt-0.5">
              Review ID: {review.id}
            </p>
          </div>
          <button
            type="button"
            aria-label="Đóng chi tiết đánh giá"
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-200/70 rounded-xl transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Cảnh báo nếu bị báo cáo vi phạm */}
          {review.isReported && (
            <div className="p-4 bg-rose-50 border border-rose-200/80 rounded-2xl space-y-1.5">
              <div className="flex items-center gap-2 text-rose-700 font-semibold text-xs uppercase tracking-wider">
                <ShieldAlert className="w-4 h-4 shrink-0" />
                Cờ báo cáo vi phạm chính sách
              </div>
              <p className="text-xs text-rose-900 font-medium leading-relaxed">
                {review.reportReason || "Đánh giá bị người dùng hoặc gian hàng báo cáo vi phạm tiêu chuẩn cộng đồng."}
              </p>
              <p className="text-[11px] text-rose-600">
                Tổng số lượt báo cáo từ các bên liên quan:{" "}
                <span className="font-bold">{review.reportCount || 1} lượt</span>
              </p>
            </div>
          )}

          {/* Sản phẩm & Gian hàng */}
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl space-y-3">
            <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Sản phẩm & Gian hàng liên quan
            </h3>
            <div className="flex items-start gap-3.5">
              {review.imageUrl ? (
                <div className="relative w-16 h-16 rounded-xl overflow-hidden bg-white border border-slate-200 shrink-0">
                  <Image
                    src={review.imageUrl}
                    alt={review.productName}
                    fill
                    className="object-cover"
                    sizes="64px"
                  />
                </div>
              ) : (
                <div className="w-16 h-16 rounded-xl bg-slate-200 flex items-center justify-center shrink-0 text-slate-400">
                  <ShoppingBag className="w-6 h-6" />
                </div>
              )}
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-slate-900 line-clamp-2">
                  {review.productName}
                </p>
                {review.variantName && (
                  <p className="text-xs text-slate-500 mt-0.5">
                    Phân loại: <span className="font-medium text-slate-700">{review.variantName}</span>
                  </p>
                )}
                {review.shopName && (
                  <div className="flex items-center gap-1.5 text-xs text-primary font-medium mt-1">
                    <Store className="w-3.5 h-3.5 shrink-0" />
                    <span className="truncate">{review.shopName}</span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Người mua & Đơn hàng */}
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl space-y-3">
            <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Khách hàng & Thông tin đơn hàng
            </h3>
            <div className="flex items-center justify-between flex-wrap gap-2">
              <div className="flex items-center gap-3">
                {review.reviewerAvatar ? (
                  <div className="relative w-10 h-10 rounded-full overflow-hidden border border-slate-200 shrink-0">
                    <Image
                      src={review.reviewerAvatar}
                      alt={review.reviewerName}
                      fill
                      className="object-cover"
                      sizes="40px"
                    />
                  </div>
                ) : (
                  <div className="w-10 h-10 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-sm shrink-0">
                    {review.reviewerName.charAt(0).toUpperCase()}
                  </div>
                )}
                <div>
                  <p className="text-sm font-bold text-slate-900">{review.reviewerName}</p>
                  <p className="text-xs text-slate-500">Người mua đã xác thực</p>
                </div>
              </div>

              {review.orderNumber && (
                <div className="text-right">
                  <span className="text-[11px] text-slate-400 uppercase font-mono">Mã đơn hàng</span>
                  <p className="text-xs font-semibold text-slate-800 font-mono">
                    {review.orderNumber}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Nội dung đánh giá */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Nội dung đánh giá của khách hàng
            </h3>

            <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs space-y-3">
              {/* Rating stars & Time */}
              <div className="flex items-center justify-between flex-wrap gap-2">
                <div className="flex items-center gap-1">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      className={`w-4 h-4 ${
                        star <= review.rating
                          ? "fill-amber-400 text-amber-400"
                          : "fill-slate-100 text-slate-200"
                      }`}
                    />
                  ))}
                  <span className="text-xs font-bold text-slate-800 ml-1.5">
                    {review.rating}/5 sao
                  </span>
                </div>
                <div className="flex items-center gap-1 text-xs text-slate-500">
                  <Clock className="w-3.5 h-3.5" />
                  <span>{formatDate(review.createdAt)}</span>
                </div>
              </div>

              {/* Comment text */}
              <p className="text-sm text-slate-800 leading-relaxed whitespace-pre-line">
                {review.comment || <span className="text-slate-400 italic">Không có nội dung bình luận</span>}
              </p>

              {/* Media attachments */}
              {review.mediaList && review.mediaList.length > 0 && (
                <div className="pt-2 border-t border-slate-100 space-y-2">
                  <p className="text-xs font-semibold text-slate-600">
                    Ảnh đính kèm ({review.mediaList.length}):
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {review.mediaList.map((media) => (
                      <div
                        key={media.id}
                        onClick={() => setActiveMediaUrl(media.mediaUrl)}
                        className="relative w-20 h-20 rounded-xl overflow-hidden border border-slate-200 cursor-pointer group hover:ring-2 hover:ring-primary transition-all shrink-0 bg-slate-50"
                      >
                        <Image
                          src={media.mediaUrl}
                          alt="Đính kèm"
                          fill
                          className="object-cover group-hover:scale-105 transition-transform"
                          sizes="80px"
                        />
                        <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity text-white">
                          <Maximize2 className="w-4 h-4" />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Phản hồi từ Shop (nếu có) */}
          {review.sellerResponse && (
            <div className="p-4 bg-sky-50/70 border border-sky-200/80 rounded-2xl space-y-2">
              <div className="flex items-center justify-between text-xs text-sky-800">
                <div className="flex items-center gap-1.5 font-bold">
                  <MessageSquare className="w-3.5 h-3.5" />
                  <span>Phản hồi từ {review.shopName || "Gian hàng"}:</span>
                </div>
                {review.sellerResponseAt && (
                  <span className="text-[11px] text-sky-600">
                    {formatDate(review.sellerResponseAt)}
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-800 leading-relaxed italic bg-white/80 p-3 rounded-xl border border-sky-100">
                &ldquo;{review.sellerResponse}&rdquo;
              </p>
            </div>
          )}
        </div>

        {/* Footer actions */}
        <div className="p-5 border-t border-slate-200 bg-slate-50 flex items-center justify-between gap-3 shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-200/70 rounded-xl transition-colors"
          >
            Đóng
          </button>
          <button
            type="button"
            onClick={() => onDeleteClick(review)}
            className="inline-flex items-center gap-2 px-5 py-2.5 text-sm font-semibold text-white bg-rose-600 hover:bg-rose-700 rounded-xl shadow-sm shadow-rose-600/30 transition-all cursor-pointer"
          >
            <Trash2 className="w-4 h-4" />
            <span>Gỡ bỏ đánh giá vi phạm</span>
          </button>
        </div>
      </div>

      {/* Lightbox Preview Modal */}
      {activeMediaUrl && (
        <div
          className="fixed inset-0 z-60 bg-black/80 flex items-center justify-center p-4 animate-in fade-in"
          onClick={() => setActiveMediaUrl(null)}
        >
          <div className="relative max-w-2xl max-h-[85vh] w-full h-[70vh] flex items-center justify-center">
            <Image
              src={activeMediaUrl}
              alt="Media phóng to"
              fill
              className="object-contain"
              sizes="100vw"
            />
          </div>
          <button
            type="button"
            onClick={() => setActiveMediaUrl(null)}
            className="absolute top-6 right-6 p-2 bg-white/10 hover:bg-white/20 text-white rounded-full transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
        </div>
      )}
    </div>
  );
}
